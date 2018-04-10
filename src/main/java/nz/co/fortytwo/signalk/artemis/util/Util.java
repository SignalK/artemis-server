package nz.co.fortytwo.signalk.artemis.util;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQPropertyConversionException;
import org.apache.activemq.artemis.api.core.ActiveMQSecurityException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import mjson.Json;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants.*;
import nz.co.fortytwo.signalk.artemis.util.JsonSerializer;

public class Util {

	static Logger logger = LogManager.getLogger(Util.class);
	// private static Pattern selfMatch = Pattern.compile("\\.self\\.");
	// private static Pattern selfEndMatch = Pattern.compile("\\.self$");
	public static final String SIGNALK_MODEL_SAVE_FILE = "./conf/self.json";
	public static final String SIGNALK_CFG_SAVE_FILE = "./conf/signalk-config.json";
	public static final String SIGNALK_RESOURCES_SAVE_FILE = "./conf/resources.json";
	public static final String SIGNALK_SOURCES_SAVE_FILE = "./conf/sources.json";
	private static boolean timeSet = false;
	
	private static ServerLocator nettyLocator;
	private static ServerLocator inVmLocator;
	protected static Pattern selfMatch = Pattern.compile("\\.self\\.|\\.self$");

	static {
		try {
			inVmLocator = ActiveMQClient
					.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
					.setMinLargeMessageSize(1024 * 1024);
			// .createSessionFactory();
			Map<String, Object> connectionParams = new HashMap<String, Object>();
			connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
			connectionParams.put(TransportConstants.PORT_PROP_NAME, 61617);
			nettyLocator = ActiveMQClient
					.createServerLocatorWithoutHA(
							new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams))
					.setMinLargeMessageSize(1024 * 1024);
			// .createSessionFactory();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Json getWelcomeMsg() {
		Json msg = Json.object();
		msg.set(version, Config.getVersion());
		msg.set(timestamp, getIsoTimeString());
		msg.set(self_str, Config.getConfigProperty(ConfigConstants.UUID));
		return msg;
	}

	   /**
     * Convert a speed in knots to meters/sec
     *
     * @param speed in knots
     * @return speed in m/s
     */
    public static double kntToMs(double speed) {
        return speed * KNOTS_TO_MS;
    }

    /**
     * Convert a speed in meter/sec to knots
     *
     * @param speed in m/s
     * @return speed in knots
     */
    public static double msToKnts(double speed) {
        return speed * MS_TO_KNOTS;
    }

    /**
     * Convert a distance in fathoms to meters
     *
     * @param fathoms
     * @return distance in meters
     */
    public static double fToM(double fathoms) {
        return fathoms / SignalKConstants.MTR_TO_FATHOM;
    }

    public static double cToFahr(double c) {
        return c * (9. / 5.) * c + 32.;
    }

    public static double fahrToC(double f) {
        return (f - 32.) * 5. / 9.;
    }
    
    /**
     * Convert a distance in ft to meters
     *
     * @param feet
     * @return distance in meters
     */
    public static double ftToM(double feet) {
        return feet / SignalKConstants.MTR_TO_FEET;
    }
    
	/**
	 * If we receive messages for 'self, convert to our UUID
	 * 
	 * @param key
	 * @return
	 */
	public static String fixSelfKey(String key) {
		if (selfMatch == null) {
			selfMatch = Pattern.compile("\\." + Config.getConfigProperty(ConfigConstants.UUID) + "\\.|\\." + Config.getConfigProperty(ConfigConstants.UUID) + "$");
		}
		key = selfMatch.matcher(key).replaceAll(dot+Config.getConfigProperty(ConfigConstants.UUID)+dot);

		return key;
	}

	public static ClientSession getVmSession(String user, String password) throws Exception {

		return inVmLocator.createSessionFactory().createSession(user, password, false, true, true, false, 10);
	}

	public static ClientSession getLocalhostClientSession(String user, String password) throws Exception {

		return nettyLocator.createSessionFactory().createSession(user, password, false, true, true, false, 10);
	}

	public static void sendDoubleAsMsg(String key, double value, String timeStamp, String srcRef, ServerSession session)
			throws Exception {
		if (StringUtils.isNotBlank(srcRef)) {
			sendObjMsg(key + dot + values + dot + srcRef, Json.make(value), timeStamp, srcRef, session);
		} else {
			sendObjMsg(key, Json.make(value), timeStamp, srcRef, session);
		}
	}

	public static SortedMap<String, Object> readAllMessages(String user, String password, String queue, String filter)
			throws Exception {
		SortedMap<String, Object> msgs = null;
		ClientSession rxSession = null;
		ClientConsumer consumer = null;
		try {
			// start polling consumer.
			rxSession = Util.getVmSession(user, password);
			rxSession.start();
			consumer = rxSession.createConsumer(queue, filter, true);

			msgs = readAllMessages(consumer);// new
												// ConcurrentSkipListMap<>();
			consumer.close();

		} catch (ActiveMQException e) {
			logger.error(e);
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
		if (msgs != null) {
			return msgs;
		}
		return new ConcurrentSkipListMap<String, Object>();
	}

	public static void sendRawMessage(String user, String password, String content) throws Exception {
		ClientSession txSession = null;
		ClientProducer producer = null;
		try {
			// start polling consumer.
			txSession = Util.getVmSession(user, password);
			Message message = txSession.createMessage(false);
			message.getBodyBuffer().writeString(content);
			producer = txSession.createProducer();
			producer.send(Config.INCOMING_RAW, message);
		} finally {
			if (producer != null) {
				try {
					producer.close();
				} catch (ActiveMQException e) {
					logger.error(e);
				}
			}
			if (txSession != null) {
				try {
					txSession.close();
				} catch (ActiveMQException e) {
					logger.error(e);
				}
			}
		}
	}

	public static void sendMsg(String key, Json body, String timeStamp, String srcRef, ServerSession sess)
			throws Exception {
		if (StringUtils.isNotBlank(srcRef) && !key.contains(values)) {
			sendObjMsg(key + dot + values + dot + srcRef, body, timeStamp, srcRef, sess);
		} else {
			sendObjMsg(key, body, timeStamp, srcRef, sess);
		}
	}

	public static void sendMsg(String key, Json body, String timeStamp, Json src, ServerSession sess) throws Exception {
		if (src != null && !src.isNull()) {
			String srclabel = src.at(label).asString();
			if (srclabel.startsWith(sources))
				srclabel = srclabel.substring(sources.length() + 1);
			sendObjMsg(key + dot + values + dot + srclabel, body, timeStamp, src, sess);
		} else {
			sendObjMsg(key, body, timeStamp, src, sess);
		}
	}

	private static void sendObjMsg(String key, Json body, String timeStamp, Object src, ServerSession sess)
			throws Exception {
		try {
			Message m2 = getMessage(key, body, timeStamp, src);
			sess.send(m2, true);
		} catch (ActiveMQSecurityException se) {
			logger.warn(se.getMessage());
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		}
	}

	protected static Message getMessage(String key, Json body, String timeStamp, Object src) {
		ClientMessage m2 = new ClientMessageImpl((byte) 0, false, 0, System.currentTimeMillis(), (byte) 4, 1024);
		if (StringUtils.isNotBlank(timeStamp))
			m2.putStringProperty(timestamp, timeStamp);
		if (src != null) {
			if (src instanceof String) {
				m2.putStringProperty(sourceRef, src.toString());
			} else {
				m2.putStringProperty(source, src.toString());
			}
		}
		String type = body.getClass().getSimpleName();
		m2.putStringProperty(Config.JAVA_TYPE, type);

		switch (type) {
		case "NullJson":
			m2.getBodyBuffer().writeString(body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			break;
		case "BooleanJson":
			m2.getBodyBuffer().writeString(body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			break;
		case "StringJson":
			m2.getBodyBuffer().writeString(body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			break;
		case "NumberJson":
			m2.getBodyBuffer().writeString(body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			break;
		case "Json":
			m2.getBodyBuffer().writeString(body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_COMPOSITE);
			break;
		case "ObjectJson":
			m2.getBodyBuffer().writeString(body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_COMPOSITE);
			break;
		case "ArrayJson":
			m2.getBodyBuffer().writeString(body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_COMPOSITE);
			break;
		default:
			logger.error("Unknown Json Class type: " + type);
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			m2.getBodyBuffer().writeString(body.toString());
			break;
		}

		m2.setAddress(new SimpleString(key));
		m2.putStringProperty(Config._AMQ_LVQ_NAME, key);
		return m2;
	}

	public static void sendSourceMsg(String key, String src, String now, ServerSession sess) throws Exception {
		sendObjMsg("sources." + key, Json.read(src), now, null, sess);

	}

	public static void sendSourceMsg(String key, Json src, String now, ServerSession sess) throws Exception {
		sendObjMsg("sources." + key, src, now, null, sess);

	}
	
    /**
     * Attempt to set the system time using the GPS time
     *
     * @param sen
     */
    @SuppressWarnings("deprecation")
    public static void checkTime(RMCSentence sen) {
        if (timeSet) {
            return;
        }
        try {
            net.sf.marineapi.nmea.util.Date dayNow = sen.getDate();
            // if we need to set the time, we will be WAAYYY out
            // we only try once, so we dont get lots of native processes
            // spawning if we fail
            timeSet = true;
            Date date = new Date();
            if ((date.getYear() + 1900) == dayNow.getYear()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Current date is " + date);
                }
                return;
            }
            // so we need to set the date and time
            net.sf.marineapi.nmea.util.Time timeNow = sen.getTime();
            String yy = String.valueOf(dayNow.getYear());
            String MM = pad(2, String.valueOf(dayNow.getMonth()));
            String dd = pad(2, String.valueOf(dayNow.getDay()));
            String hh = pad(2, String.valueOf(timeNow.getHour()));
            String mm = pad(2, String.valueOf(timeNow.getMinutes()));
            String ss = pad(2, String.valueOf(timeNow.getSeconds()));
            if (logger.isDebugEnabled()) {
                logger.debug("Setting current date to " + dayNow + " "
                    + timeNow);
            }
            String cmd = "sudo date --utc " + MM + dd + hh + mm + yy + "." + ss;
            Runtime.getRuntime().exec(cmd.split(" "));// MMddhhmm[[yy]yy]
            if (logger.isDebugEnabled()) {
                logger.debug("Executed date setting command:" + cmd);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }


    /**
     * pad the value to i places, eg 2 >> 02
     *
     * @param i
     * @param valueOf
     * @return
     */
    private static String pad(int i, String value) {
        while (value.length() < i) {
            value = "0" + value;
        }
        return value;
    }
	public static String sanitizePath(String newPath) {
		newPath = newPath.replace('/', '.');
		if (newPath.startsWith(dot)) {
			newPath = newPath.substring(1);
		}
		if (newPath.endsWith("*") || newPath.endsWith("?")) {
			newPath = newPath.substring(0, newPath.length() - 1);
		}

		return newPath;
	}

	/**
	 * Input is a list of message wrapped in a stack of hashMaps, eg Key=context
	 * Key=timestamp key=source List(messages) The method iterates through and
	 * creates the deltas as a Json array, one Json delta per context.
	 * 
	 * @param msgs
	 * @return
	 */
	public static Json generateDelta(Map<String, Map<String, Map<String, List<ClientMessage>>>> msgs) {
		Json deltaArray = Json.array();
		// add values
		if (msgs.size() == 0)
			return deltaArray;
		// each timestamp
		Json delta = Json.object();
		deltaArray.add(delta);

		Json updatesArray = Json.array();
		delta.set(UPDATES, updatesArray);

		for (String ctx : msgs.keySet()) {

			for (String ts : msgs.get(ctx).keySet()) {
				for (String src : msgs.get(ctx).get(ts).keySet()) {
					// new values object

					// make wrapper object
					Json valObj = Json.object();
					updatesArray.add(valObj);

					Json valuesArray = Json.array();
					valObj.at(values, valuesArray);
					valObj.set(timestamp, ts);
					valObj.set(sourceRef, src);

					// now the values
					for (ClientMessage msg : msgs.get(ctx).get(ts).get(src)) {
						String key = msg.getAddress().toString().substring(ctx.length() + 1);
						if (key.contains(dot + values + dot))
							key = key.substring(0, key.indexOf(dot + values + dot));
						Json v = Util.readBodyBuffer(msg);
						if (logger.isDebugEnabled())
							logger.debug("Key: " + key + ", value: " + v);
						Json val = Json.object(PATH, key);
						val.set(value, v);
						if (v.isObject())
							v.delAt(timestamp);
						if (v.isObject())
							v.delAt(sourceRef);
						valuesArray.add(val);
					}
				}
				// add context
			}
			delta.set(CONTEXT, ctx);
		}

		return deltaArray;
	}

	public static Json readBodyBuffer(ICoreMessage msg) {
		if (msg.getBodyBuffer().readableBytes() == 0) {
			if (logger.isDebugEnabled())
				logger.debug("Empty msg: " + msg.getAddress() + ": " + msg.getBodyBuffer().readableBytes());
			return Json.nil();
		}
		return Json.read(readBodyBufferToString(msg));

	}

	public static String readBodyBufferToString(ICoreMessage msg) {
		if (msg.getBodyBuffer().readableBytes() == 0) {
			return null;
		} else {
			return msg.getBodyBuffer().duplicate().readString();
		}

	}

	public static Map<String, Map<String, Map<String, List<ClientMessage>>>> readAllMessagesForDelta(
			ClientConsumer consumer) throws ActiveMQPropertyConversionException, ActiveMQException {
		ClientMessage msgReceived = null;
		Map<String, Map<String, Map<String, List<ClientMessage>>>> msgs = new HashMap<>();
		while ((msgReceived = consumer.receive(10)) != null) {
			if (logger.isDebugEnabled())
				logger.debug("message = " + msgReceived.getMessageID() + ":" + msgReceived.getAddress());
			String ctx = Util.getContext(msgReceived.getAddress().toString());
			Map<String, Map<String, List<ClientMessage>>> ctxMap = msgs.get(ctx);
			if (ctxMap == null) {
				ctxMap = new HashMap<>();
				msgs.put(ctx, ctxMap);
			}
			Map<String, List<ClientMessage>> tsMap = ctxMap.get(msgReceived.getStringProperty(timestamp));
			if (tsMap == null) {
				tsMap = new HashMap<>();
				ctxMap.put(msgReceived.getStringProperty(timestamp), tsMap);
			}
			if (logger.isDebugEnabled())
				logger.debug("$source: " + msgReceived.getStringProperty(sourceRef));
			List<ClientMessage> srcMap = tsMap.get(msgReceived.getStringProperty(sourceRef));
			if (srcMap == null) {
				srcMap = new ArrayList<>();
				tsMap.put(msgReceived.getStringProperty(sourceRef), srcMap);
			}
			srcMap.add(msgReceived);
		}
		return msgs;
	}

	public static SortedMap<String, Object> readAllMessages(ClientConsumer consumer)
			throws ActiveMQPropertyConversionException, ActiveMQException {
		ClientMessage msgReceived = null;
		SortedMap<String, Object> msgs = new ConcurrentSkipListMap<>();
		while ((msgReceived = consumer.receive(10)) != null) {
			String key = msgReceived.getAddress().toString();
			if (logger.isDebugEnabled())
				logger.debug("message = " + msgReceived.getMessageID() + ":" + key);
			String ts = msgReceived.getStringProperty(timestamp);
			String src = msgReceived.getStringProperty(source);
			if (ts != null)
				msgs.put(key + dot + timestamp, ts);
			if (src != null)
				msgs.put(key + dot + source, src);
			if (ts == null && src == null) {
				msgs.put(key, Util.readBodyBuffer(msgReceived));
			} else {
				msgs.put(key + dot + value, Util.readBodyBuffer(msgReceived));
			}

		}
		return msgs;
	}

	/**
	 * Convert map to a json object
	 * 
	 * @param msgs
	 * @return
	 * @throws IOException
	 */
	public static Json mapToJson(SortedMap<String, Object> msgs) throws IOException {
		Json json = null;
		if (msgs.size() > 0) {
			JsonSerializer ser = new JsonSerializer();
			json = Json.read(ser.write(msgs));
			if (logger.isDebugEnabled())
				logger.debug("json = " + json.toString());
		}
		return json;
	}

	public static void sendMessage(ClientSession session, ClientProducer producer, String address, String body)
			throws ActiveMQException {
		ClientMessage msg = session.createMessage(true);
		msg.getBodyBuffer().writeString(body);
		producer.send(address, msg);

	}

	public static Pattern regexPath(String newPath) {
		// regex it
		String regex = newPath.replaceAll(".", "[$0]").replace("[*]", ".*").replace("[?]", ".");
		return Pattern.compile(regex);
	}

	public static String getContext(String path) {
		// return vessels.*
		// TODO; robustness for "signalk/api/v1/", and "vessels.*" and
		// "list/vessels"
		if (StringUtils.isBlank(path)) {
			return "";
		}
		if (path.equals(resources) || path.startsWith(resources + dot)) {
			return path;
		}

		if (path.equals(sources) || path.startsWith(sources + dot)) {
			return path;
		}

		if (path.equals(CONFIG)) {
			return path;
		}
		if (path.startsWith(CONFIG + dot)) {
			int p1 = path.indexOf(CONFIG) + CONFIG.length() + 1;

			int pos = path.indexOf(".", p1);
			if (pos < 0) {
				return path;
			}
			return path.substring(0, pos);
		}
		if (path.equals(vessels)) {
			return path;
		}
		if (path.startsWith(vessels + dot) || path.startsWith(LIST + dot + vessels + dot)) {
			int p1 = path.indexOf(vessels) + vessels.length() + 1;

			int pos = path.indexOf(".", p1);
			if (pos < 0) {
				return path;
			}
			return path.substring(0, pos);
		}
		return "";
	}

	public static boolean sameNetwork(String localAddress, String remoteAddress) throws Exception {
		InetAddress addr = InetAddress.getByName(localAddress);
		NetworkInterface networkInterface = NetworkInterface.getByInetAddress(addr);
		short netmask = -1;
		for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
			if (address.getAddress().equals(addr)) {
				netmask = address.getNetworkPrefixLength();
			}
		}
		return sameNetwork(localAddress, netmask, remoteAddress);
	}

	public static boolean sameNetwork(String localAddress, short netmask, String remoteAddress) throws Exception {
		byte[] a1 = InetAddress.getByName(localAddress).getAddress();
		byte[] a2 = InetAddress.getByName(remoteAddress).getAddress();
		byte[] m = InetAddress.getByName(normalizeFromCIDR(netmask)).getAddress();
		if (logger.isDebugEnabled()) {
			logger.debug("sameNetwork?:" + localAddress + "/" + normalizeFromCIDR(netmask) + "," + remoteAddress + ","
					+ netmask);
		}
		for (int i = 0; i < a1.length; i++) {
			if ((a1[i] & m[i]) != (a2[i] & m[i])) {
				return false;
			}
		}

		return true;

	}

	public static boolean inNetworkList(List<String> ipList, String ip) throws Exception {
		for (String denyIp : ipList) {
			short netmask = 0;
			String[] p = denyIp.split("/");
			if (p.length > 1) {
				netmask = (short) (32 - Short.valueOf(p[1]));
			}
			if (Util.sameNetwork(p[0], netmask, ip)) {
				if (logger.isDebugEnabled()) {
					logger.debug("IP found " + ip + " in list: " + denyIp);
				}
				return true;
			}

		}
		return false;
	}

	/*
	 * RFC 1518, 1519 - Classless Inter-Domain Routing (CIDR) This converts from
	 * "prefix + prefix-length" format to "address + mask" format, e.g. from
	 * xxx.xxx.xxx.xxx/yy to xxx.xxx.xxx.xxx/yyy.yyy.yyy.yyy.
	 */
	public static String normalizeFromCIDR(short bits) {
		final int mask = (bits == 32) ? 0 : 0xFFFFFFFF - ((1 << bits) - 1);

		return Integer.toString(mask >> 24 & 0xFF, 10) + "." + Integer.toString(mask >> 16 & 0xFF, 10) + "."
				+ Integer.toString(mask >> 8 & 0xFF, 10) + "." + Integer.toString(mask >> 0 & 0xFF, 10);
	}

	/**
	 * Recursive findNode(). Returns null if not found
	 *
	 * @param node
	 * @param fullPath
	 * @return
	 */
	public static Json findNode(Json node, String fullPath) {
		String[] paths = fullPath.split("\\.");
		// Json endNode = null;
		for (String path : paths) {
			logger.debug("findNode:" + path);
			node = node.at(path);
			if (node == null) {
				return null;
			}
		}
		return node;
	}

	public static long getMillisFromIsoTime(String iso) {
		return ISODateTimeFormat.dateTimeParser().withZoneUTC().parseDateTime(iso).getMillis();

	}

	public static String getIsoTimeString() {

		return getIsoTimeString(System.currentTimeMillis());
		// return ISO8601DateFormat.getDateInstance().format(new Date());
	}

	public static String getIsoTimeString(DateTime now) {

		return now.toDateTimeISO().toString(ISODateTimeFormat.dateTimeParser());
	}

	public static String getIsoTimeString(long timestamp) {
		return new DateTime(timestamp, DateTimeZone.UTC).toDateTimeISO().toString();
	}
}
