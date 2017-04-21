package nz.co.fortytwo.signalk.artemis.util;

import java.io.File;
import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
	private static SortedMap<String, Json> map = new ConcurrentSkipListMap<>();

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
	public static final String JSON_FULL = "JSON_FULL";
	public static final String JSON_DELTA = "JSON_DELTA";
	public static final String JSON_SUBSCRIBE = "JSON_SUBSCRIBE";
	public static final String _0183 = "0183";
	public static final String AMQ_REPLY_Q = "AMQ_REPLY_Q";
	public static final String JAVA_TYPE = "JAVA_TYPE";
	public static final String SK_TYPE = "SK_TYPE";
	public static final String SK_TYPE_COMPOSITE = "SK_COMPOSITE";
	public static final String SK_TYPE_VALUE = "SK_VALUE";
	public static final String SK_TYPE_ATTRIBUTE = "SK_ATTRIBUTE";

	protected Config(){
		listener = new ConfigListener(map, (String) map.get(ADMIN_USER).asString(),
				(String) map.get(ADMIN_PWD).asString());
	}
	public static Config getInstance() {
		return config;
	}

	public static void startConfigListener(){
		if(listener!=null){
			Thread t = new Thread(listener);
			t.setDaemon(true);
			t.start();
			logger.info("Config listener started for user:" + map.get(ADMIN_USER));
		}
	}
	
	public static void stopConfigListener(){
		if(listener!=null){
			listener.stop();
		}
	}
	private Map<String, Json> getMap() {
		return map;
	}

	public static Json getDiscoveryMsg(String hostname) {
		
		Json version = Json.object();
		String ver = getConfigProperty(ConfigConstants.VERSION);
		version.set("version", ver);
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
		endpoints.set("v"+ver.substring(0, 1), version );
		return Json.object().set("endpoints", endpoints);
	}
	/**
	 * Config defaults
	 * 
	 * @param props
	 */
	public static void setDefaults(SortedMap<String, Json> model) {
		// populate sensible defaults here
		model.put(ConfigConstants.UUID, Json.make("self"));
		model.put(ConfigConstants.WEBSOCKET_PORT, Json.make(8080));
		model.put(ConfigConstants.REST_PORT, Json.make(8080));
		model.put(ConfigConstants.STORAGE_ROOT, Json.make("./storage/"));
		model.put(ConfigConstants.STATIC_DIR, Json.make("./signalk-static/"));
		model.put(ConfigConstants.MAP_DIR, Json.make("./mapcache/"));
		model.put(ConfigConstants.DEMO, Json.make(false));
		model.put(ConfigConstants.STREAM_URL, Json.make("motu.log"));
		model.put(ConfigConstants.USBDRIVE, Json.make("/media/usb0"));
		model.put(ConfigConstants.SERIAL_PORTS,
				Json.make("[\"/dev/ttyUSB0\",\"/dev/ttyUSB1\",\"/dev/ttyUSB2\",\"/dev/ttyACM0\",\"/dev/ttyACM1\",\"/dev/ttyACM2\"]"));
		if (SystemUtils.IS_OS_WINDOWS) {
			model.put(ConfigConstants.SERIAL_PORTS, Json.make("[\"COM1\",\"COM2\",\"COM3\",\"COM4\"]"));
		}
		model.put(ConfigConstants.SERIAL_PORT_BAUD, Json.make(38400));
		model.put(ConfigConstants.ENABLE_SERIAL, Json.make(true));
		model.put(ConfigConstants.TCP_PORT, Json.make(55555));
		model.put(ConfigConstants.UDP_PORT, Json.make(55554));
		model.put(ConfigConstants.TCP_NMEA_PORT, Json.make(55557));
		model.put(ConfigConstants.UDP_NMEA_PORT, Json.make(55556));
		model.put(ConfigConstants.STOMP_PORT, Json.make(61613));
		model.put(ConfigConstants.MQTT_PORT, Json.make(1883));
		model.put(ConfigConstants.CLOCK_source, Json.make("system"));
	
		model.put(ConfigConstants.HAWTIO_PORT, Json.make(8000));
		model.put(ConfigConstants.HAWTIO_AUTHENTICATE, Json.make(false));
		model.put(ConfigConstants.HAWTIO_CONTEXT, Json.make("/hawtio"));
		model.put(ConfigConstants.HAWTIO_WAR, Json.make("./hawtio/hawtio-default-offline-1.4.48.war"));
		model.put(ConfigConstants.HAWTIO_START, Json.make(false));
	
		model.put(ConfigConstants.JOLOKIA_PORT, Json.make(8001));
		model.put(ConfigConstants.JOLOKIA_AUTHENTICATE, Json.make(false));
		model.put(ConfigConstants.JOLOKIA_CONTEXT, Json.make("/jolokia"));
		model.put(ConfigConstants.JOLOKIA_WAR, Json.make("./hawtio/jolokia-war-1.3.3.war"));
	
		model.put(ConfigConstants.VERSION, Json.make("1.0.0"));
		model.put(ConfigConstants.ALLOW_INSTALL, Json.make(true));
		model.put(ConfigConstants.ALLOW_UPGRADE, Json.make(true));
		model.put(ConfigConstants.GENERATE_NMEA0183, Json.make(true));
		model.put(ConfigConstants.ZEROCONF_AUTO, Json.make(true));
		model.put(ConfigConstants.START_MQTT, Json.make(true));
		model.put(ConfigConstants.START_STOMP, Json.make(true));
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
			model.put(ConfigConstants.SECURITY_CONFIG, Json.make(ips.toString()));
	
			// default users
			model.put(ADMIN_USER, Json.make("admin"));
			model.put(ADMIN_PWD, Json.make("admin"));
	
		} catch (SocketException e) {
			Util.logger.error(e.getMessage(), e);
		}
	
	}
	public static SortedMap<String, Json> loadConfig(SortedMap<String, Json> model) throws IOException {
		File jsonFile = new File(Util.SIGNALK_CFG_SAVE_FILE);
		Util.logger.info("Checking for previous config: " + jsonFile.getAbsolutePath());
	
		if (!jsonFile.exists()) {
			Util.logger.info("   Saved config not found, creating default");
			Config.setDefaults(model);
			// write a new one for next time
			// create a uuid
			String self = SignalKConstants.URN_UUID + UUID.randomUUID().toString();
			model.put(ConfigConstants.UUID, Json.make(self));
			saveConfig(model);
	
		} else {
			Json json = Json.read(jsonFile.toURI().toURL());
			JsonSerializer ser = new JsonSerializer();
			model = ser.readToJsonMap(json);
		}
		return model;
	}

	public static Json load(String fileName) {
		File jsonFile = new File(fileName);
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
	
	/**
	 * Save the current state of the signalk config
	 * 
	 * @throws IOException
	 */
	public static void saveConfig(Map<String, Json> config) throws IOException {
		saveMap(config, new File(Util.SIGNALK_CFG_SAVE_FILE));
	}
	
	public static void saveMap(Map<String, Json> config, File jsonFile) throws IOException {
		if (config != null) {
			//de-json it
			SortedMap<String, Object> model = new ConcurrentSkipListMap<>();
			for(Entry<String, Json> e : config.entrySet()){
				model.put(e.getKey(), e.getValue().getValue());
			}
			JsonSerializer ser = new JsonSerializer();
			ser.setPretty(3);
			StringBuilder buffer = new StringBuilder();
			if (model != null && model.size() > 0) {
				ser.write(model.entrySet().iterator(), '.', buffer);
			} else {
				buffer.append("{}");
			}
			FileUtils.writeStringToFile(jsonFile, buffer.toString(), StandardCharsets.UTF_8);
			Util.logger.debug("   Saved model state to " + jsonFile);
		}
	
	}
	public static String getConfigProperty(String prop) {
		try {
			return (String) config.getMap().get(prop).getValue();
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		return null;
	}

	public static Json getConfigJsonArray(String prop) {
			return config.getMap().get(prop);
				
	}

	public static Integer getConfigPropertyInt(String prop) {
		try {
			return config.getMap().get(prop).asInteger();
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		return null;
		
	}

	public static Double getConfigPropertyDouble(String prop) {
		
		try {
			return config.getMap().get(prop).asDouble();
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		return null;
	}

	public static Boolean getConfigPropertyBoolean(String prop) {
		try {
			return config.getMap().get(prop).asBoolean();
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		return null;
	}

	private class ConfigListener implements Runnable {

		private String user;
		private String password;
		private boolean saved = true;
		private boolean running = true;

		public ConfigListener(SortedMap<String, Json> map, String user, String password) {
			this.user = user;
			this.password = password;
		}
		
		public void stop(){
			running=false;
		}

		@Override
		public void run() {
			ClientSession rxSession = null;
			ClientConsumer consumer = null;
			try {
				// start polling consumer.
				rxSession = Util.getVmSession(user, password);
				consumer = rxSession.createConsumer("config", "_AMQ_LVQ_NAME like 'config.%'", true);

				while (running) {
					ClientMessage msgReceived = consumer.receive(1000);
					if (msgReceived == null){
						//when no changes every 1 seconds we will get a null message, so save first time if we need to.
						if(!saved){
							if (logger.isDebugEnabled())
								logger.debug("ConfigListener: saving config");
							Config.saveConfig(map);
							saved=true;
						}
						continue;
					}
					//if we have changes, we read until there are more and trigger a save later.
					Json json = Util.readBodyBuffer(msgReceived);
					if (logger.isDebugEnabled())
						logger.debug("ConfigListener: message = " + msgReceived.getMessageID() + ":" + msgReceived.getAddress() + ", " + json);
					map.put(msgReceived.getAddress().toString(), json);
					saved=false;
					
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
