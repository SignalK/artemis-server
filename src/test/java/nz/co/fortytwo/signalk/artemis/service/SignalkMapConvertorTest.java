package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.GET;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class SignalkMapConvertorTest {

	private static Logger logger = LogManager.getLogger(SignalkMapConvertorTest.class);
	@Test
	public void shouldConvertFull() throws IOException {
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/full/docs-data_model_multiple_values.json"));
		Json in = Json.read(body);
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseFull(in, map, "");
		Json out = SignalkMapConvertor.mapToFull(map);
		logger.debug(in);
		logger.debug(out);
		assertEquals(in, out);
	}
	
	@Test
	public void shouldConvertUpdate() throws IOException {
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/delta/docs-data_model_multiple_values.json"));
		Json in = Json.read(body);
		//convert source element
		in.at(UPDATES).asJsonList().forEach((j) -> {
			logger.debug(Util.convertSourceToRef(j,null,null));
		});
		
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseDelta(in, map);
		Json out = SignalkMapConvertor.mapToUpdatesDelta(map);
		logger.debug(in);
		logger.debug(out);
		assertEquals(in, out);
	}

	
	
		@Test
		public void shouldConvertPut() throws IOException {
			
			Json in = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"put\":[{\"timestamp\":\"2018-05-13T01:11:09.832Z\",\"$source\":\"none\",\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}");
			//convert source element
			in.at(PUT).asJsonList().forEach((j) -> {
				logger.debug("srcToRef: {}", Util.convertSourceToRef(j,null,null));
			});
			
			NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
			SignalkMapConvertor.parseDelta(in, map);
			logger.debug("Map: {}",map);
			assertTrue(map.containsKey("vessels.urn:mrn:imo:mmsi:234567890.propulsion.0.boostPressure.values.none"));
			assertEquals(45500.0d,map.get("vessels.urn:mrn:imo:mmsi:234567890.propulsion.0.boostPressure.values.none").at(value).asDouble(),0.001);
		}
		
		@Test
		public void shouldNotConvertGet() throws IOException {
			
			Json in = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"get\":[{\"path\":\"propulsion.0.boostPressure\"}]}");
			//convert source element
			in.at(GET).asJsonList().forEach((j) -> {
				logger.debug("srcToRef: {}", Util.convertSourceToRef(j,null,null));
			});
			
			NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
			SignalkMapConvertor.parseDelta(in, map);
			logger.debug("Map: {}",map);
			Json out = SignalkMapConvertor.mapToUpdatesDelta(map);
			assertEquals(out,Json.object());
			 out = SignalkMapConvertor.mapToPutDelta(map);
			assertEquals(out,Json.object());
			 out = SignalkMapConvertor.mapToConfigDelta(map);
			assertEquals(out,Json.object());
			logger.debug(in);
			logger.debug(out);
			
		}
}
