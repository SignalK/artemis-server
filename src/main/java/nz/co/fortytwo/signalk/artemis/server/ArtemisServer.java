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

import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManagerImpl;

/**
 * ActiveMQ Artemis embedded with JMS
 */
public final class ArtemisServer {

	public static EmbeddedActiveMQ embedded;

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

		addShutdownHook(embedded);

	}

	private static void addShutdownHook(final EmbeddedActiveMQ embedded) {
		Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
			@Override
			public void run() {
				try {
					embedded.stop();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}
}
