package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.timestamp;
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
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
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
		if(server!=null)server.stop();
	}

	@Test
	public void checkSimpleVMConnection() throws Exception {

		ClientSession session = Util.getVmSession("guest", "guest");
		session.start();

		ClientProducer producer = session.createProducer("vessels.example");

		ClientMessage message = session.createMessage(true);
		message.getBodyBuffer().writeString("Hello1");
		message.putStringProperty(Config._AMQ_LVQ_NAME, "KEY_1");
		producer.send("vessels.example", message);

		message = session.createMessage(true);
		message.getBodyBuffer().writeString("Hello2");
		message.putStringProperty(Config._AMQ_LVQ_NAME, "KEY_1");
		producer.send("vessels.example", message);

		message = session.createMessage(true);
		message.getBodyBuffer().writeString("Hello3");
		message.putStringProperty(Config._AMQ_LVQ_NAME, "KEY_2");
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

		ClientSession session = Util.getVmSession("guest", "guest");
		session.start();


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
				"_AMQ_LVQ_NAME like 'vessels.urn:mrn:imo:mmsi:123456789.navigation.%'", true);
		ClientMessage msgReceived = null;
		while ((msgReceived = consumer.receive(10)) != null) {
			Object recv = Util.readBodyBuffer(msgReceived);
			if (logger.isDebugEnabled())
				logger.debug("message = " + msgReceived.getAddress() + ", " + recv);
				logger.debug("message timestamp: " + msgReceived.getStringProperty(timestamp));
				logger.debug("message source: " + msgReceived.getStringProperty(source));
			// if(logger.isDebugEnabled())logger.debug("message = "
			// +msgReceived.toString());
			d++;
		}
		consumer.close();
		session.close();
		if (logger.isDebugEnabled())
			logger.debug("Sent = " + c + ", recd=" + d);
		assertEquals(1000, c);
		assertEquals(27, d);
	}

	@Test
	public void shouldReadPartialKeysForAdmin() throws Exception {

		ClientSession session = Util.getVmSession("admin", "admin");
		session.start();

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
				"_AMQ_LVQ_NAME like 'vessels.urn:mrn:imo:mmsi:123456789.navigation.%'", true);
		ClientMessage msgReceived = null;
		while ((msgReceived = consumer.receive(10)) != null) {
			d++;
			//logger.debug(msgReceived.getAddress().toString()+":bytes:"+msgReceived.getBodyBuffer().readableBytes());
			if(msgReceived.getBodyBuffer().readableBytes()==1){
				logger.debug("Value:"+msgReceived.getBodyBuffer().readBoolean());
				continue;
			}
			if(msgReceived.getBodyBuffer().readableBytes()==0){
				logger.debug("Value:"+null);
				continue;
			}
			Object recv = Util.readBodyBuffer(msgReceived);
			
			if (logger.isDebugEnabled())
				logger.debug("message = " + msgReceived.getMessageID() + ":" + msgReceived.getAddress() + ", " + recv);
			// if(logger.isDebugEnabled())logger.debug("message = "
			// +msgReceived.toString());
			
		}
		consumer.close();
		session.close();
		if (logger.isDebugEnabled())
			logger.debug("Sent = " + c + ", recd=" + d);
		assertEquals(1000, c);
		assertEquals(32, d);
	}

	@Test
	public void shouldEditConfigForAdmin() throws Exception {
		Map<String, Json> model = shouldReadConfigForUser("admin", 38);
		//check String
		assertEquals("system", model.get("config.server.clock.src").asString());
		//check boolean
		assertEquals(true, model.get("config.server.apps.install.allow").asBoolean());
		//check int
		assertEquals(38400L, model.get("config.server.serial.baud").asLong());
		
		
		
		ClientSession session = Util.getVmSession("admin", "admin");

		ClientProducer producer = session.createProducer();
		session.start();
		
		ClientMessage message = session.createMessage(true);
		message.getBodyBuffer().writeString("{ \"config\": { \"server\": { \"clock\": { \"src\": \"gps\" } } }}");
		producer.send("incoming.delta", message);
		
		model = shouldReadConfigForUser("admin", 38);
		assertEquals("gps", model.get("config.server.clock.src").asString());
		
		message = session.createMessage(true);
		message.getBodyBuffer().writeString("{ \"config\": { \"server\": { \"clock\": { \"src\": \"system\" } } }}");
		producer.send("incoming.delta", message);
		
		model = shouldReadConfigForUser("admin", 38);
		assertEquals("system", model.get("config.server.clock.src").asString());
		
		session.close();

	}
	
	@Test
	public void shouldReadConfigForAdmin() throws Exception {
		shouldReadConfigForUser("admin", 38);
	}
	
	@Test
	public void shouldNotReadConfigForGuest() throws Exception {
		try{
			shouldReadConfigForUser("guest", 0);
		}catch (ActiveMQSecurityException se){
			return;
		}
		fail("Should throw security exception");
	}
	
	
	public Map<String,Json> shouldReadConfigForUser(String user, int items) throws Exception {
		Map<String,Json> model = new HashMap<>();
		ClientSession session = Util.getVmSession(user, user);
		session.start();

		int d = 0;
		// now read config
		ClientConsumer consumer = session.createConsumer("config",
				"_AMQ_LVQ_NAME like 'config.%'", true);
		ClientMessage msgReceived = null;
		while ((msgReceived = consumer.receive(10)) != null) {
			//logger.debug(msgReceived.getAddress().toString()+":bytes:"+msgReceived.getBodyBuffer().readableBytes());
			d++;
			
			Json recv = Util.readBodyBuffer(msgReceived);
			if (logger.isDebugEnabled())
				logger.debug("message = " + msgReceived.getMessageID() + ":" + msgReceived.getAddress() + ", " + recv);
			model.put(msgReceived.getAddress().toString(), recv);
			
		}
		consumer.close();
		session.close();
		if (logger.isDebugEnabled())
			logger.debug("Recd=" + d);
		
		assertEquals(items, d);
		return model;
	}
	
	


	

}
