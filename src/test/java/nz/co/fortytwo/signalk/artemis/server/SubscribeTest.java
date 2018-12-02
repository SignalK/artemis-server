package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgroups.util.UUID;
import org.junit.Ignore;
import org.junit.Test;

import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class SubscribeTest extends BaseServerTest{
	
	private static Logger logger = LogManager.getLogger(SubscribeTest.class);

	@Test
	public void checkSelfSubscribe() throws Exception {

		String tempQ = UUID.randomUUID().toString();
		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();	
				){
			
			session.createTemporaryQueue("outgoing.reply." + tempQ, RoutingType.ANYCAST, tempQ);
			ClientConsumer consumer = session.createConsumer(tempQ);
			List<ClientMessage> replies = createListener(session, consumer, tempQ);
			session.start();
			String token = SecurityUtils.authenticateUser("admin", "admin");
			sendSubsribeMsg(session,producer, "vessels.self", nav,tempQ, token);
			
			sendMessage(session, producer, "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78", token);
			
			replies = listen(replies, 3, 10);
			assertTrue(replies.size()>1);
		} 
	}
	
	
	@Test
	public void shouldReadPartialKeysForGuest() throws Exception {
		readPartialKeys("public", 2);
	}
	
	@Test
	public void shouldReadPartialKeysForAdmin() throws Exception {
		readPartialKeys("admin", 2);
	}

	private void readPartialKeys(String user, int expected) throws Exception{
		try (ClientSession session = Util.getLocalhostClientSession(user, user);
				ClientProducer producer = session.createProducer();	){
		
			String tempQ = UUID.randomUUID().toString();
			session.createTemporaryQueue("outgoing.reply." + tempQ, RoutingType.ANYCAST, tempQ);
			ClientConsumer consumer = session.createConsumer(tempQ);
			List<ClientMessage> replies = createListener(session, consumer, tempQ);
			session.start();
			String token = SecurityUtils.authenticateUser(user, user);
			sendSubsribeMsg(session,producer, "vessels.self", nav, tempQ, token);
			
			sendMessage(session, producer, "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78", token);
			
			logger.debug("Subscribe sent");
		
			logger.debug("Receive started");
			replies = listen(replies, 5, 5);
			//assertEquals(expected, replies.size());
			logger.debug("Received {} replies", replies.size());
			for(ClientMessage c: replies) {
				logger.debug("Received {} ", Util.readBodyBuffer(c.toCore()));
			}
			assertTrue(replies.size()>=expected);
		} 
	}

	

	

}
