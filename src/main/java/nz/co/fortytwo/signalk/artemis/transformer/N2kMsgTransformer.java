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
package nz.co.fortytwo.signalk.artemis.transformer;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.JSON_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.Config.MSG_SRC_BUS;
import static nz.co.fortytwo.signalk.artemis.util.Config.MSG_SRC_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.N2K;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Context;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.SignalkKvConvertor;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Processes N2K messages from canboat or similar. Converts the N2k messages to signalk
 * 
 * @author robert 
 * 
 */

public class N2kMsgTransformer extends JsBaseTransformer implements Transformer {

	private static Logger logger = LogManager.getLogger(N2kMsgTransformer.class);
	
	
	public N2kMsgTransformer() throws Exception {
		logger.info("Started N2kMsgTransformer");
	
	}

	@Override
	public Message transform(Message message) {
		
		if (!N2K.equals(message.getStringProperty(AMQ_CONTENT_TYPE)))
			return message;
		
		String bodyStr = Util.readBodyBufferToString(message.toCore()).trim();
		if (logger.isDebugEnabled())
			logger.debug("N2K Message: {}", bodyStr);
		
		if (StringUtils.isNotBlank(bodyStr) ) {
			try {
				if (logger.isDebugEnabled())
					logger.debug("Processing N2K: {}",bodyStr);
				Context ctx = pool.borrowObject();
				
				Object result = ctx.getBindings("js").getMember("n2kMapper").invokeMember("toDelta", bodyStr);

				if (logger.isDebugEnabled())
					logger.debug("Processed N2K: {} ",result);

				if (result == null || StringUtils.isBlank(result.toString()) || result.toString().startsWith("Error")) {
					logger.error("{},{}", bodyStr, result);
					return message;
				}
				if (result==null || StringUtils.isBlank(result.toString())|| result.toString().startsWith("WARN")) {
					logger.warn(bodyStr + "," + result);
					//ctx.leave();
					pool.returnObject(ctx);
					return message;
				}
				
				Json json = Json.read(result.toString());
				pool.returnObject(ctx);
				if(!json.isObject())return message;
						
				json.set(SignalKConstants.CONTEXT, vessels + dot + Util.fixSelfKey(self_str));
				
				if (logger.isDebugEnabled())
					logger.debug("Converted N2K msg: {}", json.toString());
				//add type.bus to source
				String type = message.getStringProperty(MSG_SRC_TYPE);
				String bus = message.getStringProperty(MSG_SRC_BUS);
				for(Json j:json.at(UPDATES).asJsonList()){
					convertSource(this, message, j, bus, type);
				}
				//now its a signalk delta msg
				message.putStringProperty(AMQ_CONTENT_TYPE, JSON_DELTA);
				SignalkKvConvertor.parseDelta(this,message, json);
				json.clear(true);
			} catch (Exception e) {
				logger.error(e, e);
				
			}
		}
		return message;
	}

	

	


}
