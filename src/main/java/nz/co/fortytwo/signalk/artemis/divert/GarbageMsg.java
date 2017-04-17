package nz.co.fortytwo.signalk.artemis.divert;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONFIG;
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
import static nz.co.fortytwo.signalk.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.message.impl.MessageInternal;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.cluster.Transformer;
import org.apache.commons.lang3.StringUtils;
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
 * Drops garbage messages
 * 
 * @author robert
 * 
 */

public class GarbageMsg implements Transformer {
	// private ClientSession session;
	// private ClientProducer producer;

	private static Logger logger = LogManager.getLogger(GarbageMsg.class);

	/**
	 * Reads msg and drops it if it is not a recognised type
	 * 
	 * @param node
	 * @return
	 */
	@Override
	public ServerMessage transform(ServerMessage message) {

		// if(logger.isDebugEnabled())logger.debug("SessionSendMessage: " +
		// message.getBodyBuffer().toString());

		String msgType = getContentType(message);
		if (logger.isDebugEnabled())
			logger.debug("Msg type is:" + msgType);

		if (msgType != null) {
			message.putStringProperty(Config.AMQ_CONTENT_TYPE, msgType);
		} else {
			// message.getBodyBuffer().writeString("{}");
		}
		return message;
	}

	protected String getContentType(ServerMessage message) {
		if (message.getBodyBuffer().readableBytes() > 0) {
			String msg = message.getBodyBuffer().readString();
			if (logger.isDebugEnabled())
				logger.debug("Msg body is:" + msg);

			if (msg != null) {
				msg = msg.trim();
				// stomp messages are prefixed with 'ascii:'
				if (msg.startsWith("ascii:")) {
					return "STOMP";
				}
				msg = StringUtils.chomp(msg);
				if (msg.startsWith("!AIVDM")) {
					// AIS
					// !AIVDM,1,1,,B,15MwkRUOidG?GElEa<iQk1JV06Jd,0*6D
					return "AIS";
				} else if (msg.startsWith("$")) {
					return "0183";
				} else if (msg.startsWith("{") && msg.endsWith("}")) {
					Json node = Json.read(msg);
					// avoid full signalk syntax
					if (node.has(vessels) || node.has(CONFIG)|| node.has(sources) || node.has(resources))
						return Config.JSON_FULL;
					if (node.has(CONTEXT) && (node.has(SUBSCRIBE)))
						return Config.JSON_SUBSCRIBE;
					if (node.has(CONTEXT))
						return Config.JSON_DELTA;
				}
			}
		}
		return null;
	}
}
