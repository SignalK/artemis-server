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

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.CLOCK_SOURCE;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.ENABLE_SERIAL;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.GENERATE_NMEA0183;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.OUTPUT_NMEA;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.OUTPUT_TCP;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.REST_PORT;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.START_TCP;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.START_UDP;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.TCP_NMEA_PORT;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.TCP_PORT;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.UDP_NMEA_PORT;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.UDP_PORT;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.UUID;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.VERSION;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.WEBSOCKET_PORT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_DISCOVERY;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants._SIGNALK_HTTP_TCP_LOCAL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants._SIGNALK_WS_TCP_LOCAL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.nettosphere.Nettosphere;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.handler.AlarmHandler;
import nz.co.fortytwo.signalk.artemis.handler.AnchorWatchHandler;
import nz.co.fortytwo.signalk.artemis.handler.InfluxDbHandler;
import nz.co.fortytwo.signalk.artemis.handler.TrueWindHandler;
import nz.co.fortytwo.signalk.artemis.scheduled.DeclinationUpdater;
import nz.co.fortytwo.signalk.artemis.serial.SerialPortManager;
import nz.co.fortytwo.signalk.artemis.service.ChartService;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.subscription.SubscriptionManagerFactory;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * ActiveMQ Artemis embedded with JMS
 */
public final class ArtemisServer {

	private static Logger logger;
	private static EmbeddedActiveMQ embedded;
	private static Nettosphere server;
	private JmmDNS jmdns;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private nz.co.fortytwo.signalk.artemis.serial.SerialPortManager serialPortManager;
	private nz.co.fortytwo.signalk.artemis.server.NettyServer skServer;
	private nz.co.fortytwo.signalk.artemis.server.NettyServer nmeaServer;
	private ClientSession session;
	private ClientConsumer consumer;
	private InfluxDbHandler influxHandler;
	private TrueWindHandler trueWindHandler;
	private AnchorWatchHandler anchorWatchHandler;
	private AlarmHandler alarmHandler;

	public ArtemisServer() throws Exception {
		init();
	}

	public ArtemisServer(String dbName) throws Exception {
		InfluxDbService.setDbName(dbName);
		init();
	}

	private void init() throws Exception {
		Properties props = System.getProperties();
		props.setProperty("java.net.preferIPv4Stack", "true");
		props.setProperty("log4j.configurationFile", "./conf/log4j2.json");
		props.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "TRACE");
		System.setProperties(props);
		logger = LogManager.getLogger(ArtemisServer.class);
		Config.saveConfig();
		
		ensureSecurityConf();

		embedded = new EmbeddedActiveMQ();
		embedded.start();

		// start incoming message consumer
		startIncomingConsumer();
		startKvHandlers();

		addShutdownHook(this);

		server = new Nettosphere.Builder().config(new org.atmosphere.nettosphere.Config.Builder().supportChunking(true)
				.maxChunkContentLength(1024 * 1024).socketKeepAlive(true).enablePong(false)
				.initParam(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true")
				.initParam(ApplicationConfig.ANALYTICS, "false")
				.initParam("jersey.config.server.provider.packages",
						"nz.co.fortytwo.signalk.artemis,io.swagger.jaxrs.listing")
				.initParam("jersey.config.server.provider.classnames",
						"org.glassfish.jersey.media.multipart.MultiPartFeature")
				.initParam("org.atmosphere.cpr.broadcaster.shareableThreadPool", "true")
				.initParam("org.atmosphere.cpr.broadcaster.maxProcessingThreads", "10")
				.initParam("org.atmosphere.cpr.broadcaster.maxAsyncWriteThreads", "10")
				.initParam("org.atmosphere.websocket.maxIdleTime", "10000")
				.initParam("org.atmosphere.cpr.Broadcaster.writeTimeout", "30000")
				.initParam("org.atmosphere.cpr.broadcasterLifeCyclePolicy", "EMPTY_DESTROY")
				.initParam("org.atmosphere.websocket.WebSocketProcessor",
						"nz.co.fortytwo.signalk.artemis.server.SignalkWebSocketProcessor")

				.port(8080).host("0.0.0.0").build()).build();

		server.start();

		if (Config.getConfigPropertyBoolean(START_TCP)||Config.getConfigPropertyBoolean(START_UDP)) {
			logger.info("Starting tcp/udp server");
			skServer = new NettyServer(null, OUTPUT_TCP);
			if (Config.getConfigPropertyBoolean(START_TCP)){
				skServer.setTcpPort(Config.getConfigPropertyInt(TCP_PORT));
			}else {
				skServer.setTcpPort(0);
			}
			if (Config.getConfigPropertyBoolean(START_UDP)){
				skServer.setUdpPort(Config.getConfigPropertyInt(UDP_PORT));
			}else {
				skServer.setUdpPort(0);
			}
			skServer.run();
		}

		if (Config.getConfigPropertyBoolean(GENERATE_NMEA0183)) {
			logger.info("Starting NMEA output");
			nmeaServer = new NettyServer(null, OUTPUT_NMEA);
			nmeaServer.setTcpPort(Config.getConfigPropertyInt(TCP_NMEA_PORT));
			nmeaServer.setUdpPort(Config.getConfigPropertyInt(UDP_NMEA_PORT));
			nmeaServer.run();
		}

		startMdns();
		startScheduledServices();
		ChartService.reloadCharts();
	}

	private void startScheduledServices() {
		logger.info("Starting scheduled services");
		//serial manager
		// start serial?
		if (Config.getConfigPropertyBoolean(ENABLE_SERIAL)) {
			// start a serial port manager
			if (serialPortManager == null) {
				serialPortManager = new SerialPortManager();
				scheduler.scheduleAtFixedRate(serialPortManager, 0, 30, TimeUnit.SECONDS);
			}

		}
		// declination
		final Runnable declination = new DeclinationUpdater();
		scheduler.scheduleAtFixedRate(declination, 0, 1, TimeUnit.HOURS);
		//system tokens
		final Runnable systemTokens = new Runnable() {
			
			@Override
			public void run() {
				try {
					SecurityUtils.validateTokenStore();
				} catch (Exception e) {
					logger.error(e,e);
				}
			}
		};
		scheduler.scheduleAtFixedRate(systemTokens, 1, 1, TimeUnit.HOURS);
	}

	private void ensureSecurityConf() {
		File secureConf = new File("./conf/security-conf.json");
		if (!secureConf.exists()) {
			try (InputStream in = getClass().getClassLoader().getResource("security-conf.json.default").openStream()) {
				String defaultSecurity = IOUtils.toString(in);
				SecurityUtils.save(defaultSecurity);
			} catch (Exception e) {
				logger.error(e, e);
			}

		} 
		// make sure we have system users serial, n2k, ais
		try {
			SecurityUtils.checkSystemUsers();
			
			//make sure all passwords are encrypted.
			Json conf = SecurityUtils.getSecurityConfAsJson();
			SecurityUtils.save(conf.toString());
		} catch (Exception e) {
			logger.error(e, e);
		}

	}

	private void startIncomingConsumer() throws Exception {
		if (session == null)
			session = Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
					Config.getConfigProperty(Config.ADMIN_PWD));
		if (consumer == null)
			consumer = session.createConsumer(Config.INCOMING_RAW);
		consumer.setMessageHandler(new MessageHandler() {

			@Override
			public void onMessage(ClientMessage message) {
				try {
					message.acknowledge();
				} catch (ActiveMQException e) {
					logger.error(e, e);
				}
				logger.debug("Acknowledge {}", message);

			}
		});
		session.start();

	}

	private void startKvHandlers() throws Exception {

		influxHandler = new InfluxDbHandler();
		influxHandler.startConsumer();
		trueWindHandler = new TrueWindHandler();
		trueWindHandler.startConsumer();
		anchorWatchHandler = new AnchorWatchHandler();
		anchorWatchHandler.startConsumer();
		alarmHandler = new AlarmHandler();
		alarmHandler.startConsumer();
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
		//stop scheduler
		scheduler.shutdown();
		//stop subscriptions
		SubscriptionManagerFactory.getInstance().stopAll();
		stopKvHandlers();
		if (consumer != null) {
			try {
				consumer.close();
			} catch (ActiveMQException e) {
				logger.error(e, e);
			}
		}
		if (session != null) {
			try {
				session.close();
			} catch (ActiveMQException e) {
				logger.error(e, e);
			}
		}
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

	private void stopKvHandlers() {
		if (influxHandler != null) {
			influxHandler.stop();
		}
		if (trueWindHandler != null) {
			trueWindHandler.stop();
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
				logger.info("Starting Zeroconf discovery agent");
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

	static {
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("log4j.configurationFile", "./conf/log4j2.json");
		System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "TRACE");

	}

	public static void main(String[] args) throws Exception {

		PropertyConfigurator.configure("./conf/log4j2.json");
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
		LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);

		File file = new File("./conf/log4j2.json");
		if (!file.exists()) {
			FileUtils.copyFile(new File("./conf/log4j2.json.sample"), file);
		}
		// this will force a reconfiguration
		context.setConfigLocation(file.toURI());

		new ArtemisServer();

	}

}
