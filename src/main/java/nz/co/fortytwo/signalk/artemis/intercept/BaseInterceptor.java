package nz.co.fortytwo.signalk.artemis.intercept;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.MessagePacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.handler.MessageSupport;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.SignalkKvConvertor;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.service.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseInterceptor extends MessageSupport{
	private static Logger logger = LogManager.getLogger(BaseInterceptor.class);
	protected static TDBService influx = new InfluxDbService();
	
	protected void init() {
		try{
			super.initSession();
			
			if(Config.getConfigProperty(ConfigConstants.CLOCK_SOURCE).equals("system")) {
				influx.setWrite(true);
			}
			}catch(Exception e){
				logger.error(e,e);
			}
	}

	protected boolean isDelta(Json node){
		return Util.isDelta(node);
	}
	
	protected boolean isGet(Json node){
		return Util.isGet(node);
	}
	
	protected boolean isSubscribe(Json node){
		return Util.isSubscribe(node);
	}
	
	protected boolean isFullFormat(Json node) {
		return Util.isFullFormat(node);
	}
	
	protected boolean isN2k(Json node) {
		return Util.isN2k(node);
	}
	
	public void convertSource(MessageSupport support, Message message, Json j, String srcBus, String msgType)  {
		
		Json srcJson = Util.convertSourceToRef(j,srcBus,msgType);
		try {
			SignalkKvConvertor.parseFull(support, message, srcJson, "");
		} catch (ActiveMQException e) {
			logger.error(e,e);
		}
	
	}
	public void convertFullSrcToRef(MessageSupport support, Message message, Json node, String srcBus, String msgSrcType) {
		if (logger.isDebugEnabled())
			logger.debug("Converting source in full: {}", node.toString());
		//recurse keys
		if(!node.isObject()) return;
		node.asJsonMap().forEach((k,j) -> {
				if(j.isObject()) {
						
					if(j.has(SignalKConstants.source)) {
							convertSource(support, message, j,srcBus, msgSrcType);
						}
					}else {
						convertFullSrcToRef(support, message, j, srcBus, msgSrcType);
					}
				
			});
	}
	
	
	protected void saveMap(NavigableMap<String, Json> map) {
		influx.save(map);
	}
	
//	protected void saveSource(Json srcJson) {
//		if(srcJson==null)return;
//		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
//		SignalkMapConvertor.parseFull(srcJson,map, "");
//		saveMap(map);
//	}
//	
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
