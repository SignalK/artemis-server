package nz.co.fortytwo.signalk.artemis.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import mjson.Json;
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuthServiceTest {
	private static Logger logger = LogManager.getLogger(AuthServiceTest.class);
	private AuthService service;
	
	
	public AuthServiceTest() throws Exception {
		service = new AuthService();
	}

	@Test
	public void shouldLogin() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"admin\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(200,reply.at("result").asInteger());
		assertTrue(reply.has("login"));
		assertTrue(reply.at("login").has("token"));
	}

	@Test
	public void shouldFailBadCred() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(401,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	
	@Test
	public void shouldFailNoRequestId() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(500,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}

	@Test
	public void shouldFailNoName() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				//"    \"username\": \"admin\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(500,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	@Test
	public void shouldFailNoPassword() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\"\n" + 
				//"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(500,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	@Test
	public void shouldFailBlankRequestId() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(500,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}

	@Test
	public void shouldFailBlankName() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"\",\n" + 
				"    \"password\": \"nopass\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(500,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	@Test
	public void shouldFailBlankPassword() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(500,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	@Test
	public void shouldLogOut() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"admin\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		String token = reply.at("login").at("token").asString();
		uuid = UUID.randomUUID().toString();
		body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"token\": \""+token+"\"\n" +  
				"}";
		reply = service.logout(body);
		logger.debug(reply);
		assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(200,reply.at("result").asInteger());
		assertTrue(!reply.has("login"));
	}
	@Test
	public void shouldBeValidated() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"login\": {\n" + 
				"    \"username\": \"admin\",\n" + 
				"    \"password\": \"admin\"\n" + 
				"  }\n" + 
				"}";
		Json reply = service.login(body);
		logger.debug(reply);
		String token = reply.at("login").at("token").asString();
		uuid = UUID.randomUUID().toString();
		body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"token\": \""+token+"\"\n" +  
				"}";
		reply = service.validate(body);
		logger.debug(reply);
		assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("COMPLETED",reply.at("state").asString());
		assertEquals(200,reply.at("result").asInteger());
		assertTrue(reply.has("token"));
	}
	
	@Test
	public void shouldFailValidate() {
		String uuid = UUID.randomUUID().toString();
		String body = "{\n" + 
				"  \"requestId\": \""+uuid+"\",\n" + 
				"  \"token\": \"eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6IltcInNraXBwZXJcIl0iLCJpYXQiOjE1NDY1NTA5OTYsImV4cCI6MTU0NjYzNzM5Nn0.w0bkfYhCJLwMyCBKiWFLbXTuu6VFaS_BxBPBjgBS-9aPMv22fOI5GzzG3jK7rbW43_4KvjrfLZ6RAibiDnB2ug\"\n" +  
				"}";
		Json reply = service.logout(body);
		reply = service.validate(body);
		logger.debug(reply);
		//assertEquals(uuid,reply.at("requestId").asString());
		assertEquals("FAILED",reply.at("state").asString());
		assertEquals(403,reply.at("result").asInteger());
		assertTrue(!reply.has("token"));
	}

}
