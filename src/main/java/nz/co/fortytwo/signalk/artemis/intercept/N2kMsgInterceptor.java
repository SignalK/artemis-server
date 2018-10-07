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

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.io.File;

import javax.script.Invocable;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.coveo.nashorn_modules.FilesystemFolder;
import com.coveo.nashorn_modules.Folder;
import com.coveo.nashorn_modules.ResourceFolder;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Processes N2K messages from canboat or similar. Converts the N2k messages to signalk
 * 
 * @author robert 
 * 
 */

public class N2kMsgInterceptor extends JsBaseInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(N2kMsgInterceptor.class);
	
	private Invocable inv;
	private Object n2kMapper;
	// private boolean rmcClock = false;


	
	@SuppressWarnings("restriction")
	public N2kMsgInterceptor() throws Exception {
		super();
	
		String resourceDir = getClass().getClassLoader().getResource("n2k-signalk/dist/bundle.js").toString();
		resourceDir = StringUtils.substringBefore(resourceDir, "dist/bundle.js");
		resourceDir = StringUtils.substringAfter(resourceDir, "file:");
		if(logger.isDebugEnabled())logger.debug("Javascript jsRoot: {}", resourceDir);

		Folder rootFolder = null;
		if (new File(resourceDir).exists()) {
			rootFolder = FilesystemFolder.create(new File(resourceDir), "UTF-8");
		} else {
			rootFolder = ResourceFolder.create(getClass().getClassLoader(), resourceDir, Charsets.UTF_8.name());
		}
		
		if(logger.isDebugEnabled())logger.debug("Starting nashorn env from: {}", rootFolder.getPath());
		
		if(logger.isDebugEnabled())logger.debug("Load parser: {}", "n2k-signalk/dist/bundle.js");
		
		engine.eval(IOUtils.toString(getIOStream("n2k-signalk/dist/bundle.js")));
		n2kMapper = engine.get("n2kMapper");
		if(logger.isDebugEnabled())logger.debug("Parser: {}",n2kMapper);
		
		// create an Invocable object by casting the script engine object
		inv = (Invocable) engine;
	
	}

	

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if (isResponse(packet))
			return true;

		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage message = realPacket.getMessage();

			if (!Config.N2K.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))
				return true;
			
			String bodyStr = Util.readBodyBufferToString(message).trim();
			if (logger.isDebugEnabled())
				logger.debug("N2K Message: {}", bodyStr);
			
			if (StringUtils.isNotBlank(bodyStr) ) {
				try {
					if (logger.isDebugEnabled())
						logger.debug("Processing N2K: {}",bodyStr);

					Object result = inv.invokeMethod(n2kMapper,"toDelta", bodyStr);

					if (logger.isDebugEnabled())
						logger.debug("Processed N2K: {} ",result);

					if (result == null || StringUtils.isBlank(result.toString()) || result.toString().startsWith("Error")) {
						logger.error("{},{}", bodyStr, result);
						return true;
					}
					Json json = Json.read(result.toString());
					if(!json.has("delta"))return false;
					
					json = json.at("delta");
					json.set(SignalKConstants.CONTEXT, vessels + dot + Util.fixSelfKey(self_str));
					
					message.putStringProperty(Config.AMQ_CONTENT_TYPE, Config.JSON_DELTA);
					message.getBodyBuffer().clear();
					message.getBodyBuffer().writeString(json.toString());
					if (logger.isDebugEnabled())
						logger.debug("Converted N2K msg: {}", json.toString());
				} catch (Exception e) {
					logger.error(e, e);
					throw new ActiveMQException(ActiveMQExceptionType.INTERNAL_ERROR, e.getMessage(), e);
				}
				return true;
			}
		}
		return true;
	}

}
