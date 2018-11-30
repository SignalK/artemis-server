package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.RoutingType;
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
			session.start();
			session.createTemporaryQueue("outgoing.reply." + tempQ, RoutingType.ANYCAST, tempQ);
			String token = SecurityUtils.authenticateUser("admin", "admin");
			sendSubsribeMsg(session,producer, "vessels." + self, "navigation",tempQ);
			
			sendMessage(session, producer, "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78", token);
			
			List<ClientMessage> replies = listen(session, tempQ, 10);
			assertTrue(replies.size()>1);
		} 
	}
	
	

	@Test
	@Ignore
	public void shouldStartNetty() throws Exception {
		
		
		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();	){
			session.start();

//			for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {
//
//				ClientMessage message = session.createMessage(true);
//				message.getBodyBuffer().writeString(line);
//				producer.send(Config.INCOMING_RAW, message);
//			}
		}
		CountDownLatch latch = new CountDownLatch(1);
		latch.await(60, TimeUnit.SECONDS);
	}
	@Test
	public void shouldReadPartialKeysForGuest() throws Exception {
		readPartialKeys("guest", 2);
	}
	
	@Test
	public void shouldReadPartialKeysForAdmin() throws Exception {
		readPartialKeys("admin", 2);
	}

	private void readPartialKeys(String user, int expected) throws Exception{
		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();	){
			session.start();
			String tempQ = UUID.randomUUID().toString();
			session.createTemporaryQueue("outgoing.reply." + tempQ, RoutingType.ANYCAST, tempQ);
			String token = SecurityUtils.authenticateUser("admin", "admin");
			sendSubsribeMsg(session,producer, "vessels." + self, "navigation",tempQ);
			
			sendMessage(session, producer, "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78", token);
			
			logger.debug("Subscribe sent");
		
			logger.debug("Receive started");
			List<ClientMessage> replies = listen(session, tempQ, 5);
			//assertEquals(expected, replies.size());
			logger.debug("Received {} replies", replies.size());
			assertTrue(replies.size()>=expected);
		} 
	}

	

	

}
