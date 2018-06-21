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
import java.io.FileNotFoundException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.coveo.nashorn_modules.FilesystemFolder;
import com.coveo.nashorn_modules.Folder;
import com.coveo.nashorn_modules.Require;

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
	private File jsRoot;
	@SuppressWarnings("restriction")
	private NashornScriptEngine engine;
	private Invocable inv;
	//private boolean rmcClock = false;
	
	private static ScheduledExecutorService globalScheduledThreadPool = Executors.newScheduledThreadPool(20);

	@SuppressWarnings("restriction")
	public NMEAMsgInterceptor() throws ScriptException, FileNotFoundException, NoSuchMethodException {
		super();
		
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		//console.error
	    engine = (NashornScriptEngine) factory.getScriptEngine(new String[] { "--language=es6" });
	    
	 // Injection of __NASHORN_POLYFILL_TIMER__ in ScriptContext
	    engine.getContext().setAttribute("__NASHORN_POLYFILL_TIMER__", globalScheduledThreadPool, ScriptContext.ENGINE_SCOPE);
	    
	    jsRoot = new File(getClass().getClassLoader().getResource("signalk-parser-nmea0183/index-es5.js").getPath()).getParentFile();
	    logger.debug("Javascript jsRoot: {}",jsRoot.getAbsolutePath());
	    
	    Folder rootFolder = FilesystemFolder.create(jsRoot, "UTF-8");
	    
		Require.enable(engine, rootFolder);
		logger.debug("Starting nashorn");
		
		engine.eval(new java.io.FileReader(new File(jsRoot.getParent(),"parser.js")));
        
		// create an Invocable object by casting the script engine object
        inv = (Invocable) engine;
        File dir = new File(jsRoot,"hooks-es5");
        for(File f:dir.listFiles()){
        	if(!f.isFile())continue;
        	if(!f.getName().endsWith(".js"))continue;
        	//seatalk breaks
        	if(f.getName().startsWith("ALK"))continue;
        	logger.debug(f.getName());
        	inv.invokeFunction("loadHook",StringUtils.substringBefore(f.getName(),".") );
        }
	}

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if(isResponse(packet))return true;
		
		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage message = realPacket.getMessage();
			
			if(!Config._0183.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))return true;
			//String sessionId = message.getStringProperty(Config.AMQ_SESSION_ID);
			//ServerSession sess = ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
			String bodyStr = Util.readBodyBufferToString(message);
			if(logger.isDebugEnabled())logger.debug("NMEA Message: " +bodyStr);
			
			if (StringUtils.isNotBlank(bodyStr) && bodyStr.startsWith("$")) {
				try {
					if (logger.isDebugEnabled())
						logger.debug("Processing NMEA:[" + bodyStr + "]");
					
			        // invoke the method named "hello" on the object defined in the script
			        // with "Script Method!" as the argument
					Object result = inv.invokeFunction("parse",bodyStr );
					
					if (logger.isDebugEnabled())
						logger.debug("Processed NMEA:[" + result + "]");
					
					if(result==null || result.toString().startsWith("Error")){
						logger.error(bodyStr+","+result);
						return true;
					}
					Json json = Json.read(result.toString());
					json=json.at("delta");
					json.set(SignalKConstants.CONTEXT,vessels+dot+Util.fixSelfKey(self_str));
					//NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
					//SignalkMapConvertor.parseDelta(Json.read(result.toString()),map );
					message.putStringProperty(Config.AMQ_CONTENT_TYPE,Config.JSON_DELTA);
					message.getBodyBuffer().clear();
					message.getBodyBuffer().writeString(json.toString());
					if (logger.isDebugEnabled())
						logger.debug("Converted NMEA msg:" + json.toString());
				} catch (Exception e) {
					logger.error(e,e);
					throw new ActiveMQException(ActiveMQExceptionType.INTERNAL_ERROR,e.getMessage(),e);
				}
				
			}
		}
		return true;
	}
	

}
