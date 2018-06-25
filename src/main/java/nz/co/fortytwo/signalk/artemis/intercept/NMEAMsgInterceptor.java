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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptException;

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
public class NMEAMsgInterceptor extends BaseInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(NMEAMsgInterceptor.class);
	// private File jsRoot;
	@SuppressWarnings("restriction")
	private NashornScriptEngine engine;
	private Invocable inv;
	private Object parser;
	// private boolean rmcClock = false;

	private static ScheduledExecutorService globalScheduledThreadPool = Executors.newScheduledThreadPool(20);

	@SuppressWarnings("restriction")
	public NMEAMsgInterceptor() throws Exception {
		super();

		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		// console.error
		engine = (NashornScriptEngine) factory.getScriptEngine(new String[] { "--language=es6" });

		// Injection of __NASHORN_POLYFILL_TIMER__ in ScriptContext
		engine.getContext().setAttribute("__NASHORN_POLYFILL_TIMER__", globalScheduledThreadPool,
				ScriptContext.ENGINE_SCOPE);
		URL resource = getClass().getClassLoader().getResource("signalk-parser-nmea0183/index-es5.js");
		logger.debug("Resource : {}", resource);
		String resourceDir = getClass().getClassLoader().getResource("signalk-parser-nmea0183/parser.js").toString();
		resourceDir = StringUtils.substringBefore(resourceDir, "index-es5.js");
		resourceDir = StringUtils.substringAfter(resourceDir, "file:");
		logger.debug("Javascript jsRoot: {}", resourceDir);

		Folder rootFolder = null;
		if (new File(resourceDir).exists()) {
			rootFolder = FilesystemFolder.create(new File(resourceDir), "UTF-8");
		} else {
			rootFolder = ResourceFolder.create(getClass().getClassLoader(), resourceDir, Charsets.UTF_8.name());
		}
		Require.enable(engine, rootFolder);
		logger.debug("Starting nashorn from: {}", rootFolder.getPath());
		engine.eval(IOUtils.toString(getIOStream("signalk-parser-nmea0183/dist/nashorn-polyfill.js")));
		
		logger.debug("Load parser: {}", "signalk-parser-nmea0183/dist/bundle.js");
		engine.eval(IOUtils.toString(getIOStream("signalk-parser-nmea0183/dist/bundle.js")));
		parser = engine.get("parser");
		logger.debug("Parser: {}",parser);
		
		// create an Invocable object by casting the script engine object
		inv = (Invocable) engine;
		List<String> files = IOUtils.readLines(getIOStream("signalk-parser-nmea0183/hooks-es5"), Charsets.UTF_8);
		
		for (String f : files) {
			if (!f.endsWith(".js"))
				continue;
			// seatalk breaks
			if (f.startsWith("ALK"))
				continue;
			logger.debug(f);
			inv.invokeMethod(parser, "loadHook", StringUtils.substringBefore(f, "."));
		}
	}

	private InputStream getIOStream(String path) {

		logger.debug("Return resource {}", path);
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

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
			// String sessionId =
			// message.getStringProperty(Config.AMQ_SESSION_ID);
			// ServerSession sess =
			// ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
			String bodyStr = Util.readBodyBufferToString(message);
			if (logger.isDebugEnabled())
				logger.debug("NMEA Message: " + bodyStr);

			if (StringUtils.isNotBlank(bodyStr) && bodyStr.startsWith("$")) {
				try {
					if (logger.isDebugEnabled())
						logger.debug("Processing NMEA:[" + bodyStr + "]");

					Object result = inv.invokeMethod(parser,"parse", bodyStr);

					if (logger.isDebugEnabled())
						logger.debug("Processed NMEA:[" + result + "]");

					if (result == null || result.toString().startsWith("Error")) {
						logger.error(bodyStr + "," + result);
						return true;
					}
					Json json = Json.read(result.toString());
					json = json.at("delta");
					json.set(SignalKConstants.CONTEXT, vessels + dot + Util.fixSelfKey(self_str));
					// NavigableMap<String, Json> map = new
					// ConcurrentSkipListMap<>();
					// SignalkMapConvertor.parseDelta(Json.read(result.toString()),map
					// );
					message.putStringProperty(Config.AMQ_CONTENT_TYPE, Config.JSON_DELTA);
					message.getBodyBuffer().clear();
					message.getBodyBuffer().writeString(json.toString());
					if (logger.isDebugEnabled())
						logger.debug("Converted NMEA msg:" + json.toString());
				} catch (Exception e) {
					logger.error(e, e);
					throw new ActiveMQException(ActiveMQExceptionType.INTERNAL_ERROR, e.getMessage(), e);
				}

			}
		}
		return true;
	}

}
