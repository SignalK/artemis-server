package nz.co.fortytwo.signalk.artemis.handler;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQQueueExistsException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;

import mjson.Json;

import static nz.co.fortytwo.signalk.artemis.util.Config.*;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;

import java.util.UUID;

import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Base for handlers that read key value pairs from Config.INTERNAL_KV
 * 
 * @author robert
 *
 */
public abstract class BaseHandler {
	
	private static Logger logger = LogManager.getLogger(BaseHandler.class);
	
	protected  ThreadLocal<ClientProducer> producer=new ThreadLocal<>();

	protected  ThreadLocal<ClientSession> txSession=new ThreadLocal<>();

	protected ClientConsumer consumer;
	
	protected static TDBService influx = new InfluxDbService();
	
	protected String filter=null;
	protected String uuid;
	
	public BaseHandler() {
		uuid = Config.getConfigProperty(ConfigConstants.UUID);
	}

	protected void initSession(String filter) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("initSession: {}", filter);
		this.filter=filter;
		getTxSession();
		getProducer();
		getConsumer(filter);
	}
	
	protected void sendKvMessage(Message origMessage, String k, Json j) throws ActiveMQException {
		ClientMessage txMsg = getTxSession().createMessage(false);
		txMsg.copyHeadersAndProperties(origMessage);
		txMsg.setRoutingType(RoutingType.MULTICAST);
		txMsg.putStringProperty(Config.AMQ_INFLUX_KEY, k);
		
		txMsg.setExpiration(System.currentTimeMillis()+5000);
		txMsg.getBodyBuffer().writeString(j.toString());
		if (logger.isDebugEnabled())
			logger.debug("Msg body signalk.kv: {} = {}",k, j.toString());
		
		getProducer().send(Config.INTERNAL_KV,txMsg);
		
	}
	public abstract void consume(Message message);
	
	protected void send(Message message, String key, double d) throws ActiveMQException {
    	if (logger.isDebugEnabled())
			logger.debug("Sending: key: {}, value: {}", key, d);
		Json json = Json.object().set(value, d).set(timestamp, Util.getIsoTimeString());
		if (logger.isDebugEnabled())
			logger.debug("Sending: key: {}, value: {}", key, json);
    	sendKvMessage(message, key, json);
    	
	}
	
	protected void sendJson(Message message, String key, Json json) throws ActiveMQException {
		if(!json.isNull()) {
			json.set(timestamp, Util.getIsoTimeString());
		}
    	if (logger.isDebugEnabled())
			logger.debug("Sending json: key: {}, value: {}", key, json);
		
    	sendKvMessage(message, key, json);
    	
	}
	
	/**
	 *  Will start a consumer and call the consume(message) method for each matching message
	 *  
	 * @param filter
	 * @return
	 * @throws ActiveMQException
	 */
	public boolean startConsumer() throws ActiveMQException {

		if (getConsumer(filter).getMessageHandler() == null) {
			if (logger.isDebugEnabled())
				logger.debug("Adding consumer messageHandler : {}",INTERNAL_KV);

			getConsumer(filter).setMessageHandler(new MessageHandler() {

				@Override
				public void onMessage(ClientMessage message) {
					try {
						if (logger.isDebugEnabled())
							logger.debug("onMessage for client {}", message);
						
						message.acknowledge();
						
						consume(message);
					} catch (ActiveMQException e) {
						logger.error(e, e);
					}
				}
			});
			logger.debug("Set handler");
			return true;
		}
		return false;
	}
	

	public ClientSession getTxSession() {
		
		if(txSession==null)txSession=new ThreadLocal<>();

		if (txSession.get() == null) {
			if (logger.isDebugEnabled())
				logger.debug("Start amq session: {}", INTERNAL_KV);
			try {
				txSession.set(Util.getVmSession(getConfigProperty(ADMIN_USER),
						getConfigProperty(ADMIN_PWD)));
			} catch (Exception e) {
				logger.error(e, e);
			}
		}
		return txSession.get();
	}

	public ClientProducer getProducer() {
		
		if(producer==null)producer=new ThreadLocal<>();
		
		if (producer.get() == null && getTxSession() != null && !getTxSession().isClosed()) {
			if (logger.isDebugEnabled())
				logger.debug("Start producer: {}",INCOMING_RAW);
			
			try {
				producer.set(getTxSession().createProducer());
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}

		}
		return producer.get();

	}

	/**
	 * Create a consumer on the INTERNAL.KV queue, using the provided filter if not null.
	 *
	 * @param filter
	 * @return
	 */
	public ClientConsumer getConsumer(String filter) {

		if (consumer == null && getTxSession() != null && !getTxSession().isClosed()) {
			String uuid = INTERNAL_KV+"."+UUID.randomUUID().toString();
			if (logger.isDebugEnabled())
				logger.debug("Start consumer: {}", uuid);
			try {
				getTxSession().createQueue(INTERNAL_KV, RoutingType.MULTICAST, uuid);
				if(StringUtils.isBlank(filter)) {
					consumer = getTxSession().createConsumer(uuid);
				}else {
					consumer = getTxSession().createConsumer(uuid, filter);
				}
				getTxSession().start();
				
			} catch (ActiveMQException e) {
				logger.error(e, e);
			}
		}
		return consumer;

	}
	
	public void stop() {
		if(consumer!=null) {
			try {
				consumer.close();
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		
		if(producer.get()!=null) {
			try {
				producer.get().close();
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		if(txSession.get()!=null) {
			try {
				txSession.get().close();
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		
	}
	
	protected void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
