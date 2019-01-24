package nz.co.fortytwo.signalk.artemis.transformer;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.JSON_AUTH;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.authenticateUser;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.LOGIN;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.LOGOUT;

import java.util.UUID;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.intercept.BaseInterceptor;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
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
		if (!JSON_AUTH.equals(message.getStringProperty(AMQ_CONTENT_TYPE)))
			return message;
		
		Json node = Util.readBodyBuffer(message.toCore());
		if (logger.isDebugEnabled())
			logger.debug("AUTH msg: {}", node.toString());
		String correlation = message.getStringProperty(Config.AMQ_CORR_ID);
		String destination = message.getStringProperty(Config.AMQ_REPLY_Q);
		
		//auth and validate
		if (node.has(LOGIN)) {
			if (logger.isDebugEnabled())
				logger.debug("LOGIN msg: {}", node.toString());
				try {
					Json reply = node.at(LOGIN).has("token")?validate(node):authenticate(node);
					sendReply(destination,FORMAT_DELTA,correlation,reply, getToken(reply));
				} catch (Exception e) {
					logger.error(e,e);
				}
		}
		
		//logout
		if (node.has(LOGOUT)) {
			if (logger.isDebugEnabled())
				logger.debug("LOGOUT msg: {}", node.toString());
				try {
					Json reply = logout(node);
					
					sendReply(destination,FORMAT_DELTA,correlation,reply, getToken(reply));
				} catch (Exception e) {
					logger.error(e,e);
				}

		}
		
		return message;
	}

	private Json authenticate( Json authRequest) throws Exception {
		if( authRequest==null 
				|| !authRequest.has("requestId")
				|| !authRequest.has("login")
				|| !authRequest.at("login").has("username") 
				|| !authRequest.at("login").has("password") ) {
			return error(getRequestId(authRequest),"FAILED",500,"Must have requestId, username and password");
		}
		String requestId = getRequestId(authRequest);
		String username = authRequest.at("login").at("username").asString();
		String password = authRequest.at("login").at("password").asString();
		if(logger.isDebugEnabled())logger.debug("Authentication request, {} : {}", requestId, username);
		//no auth, return unauthorised
		
		if( StringUtils.isBlank(requestId)||StringUtils.isBlank(username)||StringUtils.isBlank(password)) {
			return error(getRequestId(authRequest),"COMPLETED",400,"requestId, username or password cannot be blank");
		}
		// Validate the Authorization header			
		try {
			String token = authenticateUser(username, password);
			//create the reply
			return reply(requestId,"COMPLETED",200)
					.set("login", Json.object("token",token, "type", "JWT"));
		} catch (Exception e) {
			return error(requestId,"COMPLETED",401,e.getMessage());
		}
		
	}

	private Json logout( Json authRequest) throws Exception {
		if( authRequest==null 
				|| !authRequest.has("requestId")
				|| !authRequest.has("logout")
				|| !authRequest.at("logout").has("token") ) {
			return error(getRequestId(authRequest),"COMPLETED",400,"Must have requestId and logout.token");
		}
		String requestId = getRequestId(authRequest);
		String token = authRequest.at("logout").at("token").asString();
		
		if(logger.isDebugEnabled())logger.debug("Logout request, {} : {}", requestId, token);
		//no auth, return unauthorised
		
		if( StringUtils.isBlank(requestId)||StringUtils.isBlank(token)) {
			return error(getRequestId(authRequest),"COMPLETED",400,"requestId, logout.token cannot be blank");
		}
		// Invalidate the Authorization header			
		try {
			SecurityUtils.invalidateToken(token);
			//create the reply
			return reply(requestId,"COMPLETED",200)
					.set("logout", Json.object("token",token));
		} catch (Exception e) {
			return error(requestId,"COMPLETED",401,e.getMessage());
		}
		
	}
	
	private Json validate( Json authRequest) throws Exception {
		if( authRequest==null 
				|| !authRequest.has("requestId")
				|| !authRequest.has("login")
				|| !authRequest.at("login").has("token") ) {
			return error(getRequestId(authRequest),"COMPLETED",400,"Must have requestId and login.token");
		}
		String requestId = getRequestId(authRequest);
		String token = authRequest.at("login").at("token").asString();
		
		if(logger.isDebugEnabled())logger.debug("Validate request, {} : {}", requestId, token);
		//no auth, return unauthorised
		
		if( StringUtils.isBlank(requestId)||StringUtils.isBlank(token)) {
			return error(getRequestId(authRequest),"COMPLETED",400,"requestId, login.token cannot be blank");
		}
		// Invalidate the Authorization header			
		try {
			SecurityUtils.validateToken(token);
			//create the reply
			return reply(requestId,"COMPLETED",200)
					.set("login", Json.object("token",token));
		} catch (Exception e) {
			return error(requestId,"COMPLETED",401,e.getMessage());
		}
		
	}

	

}
