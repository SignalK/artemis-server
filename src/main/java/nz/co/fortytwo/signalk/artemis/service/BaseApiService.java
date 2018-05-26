package nz.co.fortytwo.signalk.artemis.service;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgroups.util.UUID;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseApiService {
	static Logger logger = LogManager.getLogger(SignalkApiService.class);

	protected ClientSession txSession;
	protected ClientProducer producer;
	protected ClientConsumer consumer;
	protected String tempQ = UUID.randomUUID().toString();

	public BaseApiService() throws Exception {
		super();
		txSession = Util.getVmSession("admin", "admin");
		producer = txSession.createProducer();

		txSession.start();
		txSession.createTemporaryQueue("outgoing.reply." + tempQ, RoutingType.ANYCAST, tempQ);
		consumer = txSession.createConsumer(tempQ, false);
	}

	protected String sendMessage(String body) throws ActiveMQException {
		return sendMessage(body,null);
	}

	protected String sendMessage(String body, String correlation) throws ActiveMQException {
		ClientMessage message = txSession.createMessage(false);
		message.getBodyBuffer().writeString(body);
		message.putStringProperty(Config.AMQ_REPLY_Q, tempQ);
		if(correlation!=null){
			message.putStringProperty(Config.AMQ_CORR_ID,correlation);
		}
		//producer = txSession.createProducer();
		producer.send(Config.INCOMING_RAW, message);
		return correlation;
	}

	@Override
	protected void finalize() throws Throwable {
		if(consumer!=null){
			try {
				consumer.close();
			} catch (ActiveMQException e) {
				logger.warn(e,e);
			}
		}
		if(producer!=null){
			try{
				producer.close();
			} catch (ActiveMQException e) {
				logger.warn(e,e);
			}
		}
		if(txSession!=null){
			try{
				txSession.deleteQueue(tempQ);
				txSession.close();
			} catch (ActiveMQException e) {
				logger.warn(e,e);
			}
		}
		super.finalize();
	}

}