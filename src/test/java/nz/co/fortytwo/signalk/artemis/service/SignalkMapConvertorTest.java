package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
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
	public void shouldConvertDelta() throws IOException {
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/delta/docs-data_model_multiple_values.json"));
		Json in = Json.read(body);
		//convert source element
		in.at(SignalKConstants.UPDATES).asJsonList().forEach((j) -> {
			logger.debug(Util.convertSourceToRef(j,null,null));
		});
		
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseDelta(in, map);
		Json out = SignalkMapConvertor.mapToDelta(map);
		logger.debug(in);
		logger.debug(out);
		assertEquals(in, out);
	}

	
	
}
