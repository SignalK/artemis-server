package nz.co.fortytwo.signalk.artemis.divert;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.util.SignalKConstants.FORMAT;
import static nz.co.fortytwo.signalk.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.util.SignalKConstants.MIN_PERIOD;
import static nz.co.fortytwo.signalk.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.util.SignalKConstants.PERIOD;
import static nz.co.fortytwo.signalk.util.SignalKConstants.POLICY;
import static nz.co.fortytwo.signalk.util.SignalKConstants.POLICY_FIXED;
import static nz.co.fortytwo.signalk.util.SignalKConstants.SUBSCRIBE;
import static nz.co.fortytwo.signalk.util.SignalKConstants.UNSUBSCRIBE;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.cluster.Transformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.server.ArtemisServer;
import nz.co.fortytwo.signalk.artemis.server.Subscription;
import nz.co.fortytwo.signalk.artemis.server.SubscriptionManager;
import nz.co.fortytwo.signalk.artemis.server.SubscriptionManagerFactory;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.util.ConfigConstants;
import nz.co.fortytwo.signalk.util.Util;

/*
*
* Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
* Web: www.42.co.nz
* Email: robert@42.co.nz
* Author: R T Huitema
*
* This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
* WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/

/**
 * handles a SignalK subscribe or unsubscribe message
 * 
 * @author robert
 * 
 */

public class SubscribeMsg implements Transformer {
	// private ClientSession session;
	// private ClientProducer producer;

	private static Logger logger = LogManager.getLogger(SubscribeMsg.class);
	/**
	 * Reads Subscribe format JSON and creates a subscription. Does nothing if
	 * json is not a subscribe, and returns the original message
	 * 
	 * @param node
	 * @return
	 */
	@Override
	public ServerMessage transform(ServerMessage message) {
		if(!Config.JSON_SUBSCRIBE.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))return message;
		if(logger.isTraceEnabled())logger.trace("Processing: " + message);
		Json node = Json.read(message.getBodyBuffer().readString());
		// avoid full signalk syntax
		if (node.has(vessels))
			return message;
		// deal with diff format
		if (node.has(CONTEXT) && (node.has(SUBSCRIBE))) {
			if(logger.isDebugEnabled())logger.debug("Processing SUBSCRIBE: " + message);
			String ctx = node.at(CONTEXT).asString();
			ctx = Util.fixSelfKey(ctx);
			Json subscribe = node.at(SUBSCRIBE);
			if (subscribe == null)
				return message;

			try {
				parseSubscribe(node, subscribe, ctx, message);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}

			 if(logger.isDebugEnabled())logger.debug("SubscribeMsg processed subscribe "+node );
			return message;
		}
		
		if (node.has(CONTEXT) && (node.has(UNSUBSCRIBE))) {
			if(logger.isDebugEnabled())logger.debug("Processing UNSUBSCRIBE: " + message);
			String ctx = node.at(CONTEXT).asString();
			ctx = Util.fixSelfKey(ctx);
			Json subscribe = node.at(UNSUBSCRIBE);
			if (subscribe == null)
				return message;

			//try {
			//	parseUnSubscribe(node, subscribe, ctx, message);
			//} catch (Exception e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}

			 if(logger.isDebugEnabled())logger.debug("SubscribeMsg processed unsubscribe "+node );
			return message;
		}
		return message;

	}

	protected void parseSubscribe(Json node, Json subscriptions, String ctx, ServerMessage m1) throws Exception {

		if(subscriptions!=null){
			//MQTT and STOMP wont have created proper session links
	
		
			String sessionId = m1.getStringProperty(Config.AMQ_SESSION_ID);
			String destination = m1.getStringProperty(Config.AMQ_REPLY_Q);
			ServerSession s = ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
			
			if(node.has(ConfigConstants.OUTPUT_TYPE)){
				String outputType = node.at(ConfigConstants.OUTPUT_TYPE).asString();
				SubscriptionManagerFactory.getInstance().add(sessionId, sessionId, outputType,"127.0.0.1","127.0.0.1");
			}
			
			if(subscriptions.isArray()){
				for(Json subscription: subscriptions.asJsonList()){
					parseSubscribe(sessionId, destination, s.getUsername(), s.getPassword(), ctx, subscription);
				}
			}
			if(logger.isDebugEnabled())logger.debug("processed subscribe  "+node );
		}
	}

	/**
	 *  
	 *   <pre>{
                    "path": "navigation.speedThroughWater",
                    "period": 1000,
                    "format": "delta",
                    "policy": "ideal",
                    "minPeriod": 200
                }
                </pre>
	 * @param context
	 * @param subscription
	 * @throws Exception 
	 */
	private void parseSubscribe(String sessionId, String destination, String user, String password, String context, Json subscription) throws Exception {
		//get values
		if(logger.isDebugEnabled())logger.debug("Parsing subscribe for : "+user+" : "+password+" : " +subscription );
		
		String path = context+"."+subscription.at(PATH).asString();
		long period = 1000;
		if(subscription.at(PERIOD)!=null)period = subscription.at(PERIOD).asInteger();
		String format = FORMAT_DELTA;
		if(subscription.at(FORMAT)!=null)format=subscription.at(FORMAT).asString();
		String policy = POLICY_FIXED;
		if(subscription.at(POLICY)!=null)policy=subscription.at(POLICY).asString();
		long minPeriod = 0;
		if(subscription.at(MIN_PERIOD)!=null)minPeriod=subscription.at(MIN_PERIOD).asInteger();
	
		Subscription sub = new Subscription(sessionId, destination, user, password,  path, period, minPeriod, format, policy);
		
		
		//STOMP, MQTT
		//if(headers.containsKey(ConfigConstants.DESTINATION)){
		//	sub.setDestination( headers.get(ConfigConstants.DESTINATION).toString());
		//}
	
		if(logger.isDebugEnabled())logger.debug("Created subscription; "+sub.toString() );
		SubscriptionManagerFactory.getInstance().addSubscription(sub);
		
	}
	

}
