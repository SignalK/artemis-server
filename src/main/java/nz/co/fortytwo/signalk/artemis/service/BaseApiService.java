package nz.co.fortytwo.signalk.artemis.service;

import java.io.IOException;

import javax.ws.rs.core.Context;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceSession;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;

import nz.co.fortytwo.signalk.artemis.server.Subscription;
import nz.co.fortytwo.signalk.artemis.server.SubscriptionManagerFactory;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseApiService {
	protected static final String PRODUCER = "producer";

	protected static final String TX_SESSION = "txSession";

	protected static final String TEMP_Q = "tempQ";

	protected static final String CONSUMER = "consumer";

	private static Logger logger = LogManager.getLogger(BaseApiService.class);

	protected AtmosphereResourceSession resourceSession;
	
	@Context
	protected BroadcasterFactory broadCasterFactory;
	
	public BaseApiService()  {
		super();
		
	}

	protected void initSession(String tempQ, AtmosphereResourceSession resourceSession) throws Exception {
			this.resourceSession=resourceSession;
			if(getTempQ()==null){
				resourceSession.setAttribute(TEMP_Q,tempQ);
			}	
	}
	
	protected String sendMessage(String body) throws ActiveMQException {
		return sendMessage(body,null);
	}

	protected String sendMessage(String body, String correlation) throws ActiveMQException {
		ClientMessage message = getTxSession().createMessage(false);
		message.getBodyBuffer().writeString(body);
		message.putStringProperty(Config.AMQ_REPLY_Q, getTempQ());
		if(correlation!=null){
			message.putStringProperty(Config.AMQ_CORR_ID,correlation);
		}
		//producer = getTxSession().createProducer();
		getProducer().send(Config.INCOMING_RAW, message);
		return correlation;
	}

	

	@Override
	protected void finalize() throws Throwable {
		closeSession();
		super.finalize();
	}

	protected void addCloseListener(AtmosphereResource resource) {
		resource.addEventListener( new WebSocketEventListenerAdapter() {
			
			@Override
			public void onThrowable(AtmosphereResourceEvent event) {
				closeSession();
		       logger.debug("onThrowable: {}",event);
				
			}
		    /**
		     * {@inheritDoc}
		     */
		    @Override
		    public void onDisconnect(AtmosphereResourceEvent event) {
		    	logger.debug("onDisconnect: {}",event);
		        closeSession();
		        try {
		        	event.getResource().removeFromAllBroadcasters();
					event.getResource().close();
				} catch (IOException e) {
					logger.error(e,e);
				}
		        super.onDisconnect(event);
		    }

			@Override
			public void onClose(AtmosphereResourceEvent event) {
				logger.debug("onClose: {}",event);
				closeSession();
				try {
					SubscriptionManagerFactory.getInstance().removeByTempQ(getTempQ());
				} catch (Exception e) {
					logger.error(e,e);
				}
		        event.getResource().removeFromAllBroadcasters();   
				super.onClose(event);
			}

			@Override
			public void onClose(WebSocketEvent event) {
				logger.debug("onClose: {}",event);
				//closeSession();
//				event.webSocket().resource().removeFromAllBroadcasters();
//				try {
//					event.webSocket().resource().close();
//				} catch (IOException e) {
//					logger.error(e,e);
//				}
				super.onClose(event);
			}

			@Override
			public void onDisconnect(WebSocketEvent event) {
				logger.debug("onDisconnect: {}",event);
//				closeSession();
//				event.webSocket().resource().removeFromAllBroadcasters();
//				try {
//					event.webSocket().resource().close();
//				} catch (IOException e) {
//					logger.error(e,e);
//				}
				super.onDisconnect(event);
			}

			
		});
		
	}
	protected void closeSession()  {
		if(getConsumer()!=null){
			try {
				getConsumer().close();
			} catch (ActiveMQException e) {
				logger.warn(e,e);
			}
		}
		if(getProducer()!=null){
			try{
				getProducer().close();
			} catch (ActiveMQException e) {
				logger.warn(e,e);
			}
		}
		if(getTxSession()!=null&& !getTxSession().isClosed()){
			try{
				try{
					getTxSession().deleteQueue(getTempQ());
				} catch (ActiveMQException e) {
					logger.warn(e);
				}
				getTxSession().close();
			} catch (ActiveMQException e) {
				logger.warn(e,e);
			}
		}
		
	}

	public ClientSession getTxSession()  {
		if(resourceSession.getAttribute(TX_SESSION)==null){
			try{
				ClientSession txSession = Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
						Config.getConfigProperty(Config.ADMIN_PWD));
				resourceSession.setAttribute(TX_SESSION,txSession);
				txSession.start();
			
				txSession.createTemporaryQueue("outgoing.reply." + getTempQ(), RoutingType.ANYCAST, getTempQ());
			}catch(Exception e){
				logger.error(e,e);
			}
		}
		return (ClientSession) resourceSession.getAttribute(TX_SESSION);
	}

	public ClientProducer getProducer() {
		if(resourceSession.getAttribute(PRODUCER)==null && getTxSession()!=null && !getTxSession().isClosed()){
			try {
				resourceSession.setAttribute(PRODUCER,getTxSession().createProducer());
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		return (ClientProducer) resourceSession.getAttribute(PRODUCER);
	}

	public ClientConsumer getConsumer() {
		if(resourceSession.getAttribute(CONSUMER)==null&& getTxSession()!=null && !getTxSession().isClosed()){
			try{
				resourceSession.setAttribute(CONSUMER,getTxSession().createConsumer(getTempQ()));
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		return (ClientConsumer) resourceSession.getAttribute(CONSUMER);
	}

	public String getTempQ() {
		return (String) resourceSession.getAttribute(TEMP_Q);
	}

}