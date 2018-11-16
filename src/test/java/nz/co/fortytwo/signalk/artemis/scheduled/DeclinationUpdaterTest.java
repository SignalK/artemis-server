package nz.co.fortytwo.signalk.artemis.scheduled;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.server.BaseServerTest;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class DeclinationUpdaterTest {
	private static final String SELF_UUID = "705f5f1a-efaf-44aa-9cb8-a0fd6305567c";
	private static Logger logger = LogManager.getLogger(DeclinationUpdaterTest.class);
	private InfluxDbService influx;
	
	@Before
	public void setUpInfluxDb() throws IOException {
		logger.debug("Start influxdb client");
		InfluxDbService.setDbName(BaseServerTest.SIGNALK_TEST_DB);
		influx = new InfluxDbService(BaseServerTest.SIGNALK_TEST_DB);
		influx.setWrite(true);
		NavigableMap<String, Json> map = getJsonMap("./src/test/resources/samples/full/docs-data_model.json");
		
		//save and flush
		influx.save(map);
	}

	@After
	public void closeInfluxDb() {
		influx.close();
	}

	@Test
	public void updateDeclination() {
		logger.debug("start");
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put(skey,Util.regexPath(nav+dot+"position").toString());
		queryMap.put("uuid",Util.regexPath(SELF_UUID).toString());
		influx.loadData(map, vessels, queryMap);
		logger.debug(map);
		
		DeclinationUpdater updater = new DeclinationUpdater();
		updater.calculate(SELF_UUID);
		
		map = new ConcurrentSkipListMap<>();
		queryMap.clear();
		queryMap.put(skey,Util.regexPath(nav+dot+"magneticVariation").toString());
		queryMap.put("uuid",Util.regexPath(SELF_UUID).toString());
		influx.loadData(map, vessels, queryMap);
		logger.debug(map);
		
		assertEquals(0.235848, map.get(vessels+dot+ SELF_UUID+dot+nav+dot+"magneticVariation").at(value).asDouble(), 0.0001);
		
	}
	
	private NavigableMap<String, Json> getJsonMap(String file) throws IOException {
		//convert to map
		Json json = getJson(file);
		return getJsonMap(json);
	}
	
	private Json getJson(String file) throws IOException {
		String body = FileUtils.readFileToString(new File(file));
		Json json = Json.read(body);
		return json;
	}
	private NavigableMap<String, Json> getJsonMap(Json json) throws IOException {
		//convert to map
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseFull(json, map, "");
		map.forEach((t, u) -> logger.debug(t + "=" + u));
		return map;
	}

}
