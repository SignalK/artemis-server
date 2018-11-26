package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.Config.INTERNAL_KV;

import java.util.UUID;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;

/**
 * Base for handlers that read key value pairs from Config.INTERNAL_KV
 * 
 * @author robert
 *
 */
public abstract class BaseHandler extends MessageSupport{
	
	private static Logger logger = LogManager.getLogger(BaseHandler.class);
	
	protected static TDBService influx = new InfluxDbService();
	
	protected String filter=null;
	
	protected ClientConsumer consumer;
	
	public BaseHandler() {
		uuid = Config.getConfigProperty(ConfigConstants.UUID);
	}

	protected void initSession(String filter) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("initSession: {}", filter);
		super.initSession();
		this.filter=filter;
		getConsumer(filter);
	}
	
	public abstract void consume(Message message);
	
	
	
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
				consumer.setMessageHandler(null);
				consumer.close();
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		
		super.stop();
		
	}
	
	protected void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
