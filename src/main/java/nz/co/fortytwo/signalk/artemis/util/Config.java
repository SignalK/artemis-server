package nz.co.fortytwo.signalk.artemis.util;

import java.io.File;
import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.util.ConfigConstants;
import nz.co.fortytwo.signalk.util.JsonSerializer;
import nz.co.fortytwo.signalk.util.SignalKConstants;

/**
 * Listen to the config.* queues and cache the results for easy access.
 * 
 * @author robert
 *
 */
public class Config {

	public static final String ADMIN_USER = "config.server.admin.user";
	public static final String ADMIN_PWD = "config.server.admin.password";

	private static Logger logger = LogManager.getLogger(Config.class);

	private static ConfigListener listener;
	private static SortedMap<String, Object> map = new ConcurrentSkipListMap<>();

	private static Config config = null;

	static {
		try {
			map = Config.loadConfig(map);
			config=new Config();
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	public static final String _AMQ_LVQ_NAME = "_AMQ_LVQ_NAME";
	public static final String AMQ_CONTENT_TYPE = "AMQ_content_type";
	public static final String AMQ_SESSION_ID = "AMQ_session_id";
	public static final String JSON = "JSON";
	public static final String _0183 = "0183";
	public static final String AMQ_REPLY_Q = "AMQ_REPLY_Q";
	public static final String JAVA_TYPE = "JAVA_TYPE";
	public static final String SK_TYPE = "SK_TYPE";
	public static final String SK_TYPE_COMPOSITE = "SK_COMPOSITE";
	public static final String SK_TYPE_VALUE = "SK_VALUE";
	public static final String SK_TYPE_ATTRIBUTE = "SK_ATTRIBUTE";

	protected Config(){
		listener = new ConfigListener(map, (String) map.get(ADMIN_USER),
				(String) map.get(ADMIN_PWD));
		logger.info("Config listener started for user:" + map.get(ADMIN_USER));
	}
	public static Config getInstance() {
		return config;
	}

	private Map<String, Object> getMap() {
		return map;
	}

	public static Json getDiscoveryMsg(String hostname) {
		
		Json version = Json.object();
		String ver = getConfigProperty(ConfigConstants.VERSION);
		version.set("version", ver.substring(1));
		version.set(SignalKConstants.websocketUrl, "ws://" + hostname + ":"
				+ getConfigPropertyInt(ConfigConstants.WEBSOCKET_PORT)
				+ SignalKConstants.SIGNALK_WS);
		version.set(SignalKConstants.restUrl, "http://" + hostname + ":"
				+ getConfigPropertyInt(ConfigConstants.REST_PORT)
				+ SignalKConstants.SIGNALK_API + "/");
		version.set(SignalKConstants.signalkTcpPort, "tcp://" + hostname + ":"
				+ getConfigPropertyInt(ConfigConstants.TCP_PORT));
		version.set(SignalKConstants.signalkUdpPort, "udp://" + hostname + ":"
				+ getConfigPropertyInt(ConfigConstants.UDP_PORT));
		version.set(SignalKConstants.nmeaTcpPort, "tcp://" + hostname + ":"
				+ getConfigPropertyInt(ConfigConstants.TCP_NMEA_PORT));
		version.set(SignalKConstants.nmeaUdpPort, "udp://" + hostname + ":"
				+ getConfigPropertyInt(ConfigConstants.UDP_NMEA_PORT));
		if (getConfigPropertyBoolean(ConfigConstants.START_STOMP))
			version.set(SignalKConstants.stompPort, "stomp+nio://" + hostname + ":"
					+ getConfigPropertyInt(ConfigConstants.STOMP_PORT));
		if (getConfigPropertyBoolean(ConfigConstants.START_MQTT))
			version.set(SignalKConstants.mqttPort, "mqtt://" + hostname + ":"
					+ getConfigPropertyInt(ConfigConstants.MQTT_PORT));
		Json endpoints = Json.object();
		endpoints.set(ver.substring(0, 2), version );
		return Json.object().set("endpoints", endpoints);
	}
	/**
	 * Config defaults
	 * 
	 * @param props
	 */
	public static void setDefaults(SortedMap<String, Object> model) {
		// populate sensible defaults here
		model.put(ConfigConstants.UUID, "self");
		model.put(ConfigConstants.WEBSOCKET_PORT, 8080);
		model.put(ConfigConstants.REST_PORT, 8080);
		model.put(ConfigConstants.STORAGE_ROOT, "./storage/");
		model.put(ConfigConstants.STATIC_DIR, "./signalk-static/");
		model.put(ConfigConstants.MAP_DIR, "./mapcache/");
		model.put(ConfigConstants.DEMO, false);
		model.put(ConfigConstants.STREAM_URL, "motu.log");
		model.put(ConfigConstants.USBDRIVE, "/media/usb0");
		model.put(ConfigConstants.SERIAL_PORTS,
				"[\"/dev/ttyUSB0\",\"/dev/ttyUSB1\",\"/dev/ttyUSB2\",\"/dev/ttyACM0\",\"/dev/ttyACM1\",\"/dev/ttyACM2\"]");
		if (SystemUtils.IS_OS_WINDOWS) {
			model.put(ConfigConstants.SERIAL_PORTS, "[\"COM1\",\"COM2\",\"COM3\",\"COM4\"]");
		}
		model.put(ConfigConstants.SERIAL_PORT_BAUD, 38400);
		model.put(ConfigConstants.ENABLE_SERIAL, true);
		model.put(ConfigConstants.TCP_PORT, 55555);
		model.put(ConfigConstants.UDP_PORT, 55554);
		model.put(ConfigConstants.TCP_NMEA_PORT, 55557);
		model.put(ConfigConstants.UDP_NMEA_PORT, 55556);
		model.put(ConfigConstants.STOMP_PORT, 61613);
		model.put(ConfigConstants.MQTT_PORT, 1883);
		model.put(ConfigConstants.CLOCK_source, "system");
	
		model.put(ConfigConstants.HAWTIO_PORT, 8000);
		model.put(ConfigConstants.HAWTIO_AUTHENTICATE, false);
		model.put(ConfigConstants.HAWTIO_CONTEXT, "/hawtio");
		model.put(ConfigConstants.HAWTIO_WAR, "./hawtio/hawtio-default-offline-1.4.48.war");
		model.put(ConfigConstants.HAWTIO_START, false);
	
		model.put(ConfigConstants.JOLOKIA_PORT, 8001);
		model.put(ConfigConstants.JOLOKIA_AUTHENTICATE, false);
		model.put(ConfigConstants.JOLOKIA_CONTEXT, "/jolokia");
		model.put(ConfigConstants.JOLOKIA_WAR, "./hawtio/jolokia-war-1.3.3.war");
	
		model.put(ConfigConstants.VERSION, "1.0.0");
		model.put(ConfigConstants.ALLOW_INSTALL, true);
		model.put(ConfigConstants.ALLOW_UPGRADE, true);
		model.put(ConfigConstants.GENERATE_NMEA0183, true);
		model.put(ConfigConstants.ZEROCONF_AUTO, true);
		model.put(ConfigConstants.START_MQTT, true);
		model.put(ConfigConstants.START_STOMP, true);
		// control config, only local networks
		Json ips = Json.array();
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
	
			while (interfaces.hasMoreElements()) {
				NetworkInterface i = interfaces.nextElement();
				for (InterfaceAddress iAddress : i.getInterfaceAddresses()) {
					// ignore IPV6 for now.
					if (iAddress.getAddress().getAddress().length > 4)
						continue;
					ips.add(iAddress.getAddress().getHostAddress() + "/" + iAddress.getNetworkPrefixLength());
				}
			}
			model.put(ConfigConstants.SECURITY_CONFIG, ips.toString());
	
			// default users
			model.put(ADMIN_USER, "admin");
			model.put(ADMIN_PWD, "admin");
	
		} catch (SocketException e) {
			Util.logger.error(e.getMessage(), e);
		}
	
	}
	public static SortedMap<String, Object> loadConfig(SortedMap<String, Object> model) throws IOException {
		File jsonFile = new File(Util.SIGNALK_CFG_SAVE_FILE);
		Util.logger.info("Checking for previous config: " + jsonFile.getAbsolutePath());
	
		if (!jsonFile.exists()) {
			Util.logger.info("   Saved config not found, creating default");
			Config.setDefaults(model);
			// write a new one for next time
			// create a uuid
			String self = SignalKConstants.URN_UUID + UUID.randomUUID().toString();
			model.put(ConfigConstants.UUID, self);
			saveConfig(model);
	
		} else {
			Json json = Json.read(jsonFile.toURI().toURL());
			JsonSerializer ser = new JsonSerializer();
			model = ser.read(json);
		}
		return model;
	}
	/**
	 * Save the current state of the signalk config
	 * 
	 * @throws IOException
	 */
	public static void saveConfig(Map<String, Object> config) throws IOException {
		saveConfig(config, new File(Util.SIGNALK_CFG_SAVE_FILE));
	}
	public static Json load() {
		File jsonFile = new File(Util.SIGNALK_MODEL_SAVE_FILE);
		Util.logger.info("Checking for previous state: " + jsonFile.getAbsolutePath());
		if (jsonFile.exists()) {
			try {
				Json temp = Json.read(jsonFile.toURI().toURL());
				Util.logger.info("   Saved state loaded from " + Util.SIGNALK_MODEL_SAVE_FILE);
				return temp;
			} catch (Exception ex) {
				Util.logger.error(ex.getMessage());
			}
		} else {
			Util.logger.info("   Saved state not found");
		}
		return Json.nil();
	}
	public static void saveConfig(Map<String, Object> config, File jsonFile) throws IOException {
		if (config != null) {
	
			JsonSerializer ser = new JsonSerializer();
			ser.setPretty(3);
			StringBuilder buffer = new StringBuilder();
			if (config != null && config.size() > 0) {
				ser.write(config.entrySet().iterator(), '.', buffer);
			} else {
				buffer.append("{}");
			}
			FileUtils.writeStringToFile(jsonFile, buffer.toString(), StandardCharsets.UTF_8);
			Util.logger.debug("   Saved model state to " + Util.SIGNALK_CFG_SAVE_FILE);
		}
	
	}
	public static String getConfigProperty(String prop) {
		try {
			return (String) config.getMap().get(prop);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static Json getConfigJsonArray(String prop) {
		try {
			String arrayStr = (String) config.getMap().get(prop);
			if (StringUtils.isNotBlank(arrayStr) && arrayStr.length() > 2) {
				Json array = Json.read(arrayStr);
				return array;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static Integer getConfigPropertyInt(String prop) {
		try {
			if (config.getMap().get(prop) instanceof String) {
				return (Integer.valueOf((String) config.getMap().get(prop)));
			}
			if (config.getMap().get(prop) instanceof Number) {
				return ((Number) config.getMap().get(prop)).intValue();
			}

			return (Integer) config.getMap().get(prop);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static Double getConfigPropertyDouble(String prop) {
		try {
			if (config.getMap().get(prop) instanceof String) {
				return (Double.valueOf((String) config.getMap().get(prop)));
			}
			return (Double) config.getMap().get(prop);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static Boolean getConfigPropertyBoolean(String prop) {
		try {
			if (config.getMap().get(prop) instanceof Boolean) {
				return ((Boolean) config.getMap().get(prop));
			}
			return new Boolean((String) config.getMap().get(prop));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	private class ConfigListener implements Runnable {

		private String user;
		private String password;

		public ConfigListener(SortedMap<String, Object> map, String user, String password) {
			this.user = user;
			this.password = password;
		}

		@Override
		public void run() {
			ClientSession rxSession = null;
			ClientConsumer consumer = null;
			try {
				// start polling consumer.
				rxSession = Util.getVmSession(user, password);
				consumer = rxSession.createConsumer("vessels", "_AMQ_LVQ_NAME like 'config.%'", true);

				while (true) {
					ClientMessage msgReceived = consumer.receive(100000);
					if (msgReceived == null)
						continue;
					if (logger.isDebugEnabled())
						logger.debug("message = " + msgReceived.getMessageID() + ":" + msgReceived.getAddress() + ", "
								+ msgReceived.getBodyBuffer().readString());
					map.put(msgReceived.getAddress().toString(), msgReceived.getBodyBuffer().readNullableString());
				}

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (consumer != null) {
					try {
						consumer.close();
					} catch (ActiveMQException e) {
						logger.error(e);
					}
				}
				if (rxSession != null) {
					try {
						rxSession.close();
					} catch (ActiveMQException e) {
						logger.error(e);
					}
				}
			}

		}
	}

}
