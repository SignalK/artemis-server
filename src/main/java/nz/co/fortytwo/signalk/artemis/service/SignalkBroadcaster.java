package nz.co.fortytwo.signalk.artemis.service;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.server.RoutingType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;

import nz.co.fortytwo.signalk.artemis.util.Util;

public class SignalkBroadcaster {

	private static Logger logger = LogManager.getLogger(SignalkBroadcaster.class);

	private final Broadcaster broadcaster;
	private ClientSession session;
	private ClientProducer producer;
	private ClientConsumer consumer;
	private String user;

	private SimpleString tempQ=new SimpleString("temp-001");

	public SignalkBroadcaster(Broadcaster broadcaster) {
		this.broadcaster = broadcaster;
	}

	public SignalkBroadcaster broadcast(String message) {
		logger.debug("Sending out to: "+user+" : "+message);
		broadcaster.broadcast(message);
		return this;
	}

	public void onOpen(AtmosphereResource resource) throws Exception {
		user = "guest";// resource.getRequest().getUserPrincipal().getName();
		session = Util.getLocalhostClientSession(user, user);
		logger.debug("Starting session for: "+user);
		session.start();
		producer = session.createProducer();
		session.createTemporaryQueue(new SimpleString("outgoing.reply"), RoutingType.MULTICAST, tempQ);
		consumer = session.createConsumer("temp-001", false);
		consumer.setMessageHandler(new MessageHandler() {
			
			@Override
			public void onMessage(ClientMessage message) {
				broadcast(message.getBodyBuffer().readString());
			}
		});
	}
	
	public void onClose(AtmosphereResource resource) throws Exception {
		if(producer!=null){
			producer.close();
		}
		if(session!=null){
			session.close();
		}
		
	}

	public void onMessage(String message) throws ActiveMQException {
		if(session==null || session.isClosed()){
			try {
				onOpen(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ClientMessage msg = session.createMessage(true);
		msg.getBodyBuffer().writeString(message);
		msg.putStringProperty("AMQ_REPLY_Q", tempQ.toString());
		producer.send("incoming.delta", msg);
		if (logger.isDebugEnabled())
			logger.debug("Sent:" + msg.getMessageID() + ":" + message);
	}
}
