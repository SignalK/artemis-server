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

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManagerImpl;
import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.nettosphere.Nettosphere;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.SignalkManagedApiService;
import nz.co.fortytwo.signalk.artemis.service.SignalkManagedChartService;
import nz.co.fortytwo.signalk.artemis.service.SignalkManagedStreamService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.*;



/**
 * ActiveMQ Artemis embedded with JMS
 */
public final class ArtemisServer {

	private static Logger logger;
	private static EmbeddedActiveMQ embedded;
	private static Nettosphere server;
	private JmmDNS jmdns;

	private nz.co.fortytwo.signalk.artemis.server.SerialPortManager serialPortManager;
	private nz.co.fortytwo.signalk.artemis.server.NettyServer skServer;
	private nz.co.fortytwo.signalk.artemis.server.NettyServer nmeaServer;

	public ArtemisServer() throws Exception {
		Properties props = System.getProperties();
		props.setProperty("java.net.preferIPv4Stack", "true");
		props.setProperty("log4j.configurationFile", "./conf/log4j2.json");
		props.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level","TRACE");
		System.setProperties(props);
		logger = LogManager.getLogger(ArtemisServer.class);
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

		
		// start serial manager

		// start a serial port manager
		if (serialPortManager == null) {
			serialPortManager = new SerialPortManager();
		}
		new Thread(serialPortManager).start();

		addShutdownHook(this);
		server = new Nettosphere.Builder().config(new org.atmosphere.nettosphere.Config.Builder()
				.host("0.0.0.0")
				.port(8080)
				.initParam(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true")
				//.interceptor(new AuthenticationInterceptor(conf) )
				.resource(SignalkManagedStreamService.class)
				.resource(SignalkManagedApiService.class)
				.resource(SignalkManagedChartService.class)
				.resource("./signalk-static").build()).build();
		server.start();

		skServer = new NettyServer(null, OUTPUT_TCP);
		skServer.setTcpPort(Config.getConfigPropertyInt(TCP_PORT));
		skServer.setUdpPort(Config.getConfigPropertyInt(UDP_PORT));
		skServer.run();

		nmeaServer = new NettyServer(null, OUTPUT_NMEA);
		nmeaServer.setTcpPort(Config.getConfigPropertyInt(TCP_NMEA_PORT));
		nmeaServer.setUdpPort(Config.getConfigPropertyInt(UDP_NMEA_PORT));
		nmeaServer.run();

		// create a new Camel Main so we can easily start Camel
		//Main main = new Main();
		startMdns();
		reloadCharts();
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
		if (skServer != null) {
			skServer.shutdownServer();
			skServer = null;
		}

		if (nmeaServer != null) {
			nmeaServer.shutdownServer();
			nmeaServer = null;
		}
		try {
			if (serialPortManager != null)
				serialPortManager.stopSerial();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		try {
			stopMdns();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		try {
			server.stop();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		try {
			embedded.stop();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public static ActiveMQServer getActiveMQServer() {
		return embedded.getActiveMQServer();
	}

	/**
	 * Stop the DNS-SD server.
	 * 
	 * @throws IOException
	 */
	public void stopMdns() throws IOException {
		if (jmdns != null) {
			jmdns.unregisterAllServices();
			jmdns.close();
			jmdns = null;
		}
	}

	private void startMdns() {
		// DNS-SD
		// NetworkTopologyDiscovery netTop =
		// NetworkTopologyDiscovery.Factory.getInstance();
		Runnable r = new Runnable() {

			@Override
			public void run() {
				jmdns = JmmDNS.Factory.getInstance();

				jmdns.registerServiceType(_SIGNALK_WS_TCP_LOCAL);
				jmdns.registerServiceType(_SIGNALK_HTTP_TCP_LOCAL);
				ServiceInfo wsInfo = ServiceInfo.create(_SIGNALK_WS_TCP_LOCAL, "signalk-ws",
						Config.getConfigPropertyInt(WEBSOCKET_PORT), 0, 0, getMdnsTxt());
				try {
					jmdns.registerService(wsInfo);
					ServiceInfo httpInfo = ServiceInfo.create(_SIGNALK_HTTP_TCP_LOCAL, "signalk-http",
							Config.getConfigPropertyInt(REST_PORT), 0, 0, getMdnsTxt());
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

	private Map<String, String> getMdnsTxt() {
		Map<String, String> txtSet = new HashMap<String, String>();
		txtSet.put("path", SIGNALK_DISCOVERY);
		txtSet.put("server", "signalk-server");
		txtSet.put("version", Config.getConfigProperty(VERSION));
		txtSet.put("vessel_name", Config.getConfigProperty(UUID));
		txtSet.put("vessel_mmsi", Config.getConfigProperty(UUID));
		txtSet.put("vessel_uuid", Config.getConfigProperty(UUID));
		return txtSet;
	}

	public static void main(String[] args) throws Exception {
		Properties props = System.getProperties();
		props.setProperty("java.net.preferIPv4Stack", "true");
		//props.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
		props.setProperty("log4j.configurationFile", "./conf/log4j2.json");
		props.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level","TRACE");
		System.setProperties(props);
		
		PropertyConfigurator.configure("./conf/log4j2.json");
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
		LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		File file = new File("./conf/log4j2.json");

		// this will force a reconfiguration
		context.setConfigLocation(file.toURI());
		new ArtemisServer();

	}
	
	private void reloadCharts() throws Exception {
		String staticDir = Config.getConfigProperty(STATIC_DIR);
		if(!staticDir.endsWith("/")){
			staticDir=staticDir+"/";
		}
		
		File mapDir = new File(staticDir+Config.getConfigProperty(MAP_DIR));
		logger.debug("Reloading charts from: "+mapDir.getAbsolutePath());
		if(mapDir==null || !mapDir.exists() || mapDir.listFiles()==null)return;
		//TreeMap<String, Object> treeMap = new TreeMap<String, Object>(signalkModel.getSubMap("resources.charts"));
		for(File chart:mapDir.listFiles()){
			if(chart.isDirectory()){
				Json chartJson = SignalkManagedChartService.loadChart(chart.getName());
					logger.debug("Reloading: {}= {}",chart.getName(),chartJson);
					try {
						Util.sendRawMessage("admin", "admin",chartJson.toString());
					} catch (Exception e) {
						logger.warn(e.getMessage());
					}
				
			}
		}
		
	}

}
