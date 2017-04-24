package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.util.SignalKConstants.POLICY_FIXED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.core.server.RoutingType;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgroups.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.util.SignalKConstants;

public class SubscribeTest {
	ArtemisServer server;
	private static Logger logger = LogManager.getLogger(SubscribeTest.class);

	@Before
	public void startServer() throws Exception {
		server = new ArtemisServer();
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void checkSelfSubscribe() throws Exception {

		ClientSession session = Util.getLocalhostClientSession("guest", "guest");
		try {
			session.start();

			ClientProducer producer = session.createProducer(Config.INCOMING_RAW);

			ClientMessage message = session.createMessage(true);
			Json msg = getJson("vessels." + SignalKConstants.self, "navigation", 1000, 0, FORMAT_DELTA, POLICY_FIXED);
			message.getBodyBuffer().writeString(msg.toString());
			String tempQ = UUID.randomUUID().toString();
			session.createTemporaryQueue("outgoing.reply."+tempQ, RoutingType.MULTICAST, tempQ);
			message.putStringProperty(Config.AMQ_REPLY_Q, tempQ);
			
			producer.send(message);
			
			message = session.createMessage(true);
			String line = "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78";
			message.getBodyBuffer().writeString(line);
			producer.send(Config.INCOMING_RAW, message);
			
			producer.close();
			
			ClientConsumer consumer = session.createConsumer(tempQ,false);
			CountDownLatch latch = new CountDownLatch(1);
			latch.await(3, TimeUnit.SECONDS);
			ClientMessage msgReceived = consumer.receive(10000);
			String recv = msgReceived.getBodyBuffer().readString();
			logger.debug("rcvd message = " + recv);
			// assertEquals("Hello", recv);
			assertNotNull(msgReceived);
			consumer.close();
		} finally {
			session.close();
		}
	}

	@Test
	public void shouldStartNetty() throws Exception {
		ClientSession session = Util.getLocalhostClientSession("admin", "admin");
		
		try {
			session.start();
			ClientProducer producer = session.createProducer();

			for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {

				ClientMessage message = session.createMessage(true);
				message.getBodyBuffer().writeString(line);
				producer.send(Config.INCOMING_RAW, message);
				
			}
			producer.close();
		}finally{
			session.close();
		}
		CountDownLatch latch = new CountDownLatch(1);
		latch.await(60, TimeUnit.SECONDS);
	}
	@Test
	public void shouldReadPartialKeysForGuest() throws Exception {
		readPartialKeys("guest", 6);
	}
	
	@Test
	public void shouldReadPartialKeysForAdmin() throws Exception {
		readPartialKeys("admin", 6);
	}

	private void readPartialKeys(String user, int expected) throws Exception{
		ClientSession session = Util.getLocalhostClientSession(user, user);
	
		try {
			session.start();
			ClientProducer producer = session.createProducer();

			
			int c = 0;
			for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {

				ClientMessage message = session.createMessage(true);
				message.getBodyBuffer().writeString(line);
				producer.send(Config.INCOMING_RAW, message);
				if (logger.isDebugEnabled())
					logger.debug("Sent:" + message.getMessageID() + ":" + line);
				c++;

			}
			//subscribe, and wait for some responses, should be 1 per second
			ClientMessage subMsg = session.createMessage(true);
			Json msg = getJson("vessels.urn:mrn:imo:mmsi:123456789", "navigation", 1000, 0, FORMAT_DELTA, POLICY_FIXED);
			subMsg.getBodyBuffer().writeString(msg.toString());
			String tempQ = UUID.randomUUID().toString();
			session.createTemporaryQueue("outgoing.reply."+tempQ, RoutingType.MULTICAST, tempQ);
			subMsg.putStringProperty(Config.AMQ_REPLY_Q, tempQ);
			producer.send(Config.INCOMING_RAW, subMsg);
			producer.close();
			logger.debug("Subscribe sent");
			
			CountDownLatch latch = new CountDownLatch(1);
			latch.await(5, TimeUnit.SECONDS);
			logger.debug("Receive started");
			
			int d = 0; // now read partial
			ClientConsumer consumer = session.createConsumer(tempQ, false);
			ClientMessage msgReceived = null;
			while ((msgReceived = consumer.receive(100)) != null) {
				Object recv = Util.readBodyBuffer(msgReceived);
				if (logger.isDebugEnabled())
					logger.debug("Client message = " + msgReceived.getAddress() + ", " + recv); //
				if (logger.isDebugEnabled())
					logger.debug("Client message = " + msgReceived.toString());
				d++;
			}

			
			consumer.close();

			// session.close();
			// if(logger.isDebugEnabled())logger.debug("Sent = " + c + ", recd="
			// +
			// d);
			assertEquals(1000, c);
			assertEquals(expected, d);
		} finally {
			session.close();
		}
	}

	private Json getJson(String context, String path, int period, int minPeriod, String format, String policy) {
		Json json = Json.read("{\"context\":\"" + context + "\", \"subscribe\": []}");
		Json sub = Json.object();
		sub.set("path", path);
		sub.set("period", period);
		sub.set("minPeriod", minPeriod);
		sub.set("format", format);
		sub.set("policy", policy);
		json.at("subscribe").add(sub);
		logger.debug("Created json sub: " + json);
		return json;
	}

}
