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

import nz.co.fortytwo.signalk.artemis.tdb.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.tdb.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.MessageSupport;

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
	protected String clazz;

	private String queue=null;

	private RoutingType route;
	
	public BaseHandler() {
		uuid = Config.getConfigProperty(ConfigConstants.UUID);
		clazz = getClass().getSimpleName();
	}

	protected void initSession(String filter) throws Exception {
		initSession(filter, INTERNAL_KV,RoutingType.MULTICAST);
	}
	protected void initSession(String filter, String queue, RoutingType route) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("{}: initSession: {}",clazz, filter);
		super.initSession();
		this.filter=filter;
		this.queue=queue;
		this.route=route;
		getConsumer(filter, queue, route);
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

		if (getConsumer(filter, queue, route).getMessageHandler() == null) {
			if (logger.isDebugEnabled())
				logger.debug("{}: Adding consumer messageHandler for  {}", clazz,INTERNAL_KV);

			getConsumer(filter, queue, route).setMessageHandler(new MessageHandler() {

				@Override
				public void onMessage(ClientMessage message) {
					try {
						if (logger.isDebugEnabled())
							logger.debug("{}: onMessage for {}", clazz,message);
						
						message.acknowledge();
						
						consume(message);
					} catch (ActiveMQException e) {
						logger.error(e, e);
					}
				}
			});
			logger.debug("{}: Set handler ", clazz);
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
		return getConsumer(filter, INTERNAL_KV, RoutingType.MULTICAST);
	}
	
	public ClientConsumer getConsumer(String filter, String queue, RoutingType route) {

		if (consumer == null && getTxSession() != null && !getTxSession().isClosed()) {
			String qName = queue+"."+clazz+"-"+UUID.randomUUID().toString();
			if (logger.isDebugEnabled())
				logger.debug("{}: Start consumer for {} ", clazz, qName);
			try {
				if(StringUtils.isBlank(filter)) {
					getTxSession().createQueue(queue, route, qName, false);
				}else {
					getTxSession().createQueue(queue, route, qName, filter, false);
				}
				
				consumer = getTxSession().createConsumer(qName);
				
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
				//consumer.setMessageHandler(null);
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
