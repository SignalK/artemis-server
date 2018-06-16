package nz.co.fortytwo.signalk.artemis.service;

import java.io.IOException;

import javax.ws.rs.core.Context;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQIllegalStateException;
import org.apache.activemq.artemis.api.core.ActiveMQNonExistentQueueException;
import org.apache.activemq.artemis.api.core.ActiveMQQueueExistsException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.jgroups.util.UUID;

import nz.co.fortytwo.signalk.artemis.server.SubscriptionManagerFactory;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseApiService {

	protected static ClientProducer producer;

	protected static ClientSession txSession;

	protected  String tempQ;
	
	protected  ClientConsumer consumer;

	private static Logger logger = LogManager.getLogger(BaseApiService.class);
	
	protected long lastBroadcast = System.currentTimeMillis();
	
	@Context
	protected BroadcasterFactory broadCasterFactory;
	
	public BaseApiService()  {
		super();
		
	}

	protected void initSession(String tempQ) throws Exception {
			if(getTempQ()==null){
				this.tempQ=tempQ;
			}	
			getTxSession();
			getProducer();
			getConsumer();
	}
	
	protected String sendMessage(String body) throws ActiveMQException {
		return sendMessage(body,UUID.randomUUID().toString());
	}

	protected String sendMessage(String body, String correlation) throws ActiveMQException {
		ClientMessage message = new ClientMessageImpl((byte) 0, false, 0, System.currentTimeMillis(), (byte) 4, 1024);
		message.getBodyBuffer().writeString(body);
		message.putStringProperty(Config.AMQ_REPLY_Q, getTempQ());
		if(correlation!=null){
			message.putStringProperty(Config.AMQ_CORR_ID,correlation);
		}
		getProducer().send(new SimpleString(Config.INCOMING_RAW), message);
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
				try {
					event.getResource().close();
				} catch ( IllegalStateException | IOException e) {
					logger.error(e,e);
				}
		       logger.debug("onThrowable: {}",event);
				
			}
		    /**
		     * {@inheritDoc}
		     */
		    @Override
		    public void onDisconnect(AtmosphereResourceEvent event) {
		    	logger.debug("onDisconnect: {}",event);
		        try {
					event.getResource().close();
				} catch ( IllegalStateException | IOException e) {
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
				super.onClose(event);
			}

			@Override
			public void onHeartbeat(AtmosphereResourceEvent event) {
				logger.debug("onHeartbeat: {}",event);
				super.onHeartbeat(event);
			}
			@Override
			public void onBroadcast(AtmosphereResourceEvent event) {
				lastBroadcast=System.currentTimeMillis();
				logger.debug("onBroadcast: {}",event);
				super.onBroadcast(event);
			}
			
			@Override
			public void onClose(WebSocketEvent event) {
				logger.debug("onWebsocketClose: {}",event);
				super.onClose(event);
			}

			@Override
			public void onDisconnect(WebSocketEvent event) {
				logger.debug("onWebsocketDisconnect: {}",event);
				super.onDisconnect(event);
			}
			
			@Override
			public void onMessage(WebSocketEvent event) {
				lastBroadcast=System.currentTimeMillis();
				logger.debug("onWebsocketMessage: {}",event);
				super.onMessage(event);
			}
			@Override
			public void onControl(WebSocketEvent event) {
				logger.debug("onWebsocketControl: {}",event);
				super.onControl(event);
			}
			
			
			@Override
			public void onConnect(WebSocketEvent event) {
				logger.debug("onWebsocketConnect: {}",event);
				
				super.onConnect(event);
			}

			
		});
		
	}
	public void setConsumer(AtmosphereResource resource) throws ActiveMQException {
		
		if(getConsumer().getMessageHandler()==null){
			logger.debug("Adding consumer messageHandler : {}",getTempQ());
			resource.setBroadcaster(broadCasterFactory.get());
			getConsumer().setMessageHandler(new MessageHandler() {

				@Override
				public void onMessage(ClientMessage message) {
					try {
						String recv = message.getBodyBuffer().readString();
						message.acknowledge();
						logger.debug("onMessage for {}, {}",getTempQ(),recv);
						
						resource.getBroadcaster().broadcast(recv == null ? "{}" : recv, resource);
						logger.debug("Sent to resource: {}",resource);

					} catch (ActiveMQException e) {
						logger.error(e,e);
					} 
				}
			});
		}
		
	}
	private void closeSession()  {
	
		if(getConsumer()!=null){
			logger.debug("Close consumer: {}", tempQ);
			try {
				getConsumer().close();
				try {
					if(txSession.queueQuery(new SimpleString(getTempQ())).getConsumerCount()==0){
						logger.debug("Delete queue: {}", tempQ);
						txSession.deleteQueue(getTempQ());
					}
				} catch (ActiveMQNonExistentQueueException | ActiveMQIllegalStateException e) {
					logger.debug(e.getMessage());
				} 
			} catch (ActiveMQException e) {
				logger.warn(e,e);
			}
			
		}

	}

	public ClientSession getTxSession()  {

		if(txSession==null){
			logger.debug("Start amq session: {}", getTempQ());
			try{
				txSession = Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
						Config.getConfigProperty(Config.ADMIN_PWD));
				txSession.start();
			}catch(Exception e){
				logger.error(e,e);
			}
		}
		return txSession;
	}

	public ClientProducer getProducer() {
		if(producer==null && getTxSession()!=null && !getTxSession().isClosed()){
			logger.debug("Start producer: {}", getTempQ());
			try {
				producer=getTxSession().createProducer();
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		return producer;

	}

	public ClientConsumer getConsumer() {
		
		if(consumer==null&& getTxSession()!=null && !getTxSession().isClosed()){
			logger.debug("Start consumer: {}", getTempQ());
			try{
				try{
					txSession.createTemporaryQueue("outgoing.reply." + getTempQ(), RoutingType.ANYCAST, getTempQ());
				} catch (ActiveMQQueueExistsException e) {
					logger.debug(e.getMessage());
				}
				consumer=getTxSession().createConsumer(getTempQ());
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		return consumer;
		
	}

	public String getTempQ() {
		return tempQ;
	}

	

}