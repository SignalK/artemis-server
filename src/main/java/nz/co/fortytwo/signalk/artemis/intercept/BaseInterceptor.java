package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_FULL;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.MessagePacket;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.SecurityService;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseInterceptor {
	private static Logger logger = LogManager.getLogger(BaseInterceptor.class);
	protected static InfluxDbService influx = new InfluxDbService();
	protected static SecurityService security = new SecurityService();

	protected boolean isDelta(Json node){
		return Util.isDelta(node);
	}
	
	protected boolean isFullFormat(Json node) {
		return Util.isFullFormat(node);
	}
	
	protected void sendReply(String simpleName, String destination, String formatFull, Json nil, ServerSession s) throws Exception {
		Util.sendReply(String.class.getSimpleName(),destination,FORMAT_FULL,Json.nil(),s);
	}
	
	protected void saveMap(NavigableMap<String, Json> map) {
		influx.save(map);
	}
	
	protected void saveSource(Json srcJson) {
		if(srcJson==null)return;
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		SignalkMapConvertor.parseFull(srcJson,map, "");
		map = security.addAttributes(map);
		saveMap(map);
	}
	
	protected boolean isResponse(Packet packet) {
		if (packet.isResponse())
			return true;
		if (packet instanceof MessagePacket) {
			ICoreMessage message = ((MessagePacket) packet).getMessage();
			if(message.getBooleanProperty(SignalKConstants.REPLY))return true;
		}
		return false;
	}
}
