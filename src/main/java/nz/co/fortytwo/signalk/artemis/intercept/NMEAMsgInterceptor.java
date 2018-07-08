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
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

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
import com.coveo.nashorn_modules.Require;
import com.coveo.nashorn_modules.ResourceFolder;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Processes NMEA sentences in the body of a message, firing events to
 * interested listeners Converts the NMEA messages to signalk
 * 
 * @author robert
 * 
 */

public class NMEAMsgInterceptor extends JsBaseInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(NMEAMsgInterceptor.class);
	
	private Invocable inv;
	private Object parser;
	//private ScriptContext nmeaContext = new SimpleScriptContext();
	// private boolean rmcClock = false;
	
	@SuppressWarnings("restriction")
	public NMEAMsgInterceptor() throws Exception {
		super();

		String resourceDir = getClass().getClassLoader().getResource("signalk-parser-nmea0183/parser.js").toString();
		resourceDir = StringUtils.substringBefore(resourceDir, "index-es5.js");
		resourceDir = StringUtils.substringAfter(resourceDir, "file:");
		if(logger.isDebugEnabled())logger.debug("Javascript jsRoot: {}", resourceDir);

		Folder rootFolder = null;
		if (new File(resourceDir).exists()) {
			rootFolder = FilesystemFolder.create(new File(resourceDir), "UTF-8");
		} else {
			rootFolder = ResourceFolder.create(getClass().getClassLoader(), resourceDir, Charsets.UTF_8.name());
		}
		if(logger.isDebugEnabled())logger.debug("Starting nashorn env from: {}", rootFolder.getPath());
		

		//nmeaContext.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);

		//Bindings bindings = nmeaContext.getBindings(ScriptContext.ENGINE_SCOPE);
		//Require.enable(engine, rootFolder, bindings);
		
		if(logger.isDebugEnabled())logger.debug("Load parser: {}", "signalk-parser-nmea0183/dist/bundle.js");
		
		engine.eval(IOUtils.toString(getIOStream("signalk-parser-nmea0183/dist/bundle.js")));

		parser = engine.get("parser");
		if(logger.isDebugEnabled())logger.debug("Parser: {}",parser);
		
		// create an Invocable object by casting the script engine object
		inv = (Invocable) engine;
		String hooks = IOUtils.toString(getIOStream("signalk-parser-nmea0183/hooks-es5/supported.txt"), Charsets.UTF_8);
		if(logger.isDebugEnabled())logger.debug("Hooks: {}",hooks);

		String[] files = hooks.split("\n");
		
		for (String f : files) {
			// seatalk breaks
			if (f.startsWith("ALK"))
				continue;
			if(logger.isDebugEnabled())logger.debug(f);
			inv.invokeMethod(parser, "loadHook", f.trim());
		}
	}

	

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if (isResponse(packet))
			return true;

		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage message = realPacket.getMessage();

			if (!Config._0183.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))
				return true;
			
			String bodyStr = Util.readBodyBufferToString(message).trim();
			if (logger.isDebugEnabled())
				logger.debug("NMEA Message: " + bodyStr);
			
			if (StringUtils.isNotBlank(bodyStr) && bodyStr.startsWith("$")) {
				try {
					if (logger.isDebugEnabled())
						logger.debug("Processing NMEA:[" + bodyStr + "]");

					Object result = inv.invokeMethod(parser,"parse", bodyStr);

					if (logger.isDebugEnabled())
						logger.debug("Processed NMEA:[" + result + "]");

					if (result==null || StringUtils.isBlank(result.toString())|| result.toString().startsWith("Error")) {
						logger.error(bodyStr + "," + result);
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
						logger.debug("Converted NMEA msg:" + json.toString());
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
