package nz.co.fortytwo.signalk.artemis.intercept;

import org.apache.activemq.artemis.core.server.ServerSession;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseInterceptor {

	
	protected void sendSourceMsg(String srcRef, Json src, String timeStamp, ServerSession sess) throws Exception {
		Util.sendSourceMsg(srcRef, (Json)src,timeStamp, sess);
	}

	protected void sendMsg(String key, Json value, String timeStamp, String srcRef, ServerSession sess) throws Exception {
		Util.sendMsg(key, value, timeStamp, srcRef, sess);
	}
	
	protected void sendDoubleAsMsg(String key, double value, String timestamp, String sourceRef, ServerSession session) throws Exception {
		Util.sendDoubleAsMsg(key, value, timestamp, sourceRef, session);
	}
}
