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

import static nz.co.fortytwo.signalk.artemis.util.Config.SERIAL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POLICY_IDEAL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_API;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_DISCOVERY;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_WS;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.electrical;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.restUrl;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.tanks;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.version;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.websocketUrl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import static org.asynchttpclient.Dsl.*;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.Util;


public class SubscribeWsTest extends BaseServerTest {

	private static Logger logger = LogManager.getLogger(SubscribeWsTest.class);
	String jsonDiff = null;
	

	public SubscribeWsTest() {

		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();	
				){
			String token = SecurityUtils.authenticateUser("admin", "admin");
			sendMessage(session, producer, "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78", token, "/dev/ttyUSB0", SERIAL, null);
			//wait a sec to flush through
			CountDownLatch latch = new CountDownLatch(1);
			latch.await(2,TimeUnit.SECONDS);
		} catch (ActiveMQException e) {
			logger.error(e,e);
		} catch (Exception e) {
			logger.error(e,e);
		}
	}

	
	@Test
	public void shouldGetAllData() throws Exception {

		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {
		
			String resp = getUrlAsString(c,SIGNALK_API,restPort);
			 Json respJson = Json.read(resp);
			 assertTrue(resp, respJson.has(vessels));
			 
		}
	}
	@Test
	public void shouldGetWsPublicUrls() throws Exception {

		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {
			Json json = getUrlAsJson(c,SIGNALK_DISCOVERY, restPort);
			logger.debug("shouldGetWsPublicUrls: {}", json);
			assertEquals(wsScheme+"://localhost:" + wsPort + SIGNALK_WS,
					json.at("endpoints").at("v1").at(websocketUrl).asString());
			assertEquals(httpScheme+"://localhost:" + restPort + SIGNALK_API+"/",
					json.at("endpoints").at("v1").at(restUrl).asString());
			assertEquals("1.0.0",
					json.at("endpoints").at("v1").at(version).asString());

			String resp = getUrlAsString(c,SIGNALK_API + "/self", restPort);
			
			 assertEquals("\"vessels."+Config.getConfigProperty(ConfigConstants.UUID)+"\"", resp);
			
			 resp = getUrlAsString(c,SIGNALK_API,restPort);
			 Json respJson = Json.read(resp);
			 assertTrue(resp, respJson.has(vessels));
			
			 resp = getUrlAsString(c,SIGNALK_API+"/",restPort);
			 respJson = Json.read(resp);
			 assertTrue(resp, respJson.has(vessels));
			
			 resp = getUrlAsString(c,SIGNALK_API+"/"+vessels,restPort);
			 respJson = Json.read(resp);
			 assertTrue(resp, respJson.has(Config.getConfigProperty(ConfigConstants.UUID)));
			
			 resp = getUrlAsString(c,SIGNALK_API+"/"+vessels+"/",restPort);
			 respJson = Json.read(resp);
			 assertTrue(resp, respJson.has(Config.getConfigProperty(ConfigConstants.UUID)));
			
			 resp = getUrlAsString(c,SIGNALK_API+"/ves",restPort);
			 respJson = Json.read(resp);
			 assertTrue(resp, respJson.at(vessels).has(Config.getConfigProperty(ConfigConstants.UUID)));
			
			 resp = getUrlAsString(c,SIGNALK_API+"/vssls",restPort);
			 assertEquals("{}",resp);
			
			resp = getUrlAsString(c,SIGNALK_API + "/" + vessels + "/", restPort);
			respJson = Json.read(resp);
			assertTrue(respJson.has(Config.getConfigProperty(ConfigConstants.UUID)));

			resp = getUrlAsString(c,SIGNALK_API + "/" + vessels + "/self/nav", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.at(nav).has("courseOverGroundTrue"));

			resp = getUrlAsString(c,SIGNALK_API + "/" + vessels + "/self/nav*", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.at(nav).has("courseOverGroundTrue"));

			//logger.debug(getUrlAsString(c,SIGNALK_API + "/", "admin","admin",restPort));
			resp = getUrlAsString(c,SIGNALK_API + "/" + sources, "admin","admin",restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has("NMEA0183"));

			resp = getUrlAsString(c,SIGNALK_API + "/" + sources + "/", "admin","admin",restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has("NMEA0183"));

			resp = getUrlAsString(c,SIGNALK_API + "/sou","admin","admin", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.at(sources).has("NMEA0183"));

			resp = getUrlAsString(c,SIGNALK_API + "/" + CONFIG, "admin", "admin", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has(CONFIG));

			resp = getUrlAsString(c,SIGNALK_API + "/" + CONFIG + "/", "admin", "admin", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has(CONFIG));
			// assertTrue( resp.contains("\"ports\": [\""));
			resp = getUrlAsString(c,SIGNALK_API + "/con", "admin", "admin", restPort);
			respJson = Json.read(resp);
			assertTrue(resp, respJson.has(CONFIG));
		}
			//no login, authenticationInterceptor sends back 301 redirect so no result
		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {	
			String resp = getUrlAsString(c,"/" + CONFIG, restPort);
			assertEquals("",resp);

			resp = getUrlAsString(c,"/" + CONFIG + "/", restPort);
			assertEquals("",resp);
			
			
			// special case, we have /vessels/self/, which should be found by
			// this..
			resp = getUrlAsString(c,SIGNALK_API + "/" + vessels + "/urn", restPort);
			 Json respJson = Json.read(resp);
			logger.debug(resp);
			assertTrue(resp, respJson.has(uuid));
		}
	}

	@Test
	public void shouldGetApiData() throws Exception {

		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {
		
			Json json = getUrlAsJson(c,  SIGNALK_API +"/vessels", "admin","admin",restPort);
					
			logger.debug("Endpoint json:" + json);
	
			//
			assertTrue(json.has(Config.getConfigProperty(ConfigConstants.UUID)));
		}

	}

	@Test
	public void shouldGetApiSelfData() throws Exception {

		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {

			Json json = getUrlAsJson(c,  SIGNALK_API +"/self", "admin","admin",restPort);
			
			logger.debug("Self json:" + json);
			assertEquals("\"vessels."+Config.getConfigProperty(ConfigConstants.UUID)+"\"", json.toString());
		}
	}

	@Test
	public void shouldGetApiUUIDData() throws Exception {

		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {

			String resp = getUrlAsString(c, SIGNALK_API + "/vessels/"+Config.getConfigProperty(ConfigConstants.UUID)+"/uuid","admin","admin",restPort);
			logger.debug("Endpoint json:" + resp);
			assertEquals("\""+Config.getConfigProperty(ConfigConstants.UUID)+"\"", resp);
		}
	}

	@Test
	public void shouldGetApiSubset() throws Exception {

		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {

			Json json = getUrlAsJson(c,  SIGNALK_API +"/vessels/urn:mrn:imo:mmsi:123456789", "admin","admin",restPort);
			
			logger.debug("Subset json:" + json);
			
			
		}
	}

	@Test
	public void shouldGetApiForSelf() throws Exception {

		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {

			Json json = getUrlAsJson(c,  SIGNALK_API +"/vessels/self", "admin","admin",restPort);
			
			logger.debug("Self json:" + json);
			assertFalse(json.has("NMEA2000"));
			//assertTrue(json.has(env));
			assertTrue(json.has(nav));
			//assertTrue(json.has(electrical));
			//assertTrue(json.has(tanks));
		}
	}

	@Test
	public void shouldGetApiForSources() throws Exception {

		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {
			String rslt = getUrlAsString(c, SIGNALK_API + "/sources","admin","admin",restPort);
			
			Json json = Json.read(rslt);
			logger.debug("Endpoint json:" + json);
			assertTrue(json.has("NMEA0183"));
			assertFalse(json.has(nav));
			assertFalse(json.has(electrical));
			assertFalse(json.has(tanks));
		}
	}

	@Test
	public void shouldGetSubscribeWsResponse() throws Exception {
		final List<String> received = new ArrayList<String>();
		final CountDownLatch latch = new CountDownLatch(5);
		try (final AsyncHttpClient c = asyncHttpClient(config().setUseInsecureTrustManager(true));) {

			String restUrl = wsScheme+"://localhost:" + restPort + SIGNALK_WS;
			logger.debug("Open websocket at: " + restUrl);		   
			WebSocket websocket = c.prepareGet(restUrl).setCookies(getCookies("admin", "admin"))
					.execute(new WebSocketUpgradeHandler.Builder().build()).get();

			websocket.addWebSocketListener(new WebSocketListener() {

				@Override
				public void onBinaryFrame(byte[] message, boolean b, int i) {
					logger.info("received BYTES --> " + String.valueOf(message));
				}

				@Override
				public void onTextFrame(String message, boolean b, int i) {
					logger.info("received --> " + message);
					received.add(message);
				}

				@Override
				public void onError(Throwable t) {
					logger.error(t);
				}

				@Override
				public void onOpen(WebSocket websocket) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onClose(WebSocket websocket, int code, String reason) {
					// TODO Auto-generated method stub
					
				}


			});
			// subscribe
			String token = SecurityUtils.authenticateUser("admin", "admin");
			String subscribeMsg = getSubscriptionJson("vessels.self","navigation",1000,1000,FORMAT_DELTA,POLICY_IDEAL, token).toString();
			websocket.sendTextFrame(subscribeMsg, true, 0);
			
			logger.debug("Sent subscribe = " + subscribeMsg);
	
			latch.await(10, TimeUnit.SECONDS);
			logger.debug("Completed receive ");

			websocket.sendCloseFrame();
			
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
