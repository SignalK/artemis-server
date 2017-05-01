/*
 *
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.util.SignalKConstants.SIGNALK_API;
import static nz.co.fortytwo.signalk.util.SignalKConstants.SIGNALK_DISCOVERY;
import static nz.co.fortytwo.signalk.util.SignalKConstants.SIGNALK_WS;
import static nz.co.fortytwo.signalk.util.SignalKConstants.electrical;
import static nz.co.fortytwo.signalk.util.SignalKConstants.env;
import static nz.co.fortytwo.signalk.util.SignalKConstants.nav;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.tanks;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;
import static nz.co.fortytwo.signalk.util.SignalKConstants.websocketUrl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.client.ws.DefaultWebSocketListener;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketUpgradeHandler;
import com.ning.http.util.Base64;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.util.ConfigConstants;

public class SubcribeWsTest {

	private static Logger logger = LogManager.getLogger(SubcribeWsTest.class);
	String jsonDiff = null;
	private String restPort = "8080";
	private String wsPort = "8080";
	private ArtemisServer server;

	public SubcribeWsTest() {

	}

	@Before
	public void startServer() throws Exception {
		server = new ArtemisServer();
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void shouldGetWsUrl() throws Exception {

		final AsyncHttpClient c = new AsyncHttpClient();

		// get a sessionid
		Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_DISCOVERY).execute().get();
		Json json = Json.read(r2.getResponseBody());
		logger.debug("Endpoint json:" + json);
		assertEquals("ws://localhost:" + wsPort + SIGNALK_WS,
				json.at("endpoints").at("v1").at(websocketUrl).asString());
		c.close();
	}

	@Test
	public void shouldGetWsPublicUrls() throws Exception {

		final AsyncHttpClient c = new AsyncHttpClient();
		Json json = getUrlAsJson(SIGNALK_DISCOVERY);
		assertEquals("ws://localhost:" + wsPort + SIGNALK_WS,
				json.at("endpoints").at("v1").at(websocketUrl).asString());
		
		String resp = getUrlAsString(SIGNALK_API+"/self");
		assertTrue( resp.startsWith("\"urn:mrn:signalk:uuid:"));
		
		resp = getUrlAsString(SIGNALK_API);
		assertTrue( resp.startsWith("{\"vessels\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/");
		assertTrue( resp.startsWith("{\"vessels\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+vessels);
		assertTrue( resp.startsWith("{\"urn:mrn:imo:mmsi"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+vessels+"/");
		assertTrue( resp.startsWith("{\"urn:mrn:imo:mmsi"));
		
		resp = getUrlAsString(SIGNALK_API+"/ves");
		assertTrue( resp.startsWith("{\"vessels\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/vssls");
		assertTrue( resp.startsWith("{}"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+vessels+"/self");
		assertTrue( resp.startsWith("{\"environment\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+vessels+"/urn");
		assertTrue( resp.startsWith("{\"urn:mrn:"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+vessels+"/self/nav");
		assertTrue( resp.startsWith("{\"navigation\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+vessels+"/self/nav*");
		assertTrue( resp.startsWith("{\"navigation\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+sources);
		assertTrue( resp.contains("{\"value\":{\"label\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+sources+"/");
		assertTrue( resp.contains("{\"value\":{\"label\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/sou");
		assertTrue( resp.startsWith("{\"sources\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+CONFIG, "admin", "admin");
		assertTrue( resp.startsWith("{\"config\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+CONFIG+"/", "admin", "admin");
		assertTrue( resp.startsWith("{\"config\":"));
		//assertTrue( resp.contains("\"ports\": [\""));
		resp = getUrlAsString(SIGNALK_API+"/con", "admin", "admin");
		assertTrue( resp.startsWith("{\"config\":"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+CONFIG);
		assertTrue( resp.startsWith("{}"));
		
		resp = getUrlAsString(SIGNALK_API+"/"+CONFIG+"/");
		assertTrue( resp.startsWith("{}"));
		
		resp = getUrlAsString(SIGNALK_API+"/con");
		assertTrue( resp.startsWith("{}"));
	}

	

	@Test
	public void shouldGetApiData() throws Exception {

		ClientSession session = Util.getVmSession("admin", "admin");
		session.start();

		ClientProducer producer = session.createProducer();

		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {

			ClientMessage message = session.createMessage(true);
			message.getBodyBuffer().writeString(line);
			producer.send(Config.INCOMING_RAW, message);
			if (logger.isDebugEnabled())
				logger.debug("Sent:" + message.getMessageID() + ":" + line);
		}

		final AsyncHttpClient c = new AsyncHttpClient();
		// get auth
		Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/vessels")
				.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
		Json json = Json.read(r2.getResponseBody());
		logger.debug("Endpoint json:" + json);
		assertTrue(json.has("urn:mrn:imo:mmsi:244690118"));
		c.close();
	}

	@Test
	public void shouldGetApiSelfData() throws Exception {

		ClientSession session = Util.getVmSession("admin", "admin");
		session.start();

		final AsyncHttpClient c = new AsyncHttpClient();

		Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/self")
				.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
		String json = r2.getResponseBody();
		logger.debug("Endpoint json:" + json);
		assertEquals("\"urn:mrn:signalk:uuid:5da2f032-fc33-43f0-bc24-935bf55a17d1\"", json);
		c.close();
	}

	@Test
	public void shouldGetApiUUIDData() throws Exception {

		ClientSession session = Util.getVmSession("admin", "admin");
		session.start();

		final AsyncHttpClient c = new AsyncHttpClient();

		Response r2 = c
				.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/vessels/"
						+ Config.getConfigProperty(ConfigConstants.UUID) + "/uuid")
				.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
		String resp = r2.getResponseBody();
		logger.debug("Endpoint json:" + resp);
		assertEquals("\"urn:mrn:signalk:uuid:5da2f032-fc33-43f0-bc24-935bf55a17d1\"", resp);
		c.close();
	}

	@Test
	public void shouldGetApiSubset() throws Exception {

		ClientSession session = Util.getVmSession("admin", "admin");
		session.start();

		ClientProducer producer = session.createProducer();

		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {

			ClientMessage message = session.createMessage(true);
			message.getBodyBuffer().writeString(line);
			producer.send(Config.INCOMING_RAW, message);
			if (logger.isDebugEnabled())
				logger.debug("Sent:" + message.getMessageID() + ":" + line);
		}

		final AsyncHttpClient c = new AsyncHttpClient();

		// get a sessionid
		// Response r1 =
		// c.prepareGet("http://localhost:"+restPort+SIGNALK_AUTH+"/demo/pass").execute().get();
		// assertEquals(200, r1.getStatusCode());
		Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/vessels/urn:mrn:imo:mmsi:123456789")
				.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
		Json json = Json.read(r2.getResponseBody());
		logger.debug("Endpoint json:" + json);
		// assertEquals("ws://localhost:"+wsPort+SIGNALK_WS,
		// json.at("endpoints").at("v1").at(websocketUrl).asString());
		c.close();
	}

	@Test
	public void shouldGetApiForSelf() throws Exception {

		ClientSession session = Util.getVmSession("admin", "admin");
		session.start();

		ClientProducer producer = session.createProducer();

		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {

			ClientMessage message = session.createMessage(true);
			line = line.replaceAll("urn:mrn:imo:mmsi:123456789", Config.getConfigProperty(ConfigConstants.UUID));
			message.getBodyBuffer().writeString(line);
			producer.send(Config.INCOMING_RAW, message);
			if (logger.isDebugEnabled())
				logger.debug("Sent:" + message.getMessageID() + ":" + line);
		}

		final AsyncHttpClient c = new AsyncHttpClient();

		// get a sessionid
		// Response r1 =
		// c.prepareGet("http://localhost:"+restPort+SIGNALK_AUTH+"/demo/pass").execute().get();
		// assertEquals(200, r1.getStatusCode());
		Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/vessels/self")
				.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
		Json json = Json.read(r2.getResponseBody());
		logger.debug("Endpoint json:" + json);
		assertFalse(json.has("NMEA2000"));
		assertTrue(json.has(env));
		assertTrue(json.has(nav));
		assertTrue(json.has(electrical));
		assertTrue(json.has(tanks));
		c.close();
	}

	@Test
	public void shouldGetApiForSources() throws Exception {

		ClientSession session = Util.getVmSession("admin", "admin");
		session.start();

		ClientProducer producer = session.createProducer();

		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {

			ClientMessage message = session.createMessage(true);
			line = line.replaceAll("urn:mrn:imo:mmsi:123456789", Config.getConfigProperty(ConfigConstants.UUID));
			message.getBodyBuffer().writeString(line);
			producer.send(Config.INCOMING_RAW, message);
			if (logger.isDebugEnabled())
				logger.debug("Sent:" + message.getMessageID() + ":" + line);
		}

		final AsyncHttpClient c = new AsyncHttpClient();

		// get a sessionid
		// Response r1 =
		// c.prepareGet("http://localhost:"+restPort+SIGNALK_AUTH+"/demo/pass").execute().get();
		// assertEquals(200, r1.getStatusCode());
		Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/sources")
				.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
		Json json = Json.read(r2.getResponseBody());
		logger.debug("Endpoint json:" + json);
		assertTrue(json.has("NMEA2000"));
		assertFalse(json.has(nav));
		assertFalse(json.has(electrical));
		assertFalse(json.has(tanks));
		c.close();
	}

	@Test
	public void shouldGetSubscribeWsResponse() throws Exception {
		final List<String> received = new ArrayList<String>();
		final CountDownLatch latch3 = new CountDownLatch(5);
		final AsyncHttpClient c = new AsyncHttpClient();

		String restUrl = "ws://localhost:" + restPort + SIGNALK_WS;
		logger.debug("Open websocket at: " + restUrl);
		WebSocket websocket = c.prepareGet(restUrl).setHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
				.execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new DefaultWebSocketListener() {

					@Override
					public void onMessage(byte[] message) {
						logger.info("received BYTES --> " + String.valueOf(message));
					}

					@Override
					public void onMessage(String message) {
						logger.info("received --> " + message);
						received.add(message);
					}

					@Override
					public void onError(Throwable t) {
						logger.error(t);
					}

				}).build()).get();

		// subscribe
		String subscribeMsg = "{\"context\":\"vessels.urn:mrn:imo:mmsi:123456789\",\"subscribe\":[{\"path\":\"navigation\"}]}";
		websocket.sendMessage(subscribeMsg);

		logger.debug("Sent subscribe = " + subscribeMsg);
		// latch4.await(2, TimeUnit.SECONDS);

		// send some data
		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {

			websocket.sendMessage(line);
			// if (logger.isDebugEnabled())
			// logger.debug("Sent:" + line);

		}

		latch3.await(10, TimeUnit.SECONDS);
		assertTrue(received.size() > 1);
		// assertTrue(latch3.await(15, TimeUnit.SECONDS));
		String fullMsg = null;
		for (String msg : received) {
			logger.debug("Received msg = " + msg);
			if (msg.contains("\"updates\":[{\"") && msg.contains("\"path\":\"navigation")) {
				fullMsg = msg;
			}
		}
		assertTrue(received.size() > 1);

		// Json sk =
		// Json.read("{\"context\":\"vessels."+SignalKConstants.self+".navigation\",\"updates\":[{\"values\":[{\"path\":\"courseOverGroundTrue\",\"value\":3.0176},{\"path\":\"speedOverGround\",\"value\":3.85}],\"source\":{\"timestamp\":\"2014-08-15T16:00:00.081+00:00\",\"device\":\"/dev/actisense\",\"src\":\"115\",\"pgn\":\"128267\"}}]}");
		// Json sk =
		// Json.read("{\"context\":\"vessels.motu\",\"updates\":[{\"values\":[{\"path\":\"navigation.courseOverGroundTrue\",\"value\":3.0176},{\"path\":\"navigation.speedOverGround\",\"value\":3.85}],\"timestamp\":\"2014-08-15T16:00:00.081+00:00\",\"source\":{\"device\":\"/dev/actisense\",\"src\":\"115\",\"pgn\":\"128267\"}}]}");
		assertNotNull(fullMsg);
		assertTrue(fullMsg.contains("\"context\":\"vessels.urn:mrn:imo:mmsi:123456789\""));
		assertTrue(fullMsg.contains("\"path\":\"navigation.courseOverGroundTrue\""));
		assertTrue(fullMsg.contains("\"value\":"));
		assertTrue(fullMsg.contains("\"updates\":[{"));

		c.close();
	}

	
	private Json getUrlAsJson(String path) throws InterruptedException, ExecutionException, IOException {
		return Json.read(getUrlAsString(path, null, null));
	}
	private Json getUrlAsJson(String path, String user, String pass) throws InterruptedException, ExecutionException, IOException {
		return Json.read(getUrlAsString(path, user, pass));
	}
	
	private String getUrlAsString(String path) throws InterruptedException, ExecutionException, IOException {
		return getUrlAsString(path, null,null);
	}
	private String getUrlAsString(String path, String user, String pass) throws InterruptedException, ExecutionException, IOException {
		final AsyncHttpClient c = new AsyncHttpClient();
		try {
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
		} finally {
			c.close();
		}
	}
}
