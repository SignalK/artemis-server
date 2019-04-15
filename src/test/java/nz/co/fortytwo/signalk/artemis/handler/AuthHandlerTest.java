package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.server.BaseServerTest;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuthHandlerTest extends BaseServerTest{
	private static Logger logger = LogManager.getLogger(AuthHandlerTest.class);
	
	public AuthHandlerTest() throws Exception {
		
	}

	@Test
	public void shouldLogin() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"admin\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(200,reply.at("result").asInteger());
		assertTrue(reply.has("login"));
		assertTrue(reply.at("login").has("token"));
		assertTrue(reply.at("login").has("timeToLive"));
	}

	private Json sendBody(String body) throws  Exception {
		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();	
				){
			String tempQ = UUID.randomUUID().toString();
			String qName=Config.OUTGOING_REPLY+dot+tempQ;
			//session.createTemporaryQueue("outgoing.reply." + destination, RoutingType.ANYCAST, destination);
			session.createQueue(qName, RoutingType.ANYCAST, qName);
			ClientConsumer consumer = session.createConsumer(qName);
			List<ClientMessage> replies = createListener(session, consumer, qName);
			//session.start();
		
			sendMessage(session, producer, body, null,null,null,tempQ);
			
			logger.debug("Input sent: {}",body);
		
			logger.debug("Receive started on {}",qName);
			replies = listen(replies, 3, 1);
			//assertEquals(expected, replies.size());
			logger.debug("Received {} replies", replies.size());
			replies.forEach((m)->{
				logger.debug("Received {}", m);
			});
			ClientMessage msg = replies.get(0);
			String msgBody = Util.readBodyBufferToString(msg);
			return Json.read(msgBody);
		}
	}

	@Test
	public void shouldFailBadCred() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(401,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	
	@Test
	public void shouldFailNoRequestId() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(500,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}

	@Test
	public void shouldFailNoName() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				//"    \"username\": \"admin\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(500,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	@Test
	public void shouldFailNoPassword() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\"\n" + 
				//"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(500,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	@Test
	public void shouldFailBlankRequestId() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(400,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}

	@Test
	public void shouldFailBlankName() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(400,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	@Test
	public void shouldFailBlankPassword() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(400,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	@Test
	public void shouldLogOut() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"admin\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		String token = reply.at("login").at("token").asString();
		uuid = UUID.randomUUID().toString();
		body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"logout\": {\n" +
				"    \"token\": \""+token+"\"\n" + 
				"  }\n" + 
				"}";
		reply = sendBody(body);
		logger.debug(reply);
		assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(200,reply.at("result").asInteger());
		
	}
	@Test
	public void shouldBeValidated() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"admin\"\n" + 
				"  }\n" + 
				"}";
		Json reply = sendBody(body);
		logger.debug(reply);
		String token = reply.at("login").at("token").asString();
		uuid = UUID.randomUUID().toString();
		body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"validate\": {\n" + 
				"    \"token\": \""+token+"\"\n" +  
				"  }\n" + 
				"}";
		reply = sendBody(body);
		logger.debug(reply);
		assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(200,reply.at("result").asInteger());
		assertTrue(reply.at("validate").has("token"));
	}
	
	@Test
	public void shouldFailValidate() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"validate\": {\n" +
				"    \"token\": \"eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6IltcInNraXBwZXJcIl0iLCJpYXQiOjE1NDY1NTA5OTYsImV4cCI6MTU0NjYzNzM5Nn0.w0bkfYhCJLwMyCBKiWFLbXTuu6VFaS_BxBPBjgBS-9aPMv22fOI5GzzG3jK7rbW43_4KvjrfLZ6RAibiDnB2ug\"\n" +  
				"  }\n" 
				+"}";
		Json reply = sendBody(body);
		reply = sendBody(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(401,reply.at("result").asInteger());
		assertTrue(!reply.has("validate"));
	}

}
