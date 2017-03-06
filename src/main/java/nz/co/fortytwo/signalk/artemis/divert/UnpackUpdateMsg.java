package nz.co.fortytwo.signalk.artemis.divert;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.util.SignalKConstants.values;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;

import org.apache.activemq.artemis.api.core.ActiveMQSecurityException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.cluster.Transformer;
import org.apache.activemq.artemis.core.server.impl.ServerMessageImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.server.ArtemisServer;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;


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
 * Converts SignalK delta format to map format
 * 
 * @author robert
 * 
 */

public class UnpackUpdateMsg implements Transformer {
	//private ClientSession session;
	//private ClientProducer producer;

	private static Logger logger = LogManager.getLogger(UnpackUpdateMsg.class);

	/**
	 * Reads Delta format JSON and sends an artemis message per value. Does
	 * nothing if json is not an update, and returns the original message
	 * 
	 * @param node
	 * @return
	 */
	@Override
	public ServerMessage transform(ServerMessage message) {
		if(!Config.JSON.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))return message;
		//if(logger.isDebugEnabled())logger.debug("Processing: " + message);
		Json node = Json.read(message.getBodyBuffer().readString());
		// avoid full signalk syntax
		if (node.has(vessels))
			return message;
		// deal with diff format
		if (node.has(CONTEXT) && (node.has(UPDATES) || node.has(PUT) || node.has(CONFIG))) {
			// if(logger.isDebugEnabled())logger.debug("processing delta "+node
			// );

			// go to context
			String ctx = node.at(CONTEXT).asString();
			ctx = Util.fixSelfKey(ctx);
			// Json pathNode = temp.addNode(path);
			
			parseUpdates(node.at(UPDATES), ctx, message);
			parseUpdates(node.at(PUT), ctx, message);
			parseUpdates(node.at(CONFIG), ctx, message);
			
			return message;
		}
		return message;

	}

	/**
	 * Unpack (if not null), and process each entry.
	 * @param updates
	 * @param ctx
	 * @param message
	 */
	private void parseUpdates(Json updates, String ctx, ServerMessage message) {
		if (updates == null) return;
		for (Json update : updates.asJsonList()) {

			try {
				parseUpdate(update, ctx, message);
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			}

		}
		
	}

	protected void parseUpdate(Json update, String ctx, ServerMessage m1) throws Exception {

		// DateTime timestamp = DateTime.parse(ts,fmt);
		if(logger.isDebugEnabled())logger.debug("message m1 = "  + m1.getMessageID()+":" + m1.getAddress() + ", " + m1.getPropertyNames());
		String sessionId = m1.getStringProperty(Config.AMQ_SESSION_ID);
		ServerSession sess = ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
		if(logger.isDebugEnabled())logger.debug("SessionId:"+sessionId+", found "+sess);
		// grab values and add
		Json array = update.at(values);
		Json src = null;
		if (update.has(source)) {
			src = update.at(source);
		}
		String timeStamp = nz.co.fortytwo.signalk.util.Util.getIsoTimeString();
		if (update.has(timestamp)) {
			timeStamp = update.at(timestamp).asString();
		}
		for (Json e : array.asJsonList()) {

			if (e == null || e.isNull() || !e.has(PATH))
				continue;
			String key = e.at(PATH).asString();
			// temp.put(ctx+"."+key, e.at(value).getValue());
			if (e.has(value)) {
				addRecursively(ctx + dot + key, e.at(value),  timeStamp, src, sess);
			}

		}

	}

	

	protected void addRecursively(String ctx, Json j, String timeStamp, Json src, ServerSession sess) throws Exception {
		if (j == null)
			return;
		if (j.isNull()) {
			Util.sendMsg(ctx, ObjectUtils.NULL, timeStamp, src, sess);
		} else if (j.isPrimitive()) {
			Util.sendMsg(ctx + dot + j.getParentKey(), j.getValue(),timeStamp, src, sess);
		} else if (j.isArray()) {
			Util.sendMsg(ctx + dot + j.getParentKey(), j, timeStamp, src,sess);
		} else {
			for (Json child : j.asJsonMap().values()) {
				if (value.equals(j.getParentKey())) {
					addRecursively(ctx, child, timeStamp, src,sess);
				} else {
					addRecursively(ctx + dot + j.getParentKey(), child, timeStamp, src, sess);
				}
			}
		}

	}

	

}
