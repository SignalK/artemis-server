package nz.co.fortytwo.signalk.artemis.util;

import static org.junit.Assert.*;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE__0183;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import mjson.Json;



public class UtilTest {
	private static Logger logger = LogManager.getLogger(UtilTest.class);

	

	@Test
	public void shouldGetUuidContext(){
		String path = "vessels.urn:mrn:signalk:uuid:28f9a6ae-ee66-4464-9ce4-a6dca3e33c7c";
		String ctx = path;
		String context = Util.getContext(path);
		logger.debug(context);
		assertEquals(ctx, context);
		
		path = path+".navigation.position";
		context = Util.getContext(path);
		logger.debug(context);
		assertEquals(ctx, context);
	}
	
	@Test
	public void shouldGetMMSIContext(){
		String path = "vessels.urn:mrn:imo:mmsi:230099999";
		String ctx = path;
		String context = Util.getContext(path);
		logger.debug(context);
		assertEquals(ctx, context);
		
		path = path+".navigation.position";
		context = Util.getContext(path);
		logger.debug(context);
		assertEquals(ctx, context);
	}
	
	/*@Test
	public void shouldGetUrlContext(){
		String path = "vessels.urn:mrn:signalk:https://motu.42.co.nz";
		String ctx = path;
		String context = Util.getContext(path);
		logger.debug(context);
		assertEquals(ctx, context);
		
		path = path+".navigation.position";
		context = Util.getContext(path);
		logger.debug(context);
		assertEquals(ctx, context);
	}*/
	
	@Test
	public void shouldGetWelcomeMsg(){
		Json msg = Util.getWelcomeMsg();
		logger.debug(msg);
		//{"timestamp":"2015-04-13T23:04:03.826Z","version":"0.1","SignalKConstants.self":"motu"}
		assertTrue(msg.has(SignalKConstants.timestamp));
		assertTrue(msg.has(SignalKConstants.version));
		assertTrue(msg.has(SignalKConstants.self_str));
	}

	
	@Test
	public void shouldMakePut() {
		logger.debug("Starting put message");
		Json body = Json.read("{\"value\": {" + 
				"            \"longitude\": 173.1693," + 
				"            \"latitude\": -41.156426" + 
				"          }}");
		Json put = Util.getJsonPutRequest("vessels.self."+nav_position, body);
		logger.debug("Outcome: {}",put);
		assertNotNull(put.at(CONTEXT));
		assertNotNull(put.at(PUT));
		assertTrue(put.at(PUT).asJsonList().size()>0);
	}

	@Test
	public void shouldConvertSourceToRef(){
		Json json = Json.read("{\"source\":{\"sentence\":\"RMC\",\"talker\":\"GP\",\"type\":\"NMEA0183\"},\"timestamp\":\"2013-11-01T14:46:29.000Z\",\"values\":[{\"path\":\"navigation.position\",\"value\":{\"longitude\":4.580064166666666,\"latitude\":51.9485185}}]}");
		Json rslt = Util.convertSourceToRef(json, AMQ_CONTENT_TYPE__0183,"/dev/ttyUSB0");
		logger.debug("Outcome: {}",rslt);
		assertEquals(Json.read("{\"sources\":{\"NMEA0183\":{\"/dev/ttyUSB0\":{\"sentence\":\"RMC\",\"talker\":\"GP\",\"type\":\"NMEA0183\"}}}}"), rslt);
		assertEquals("NMEA0183./dev/ttyUSB0",json.at(sourceRef).asString());
	}
}
