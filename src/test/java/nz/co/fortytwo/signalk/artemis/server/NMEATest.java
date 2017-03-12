package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.util.SignalKConstants.POLICY_FIXED;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.core.server.RoutingType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class NMEATest {
	ArtemisServer server;
	private static Logger logger = LogManager.getLogger(NMEATest.class);

	@Before
	public void startServer() throws Exception {
		server = new ArtemisServer();
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	

	@Test
	public void shouldReadPartialKeysForGuest() throws Exception {
		readPartialKeys("guest", 16);
	}

	@Test
	public void shouldReadPartialKeysForAdmin() throws Exception {
		readPartialKeys("admin", 16);
	}

	private void readPartialKeys(String user, int expected) throws Exception {
		ClientSession session = Util.getLocalhostClientSession(user, user);

		try {
			session.start();
			ClientProducer producer = session.createProducer();

			ClientMessage message = session.createMessage(true);
			String line = "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78";
			message.getBodyBuffer().writeString(line);
			producer.send("incoming.delta", message);
			if (logger.isDebugEnabled())
				logger.debug("Sent:" + message.getMessageID() + ":" + line);

			// subscribe, and wait for some responses, should be 1 per second
			ClientMessage subMsg = session.createMessage(true);
			Json msg = getJson("vessels.self", "navigation", 1000, 0, FORMAT_DELTA, POLICY_FIXED);
			subMsg.getBodyBuffer().writeString(msg.toString());
			session.createTemporaryQueue("outgoing.reply.temp-001",RoutingType.MULTICAST, "temp-001");
			subMsg.putStringProperty("AMQ_REPLY_Q", "temp-001");
			producer.send("incoming.delta", subMsg);
			producer.close();

			CountDownLatch latch = new CountDownLatch(1);
			latch.await(3, TimeUnit.SECONDS);

			int d = 0; // now read partial
			ClientConsumer consumer = session.createConsumer("temp-001", false);
			ClientMessage msgReceived = null;
			while ((msgReceived = consumer.receive(10)) != null) {
				Object recv = Util.readBodyBuffer(msgReceived);
				if (logger.isDebugEnabled())
					logger.debug("Client message = " + msgReceived.getStringProperty(Config._AMQ_LVQ_NAME) + ", " + recv); //
				if (logger.isDebugEnabled())
					logger.debug("Client message = " + msgReceived.toString());
				d++;
			}

			consumer.close();
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
