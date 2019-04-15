package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.Config.INTERNAL_KV;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import nz.co.fortytwo.signalk.artemis.server.BaseServerTest;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class NMEAToKvTest extends BaseServerTest{
	
	private static Logger logger = LogManager.getLogger(NMEAToKvTest.class);

	
	@Test
	public void shouldReadKvQueue() throws Exception {
		readPartialKeys("admin", 2);
	}
	
	

	private void readPartialKeys(String user, int expected) throws Exception{
		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();
				){
			String qName=Config.INTERNAL_KV+dot+UUID.randomUUID().toString();
			session.createQueue(INTERNAL_KV, RoutingType.MULTICAST, qName);
			ClientConsumer consumer = session.createConsumer(qName);
			List<ClientMessage> replies = createListener(session, consumer, qName);
			session.start();
			String token = SecurityUtils.authenticateUser("admin", "admin");
			sendMessage(session, producer, "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78", token);
			
			logger.debug("Input sent");
		
			logger.debug("Receive started");
			replies = listen(replies, 5, 10);
			session.stop();
			logger.debug("Received {} replies", replies.size());
			replies.forEach((m)->{
				logger.debug("Received {}", m);
			});
			assertTrue(replies.size()>=expected);
		} 
	}

	

	

}
