package nz.co.fortytwo.signalk.artemis.intercept;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.MessagePacket;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.service.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseInterceptor {
	private static Logger logger = LogManager.getLogger(BaseInterceptor.class);
	protected static TDBService influx = new InfluxDbService();
	protected static ThreadLocal<ClientSession> txSession = new ThreadLocal<>();
	protected static ThreadLocal<ClientProducer> producer = new ThreadLocal<>();

	protected void init() {
		try{
			txSession.set(Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
					Config.getConfigProperty(Config.ADMIN_PWD)));
			producer.set(txSession.get().createProducer());
			
			if(Config.getConfigProperty(ConfigConstants.CLOCK_SOURCE).equals("system")) {
				influx.setWrite(true);
			}
			}catch(Exception e){
				logger.error(e,e);
			}
	}

	public ClientSession getTxSession() {

		if (txSession.get() == null) {
			try {
				txSession.set(Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
						Config.getConfigProperty(Config.ADMIN_PWD)));

			} catch (Exception e) {
				logger.error(e, e);
			}
		}
		return txSession.get();
	}

	public ClientProducer getProducer() {
		if (producer.get() == null && getTxSession() != null && !getTxSession().isClosed()) {
			
			try {
				producer.set(getTxSession().createProducer());
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}

		}
		return producer.get();

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
	
	public void convertSource(Json j, String srcBus, String msgType) {
		Json srcJson = Util.convertSourceToRef(j,srcBus,msgType);
		saveSource(srcJson);
	}
	public void convertFullSrcToRef(Json node, String srcBus, String msgSrcType) {
		if (logger.isDebugEnabled())
			logger.debug("Converting source in full: {}", node.toString());
		//recurse keys
		node.asJsonMap().forEach((k,j) -> {
				if(j.isObject() && j.has(SignalKConstants.source)) {
					convertSource(j,srcBus, msgSrcType);
				}else {
					convertFullSrcToRef(j, srcBus, msgSrcType);
				}
			});
	}
	protected void sendReply(String simpleName, String destination, String format, Json json, ServerSession s) throws Exception {
		sendReply(String.class.getSimpleName(),destination,format,null,json);
	}
	
	protected void  sendReply(String simpleName, String destination, String format, String correlation, Json json) throws Exception {
		if(txSession.get()==null){
			init();
		}
		if(json==null || json.isNull())json=Json.object();
		
		ClientMessage txMsg =  txSession.get().createMessage(false);
		
		//txMsg.putStringProperty(Config.JAVA_TYPE, type);
		if(correlation!=null)
			txMsg.putStringProperty(Config.AMQ_CORR_ID, correlation);
		txMsg.putStringProperty(Config.AMQ_SUB_DESTINATION, destination);
		txMsg.putBooleanProperty(Config.SK_SEND_TO_ALL, false);
		txMsg.putStringProperty(SignalKConstants.FORMAT, format);
		txMsg.putBooleanProperty(SignalKConstants.REPLY, true);
		txMsg.putStringProperty(Config.AMQ_CORR_ID, correlation);
		txMsg.setExpiration(System.currentTimeMillis()+5000);
		txMsg.getBodyBuffer().writeString(json.toString());
		if (logger.isDebugEnabled())
			logger.debug("Msg body = {}",json.toString());
		producer.get().send("outgoing.reply." +destination,txMsg);
		
	}
	protected void sendKvJson(Message message, Json json) {
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseDelta(json, map);
		sendKvMap(message, map);
		
		
	}
	public void sendKvMap(Message message, NavigableMap<String, Json> map) {
		map.forEach((k,j) -> {
			try {
				sendKvMessage(message, k,j);
			} catch (Exception e) {
				logger.error(e,e);
			}
		});
		
	}
	protected void sendKvMessage(Message origMessage, String k, Json j) throws ActiveMQException {
		ClientMessage txMsg = getTxSession().createMessage(false);
		txMsg.copyHeadersAndProperties(origMessage);
		
		txMsg.putStringProperty(Config.AMQ_INFLUX_KEY, k);
		
		txMsg.setExpiration(System.currentTimeMillis()+5000);
		txMsg.getBodyBuffer().writeString(j.toString());
		if (logger.isDebugEnabled())
			logger.debug("Msg body signalk.kv: {} = {}",k, j.toString());
		
		getProducer().send(Config.INTERNAL_KV,txMsg);
		
	}
	
	protected void sendRawMessage(Message origMessage, String body) throws ActiveMQException {
		ClientMessage txMsg = getTxSession().createMessage(false);
		txMsg.copyHeadersAndProperties(origMessage);
		
		txMsg.removeProperty(Config.AMQ_CONTENT_TYPE);
		
		txMsg.setExpiration(System.currentTimeMillis()+5000);
		txMsg.getBodyBuffer().writeString(body);
		if (logger.isDebugEnabled())
			logger.debug("Msg body incoming.raw: {}",body);
		
		getProducer().send(Config.INCOMING_RAW,txMsg);
		
	}
	
	protected void saveMap(NavigableMap<String, Json> map) {
		influx.save(map);
	}
	
	protected void saveSource(Json srcJson) {
		if(srcJson==null)return;
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		SignalkMapConvertor.parseFull(srcJson,map, "");
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
