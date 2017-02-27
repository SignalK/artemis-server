package nz.co.fortytwo.signalk.artemis.server;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static nz.co.fortytwo.signalk.util.SignalKConstants.SIGNALK_DISCOVERY;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.atmosphere.cpr.ApplicationConfig;
import nz.co.fortytwo.signalk.artemis.util.Config;
import org.atmosphere.nettosphere.Nettosphere;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.SignalkManagedService;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.util.ConfigConstants;
import nz.co.fortytwo.signalk.util.SignalKConstants;

/**
 * ActiveMQ Artemis embedded with JMS
 */
public final class ArtemisServer {

	private static Logger logger = LogManager.getLogger(ArtemisServer.class);
	private static EmbeddedActiveMQ embedded;
	private static Nettosphere server;
	private JmmDNS jmdns;

	public ArtemisServer() throws Exception {
		Properties props = System.getProperties();
		props.setProperty("java.net.preferIPv4Stack", "true");
		props.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
		props.setProperty("log4j.configurationFile","./conf/log4j2.json");
		System.setProperties(props);
		
		Config.getInstance();
		
		embedded = new EmbeddedActiveMQ();
		SecurityConfiguration conf = new SecurityConfiguration();
		conf.addUser("guest", "guest");
		conf.addRole("guest", "guest");
		conf.addUser("admin", "admin");
		conf.addRole("admin", "guest");
		conf.addRole("admin", "admin");
		
		ActiveMQSecurityManager securityManager = new ActiveMQSecurityManagerImpl(conf);
		
		embedded.setSecurityManager(securityManager);
		embedded.start();
		
		load();
		
		addShutdownHook(this);
		server = new Nettosphere.Builder().config(
                 new org.atmosphere.nettosphere.Config.Builder()
                    .host("0.0.0.0")
                    .port(8080)
                    .initParam(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true")
                    .resource(SignalkManagedService.class)
                    .resource("./signalk-static")
                    //.resource("./src/main/resources/signalk-static")
                    .build())
                 .build();
		 server.start();

	}

	private void load() throws Exception {
		
		Json signalk = Util.load();
		//now send in
		ClientSession session = Util.getVmSession( Config.getConfigProperty(Config.ADMIN_USER),
				Config.getConfigProperty(Config.ADMIN_PWD));
		try{
			ClientProducer producer = session.createProducer();
			
			ClientMessage message = session.createMessage(true);		
			
			message.getBodyBuffer().writeString(signalk.toString());
			producer.send("incoming.delta", message);
			
			message = session.createMessage(true);
			File jsonFile = new File(Util.SIGNALK_CFG_SAVE_FILE);
			Json json = Json.read(jsonFile.toURI().toURL());
			message.getBodyBuffer().writeString(json.toString());
			producer.send("incoming.delta", message);
			
		}finally{
			if (session!=null) session.close();
		}
		
	}

	private static void addShutdownHook(final ArtemisServer server) {
		Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
			@Override
			public void run() {
				server.stop();
			}
		});

	}

	public void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		try {
			embedded.stop();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		
	}

	public static ActiveMQServer getActiveMQServer() {
		return embedded.getActiveMQServer();
	}
	
	/**
	 * Stop the DNS-SD server.
	 * @throws IOException 
	 */
	public void stopMdns() throws IOException {
		if(jmdns!=null){
			jmdns.unregisterAllServices();
			jmdns.close();
			jmdns=null;
		}
	}
	
	private void startMdns() {
		//DNS-SD
		//NetworkTopologyDiscovery netTop = NetworkTopologyDiscovery.Factory.getInstance();
		Runnable r = new Runnable() {

			@Override
			public void run() {
				jmdns = JmmDNS.Factory.getInstance();
				
				jmdns.registerServiceType(SignalKConstants._SIGNALK_WS_TCP_LOCAL);
				jmdns.registerServiceType(SignalKConstants._SIGNALK_HTTP_TCP_LOCAL);
				ServiceInfo wsInfo = ServiceInfo.create(SignalKConstants._SIGNALK_WS_TCP_LOCAL,"signalk-ws",Config.getConfigPropertyInt(ConfigConstants.WEBSOCKET_PORT), 0,0, getMdnsTxt());
				try {
					jmdns.registerService(wsInfo);
					ServiceInfo httpInfo = ServiceInfo
						.create(SignalKConstants._SIGNALK_HTTP_TCP_LOCAL, "signalk-http",Config.getConfigPropertyInt(ConfigConstants.REST_PORT),0,0, getMdnsTxt());
					jmdns.registerService(httpInfo);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		Thread t = new Thread(r);
		t.setDaemon(true);
		t.start();
		
	}

	private Map<String,String> getMdnsTxt() {
		Map<String,String> txtSet = new HashMap<String, String>();
		txtSet.put("path", SIGNALK_DISCOVERY);
		txtSet.put("server","signalk-server");
		txtSet.put("version",Config.getConfigProperty(ConfigConstants.VERSION));
		txtSet.put("vessel_name",Config.getConfigProperty(ConfigConstants.UUID));
		txtSet.put("vessel_mmsi",Config.getConfigProperty(ConfigConstants.UUID));
		txtSet.put("vessel_uuid",Config.getConfigProperty(ConfigConstants.UUID));
		return txtSet;
	}
	
	public static void main(String[] args) throws Exception {
		LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		File file = new File("./conf/log4j2.json");
		 
		// this will force a reconfiguration
		context.setConfigLocation(file.toURI());
		new ArtemisServer();

	}
}
