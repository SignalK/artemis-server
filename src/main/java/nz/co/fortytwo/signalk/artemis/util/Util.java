package nz.co.fortytwo.signalk.artemis.util;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.ALL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_FULL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.GET;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.KNOTS_TO_MS;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.LIST;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.MS_TO_KNOTS;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SUBSCRIBE;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UNSUBSCRIBE;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.aircraft;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.aton;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.label;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sar;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.type;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.values;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.version;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQPropertyConversionException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.activemq.artemis.core.postoffice.RoutingStatus;
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
	 * @param speed
	 *            in knots
	 * @return speed in m/s
	 */
	public static double kntToMs(double speed) {
		return speed * KNOTS_TO_MS;
	}

	/**
	 * Convert a speed in meter/sec to knots
	 *
	 * @param speed
	 *            in m/s
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
			selfMatch = Pattern.compile("\\." + Config.getConfigProperty(ConfigConstants.UUID) + "\\.|\\."
					+ Config.getConfigProperty(ConfigConstants.UUID) + "$");
		}
		key = selfMatch.matcher(key).replaceAll(dot + Config.getConfigProperty(ConfigConstants.UUID) + dot);

		return key;
	}

	public static ClientSession getVmSession(String user, String password) throws Exception {

		return inVmLocator.createSessionFactory().createSession(user, password, false, true, true, false, 10);
	}

	public static ClientSession getLocalhostClientSession(String user, String password) throws Exception {

		return nettyLocator.createSessionFactory().createSession(user, password, false, true, true, false, 10);
	}

	public static void sendRawMessage(String user, String password, String content) throws Exception {
		ClientSession txSession = null;
		ClientProducer producer = null;
		try {
			// start polling consumer.
			txSession = Util.getVmSession(user, password);
			ClientMessage message = txSession.createMessage(false);
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
	
	public static RoutingStatus sendReply(String type, String destination, String format, Json json, ServerSession s)
			throws Exception {
		return sendReply(String.class.getSimpleName(),destination,FORMAT_FULL,json,s,null);
	}

	public static RoutingStatus sendReply(String type, String destination, String format, Json json, ServerSession s,String correlation)
			throws Exception {
		if(json==null || json.isNull())json=Json.object();
		ClientMessage txMsg = new ClientMessageImpl((byte) 0, false, 0, System.currentTimeMillis(), (byte) 4, 1024);
		txMsg.putStringProperty(Config.JAVA_TYPE, type);
		if(correlation!=null)
			txMsg.putStringProperty(Config.AMQ_CORR_ID, correlation);
		txMsg.putStringProperty(Config.AMQ_SUB_DESTINATION, destination);
		txMsg.putBooleanProperty(Config.SK_SEND_TO_ALL, false);
		txMsg.putStringProperty(SignalKConstants.FORMAT, format);
		txMsg.putBooleanProperty(SignalKConstants.REPLY, true);

		txMsg.getBodyBuffer().writeString(json.toString());
		if (logger.isDebugEnabled())
			logger.debug("Msg body = " + json.toString());
		txMsg.setAddress(new SimpleString("outgoing.reply." + destination));
		// txMsg.setReplyTo(new SimpleString("outgoing.reply."+destination));
		RoutingStatus r = s.send(txMsg, true);
		if (logger.isDebugEnabled())
			logger.debug("Routing = " + r.name());
		return r;
	}

	

	public static Json getJsonGetRequest(String path) {
		path=Util.sanitizePath(path);
		String ctx = Util.getContext(path);
//		if (StringUtils.isBlank(ctx)) {
//			ctx = vessels + dot + self_str;
//		}
		return getJsonGetRequest(ctx, StringUtils.substringAfter(path, ctx + dot));

	}

	public static Json getJsonGetRequest(String context, String path) {
		Json json = Json.read("{\"context\":\"" + context + "\",\"get\": []}");
		Json sub = Json.object();
		sub.set("path", StringUtils.defaultIfBlank(path, "*"));
		json.at("get").add(sub);
		logger.debug("Created json sub: " + json);
		return json;
	}

	/**
	 * Attempt to set the system time using the GPS time
	 *
	 * @param sen
	 */
//	@SuppressWarnings("deprecation")
//	public static void checkTime(RMCSentence sen) {
//		if (timeSet) {
//			return;
//		}
//		try {
//			net.sf.marineapi.nmea.util.Date dayNow = sen.getDate();
//			// if we need to set the time, we will be WAAYYY out
//			// we only try once, so we dont get lots of native processes
//			// spawning if we fail
//			timeSet = true;
//			Date date = new Date();
//			if ((date.getYear() + 1900) == dayNow.getYear()) {
//				logger.debug("Current date is {}", date);
//				return;
//			}
//			// so we need to set the date and time
//			net.sf.marineapi.nmea.util.Time timeNow = sen.getTime();
//			String yy = String.valueOf(dayNow.getYear());
//			String MM = pad(2, String.valueOf(dayNow.getMonth()));
//			String dd = pad(2, String.valueOf(dayNow.getDay()));
//			String hh = pad(2, String.valueOf(timeNow.getHour()));
//			String mm = pad(2, String.valueOf(timeNow.getMinutes()));
//			String ss = pad(2, String.valueOf(timeNow.getSeconds()));
//
//			logger.debug("Setting current date to {} {}", dayNow, timeNow);
//
//			String cmd = "sudo date --utc " + MM + dd + hh + mm + yy + "." + ss;
//			Runtime.getRuntime().exec(cmd.split(" "));// MMddhhmm[[yy]yy]
//
//			logger.debug("Executed date setting command: {}", cmd);
//
//		} catch (Exception e) {
//			logger.error(e.getMessage(), e);
//		}
//
//	}



	public static  String sanitizeRoot(String root) {
		if(StringUtils.isBlank(root)) root = ALL;
		if(StringUtils.equals(dot,root)) root = ALL;
		if(StringUtils.equals(".*",root)) root = ALL;
		if(StringUtils.startsWith(vessels, root))root = vessels;
		if(StringUtils.startsWith(aircraft, root))root = aircraft;
		if(StringUtils.startsWith(sar, root))root = sar;
		if(StringUtils.startsWith(aton, root))root = aton;
		if(StringUtils.startsWith(CONFIG, root))root = CONFIG;
		if(StringUtils.startsWith(resources, root))root = resources;
		if(StringUtils.startsWith(sources, root))root = sources;
		return root;
	}
	
	public static String sanitizePath(String newPath) {
		newPath = newPath.replace('/', '.');
		if (newPath.startsWith(dot)) {
			newPath = newPath.substring(1);
		}
		if (newPath.endsWith(".") || newPath.endsWith("*") || newPath.endsWith("?")) {
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

						logger.debug("Key: {}, value: {}", key, v);
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
				logger.debug("Empty msg: {} : {}", () -> msg.getAddress(), () -> msg.getBodyBuffer().readableBytes());
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
				logger.debug("message = {} : {}", msgReceived.getMessageID(), msgReceived.getAddress());
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
				logger.debug("$source: {}", msgReceived.getStringProperty(sourceRef));
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
				logger.debug("message = {} : {}", msgReceived.getMessageID(), key);
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

		if (msgs.size() > 0) {
			JsonSerializer ser = new JsonSerializer();
			Json json = Json.read(ser.write(msgs));
			if (logger.isDebugEnabled())
				logger.debug("json = {}", () -> json.toString());
			return json;
		}
		return null;
	}

	public static void sendMessage(ClientSession session, ClientProducer producer, String address, String body)
			throws ActiveMQException {
		ClientMessage msg = session.createMessage(true);
		msg.getBodyBuffer().writeString(body);
		producer.send(address, msg);

	}

	public static Pattern regexPath(String newPath) {
		// regex it
		if(StringUtils.isBlank(newPath))newPath = "*";
		String regex = newPath.replaceAll(".", "[$0]").replace("[*]", ".*").replace("[?]", ".");
		return Pattern.compile(regex);
	}

	public static String getContext(String path) {
		path=path.trim();

		// TODO; robustness for "signalk/api/v1/", and "vessels.*" and
		// "list/vessels"
		
		if (StringUtils.isBlank(path)) {
			return "";
		}
		
		if (path.equals(resources) || path.startsWith(resources + dot) 
			|| path.equals(sources) || path.startsWith(sources + dot)
			|| path.equals(CONFIG)
			||path.equals(vessels)) {
			return path;
		}

		
		if (path.startsWith(CONFIG + dot)) {

			int pos = path.indexOf(".", CONFIG.length() + 1);
			if (pos < 0) {
				return path;
			}
			return path.substring(0, pos);
		}
		
		if (path.startsWith(vessels + dot) || path.startsWith(LIST + dot + vessels + dot)) {
			int p1 = path.indexOf(vessels) + vessels.length() + 1;

			int pos = path.indexOf(".", p1);
			if (pos < 0) {
				return path;
			}
			return path.substring(0, pos);
		}
		return path;
	}

	public static Json getJson(Json parent, String key) {
		String[] path = StringUtils.split(key, ".");
		Json node = parent;
		for (int i = 0; i < path.length; i++) {
			if (!node.has(path[i])) {
				node.set(path[i], Json.object());
			}
			node = node.at(path[i]);
		}
		return node;

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
		if (logger.isDebugEnabled())
			logger.debug("sameNetwork?: {}/{},{},{}", () -> localAddress, () -> normalizeFromCIDR(netmask),
					() -> remoteAddress, () -> netmask);

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
				if (logger.isDebugEnabled())
					logger.debug("IP found {} in list: {}", ip, denyIp);

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
	 * Does a regex search if a path element has * or [, and for the last path
	 * to ensure we get partial matches.
	 *
	 * @param node
	 * @param fullPath
	 * @return
	 */
	public static Json findNodeMatch(Json node, String fullPath) {
		String[] paths = fullPath.split("\\.");
		// Json endNode = null;
		for (int x = 0; x < paths.length; x++) {
			if (logger.isDebugEnabled())
				logger.debug("findNode: {}", paths[x]);

			if (node.has(paths[x])) {
				if (x == paths.length - 1) {
					return node.at(paths[x]);
				} else {
					node = node.at(paths[x]);
				}
			} else {
				for (String k : node.asJsonMap().keySet()) {
					if (Util.regexPath(paths[x]).matcher(k).find())
						if (x == paths.length - 1 || !node.isObject()) {
							return node;
						} else {
							node = node.at(k);
						}
				}
			}
		}
		return null;
	}

	public static long getMillisFromIsoTime(String iso) {
		return ISODateTimeFormat.dateTimeParser().withZoneUTC().parseMillis(iso);
	}

	public static String getIsoTimeString() {
		return getIsoTimeString(System.currentTimeMillis());
	}

	public static String getIsoTimeString(DateTime now) {
		return now.toDateTimeISO().toString(ISODateTimeFormat.dateTimeParser());
	}

	public static String getIsoTimeString(long timestamp) {
		return new DateTime(timestamp, DateTimeZone.UTC).toDateTimeISO().toString();
	}

	public static Json setJson(Json parent, String path, Json json) {
		String[] paths = path.split("\\.");
		Json node = parent;
		for (int x = 0; x < path.length(); x++) {
			if (logger.isDebugEnabled())
				logger.debug("setJson: {}", paths[x]);
			if (x == paths.length - 1) {
				node.set(paths[x], json);
				return json;
			}
			if (node.has(paths[x])) {
				node = node.at(paths[x]);
			} else {
				node.set(paths[x], Json.object());
				node = node.at(paths[x]);
			}
		}
		return node;

	}

	/**
	 * Converts a source key to a $source key, and returns the relevant
	 * sources.* tree Returns null if there is no source key. Only looks in the
	 * immediate object, does not recurse.
	 * 
	 * @param input
	 * @param defaultType,
	 *            if source object has no type, can be null
	 * @param defaultLabel,
	 *            if source object has no label, can be null
	 * @return
	 */
	public static Json convertSourceToRef(Json input, String defaultType, String defaultLabel) {
		if (input.has(source)) {
			if (logger.isDebugEnabled())
				logger.debug("source Json: {}", input);
			// extract as full and save
			Json src = input.at(source);
			Json srcJson = Json.object(sources, Json.object());
			StringBuffer srcRef = new StringBuffer();
			if (src.has(type)) {
				srcRef.append(src.at(type).asString());
			} else {
				srcRef.append(StringUtils.defaultString(defaultType, "unknown"));
			}
			if (src.has(label)) {
				srcRef.append(dot + src.at(label).asString());
			} else {
				srcRef.append(dot + StringUtils.defaultString(defaultLabel, "unknown"));
			}
			// replace source with sourceRef
			input.delAt(source);
			input.set(sourceRef, srcRef.toString());
			if (logger.isDebugEnabled())
				logger.debug("srcRef Json: {}", input);
			Util.setJson(srcJson, sources + dot + srcRef.toString(), src);
			return srcJson;
		}
		return null;
	}

	public static boolean isDelta(Json node) {
		if(node==null)return false;
		// deal with diff format
		if (node.has(CONTEXT) && (node.has(UPDATES) || node.has(PUT) ||node.has(GET) || node.has(CONFIG))) return true;
		return false;
	}
	
	public static boolean isSubscribe(Json node) {
		if(node==null)return false;
		// deal with diff format
		if (node.has(CONTEXT) && (node.has(SUBSCRIBE) || node.has(UNSUBSCRIBE))) return true;
		return false;
	}
	
	public static boolean isFullFormat(Json node) {
		if(node==null)return false;
		// avoid full signalk syntax
		if (node.has(vessels) 
				|| node.has(CONFIG) 
				|| node.has(sources) 
				|| node.has(resources)
				|| node.has(aircraft)
				|| node.has(sar)
				|| node.has(aton))
			return true;
		return false;
	}
}

