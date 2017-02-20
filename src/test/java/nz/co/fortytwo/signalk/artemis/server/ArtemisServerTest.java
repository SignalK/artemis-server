package nz.co.fortytwo.signalk.artemis.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.ActiveMQSecurityException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.RoutingType;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.divert.UnpackUpdateMsg;
import nz.co.fortytwo.signalk.artemis.intercept.SessionInterceptor;
import nz.co.fortytwo.signalk.handler.DeltaToMapConverter;
import nz.co.fortytwo.signalk.model.SignalKModel;
import nz.co.fortytwo.signalk.util.SignalKConstants;

public class ArtemisServerTest {
	ArtemisServer server;
	private static Logger logger = LogManager.getLogger(ArtemisServerTest.class);

	@Before
	public void startServer() throws Exception {
		server = new ArtemisServer();
	}

	@After
	public void stopServer() throws Exception {
		server.embedded.stop();
	}

	@Test
	public void checkSimpleVMConnection() throws Exception {

		ClientSession session = getVmSession("guest", "guest");
		session.start();
		// session.createAddress(new SimpleString("vessels.#"),
		// RoutingType.MULTICAST, true);
		// session.createQueue("vessels.#", RoutingType.MULTICAST, "vessels",
		// true);

		ClientProducer producer = session.createProducer("vessels.example");

		ClientMessage message = session.createMessage(true);
		message.getBodyBuffer().writeString("Hello1");
		message.putStringProperty("_AMQ_LVQ_NAME", "KEY_1");
		producer.send("vessels.example", message);

		message = session.createMessage(true);
		message.getBodyBuffer().writeString("Hello2");
		message.putStringProperty("_AMQ_LVQ_NAME", "KEY_1");
		producer.send("vessels.example", message);

		message = session.createMessage(true);
		message.getBodyBuffer().writeString("Hello3");
		message.putStringProperty("_AMQ_LVQ_NAME", "KEY_2");
		producer.send("vessels.example.navigation", message);

		ClientConsumer consumer = session.createConsumer("vessels", true);

		ClientMessage msgReceived = consumer.receive(10);
		String recv = msgReceived.getBodyBuffer().readString();
		consumer.close();
		if (logger.isDebugEnabled())
			logger.debug("message = " + recv);
		assertEquals("Hello2", recv);
		consumer = session.createConsumer("vessels", true);
		msgReceived = consumer.receive(10);
		assertNotNull(msgReceived);
		assertEquals("Hello2", msgReceived.getBodyBuffer().readString());
		// reread last message
		msgReceived = consumer.receive(10);
		assertNotNull(msgReceived);
		assertEquals("Hello3", msgReceived.getBodyBuffer().readString());
		if (logger.isDebugEnabled())
			logger.debug("message3 = " + msgReceived.getAddress());
		if (logger.isDebugEnabled())
			logger.debug("message3 = " + msgReceived.toString());
		session.close();
	}

	@Test
	public void shouldReadPartialKeysForGuest() throws Exception {

		ClientSession session = getVmSession("guest", "guest");
		session.start();

		ClientSession session2 = getVmSession("admin", "admin");
		session2.start();

		ClientProducer producer = session.createProducer();
		int c = 0;
		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {

			ClientMessage message = session.createMessage(true);
			message.getBodyBuffer().writeString(line);
			producer.send("incoming.delta", message);
			if (logger.isDebugEnabled())
				logger.debug("Sent:" + message.getMessageID() + ":" + line);
			c++;

		}
		int d = 0;
		// now read partial
		ClientConsumer consumer = session.createConsumer("vessels",
				"_AMQ_LVQ_NAME like 'vessels.urn:mrn:imo:mmsi:123456789.navigation.%.value'", true);
		ClientMessage msgReceived = null;
		while ((msgReceived = consumer.receive(10)) != null) {
			String recv = msgReceived.getBodyBuffer().readString();
			if (logger.isDebugEnabled())
				logger.debug("message = " + msgReceived.getAddress() + ", " + recv);
			// if(logger.isDebugEnabled())logger.debug("message = "
			// +msgReceived.toString());
			d++;
		}
		consumer.close();
		session.close();
		if (logger.isDebugEnabled())
			logger.debug("Sent = " + c + ", recd=" + d);
		assertEquals(1000, c);
		assertEquals(11, d);
	}

	@Test
	public void shouldReadPartialKeysForAdmin() throws Exception {

		ClientSession session = getVmSession("admin", "admin");
		session.start();

		ClientSession session2 = getVmSession("admin", "admin");
		session2.start();

		ClientProducer producer = session.createProducer();
		int c = 0;
		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {

			ClientMessage message = session.createMessage(true);
			message.getBodyBuffer().writeString(line);
			producer.send("incoming.delta", message);
			if (logger.isDebugEnabled())
				logger.debug("Sent:" + message.getMessageID() + ":" + line);
			c++;

		}
		int d = 0;
		// now read partial
		ClientConsumer consumer = session.createConsumer("vessels",
				"_AMQ_LVQ_NAME like 'vessels.urn:mrn:imo:mmsi:123456789.navigation.%.value'", true);
		ClientMessage msgReceived = null;
		while ((msgReceived = consumer.receive(10)) != null) {
			String recv = msgReceived.getBodyBuffer().readString();
			if (logger.isDebugEnabled())
				logger.debug("message = " + msgReceived.getMessageID() + ":" + msgReceived.getAddress() + ", " + recv);
			// if(logger.isDebugEnabled())logger.debug("message = "
			// +msgReceived.toString());
			d++;
		}
		consumer.close();
		session.close();
		if (logger.isDebugEnabled())
			logger.debug("Sent = " + c + ", recd=" + d);
		assertEquals(1000, c);
		assertEquals(16, d);
	}

	@Test
	public void shouldReadConfigForAdmin() throws Exception {
		shouldReadConfigForUser("admin", 36);
	}
	
	@Test
	public void shouldReadConfigForGuest() throws Exception {
		try{
			shouldReadConfigForUser("guest", 0);
		}catch (ActiveMQSecurityException se){
			return;
		}
		fail("Shoud throw security exception");
	}
	
	
	public void shouldReadConfigForUser(String user, int items) throws Exception {
		ClientSession session = getVmSession(user, user);
		session.start();

		ClientProducer producer = session.createProducer();
		int c = 0;

		String config = FileUtils.readFileToString(new File("./src/test/resources/samples/signalk-config.json"));
		ClientMessage message = session.createMessage(true);
		message.getBodyBuffer().writeString(config);
		producer.send("incoming.delta", message);
		if (logger.isDebugEnabled())
			logger.debug("Sent:" + message.getMessageID() + ":" + config);

		int d = 0;
		// now read config
		ClientConsumer consumer = session.createConsumer("config",
				"_AMQ_LVQ_NAME like 'config.%'", true);
		ClientMessage msgReceived = null;
		while ((msgReceived = consumer.receive(10)) != null) {
			String recv = msgReceived.getBodyBuffer().readString();
			if (logger.isDebugEnabled())
				logger.debug("message = " + msgReceived.getMessageID() + ":" + msgReceived.getAddress() + ", " + recv);
			// if(logger.isDebugEnabled())logger.debug("message = "
			// +msgReceived.toString());
			d++;
		}
		consumer.close();
		session.close();
		if (logger.isDebugEnabled())
			logger.debug("Sent = " + c + ", recd=" + d);
		
		assertEquals(items, d);
	}
	
	

	@Test
	public void checkSimpleTCPConnection() throws Exception {

		ClientSession session = getLocalhostClientSession("guest", "guest");

		// session.createQueue("vessels.#", RoutingType.ANYCAST, "vessels",
		// true);

		ClientProducer producer = session.createProducer("vessels.example");

		ClientMessage message = session.createMessage(true);
		message.putStringProperty("_AMQ_LVQ_NAME", "KEY_1");
		message.getBodyBuffer().writeString("Hello");
		producer.send(message);

		session.start();

		ClientConsumer consumer = session.createConsumer("vessels", true);
		ClientConsumer consumer1 = session.createConsumer("vessels", true);

		ClientMessage msgReceived = consumer.receive();

		String recv = msgReceived.getBodyBuffer().readString();
		if (logger.isDebugEnabled())
			logger.debug("message = " + recv);
		assertEquals("Hello", recv);
		msgReceived = consumer1.receive(100);
		assertNotNull(msgReceived);
		session.close();
	}

	public ClientSession getVmSession(String user, String password) throws Exception {
		ClientSessionFactory nettyFactory = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
				.createSessionFactory();
		return nettyFactory.createSession(user, password, false, true, true, false, 10);
	}

	public ClientSession getLocalhostClientSession(String user, String password) throws Exception {
		Map<String, Object> connectionParams = new HashMap<String, Object>();

		connectionParams.put(TransportConstants.PORT_PROP_NAME, 61617);

		ClientSessionFactory nettyFactory = ActiveMQClient
				.createServerLocatorWithoutHA(
						new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams))
				.createSessionFactory();
		return nettyFactory.createSession(user, password, false, true, true, false, 10);
	}

}
