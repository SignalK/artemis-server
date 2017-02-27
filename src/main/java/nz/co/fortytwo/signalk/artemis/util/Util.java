package nz.co.fortytwo.signalk.artemis.util;

import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.self;
import static nz.co.fortytwo.signalk.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels_dot_self;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels_dot_self_dot;

import java.io.File;
import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.ActiveMQSecurityException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.impl.ServerMessageImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.util.ConfigConstants;
import nz.co.fortytwo.signalk.util.JsonSerializer;
import nz.co.fortytwo.signalk.util.SignalKConstants;


public class Util {

	private static Logger logger = LogManager.getLogger(Util.class);
	private static Pattern selfMatch = Pattern.compile("\\.self\\.");
	private static Pattern selfEndMatch = Pattern.compile("\\.self$");
	private static final String SIGNALK_MODEL_SAVE_FILE = "./conf/self.json";
	public static final String SIGNALK_CFG_SAVE_FILE = "./conf/signalk-config.json";

	public static String fixSelfKey(String key) {
		key = selfMatch.matcher(key).replaceAll(dot + self + dot);
		key = selfEndMatch.matcher(key).replaceAll(dot + self);
		return key;
	}

	public static String sanitizePath(String newPath) {
		newPath = newPath.replace('/', '.');
		if (newPath.startsWith(dot))
			newPath = newPath.substring(1);

		if ((vessels + dot + self_str).equals(newPath)) {
			newPath = vessels_dot_self;
		}
		newPath = newPath.replace(vessels + dot + self_str + dot, vessels_dot_self_dot);
		return newPath;
	}

	public static Pattern regexPath(String newPath) {
		// regex it
		String regex = newPath.replaceAll(".", "[$0]").replace("[*]", ".*").replace("[?]", ".");
		return Pattern.compile(regex);
	}

	public static ClientSession getVmSession(String user, String password) throws Exception {
		ClientSessionFactory nettyFactory = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
				.createSessionFactory();
		return nettyFactory.createSession(user, password, false, true, true, false, 10);
	}

	public static ClientSession getLocalhostClientSession(String user, String password) throws Exception {
		Map<String, Object> connectionParams = new HashMap<String, Object>();
		connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
		connectionParams.put(TransportConstants.PORT_PROP_NAME, 61617);

		ClientSessionFactory nettyFactory = ActiveMQClient
				.createServerLocatorWithoutHA(
						new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams))
				.createSessionFactory();
		return nettyFactory.createSession(user, password, false, true, true, false, 10);
	}

	/**
	 * Save the current state of the signalk config
	 * 
	 * @throws IOException
	 */
	public static void saveConfig(Map<String, Object> config) throws IOException {
		saveConfig(config, new File(SIGNALK_CFG_SAVE_FILE));
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
			logger.debug("   Saved model state to " + SIGNALK_CFG_SAVE_FILE);
		}

	}
	
	public static Json load(){
		File jsonFile = new File(SIGNALK_MODEL_SAVE_FILE);
		logger.info("Checking for previous state: "+jsonFile.getAbsolutePath());
		if(jsonFile.exists()){
			try{
				Json temp = Json.read(jsonFile.toURI().toURL());
				logger.info("   Saved state loaded from "+SIGNALK_MODEL_SAVE_FILE);
				return temp;
			}catch(Exception ex){
				logger.error(ex.getMessage());
			}
		}else{
			logger.info("   Saved state not found");
		}
		return Json.nil();
	}
	public static SortedMap<String, Object> loadConfig(SortedMap<String, Object> model) throws IOException{
		File jsonFile = new File(SIGNALK_CFG_SAVE_FILE);
		logger.info("Checking for previous config: "+jsonFile.getAbsolutePath());
		
		if(!jsonFile.exists()){
			logger.info("   Saved config not found, creating default");
			setDefaults(model);
			//write a new one for next time
			//create a uuid
			String self = SignalKConstants.URN_UUID+UUID.randomUUID().toString();
			model.put(ConfigConstants.UUID, self);
			saveConfig(model);
			
		}else{
			Json json = Json.read(jsonFile.toURI().toURL());
			JsonSerializer ser = new JsonSerializer();
			model = ser.read(json);
		}
		return model;
	}

	/**
	 * Config defaults
	 * 
	 * @param props
	 */
	public static void setDefaults(SortedMap<String, Object> model) {
		// populate sensible defaults here
		model.put(ConfigConstants.UUID, "self");
		model.put(ConfigConstants.WEBSOCKET_PORT, 3000);
		model.put(ConfigConstants.REST_PORT, 8080);
		model.put(ConfigConstants.STORAGE_ROOT, "./storage/");
		model.put(ConfigConstants.STATIC_DIR, "./signalk-static/");
		model.put(ConfigConstants.MAP_DIR, "./mapcache/");
		model.put(ConfigConstants.DEMO, false);
		model.put(ConfigConstants.STREAM_URL,
				"motu.log");
		model.put(ConfigConstants.USBDRIVE, "/media/usb0");
		model.put(
				ConfigConstants.SERIAL_PORTS,
				"[\"/dev/ttyUSB0\",\"/dev/ttyUSB1\",\"/dev/ttyUSB2\",\"/dev/ttyACM0\",\"/dev/ttyACM1\",\"/dev/ttyACM2\"]");
		if (SystemUtils.IS_OS_WINDOWS) {
			model.put(ConfigConstants.SERIAL_PORTS,
					"[\"COM1\",\"COM2\",\"COM3\",\"COM4\"]");
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
		model.put(ConfigConstants.HAWTIO_WAR,
				"./hawtio/hawtio-default-offline-1.4.48.war");
		model.put(ConfigConstants.HAWTIO_START, false);
		
		model.put(ConfigConstants.JOLOKIA_PORT, 8001);
		model.put(ConfigConstants.JOLOKIA_AUTHENTICATE, false);
		model.put(ConfigConstants.JOLOKIA_CONTEXT, "/jolokia");
		model.put(ConfigConstants.JOLOKIA_WAR,
				"./hawtio/jolokia-war-1.3.3.war");
		
		model.put(ConfigConstants.VERSION, "v1.0.0");
		model.put(ConfigConstants.ALLOW_INSTALL, true);
		model.put(ConfigConstants.ALLOW_UPGRADE, true);
		model.put(ConfigConstants.GENERATE_NMEA0183, true);
		model.put(ConfigConstants.ZEROCONF_AUTO, true);
		model.put(ConfigConstants.START_MQTT, true);
		model.put(ConfigConstants.START_STOMP, true);
		//control config, only local networks
		Json ips = Json.array();
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
			
			while(interfaces.hasMoreElements()){
				NetworkInterface i = interfaces.nextElement();
				for( InterfaceAddress iAddress :i.getInterfaceAddresses()){
					//ignore IPV6 for now.
					if(iAddress.getAddress().getAddress().length>4)continue;
					ips.add(iAddress.getAddress().getHostAddress()+"/"+ iAddress.getNetworkPrefixLength());
				}
			}
			model.put(ConfigConstants.SECURITY_CONFIG, ips.toString());
			
			//default users
			model.put(Config.ADMIN_USER, "admin");
			model.put(Config.ADMIN_PWD, "admin");
			
		} catch (SocketException e) {
			logger.error(e.getMessage(),e);
		}

	}
	public static void sendMsg(String key, Object ts, ServerSession sess) throws Exception {

		ServerMessage m2 = new ServerMessageImpl(new Double(Math.random()).longValue(), 64);
		m2.writeBodyBufferString(ts.toString());
		m2.setAddress(new SimpleString(key));
		m2.putStringProperty("_AMQ_LVQ_NAME", key);
		
		try {
			sess.send(m2, true);
		} catch (ActiveMQSecurityException se) {
			logger.warn(se.getMessage());
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		}
	}
}
