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
package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.label;
import static nz.co.fortytwo.signalk.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.util.SignalKConstants.type;
import static nz.co.fortytwo.signalk.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.server.ArtemisServer;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.util.JsonSerializer;


/**
 * Processes full format into individual messages.
 * 
 * @author robert
 * 
 */
public class FullMsgInterceptor extends BaseInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(FullMsgInterceptor.class);

	private JsonSerializer ser = new JsonSerializer();

	public FullMsgInterceptor() {
		super();
	}

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if(packet.isResponse())return true;
		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage message = realPacket.getMessage();
			if(!Config.JSON_FULL.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))return true;
			//if(logger.isDebugEnabled())logger.debug("Processing: " + message);
			Json node = Util.readBodyBuffer(message);
			// avoid delta signalk syntax
			if (node.has(CONTEXT))
				return true;
			
			String sessionId = message.getStringProperty(Config.AMQ_SESSION_ID);
			ServerSession sess = ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
			// deal with full format
			if (node.has(vessels) || node.has(CONFIG) || node.has(resources) || node.has(sources)) {
				if (logger.isDebugEnabled())
					logger.debug("processing full  " + node);
				// process it by recursion
				//if its a config, trigger a save
				if(node.has(CONFIG))Config.startConfigListener();
				try {
					recurseJson(node, null, sess, message.getStringProperty(Config.MSG_SRC_BUS));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			}
		
		}
		return true;
	}

	private void recurseJson(Json node, String key, ServerSession sess, String srcBus ) throws Exception {
		//check if it has a .value
		// value type, or attribute type
		if (logger.isDebugEnabled())
			logger.debug("processing sub: "+ key+": " + node);
		if (node==null || node.isNull()) {
			sendMsg(key, Json.nil(), null, (String)null, sess);
			return;
		}
		if(node.isPrimitive()){
			//attribute type
			sendMsg(key, node,null, (String)null, sess);
			return;
		}
		if(node.isArray()){
			//attribute type
			sendMsg(key, node,null, (String)null, sess);
			return;
		}
		//if(node.has(values)){
			//just send it
		//	sendMsg(key+dot+values, node.at(values), null, (String)null, sess);
		//	return;
		//}
		String srcRef = null;
		if(node.has(sourceRef)){
			srcRef= node.at(sourceRef).asString();
		}else{
			Json src = node.at(source);
			if(src!=null){
				if(!src.has(type))
					src.set(type, srcBus);
				srcRef = src.at(type).toString()+dot+src.at(label).toString();
				sendSourceMsg(srcRef, (Json)src,node.at(timestamp).asString(), sess);
			}
		}
		if(node.has(value)){
			sendMsg(key, node.at(value), node.at(timestamp).asString(), srcRef, sess);
			return;
		}
		//either composite or path to recurse
		if(node.has(timestamp)||node.has(source)){
			//composite
			sendMsg(key, node, node.at(timestamp).asString(), srcRef, sess);
		}else{
			//recurse
			for(String k: node.asJsonMap().keySet()){
				if(key==null){
					recurseJson(node.at(k), k, sess, srcBus);
				}else{
					recurseJson(node.at(k), key+dot+k, sess, srcBus);
				}
				
			}
		}
			
		
	}

	

}
