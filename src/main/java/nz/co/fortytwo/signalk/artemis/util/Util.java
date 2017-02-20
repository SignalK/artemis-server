package nz.co.fortytwo.signalk.artemis.util;

import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.self;
import static nz.co.fortytwo.signalk.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels_dot_self;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels_dot_self_dot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.util.JsonSerializer;

public class Util {

	private static Logger logger = LogManager.getLogger(Util.class);
	private static Pattern selfMatch = Pattern.compile("\\.self\\.");
	private static Pattern selfEndMatch = Pattern.compile("\\.self$");
	private static final String SIGNALK_MODEL_SAVE_FILE = "./conf/self.json";
	private static final String SIGNALK_CFG_SAVE_FILE = "./conf/signalk-config.json";

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

	public static void sendMsg(String key, Object ts, ServerSession sess) throws Exception {

		ServerMessage m2 = new ServerMessageImpl(new Double(Math.random()).longValue(), 64);
		m2.writeBodyBufferString(ts.toString());
		m2.setAddress(new SimpleString(key));
		m2.putStringProperty("_AMQ_LVQ_NAME", key);
		// m2.putStringProperty(Message.HDR_VALIDATED_USER.toString(),
		// sess.getUsername());

		// if(logger.isDebugEnabled())logger.debug("Processing dup:
		// user="+sess.getUsername()+", " + m2);
		try {
			sess.send(m2, true);
		} catch (ActiveMQSecurityException se) {
			logger.warn(se.getMessage());
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		}
	}
}
