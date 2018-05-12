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

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_API;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_DISCOVERY;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_WS;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.electrical;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.tanks;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.websocketUrl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;

public class SubcribeWsTest extends BaseServerTest {

	private static Logger logger = LogManager.getLogger(SubcribeWsTest.class);
	String jsonDiff = null;
	private int restPort = 8080;
	private String wsPort = "8080";

	public SubcribeWsTest() {

	}

	@Before
	public void startServer() throws Exception {
		// remove self file so we have clean model
		FileUtils.writeStringToFile(new File(Util.SIGNALK_MODEL_SAVE_FILE), "{}", StandardCharsets.UTF_8);
		server = new ArtemisServer();

		try (ClientSession session = Util.getVmSession("admin", "admin");
				ClientProducer producer = session.createProducer();) {
			session.start();
//			ClientMessage message = session.createMessage(true);
//			message.getBodyBuffer().writeString(FileUtils.readFileToString(new File("./src/test/resources/samples/full/vessel-time.json")));
//			producer.send(Config.INCOMING_RAW, message);
//			
//			message = session.createMessage(true);
//			message.getBodyBuffer().writeString("$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78");
//			producer.send(Config.INCOMING_RAW, message);
			
//			for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {
//				line = line.replaceAll("urn:mrn:imo:mmsi:123456789", "self");
//				message = session.createMessage(true);
//				message.getBodyBuffer().writeString(line);
//				producer.send(Config.INCOMING_RAW, message);
//				if (logger.isDebugEnabled())
//					logger.debug("Sent:" + message.getMessageID() + ":" + line);
//			}
		}
	}

	@Test
	public void shouldGetWsUrl() throws Exception {

		try (final AsyncHttpClient c = new AsyncHttpClient();) {

			// get a sessionid
			Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_DISCOVERY).execute().get();
			Json json = Json.read(r2.getResponseBody());
			logger.debug("Endpoint json:" + json);
			assertEquals("ws://localhost:" + wsPort + SIGNALK_WS,
					json.at("endpoints").at("v1").at(websocketUrl).asString());
		}
	}

	@Test
	public void shouldGetWsPublicUrls() throws Exception {

		try (final AsyncHttpClient c = new AsyncHttpClient();) {
			Json json = getUrlAsJson(SIGNALK_DISCOVERY, restPort);
			assertEquals("ws://localhost:" + wsPort + SIGNALK_WS,
					json.at("endpoints").at("v1").at(websocketUrl).asString());

			String resp = getUrlAsString(SIGNALK_API + "/self", restPort);
			assertTrue(resp, resp.startsWith("\"urn:mrn:signalk:uuid:"));

			 resp = getUrlAsString(SIGNALK_API,restPort);
			 Json respJson = Json.read(resp);
			 assertTrue(resp, respJson.has(vessels));
			
			 resp = getUrlAsString(SIGNALK_API+"/",restPort);
			 respJson = Json.read(resp);
			 assertTrue(resp, respJson.has(vessels));
			
			 resp = getUrlAsString(SIGNALK_API+"/"+vessels,restPort);
			 respJson = Json.read(resp);
			 assertTrue(resp, respJson.has("urn:mrn:signalk:uuid:b7590868-1d62-47d9-989c-32321b349fb9"));
			
			 resp = getUrlAsString(SIGNALK_API+"/"+vessels+"/",restPort);
			 respJson = Json.read(resp);
			 assertTrue(resp, respJson.has("urn:mrn:signalk:uuid:b7590868-1d62-47d9-989c-32321b349fb9"));
			
			 resp = getUrlAsString(SIGNALK_API+"/ves",restPort);
			 respJson = Json.read(resp);
			 assertTrue(resp, respJson.isNull());
			
			 resp = getUrlAsString(SIGNALK_API+"/vssls",restPort);
			 respJson = Json.read(resp);
			 assertTrue(resp, respJson.isNull());
			
			resp = getUrlAsString(SIGNALK_API + "/" + vessels + "/urn:mrn:signalk:uuid:b7590868-1d62-47d9-989c-32321b349fb9", restPort);
			respJson = Json.read(resp);
			assertTrue(respJson.has("uuid"));

			resp = getUrlAsString(SIGNALK_API + "/" + vessels + "/self/nav", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has("courseOverGroundTrue"));

			resp = getUrlAsString(SIGNALK_API + "/" + vessels + "/self/nav*", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has("courseOverGroundTrue"));

			resp = getUrlAsString(SIGNALK_API + "/" + sources, restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has("NMEA0183"));

			resp = getUrlAsString(SIGNALK_API + "/" + sources + "/", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has("NMEA0183"));

			resp = getUrlAsString(SIGNALK_API + "/sou", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.isNull());

			resp = getUrlAsString(SIGNALK_API + "/" + CONFIG, "admin", "admin", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has(CONFIG));

			resp = getUrlAsString(SIGNALK_API + "/" + CONFIG + "/", "admin", "admin", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has(CONFIG));
			// assertTrue( resp.contains("\"ports\": [\""));
			resp = getUrlAsString(SIGNALK_API + "/con", "admin", "admin", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.isNull());

			resp = getUrlAsString(SIGNALK_API + "/" + CONFIG, restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.isNull());

			resp = getUrlAsString(SIGNALK_API + "/" + CONFIG + "/", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.isNull());
			
			resp = getUrlAsString(SIGNALK_API + "/con", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.isNull());

			// special case, we have /vessels/self/, which should be found by
			// this..
			resp = getUrlAsString(SIGNALK_API + "/" + vessels + "/urn", restPort);
			assertTrue(resp, resp.startsWith("{\"urn:mrn:"));
		}
	}

	@Test
	public void shouldGetApiData() throws Exception {

		try (final AsyncHttpClient c = new AsyncHttpClient();) {
			// get auth
			Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/vessels")
					.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
			Json json = Json.read(r2.getResponseBody());
			logger.debug("Endpoint json:" + json);
			// urn:mrn:imo:mmsi:245139000
			//
			assertTrue(json.has("urn:mrn:imo:mmsi:244690118"));
		}

	}

	@Test
	public void shouldGetApiSelfData() throws Exception {

		try (final AsyncHttpClient c = new AsyncHttpClient();) {

			Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/self")
					.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
			String json = r2.getResponseBody();
			logger.debug("Endpoint json:" + json);
			assertEquals("\"urn:mrn:signalk:uuid:5da2f032-fc33-43f0-bc24-935bf55a17d1\"", json);
		}
	}

	@Test
	public void shouldGetApiUUIDData() throws Exception {

		try (final AsyncHttpClient c = new AsyncHttpClient();) {

			Response r2 = c
					.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/vessels/"
							+ Config.getConfigProperty(ConfigConstants.UUID) + "/uuid")
					.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
			String resp = r2.getResponseBody();
			logger.debug("Endpoint json:" + resp);
			assertEquals("\"urn:mrn:signalk:uuid:5da2f032-fc33-43f0-bc24-935bf55a17d1\"", resp);
		}
	}

	@Test
	public void shouldGetApiSubset() throws Exception {

		try (final AsyncHttpClient c = new AsyncHttpClient();) {

			// get a sessionid
			// Response r1 =
			// c.prepareGet("http://localhost:"+restPort+SIGNALK_AUTH+"/demo/pass").execute().get();
			// assertEquals(200, r1.getStatusCode());
			Response r2 = c
					.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/vessels/urn:mrn:imo:mmsi:123456789")
					.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
			Json json = Json.read(r2.getResponseBody());
			logger.debug("Endpoint json:" + json);
			// assertEquals("ws://localhost:"+wsPort+SIGNALK_WS,
			// json.at("endpoints").at("v1").at(websocketUrl).asString());
		}
	}

	@Test
	public void shouldGetApiForSelf() throws Exception {

		try (final AsyncHttpClient c = new AsyncHttpClient();) {

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
		}
	}

	@Test
	public void shouldGetApiForSources() throws Exception {

		try (final AsyncHttpClient c = new AsyncHttpClient();) {

			Response r2 = c.prepareGet("http://localhost:" + restPort + SIGNALK_API + "/sources")
					.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute().get();
			Json json = Json.read(r2.getResponseBody());
			logger.debug("Endpoint json:" + json);
			assertTrue(json.has("NMEA2000"));
			assertFalse(json.has(nav));
			assertFalse(json.has(electrical));
			assertFalse(json.has(tanks));
		}
	}

	@Test
	public void shouldGetSubscribeWsResponse() throws Exception {
		final List<String> received = new ArrayList<String>();
		final CountDownLatch latch3 = new CountDownLatch(5);
		try (final AsyncHttpClient c = new AsyncHttpClient();) {

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
			String subscribeMsg = "{\"context\":\"vessels.self\",\"subscribe\":[{\"path\":\"navigation\"}]}";
			websocket.sendMessage(subscribeMsg);

			logger.debug("Sent subscribe = " + subscribeMsg);

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
			assertTrue(fullMsg.contains("\"context\":\"vessels." + Config.getConfigProperty(ConfigConstants.UUID) + "\""));
			assertTrue(fullMsg.contains("\"path\":\"navigation.courseOverGroundTrue\""));
			assertTrue(fullMsg.contains("\"value\":"));
			assertTrue(fullMsg.contains("\"updates\":[{"));

		}
	}

}
