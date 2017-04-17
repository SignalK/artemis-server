package nz.co.fortytwo.signalk.artemis.util;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.label;
import static nz.co.fortytwo.signalk.util.SignalKConstants.self;
import static nz.co.fortytwo.signalk.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.util.SignalKConstants.values;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels_dot_self;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels_dot_self_dot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.ActiveMQSecurityException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.impl.ServerMessageImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.util.ConfigConstants;

public class Util extends nz.co.fortytwo.signalk.util.Util {

	static Logger logger = LogManager.getLogger(Util.class);
	//private static Pattern selfMatch = Pattern.compile("\\.self\\.");
	//private static Pattern selfEndMatch = Pattern.compile("\\.self$");
	static final String SIGNALK_MODEL_SAVE_FILE = "./conf/self.json";
	public static final String SIGNALK_CFG_SAVE_FILE = "./conf/signalk-config.json";

	protected static Pattern selfMatch = null;

    protected static Pattern selfEndMatch = null;
	/**
	 * If we receive messages for our UUID, convert to 'self'
	 * @param key
	 * @return
	 */
	public static String fixSelfKey(String key) {
		if(selfMatch==null){
			selfMatch= Pattern.compile("\\."+Config.getConfigProperty(ConfigConstants.UUID)+"\\.");
			selfEndMatch = Pattern.compile("\\."+Config.getConfigProperty(ConfigConstants.UUID)+"$");
		}
        key = selfMatch.matcher(key).replaceAll(".self.");
        
        key = selfEndMatch.matcher(key).replaceAll(".self");
        return key;
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

	public static void sendDoubleAsMsg(String key, double value, String timeStamp, String srcRef, ServerSession session)
			throws Exception {
		if(StringUtils.isNotBlank(srcRef)){
			sendObjMsg(key+dot+values+dot+srcRef,  Json.make(value), timeStamp, srcRef, session);
		}else{
			sendObjMsg(key,  Json.make(value), timeStamp, srcRef, session);
		}
	}

	public static void sendMsg(String key, Json body, String timeStamp, String srcRef, ServerSession sess) throws Exception{
		if(StringUtils.isNotBlank(srcRef)){
			sendObjMsg(key+dot+values+dot+srcRef, body, timeStamp, srcRef, sess);
		}else{
			sendObjMsg(key, body, timeStamp, srcRef, sess);
		}
	}
	
	public static void sendMsg(String key, Json body, String timeStamp, Json src, ServerSession sess) throws Exception{
		if(src!=null && !src.isNull()){
			String srclabel = src.at(label).asString();
			if(srclabel.startsWith(sources))srclabel = srclabel.substring(sources.length()+1);
			sendObjMsg(key+dot+values+dot+srclabel, body, timeStamp, src, sess);
		}else{
			sendObjMsg(key, body, timeStamp, src, sess);
		}
	}
	private static void sendObjMsg(String key, Json body, String timeStamp, Object src, ServerSession sess)
			throws Exception {
		
		ServerMessage m2 = new ServerMessageImpl(new Double(Math.random()).longValue(), 64);
		m2.putStringProperty(timestamp, timeStamp);
		if (src != null) {
			m2.putStringProperty(source, src.toString());
		}
		String type =body.getClass().getSimpleName();
		m2.putStringProperty(Config.JAVA_TYPE, type);
		 
		switch (type) {
		case "NullJson":
			m2.getBodyBuffer().writeString( body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			break;
		case "BooleanJson":
			m2.getBodyBuffer().writeString( body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			break;
		case "StringJson":
			m2.getBodyBuffer().writeString( body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			break;
		case "NumberJson":
			m2.getBodyBuffer().writeString( body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			break;
		case "Json":
			m2.getBodyBuffer().writeString( body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_COMPOSITE);
			break;
		case "ObjectJson":
			m2.getBodyBuffer().writeString(body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_COMPOSITE);
			break;
		case "ArrayJson":
			m2.getBodyBuffer().writeString( body.toString());
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_COMPOSITE);
			break;
		default:
			logger.error("Unknown Json Class type: "+type);
			m2.putStringProperty(Config.SK_TYPE, Config.SK_TYPE_VALUE);
			m2.getBodyBuffer().writeString( body.toString());
			break;
		}
		
		m2.setAddress(new SimpleString(key));
		m2.putStringProperty(Config._AMQ_LVQ_NAME, key);

		try {
			sess.send(m2, true);
		} catch (ActiveMQSecurityException se) {
			logger.warn(se.getMessage());
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		}
	}

	public static void sendSourceMsg(String key, String src, String now, ServerSession sess) throws Exception {
		sendObjMsg("sources." + key, Json.read(src), now, null, sess);

	}
	
	public static void sendSourceMsg(String key, Json src, String now, ServerSession sess) throws Exception {
		sendObjMsg("sources." + key, src, now, null, sess);

	}
	
	public static String sanitizePath(String newPath) {
        newPath = newPath.replace('/', '.');
        if (newPath.startsWith(dot)) {
            newPath = newPath.substring(1);
        }
        if (newPath.endsWith("*") || newPath.endsWith("?")) {
            newPath = newPath.substring(0, newPath.length()-1);
        }

        return newPath;
    }

	/**
	 * Input is a list of message wrapped in a stack of hashMaps, eg
	 * Key=context
	 *    Key=timestamp
	 *       key=source
	 *       	List(messages)
	 * The method iterates through and creates the deltas as a Json array, one Json delta per context. 
	 * @param msgs
	 * @return
	 */
	public static Json generateDelta(HashMap<String, HashMap<String, HashMap<String, List<ClientMessage>>>> msgs) {
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
					// add timestamp
					valObj.set(timestamp, ts);
					//if(src.contains("{"))
					//logger.debug("GenerateDelta:src: "+src);
					valObj.set(sourceRef, src);
					//else
					//	valObj.set(sourceRef, src);
					// now the values
					for (ClientMessage msg : msgs.get(ctx).get(ts).get(src)) {
						String key = msg.getAddress().toString().substring(ctx.length()+1);
						if(key.contains(dot+values+dot))
							key = key.substring(0, key.indexOf(dot+values+dot));
						Json v = Util.readBodyBuffer(msg);
						logger.debug("Key: "+key+", value: "+v);
						Json val = Json.object(PATH, key );
						val.set(value,v);
						valuesArray.add(val);
					}
				}
				// add context
			}
			delta.set(CONTEXT, ctx);
		}

		

		return deltaArray;
	}

	public static Json readBodyBuffer(ClientMessage msg) {
		if(msg.getBodyBuffer().readableBytes()==0){
			logger.debug("Empty msg: "+msg.getAddress()+": "+msg.getBodyBuffer().readableBytes());
			return Json.nil();
		}
		return Json.read(msg.getBodyBuffer().readString());
		
	}

	public static String readBodyBufferToString(Message msg) {
		if(msg.getBodyBuffer().readableBytes()==0){
			return null;
		}else{
			return msg.getBodyBuffer().readString();
		}
		
	}
	 
}
