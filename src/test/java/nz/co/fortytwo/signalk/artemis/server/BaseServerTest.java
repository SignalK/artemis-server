package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POLICY_FIXED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.util.Base64;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseServerTest {
	ArtemisServer server;
	private static Logger logger = LogManager.getLogger(BaseServerTest.class);

	@Before
	public void startServer() throws Exception {
		//remove self file so we have clean model
		FileUtils.writeStringToFile(new File(Util.SIGNALK_MODEL_SAVE_FILE), "{}", StandardCharsets.UTF_8);
		server = new ArtemisServer();
	}

	@After
	public void stopServer() throws Exception {
		if(server!=null)server.stop();
	}

	protected Json getSubscriptionJson(String context, String path, int period, int minPeriod, String format, String policy) {
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

	protected Json getUrlAsJson(AsyncHttpClient c,String path,int restPort) throws InterruptedException, ExecutionException, IOException {
		return Json.read(getUrlAsString(c,path, null, null, restPort));
	}
	protected Json getUrlAsJson(AsyncHttpClient c,String path, String user, String pass,int restPort) throws InterruptedException, ExecutionException, IOException {
		return Json.read(getUrlAsString(c,path, user, pass, restPort));
	}
	
	protected String getUrlAsString(AsyncHttpClient c,String path,int restPort) throws InterruptedException, ExecutionException, IOException {
		return getUrlAsString(c,path, null,null, restPort);
	}
	protected String getUrlAsString(AsyncHttpClient c, String path, String user, String pass, int restPort) throws InterruptedException, ExecutionException, IOException {
		//final AsyncHttpClient c = new AsyncHttpClient();
		//try {
			// get a sessionid
			Response r2 = null;
			if(user!=null){
				String auth = Base64.encode((user+":"+pass).getBytes());
				r2 = c.prepareGet("http://localhost:" + restPort + path).setHeader("Authorization", "Basic "+auth).execute().get();
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
	
	protected List<ClientMessage> listen( ClientSession session, String tempQ, long timeout) throws ActiveMQException, InterruptedException {
		logger.debug("Receive started");
		List<ClientMessage> replies = new ArrayList<>();
		try(ClientConsumer consumer = session.createConsumer(tempQ,false);){
		
			consumer.setMessageHandler(new MessageHandler() {
				
				@Override
				public void onMessage(ClientMessage message) {
					String recv = message.getBodyBuffer().readString();
					logger.debug("onMessage = " + recv);
					assertNotNull(recv);
					replies.add(message);
				}
			});
			CountDownLatch latch = new CountDownLatch(1);
			latch.await(timeout, TimeUnit.SECONDS);
		}
		
		
		assertTrue(replies.size()>1);
		return replies;
	}
	
	protected void sendMessage(ClientSession session, ClientProducer producer, String msg) throws ActiveMQException {
		ClientMessage message = session.createMessage(true);
		message.getBodyBuffer().writeString(msg);
		producer.send(Config.INCOMING_RAW, message);
		
	}

	protected void sendSubsribeMsg(ClientSession session,  ClientProducer producer, String ctx, String path, String tempQ) throws ActiveMQException{
		ClientMessage message = session.createMessage(true);
		Json msg = getSubscriptionJson(ctx, path, 1000, 0, FORMAT_DELTA, POLICY_FIXED);
		message.getBodyBuffer().writeString(msg.toString());
		message.putStringProperty(Config.AMQ_REPLY_Q, tempQ);
		logger.debug("Sending msg: {}, {} ", producer, message);
		producer.send(Config.INCOMING_RAW, message);
	}
}
