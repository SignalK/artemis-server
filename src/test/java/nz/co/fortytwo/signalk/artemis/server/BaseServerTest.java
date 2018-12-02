package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_INFLUX_KEY;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_USER_TOKEN;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POLICY_FIXED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;
import org.easymock.EasyMockSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseServerTest extends EasyMockSupport {
	static ArtemisServer server;
	public static final String SIGNALK_TEST_DB = "signalk-test";
	private static Logger logger = LogManager.getLogger(BaseServerTest.class);

	protected String uuid;
	
	public BaseServerTest() {
		uuid = Config.getConfigProperty(ConfigConstants.UUID);
	}

	@BeforeClass
	public static void startServer() throws Exception {
		InfluxDbService.allowWrite=true;
		server = new ArtemisServer(SIGNALK_TEST_DB);
		
	}

	@AfterClass
	public static void stopServer() throws Exception {
		if(server!=null)server.stop();
		CountDownLatch latch = new CountDownLatch(1);
		latch.await(10, TimeUnit.SECONDS);
	}
	
	protected ClientMessage getClientMessage(String body, String contentType, boolean reply) {
		ClientMessage message = new ClientMessageImpl((byte) 0, false, 0, System.currentTimeMillis(), (byte) 4, 1024);
		if(reply)message.putBooleanProperty(SignalKConstants.REPLY,reply);
		message.putStringProperty(Config.AMQ_CONTENT_TYPE, contentType);
		if(body!=null)
			message.getBodyBuffer().writeString(body);
		return message;
	}

	protected ClientMessage getMessage(String jsonStr, String key, String src, String token) {
		Json json = Json.read(jsonStr);
		ClientMessage message = getClientMessage(json.toString(), MediaType.APPLICATION_JSON, false);
		message.putStringProperty(AMQ_INFLUX_KEY, "vessels."+uuid+"."+key+".values."+src);
		message.putStringProperty(AMQ_USER_TOKEN, token);
		try {
			message.putStringProperty(Config.AMQ_USER_ROLES, SecurityUtils.getRoles(token).toString());
		} catch (Exception e) {
			logger.error(e,e);
		}
		return message;
	}

	protected Json getSubscriptionJson(String context, String path, int period, int minPeriod, String format, String policy, String token) {
		Json json = Json.read("{\"context\":\"" + context + "\", \"token\":\"" + token + "\",\"subscribe\": []}");
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

	protected Json getUrlAsJson(AsyncHttpClient c,String path,int restPort) throws Exception {
		return Json.read(getUrlAsString(c,path, null, null, restPort));
	}
	protected Json getUrlAsJson(AsyncHttpClient c,String path, String user, String pass,int restPort) throws Exception {
		return Json.read(getUrlAsString(c,path, user, pass, restPort));
	}
	
	protected String getUrlAsString(AsyncHttpClient c,String path,int restPort) throws Exception {
		return getUrlAsString(c,path, null,null, restPort);
	}
	protected String getUrlAsString(AsyncHttpClient c, String path, String user, String pass, int restPort) throws Exception {
		//final AsyncHttpClient c = new AsyncHttpClient();
		//try {
			// get a sessionid
			Response r2 = null;
			if(user!=null){
				r2 = c.prepareGet("http://localhost:" + restPort + path).setCookies(getCookies(user, pass)).execute().get();
			}else{
				r2 = c.prepareGet("http://localhost:" + restPort + path).execute().get();
			}
			
			String response = r2.getResponseBody();
			logger.debug("Endpoint string:" + response);
			return response;
		//} finally {
		//	c.close();
		//}
	}
	
	protected Collection<Cookie> getCookies(String user, String pass) throws Exception {
		String jwtToken = SecurityUtils.authenticateUser(user, pass);
		Collection<Cookie> cookies = new ArrayList<>();
		cookies.add(new DefaultCookie(SecurityUtils.AUTH_COOKIE_NAME, jwtToken));
		return cookies;
	}
	protected List<ClientMessage> listen( ClientSession session, String tempQ, long timeout) throws ActiveMQException, InterruptedException {
		return listen(session, session.createConsumer(tempQ), tempQ, timeout, 2);
	}
	protected List<ClientMessage> listen( ClientSession session, ClientConsumer consumer, String tempQ, long timeout, int expected) throws ActiveMQException, InterruptedException {
		logger.debug("{}: Receive starting for {}",getClass().getSimpleName(), tempQ);
		List<ClientMessage> replies = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(expected);
		String clazz = getClass().getSimpleName();
		consumer.setMessageHandler(new MessageHandler() {
			
			@Override
			public void onMessage(ClientMessage message) {
				try{
					
					String recv = Util.readBodyBufferToString(message);
					message.acknowledge();
					logger.debug("{}: onMessage = {}",clazz,recv);
					assertNotNull(recv);
					replies.add(message);
					latch.countDown();
				} catch (ActiveMQException e) {
					logger.error(e,e);
				} 
			}
		});
		session.start();
		latch.await(timeout, TimeUnit.SECONDS);
		logger.debug("{}: Receive complete for {}",getClass().getSimpleName(), tempQ);
	
		
		assertTrue(replies.size()>1);
		return replies;
	}
	
	protected List<ClientMessage> createListener( ClientSession session, ClientConsumer consumer, String tempQ) throws ActiveMQException, InterruptedException {
		logger.debug("{}: Receive starting for {}",getClass().getSimpleName(), tempQ);
		List<ClientMessage> replies = new ArrayList<>();
		String clazz = getClass().getSimpleName();
		consumer.setMessageHandler(new MessageHandler() {
			
			@Override
			public void onMessage(ClientMessage message) {
				try{
					
					String recv = Util.readBodyBufferToString(message);
					message.acknowledge();
					logger.debug("{}: onMessage = {}",clazz,recv);
					assertNotNull(recv);
					replies.add(message);
				} catch (ActiveMQException e) {
					logger.error(e,e);
				} 
			}
		});
		session.start();

		return replies;
	}
	
	protected List<ClientMessage> listen( List<ClientMessage> replies, long timeout, int expected) throws ActiveMQException, InterruptedException {
		
		CountDownLatch latch = new CountDownLatch(expected);
		latch.await(timeout, TimeUnit.SECONDS);
		logger.debug("{}: Receive complete ",getClass().getSimpleName());
		logger.debug("{}: Received {} replies",getClass().getSimpleName(), replies.size());
		replies.forEach((m)->{
			logger.debug("{}: Received {}",getClass().getSimpleName(), m);
		});
		
		assertTrue(replies.size()>1);
		return replies;
	}
//	protected void sendMessage(ClientSession session, ClientProducer producer, String msg) throws ActiveMQException {
//		sendMessage(session, producer, msg, null); 
//	}
	protected void sendMessage(ClientSession session, ClientProducer producer, String msg, String token) throws ActiveMQException {
		sendMessage(session, producer, msg, token,null,null);
	}
	protected void sendMessage(ClientSession session, ClientProducer producer, String msg, String token, String srcBus, String srcType) throws ActiveMQException {
		ClientMessage message = session.createMessage(true);
		if(token!=null) {
			message.putStringProperty(AMQ_USER_TOKEN, token);
		}
		if(srcBus!=null)
			message.putStringProperty(Config.MSG_SRC_BUS, srcBus);
		if(srcType!=null) 
			message.putStringProperty(Config.MSG_SRC_TYPE, srcType);
		logger.debug("Sending message: {}", message);
		message.getBodyBuffer().writeString(msg);
		producer.send(Config.INCOMING_RAW, message);
		
	}

	protected void sendSubsribeMsg(ClientSession session,  ClientProducer producer, String ctx, String path, String tempQ, String token) throws ActiveMQException{
		ClientMessage message = session.createMessage(true);
		
		message.putStringProperty(Config.AMQ_SESSION_ID, tempQ);
		message.putStringProperty(Config.AMQ_CORR_ID, tempQ);
		message.putStringProperty(Config.AMQ_USER_TOKEN, token);
		Json msg = getSubscriptionJson(ctx, path, 1000, 0, FORMAT_DELTA, POLICY_FIXED,token);
		message.getBodyBuffer().writeString(msg.toString());
		message.putStringProperty(Config.AMQ_REPLY_Q, tempQ);
		logger.debug("Sending msg: {}, {} ", producer, message);
		producer.send(Config.INCOMING_RAW, message);
	}
}
