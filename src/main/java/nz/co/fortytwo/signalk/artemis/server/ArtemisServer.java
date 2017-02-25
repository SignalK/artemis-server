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
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Nettosphere;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.SignalkManagedService;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * ActiveMQ Artemis embedded with JMS
 */
public final class ArtemisServer {

	private static Logger logger = LogManager.getLogger(ArtemisServer.class);
	private static EmbeddedActiveMQ embedded;
	private static Nettosphere server;

	public ArtemisServer() throws Exception {
		// Step 1. Create ActiveMQ Artemis core configuration, and set the
		// properties accordingly
		System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
		// System.setProperty("logging.configuration","classpath://logging.properties");
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
		
		loadConfig();
		
		addShutdownHook(this);
		server = new Nettosphere.Builder().config(
                 new Config.Builder()
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

	private void loadConfig() throws Exception {
		Json config = Util.loadConfig();
		Json signalk = Util.load();
		//now send in
		ClientSession session = Util.getVmSession("admin", "admin");
		try{
			ClientProducer producer = session.createProducer();
			
			ClientMessage message = session.createMessage(true);
			message.getBodyBuffer().writeString(config.toString());
			producer.send("incoming.delta", message);
			
			message = session.createMessage(true);
			message.getBodyBuffer().writeString(signalk.toString());
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
	
	
	
	public static void main(String[] args) throws Exception {
		
		new ArtemisServer();

	}
}
