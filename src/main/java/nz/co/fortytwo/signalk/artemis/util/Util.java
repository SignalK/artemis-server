package nz.co.fortytwo.signalk.artemis.util;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.GET;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.KNOTS_TO_MS;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.LIST;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.MS_TO_KNOTS;
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
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.version;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletInputStream;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.JodaTimePermission;
import org.joda.time.format.ISODateTimeFormat;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import mjson.Json;

public class Util {

	static Logger logger = LogManager.getLogger(Util.class);
	
	public static final String SIGNALK_MODEL_SAVE_FILE = "./conf/self.json";
	public static final String SIGNALK_CFG_SAVE_FILE = "./conf/signalk-config.json";
	public static final String SIGNALK_RESOURCES_SAVE_FILE = "./conf/resources.json";
	public static final String SIGNALK_SOURCES_SAVE_FILE = "./conf/sources.json";
	//private static boolean timeSet = false;

	private static ServerLocator nettyLocator;
	private static ServerLocator inVmLocator;
	protected static Pattern selfMatch = Pattern.compile("\\.self\\.|\\.self$");

	static {
		try {
			inVmLocator = ActiveMQClient
					.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
					.setMinLargeMessageSize(1024 * 1024)
					.setConnectionTTL(60000)
					.setBlockOnAcknowledge(false)
					.setBlockOnDurableSend(false)
					.setBlockOnNonDurableSend(false);
			// .createSessionFactory();
			Map<String, Object> connectionParams = new HashMap<String, Object>();
			connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
			connectionParams.put(TransportConstants.PORT_PROP_NAME, 61617);
			connectionParams.put(TransportConstants.CONNECTION_TTL,60000);
			nettyLocator = ActiveMQClient
					.createServerLocatorWithoutHA(
							new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams))
					.setMinLargeMessageSize(1024 * 1024)
					.setConnectionTTL(60000)
					.setBlockOnAcknowledge(false)
					.setBlockOnDurableSend(false)
					.setBlockOnNonDurableSend(false);
			// .createSessionFactory();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Json getWelcomeMsg() {
		return getWelcomeMsg(null, null);
	}
	public static Json getWelcomeMsg(String startTime, Double playbackRate) {
		//TODO: add history playbackRate and startTime
		/*
		 * {
			    "name": "foobar marine server",
			    "version": "1.1.4",
			    "startTime": "2018-08-24T15:19:09Z",
			    "playbackRate": 1,
			    "self": "vessels.urn:mrn:signalk:uuid:c0d79334-4e25-4245-8892-54e8ccc8021d",
			    "roles": [
			        "master",
			        "main"
			    ]
			}
		 */
		Json msg = Json.object();
		msg.set("name", "Artemis Signalk Server");
		msg.set(version, Config.getVersion());
		msg.set(timestamp, getIsoTimeString());
		if(startTime!=null) {
			msg.set("startTime", startTime);
		}
		if(playbackRate!=null) {
			msg.set("playbackRate", playbackRate);
		}
		msg.set(self_str, Config.getConfigProperty(ConfigConstants.UUID));
		msg.set("roles", Json.read("[\"master\",\"main\"]"));
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

		return inVmLocator.createSessionFactory().createSession(user, password, false, true, true, true, 10);
	}

	public static ClientSession getLocalhostClientSession(String user, String password) throws Exception {

		return nettyLocator.createSessionFactory().createSession(user, password, false, true, true, true, 10);
	}

	public static void sendRawMessage(String user, String password, String content) throws Exception {
		
		try (ClientSession txSession = Util.getVmSession(user, password);
				ClientProducer producer = txSession.createProducer();){
			// start polling consumer.
			
			ClientMessage message = txSession.createMessage(false);
			message.getBodyBuffer().writeString(content);
			
			producer.send(Config.INCOMING_RAW, message);
		} 
	}
	public static Json getJsonGetSnapshotRequest(String path, String token, String time) {
		path=Util.sanitizePath(path);
		String ctx = Util.getContext(path);
		return getJsonGetRequest(ctx, StringUtils.substringAfter(path, ctx + dot), token, time);

	}

	public static Json getJsonGetRequest(String path, String token) {
		path=Util.sanitizePath(path);
		String ctx = Util.getContext(path);
		return getJsonGetRequest(ctx, StringUtils.substringAfter(path, ctx + dot), token);

	}

	public static Json getJsonGetRequest(String context, String path, String token) {
		return getJsonGetRequest(context, path, token, null);
	}
	public static Json getJsonGetRequest(String context, String path, String token, String time) {
		Json json = Json.read("{\"context\":\"" + context + "\",\"get\": []}");
		json.set(SignalKConstants.TOKEN, token);
		Json sub = Json.object();
		sub.set("path", StringUtils.defaultIfBlank(path, "*"));
		if(StringUtils.isNotBlank(time)) {
			sub.set("time", StringUtils.defaultIfBlank(time, time));
		}
		json.at("get").add(sub);
		if (logger.isDebugEnabled())logger.debug("Created json sub: {}", json);
		return json;
	}



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



	public static void sendMessage(ClientSession session, ClientProducer producer, String address, String body)
			throws ActiveMQException {
		synchronized (session) {
			ClientMessage msg = session.createMessage(true);
			msg.getBodyBuffer().writeString(body);
			producer.send(address, msg);
		}
		

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
			if (StringUtils.isNotBlank(paths[x])&&!node.isObject()) return node;
			if (StringUtils.isNotBlank(paths[x])&&node.has(paths[x])) {
				if (x == paths.length - 1) {
					return node.at(paths[x]);
				} else {
					node = node.at(paths[x]);
				}
			} else {
				for (String k : node.asJsonMap().keySet()) {
					if (StringUtils.isNotBlank(paths[x]) && Util.regexPath(paths[x]).matcher(k).find()){
						if (x == paths.length - 1 || !node.isObject()) {
							return node;
						} else {
							node = node.at(k);
						}
					}
				}
			}
		}
		return Json.object();
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
			if (src.isObject() && src.has(type)) {
				srcRef.append(src.at(type).asString());
			} else {
				srcRef.append(StringUtils.defaultString(defaultType, SignalKConstants.UNKNOWN));
			}
			if (src.has(label)) {
				srcRef.append(dot + src.at(label).asString());
			} else {
				srcRef.append(dot + StringUtils.defaultString(defaultLabel, SignalKConstants.UNKNOWN));
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
	public static boolean isN2k(Json node) {
		if(node==null)return false;
		// '{"timestamp":"2013-10-08-15:47:28.263Z","prio":"2","src":"204","dst":"255","pgn":"127250","description":"Vessel Heading","fields":{"Heading":"129.7","Reference":"Magnetic"}}'
		if (node.has("prio") && node.has("src")&& node.has("dst")&& node.has("pgn")) return true;
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

	public static String readString(ServletInputStream inputStream, String characterEncoding) throws IOException {
		try(ByteArrayOutputStream result = new ByteArrayOutputStream();){
		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) != -1) {
		    result.write(buffer, 0, length);
		}
		// StandardCharsets.UTF_8.name() > JDK 7
		return result.toString(characterEncoding);
		}
	}
	
	public static Json getSubscriptionJson(String context, String path, int period, int minPeriod, String format, String policy) {
		return getSubscriptionJson(context, path, period, minPeriod, format, policy, Util.getIsoTimeString(0l), -1.0d);
	}
	public static Json getSubscriptionJson(String context, String path, int period, int minPeriod, String format, String policy, String startTime, double playbackRate) {
		Json json = Json.read("{\"context\":\"" + context + "\", \"subscribe\": []}");
		Json sub = Json.object();
		sub.set("path", path);
		sub.set("period", period);
		sub.set("minPeriod", minPeriod);
		sub.set("format", format);
		sub.set("policy", policy);
		sub.set(START_TIME, startTime);
		sub.set(PLAYBACK_RATE, playbackRate);
		json.at("subscribe").add(sub);
		logger.debug("Created json sub: " + json);
		return json;
	}

	public static Json getUrlAsJson(AsyncHttpClient c,String url) throws Exception {
		return Json.read(getUrlAsString(c,url, null, null));
	}
	public static Json getUrlAsJson(AsyncHttpClient c,String url, String user, String pass) throws Exception {
		return Json.read(getUrlAsString(c,url, user, pass));
	}
	
	public static String getUrlAsString(AsyncHttpClient c,String url) throws Exception {
		return getUrlAsString(c,url, null,null);
	}
	public static String getUrlAsString(AsyncHttpClient c, String url, String user, String pass) throws Exception {
			// get a sessionid
			Response r2 = null;
			if(user!=null){
				r2 = c.prepareGet(url).setCookies(getCookies(user, pass)).execute().get();
			}else{
				r2 = c.prepareGet(url).execute().get();
			}
			
			String response = r2.getResponseBody();
			logger.debug("Endpoint string:" + response);
			return response;
	}
	
	public static Collection<Cookie> getCookies(String user, String pass) throws Exception {
		String jwtToken = SecurityUtils.authenticateUser(user, pass);
		Collection<Cookie> cookies = new ArrayList<>();
		cookies.add(new DefaultCookie(SecurityUtils.AUTH_COOKIE_NAME, jwtToken));
		return cookies;
	}
}

