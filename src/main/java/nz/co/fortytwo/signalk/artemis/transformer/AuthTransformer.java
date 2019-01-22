package nz.co.fortytwo.signalk.artemis.transformer;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.JSON_LOGIN;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.authenticateUser;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.LOGIN;

import java.util.UUID;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.intercept.BaseInterceptor;
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
 * Converts SignalK AUTH transformer
 * 
 * @author robert
 * 
 */

public class AuthTransformer extends BaseInterceptor implements Transformer {

	
	private static Logger logger = LogManager.getLogger(AuthTransformer.class);
	
	
	/**
	 * Reads Delta GET message and returns the result in full format. Does nothing if json
	 * is not a GET, and returns the original message
	 * 
	 * @param node
	 * @return
	 */

	@Override
	public Message transform(Message message) {
		if (!JSON_LOGIN.equals(message.getStringProperty(AMQ_CONTENT_TYPE)))
			return message;
		
		Json node = Util.readBodyBuffer(message.toCore());
		String correlation = message.getStringProperty(Config.AMQ_CORR_ID);
		String destination = message.getStringProperty(Config.AMQ_REPLY_Q);
		
		//auth
		if (node.has(LOGIN)) {
			if (logger.isDebugEnabled())
				logger.debug("LOGIN msg: {}", node.toString());
				try {
					Json reply = authenticate(node);
					String token = null;
					if(reply.has("login")) {
						token = reply.at("login").at("token").asString();
					}
					sendReply(destination,FORMAT_DELTA,correlation,reply, token);
				} catch (Exception e) {
					logger.error(e,e);
				}

		}
		//validate
		
		//logout
		return message;
	}

	private Json authenticate( Json authRequest) throws Exception {
		if( authRequest==null 
				|| !authRequest.has("requestId")
				|| !authRequest.has("login")
				|| !authRequest.at("login").has("username") 
				|| !authRequest.at("login").has("password") ) {
			return Json.object()
					.set("requestId", getRequestId(authRequest))
					.set("state", "FAILED")
					.set("result", 500)
					.set("message", "Must have requestId, username and password");
		}
		String requestId = authRequest.at("requestId").asString();
		String username = authRequest.at("login").at("username").asString();
		String password = authRequest.at("login").at("password").asString();
		if(logger.isDebugEnabled())logger.debug("Authentication request, {} : {}", requestId, username);
		//no auth, return unauthorised
		
		if( StringUtils.isBlank(requestId)||StringUtils.isBlank(username)||StringUtils.isBlank(password)) {
			return Json.object()
					.set("requestId", getRequestId(authRequest))
					.set("state", "FAILED")
					.set("result", 500)
					.set("message", "requestId, username or password cannot be blank");
		}
		// Validate the Authorization header			
		try {
			String token = authenticateUser(username, password);
			//create the reply
			return Json.object()
					.set("requestId", requestId)
					.set("state", "COMPLETED")
					.set("result", 200)
					.set("login", Json.object("token",token));
		} catch (Exception e) {
			return Json.object()
					.set("requestId", requestId)
					.set("state", "COMPLETED")
					.set("result", 401)
					.set("message", e.getMessage());
		}
		
	}

	private String getRequestId(Json authRequest) {
		if( authRequest!=null 
				&& authRequest.has("requestId")) {
			return authRequest.at("requestId").asString();
		}
		return UUID.randomUUID().toString();
	}

}
