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

import static nz.co.fortytwo.signalk.artemis.util.Config.*;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Base for handlers that read key value pairs from Config.INTERNAL_KV
 * 
 * @author robert
 *
 */
public abstract class BaseHandler {
	
	private static Logger logger = LogManager.getLogger(BaseHandler.class);
	
	protected static ThreadLocal<ClientProducer> producer=new ThreadLocal<>();

	protected static ThreadLocal<ClientSession> txSession=new ThreadLocal<>();

	protected ClientConsumer consumer;
	
	protected String filter=null;

	protected void initSession(String filter) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("initSession: {}", filter);
		this.filter=filter;
		getTxSession();
		getProducer();
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
	private void closeSession() {

		if (getConsumer(filter) != null) {
			if (logger.isDebugEnabled())
				logger.debug("Close consumer: {}", INTERNAL_KV);
			try {

					getConsumer(filter).close();
				
			} catch (ActiveMQException e) {
				logger.warn(e, e);
			}

		}

	}

	public ClientSession getTxSession() {

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
			if (logger.isDebugEnabled())
				logger.debug("Start consumer: {}", INTERNAL_KV);
			try {
				if(StringUtils.isBlank(filter)) {
					consumer = getTxSession().createConsumer(INTERNAL_KV);
				}else {
					consumer = getTxSession().createConsumer(INTERNAL_KV, filter);
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
}
