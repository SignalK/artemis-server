package nz.co.fortytwo.signalk.artemis.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.junit.BeforeClass;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.handler.DeltaToMapConverter;
import nz.co.fortytwo.signalk.model.SignalKModel;

public class ArtemisServerTest {
	static ArtemisServer server;

	@BeforeClass
	public static void startServer() throws Exception {
		server = new ArtemisServer();
	}

	@Test
	public void checkSimpleVMConnection() throws Exception {

		ClientSession session = getVmSession("guest","guest");
		session.start();
		// session.createAddress(new SimpleString("vessels.#"),
		// RoutingType.MULTICAST, true);
		session.createQueue("vessels.#", RoutingType.MULTICAST, "vessels", true);

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
		System.out.println("message = " + recv);
		assertEquals("Hello2", recv);
		consumer = session.createConsumer("vessels", true);
		msgReceived = consumer.receive(10);
		assertNotNull(msgReceived);
		assertEquals("Hello2", msgReceived.getBodyBuffer().readString());
		// reread last message
		msgReceived = consumer.receive(10);
		assertNotNull(msgReceived);
		assertEquals("Hello3", msgReceived.getBodyBuffer().readString());
		System.out.println("message3 = " + msgReceived.getAddress());
		System.out.println("message3 = " + msgReceived.toString());
		session.close();
	}



	@Test
	public void shouldReadPartialKeysForGuest() throws Exception {

		ClientSession session = getVmSession("guest","guest");
		session.start();
		// session.createAddress(new SimpleString("vessels.#"),
		// RoutingType.MULTICAST, true);
		session.createQueue("vessels.#", RoutingType.MULTICAST, "vessels", true);
		DeltaToMapConverter conv = new DeltaToMapConverter();
		ClientProducer producer = session.createProducer();
		int c = 0;
		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {
			
			// get json
			SignalKModel map = conv.handle(Json.read(line));
			for (String key : map.getKeys()) {
				ClientMessage message = session.createMessage(true);
				message.getBodyBuffer().writeString(map.getFullData().get(key).toString());
				message.putStringProperty("_AMQ_LVQ_NAME", key);
				try{
					producer.send(key, message);
				}catch(ActiveMQSecurityException se){
					
				}
				System.out.println("Sent: " + key + "=" + map.getFullData().get(key).toString());
				c++;
			}
		}
		int d = 0;
		// now read partial
		ClientConsumer consumer = session.createConsumer("vessels", "_AMQ_LVQ_NAME like 'vessels.urn:mrn:imo:mmsi:123456789.navigation.%.value'", true);
		ClientMessage msgReceived = null;
		while ((msgReceived = consumer.receive(10)) != null) {
			String recv = msgReceived.getBodyBuffer().readString();
			System.out.println("message = " +msgReceived.getAddress()+", "+ recv);
			//System.out.println("message = " +msgReceived.toString());
			d++;
		}
		consumer.close();
		session.close();
		System.out.println("Sent = " + c+", recd="+d);
		assertEquals(9761, c);
		assertEquals(11, d);
	}

	@Test
	public void shouldReadPartialKeysForAdmin() throws Exception {

		ClientSession session = getVmSession("admin","admin");
		session.start();
		// session.createAddress(new SimpleString("vessels.#"),
		// RoutingType.MULTICAST, true);
		session.createQueue("vessels.#", RoutingType.MULTICAST, "vessels", true);
		DeltaToMapConverter conv = new DeltaToMapConverter();
		ClientProducer producer = session.createProducer();
		int c = 0;
		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {
			
			// get json
			SignalKModel map = conv.handle(Json.read(line));
			for (String key : map.getKeys()) {
				ClientMessage message = session.createMessage(true);
				message.getBodyBuffer().writeString(map.getFullData().get(key).toString());
				message.putStringProperty("_AMQ_LVQ_NAME", key);
				try{
					producer.send(key, message);
				}catch(ActiveMQSecurityException se){
					
				}
				System.out.println("Sent: " + key + "=" + map.getFullData().get(key).toString());
				c++;
			}
		}
		int d = 0;
		// now read partial
		ClientConsumer consumer = session.createConsumer("vessels", "_AMQ_LVQ_NAME like 'vessels.urn:mrn:imo:mmsi:123456789.navigation.%.value'", true);
		ClientMessage msgReceived = null;
		while ((msgReceived = consumer.receive(10)) != null) {
			String recv = msgReceived.getBodyBuffer().readString();
			System.out.println("message = " +msgReceived.getAddress()+", "+ recv);
			//System.out.println("message = " +msgReceived.toString());
			d++;
		}
		consumer.close();
		session.close();
		System.out.println("Sent = " + c+", recd="+d);
		assertEquals(9761, c);
		assertEquals(16, d);
	}

	@Test
	public void checkSimpleTCPConnection() throws Exception {

		ClientSession session = getLocalhostClientSession("guest","guest");

		session.createQueue("vessels.#", RoutingType.ANYCAST, "vessels", true);

		ClientProducer producer = session.createProducer("vessels.example");

		ClientMessage message = session.createMessage(true);
		message.getBodyBuffer().writeString("Hello");
		producer.send(message);

		session.start();

		ClientConsumer consumer = session.createConsumer("vessels", true);
		ClientConsumer consumer1 = session.createConsumer("vessels", true);

		ClientMessage msgReceived = consumer.receive();

		String recv = msgReceived.getBodyBuffer().readString();
		System.out.println("message = " + recv);
		assertEquals("Hello", recv);
		msgReceived = consumer1.receive(100);
		assertNotNull(msgReceived);
		session.close();
	}

	public ClientSession getVmSession(String user, String password) throws Exception {
		ClientSessionFactory nettyFactory = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
				.createSessionFactory();
		return nettyFactory.createSession(user,password,false, true, true, false, 10);
	}
	
	public ClientSession getLocalhostClientSession(String user, String password) throws Exception {
		Map<String, Object> connectionParams = new HashMap<String, Object>();

		connectionParams.put(TransportConstants.PORT_PROP_NAME, 61617);

		ClientSessionFactory nettyFactory = ActiveMQClient
				.createServerLocatorWithoutHA(
						new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams))
				.createSessionFactory();
		return nettyFactory.createSession(user,password,false, true, true, false, 10);
	}

}
