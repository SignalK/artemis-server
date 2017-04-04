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
package nz.co.fortytwo.signalk.artemis.divert;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.util.SignalKConstants.UNKNOWN;
import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.label;
import static nz.co.fortytwo.signalk.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.util.SignalKConstants.type;
import static nz.co.fortytwo.signalk.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.cluster.Transformer;
import org.apache.commons.lang3.ObjectUtils;
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
public class FullMsg implements Transformer {

	private static Logger logger = LogManager.getLogger(FullMsg.class);

	private JsonSerializer ser = new JsonSerializer();

	public FullMsg() {
		super();
	}

	@Override
	public ServerMessage transform(ServerMessage message) {
		
		if(!Config.JSON.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))return message;
		//if(logger.isDebugEnabled())logger.debug("Processing: " + message);
		Json node = Json.read(message.getBodyBuffer().readString());
		// avoid diff signalk syntax
		if (node.has(CONTEXT))
			return message;
		String sessionId = message.getStringProperty(Config.AMQ_SESSION_ID);
		ServerSession sess = ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
		// deal with full format
		if (node.has(vessels) || node.has(CONFIG) || node.has(resources)) {
			if (logger.isDebugEnabled())
				logger.debug("processing full  " + node);
			// process it by recursion
			try {
				recurseJson(node, null, sess);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return message;
	}

	private void recurseJson(Json node, String key, ServerSession sess ) throws Exception {
		//check if it has a .value
		// value type, or attribute type
		if (logger.isDebugEnabled())
			logger.debug("processing sub: "+ key+": " + node);
		if (node==null || node.isNull()) {
			Util.sendMsg(key, Json.nil(), null, (String)null, sess);
			return;
		}
		if(node.isPrimitive()){
			//attribute type
			Util.sendMsg(key, node,null, (String)null, sess);
			return;
		}
		if(node.isArray()){
			//attribute type
			Util.sendMsg(key, node,null, (String)null, sess);
			return;
		}
		Json src = node.at(source);
		String srcRef = null;
		if(src!=null){
			srcRef = src.at(type).toString()+dot+sess.getRemotingConnection().getRemoteAddress()+dot+src.at(label).toString();
			Util.sendSourceMsg(srcRef, (Json)src,node.at(timestamp).asString(), sess);
		}
		if(node.has(value)){
			Util.sendMsg(key, node.at(value), node.at(timestamp).asString(), srcRef, sess);
			return;
		}
		//either composite or path to recurse
		if(node.has(timestamp)||node.has(source)){
			//composite
			Util.sendMsg(key, node, node.at(timestamp).asString(), srcRef, sess);
		}else{
			//recurse
			for(String k: node.asJsonMap().keySet()){
				if(key==null){
					recurseJson(node.at(k), k, sess);
				}else{
					recurseJson(node.at(k), key+dot+k, sess);
				}
				
			}
		}
			
		
	}

}
