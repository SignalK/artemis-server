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

import static nz.co.fortytwo.signalk.artemis.util.Config.AIS;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.JSON_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.Config.MSG_SRC_BUS;
import static nz.co.fortytwo.signalk.artemis.util.Config.MSG_SRC_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config._0183;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.graal.ContextHolder;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.SignalkKvConvertor;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Processes NMEA sentences in the body of a message, firing events to
 * interested listeners Converts the NMEA messages to signalk
 * 
 * @author robert
 * 
 */

public class NMEAMsgTransformer extends JsBaseTransformer implements Transformer {

	private static Logger logger = LogManager.getLogger(NMEAMsgTransformer.class);
	
	public NMEAMsgTransformer() throws Exception {
		logger.info("Started NMEAMsgTransformer with {} JS engine..", pool.getEngineName());
	}

	@Override
	public Message transform(Message message) {
		
		if (!(_0183.equals(message.getStringProperty(AMQ_CONTENT_TYPE))|| AIS.equals(message.getStringProperty(AMQ_CONTENT_TYPE))))
			return message;
		
		String bodyStr = Util.readBodyBufferToString(message.toCore()).trim();
		if (logger.isDebugEnabled())
			logger.debug("NMEA Message: " + bodyStr);
		
		if (StringUtils.isNotBlank(bodyStr) && bodyStr.startsWith("$")) {
			try {
				ContextHolder ctx = pool.borrowObject();
				//ctx.enter();
				if (logger.isDebugEnabled()) {
					logger.debug("Processing NMEA:[" + bodyStr + "]");
					//logger.debug("Parser inv: {}",ctx.getMember("parser"));
				}
				
				Object result =  ctx.invokeMember("parser","parse", bodyStr);
				
				if (logger.isDebugEnabled())
					logger.debug("Processed NMEA: " + result );

				if (result==null || StringUtils.isBlank(result.toString())|| result.toString().startsWith("WARN")) {
					logger.warn(bodyStr + "," + result);
					//ctx.leave();
					pool.returnObject(ctx);
					return message;
				}
				
				Json json = Json.read(result.toString());
				
				//ctx.leave();
				pool.returnObject(ctx);
				
				if(!json.isObject())return message;
				
				json.set(SignalKConstants.CONTEXT, vessels + dot + Util.fixSelfKey(self_str));
				//add type.bus to source
				String type = message.getStringProperty(MSG_SRC_TYPE);
				String bus = message.getStringProperty(MSG_SRC_BUS);
				//now its a signalk delta msg
				message.putStringProperty(AMQ_CONTENT_TYPE, JSON_DELTA);
				for(Json j:json.at(UPDATES).asJsonList()){
					Util.convertSource(this, message, j, bus, type);
				}
				
				if (logger.isDebugEnabled())
					logger.debug("Converted NMEA msg:" + json.toString());
				
				SignalkKvConvertor.parseDelta(this,message, json);
				json.clear(true);
				
				
			} catch (Exception e) {
				logger.error(e, e);
				
			}
			
		}
		return message;
	}

	
}
