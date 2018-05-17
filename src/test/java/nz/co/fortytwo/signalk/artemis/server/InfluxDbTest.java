package nz.co.fortytwo.signalk.artemis.server;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.dto.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.SecurityService;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;

public class InfluxDbTest {

	private static Logger logger = LogManager.getLogger(InfluxDbTest.class);
	private InfluxDbService influx;
	//private JsonSerializer ser = new JsonSerializer();
	@Before
	public void setUpInfluxDb() {
		logger.debug("Start influxdb client");
		influx = new InfluxDbService();
		
		//ser.setPretty(2);
	}

	@After
	public void closeInfluxDb() {
		influx.close();
	}
	
	private void clearDb(){
		influx.getInfluxDB().query(new Query("drop measurement vessels", "signalk"));
		influx.getInfluxDB().query(new Query("drop measurement sources", "signalk"));
		//influx.getInfluxDB().query(new Query("drop measurement config", "signalk"));
		influx.getInfluxDB().query(new Query("drop measurement resources", "signalk"));
	}
	
	@Test
	public void shouldProcessFullModel() throws IOException {
		
		NavigableMap<String, Json> map = getJsonMap("./src/test/resources/samples/full/docs-data_model.json");
		assertEquals(13,map.size());
	}

	

	@Test
	public void shouldSaveMultipleValuesAndReturnLatest() throws IOException {
		clearDb();
		influx.setPrimary("vessels.urn:mrn:signalk:uuid:c0d79334-4e25-4245-8892-54e8ccc8021d.navigation.courseOverGroundTrue","ttyUSB0.GP.sentences.RMC");
		// get a sample of signalk
		NavigableMap<String, Json> map = getJsonMap("./src/test/resources/samples/full/docs-data_model_multiple_values.json");
		SecurityService secure = new SecurityService();
		secure.addAttributes(map);
		//save and flush
		
		influx.save(map);
		//reload from db
		NavigableMap<String, Json> rslt = loadFromDb("urn:mrn:signalk:uuid:c0d79334-4e25-4245-8892-54e8ccc8021d");
		compareMaps(map,rslt);
	}
	
	@Test
	public void shouldSaveFullModelAndReturnLatest() throws IOException {
		clearDb();
		// get a sample of signalk
		NavigableMap<String, Json> map = getJsonMap("./src/test/resources/samples/full/docs-data_model.json");
		SecurityService secure = new SecurityService();
		secure.addAttributes(map);
		//save and flush
		influx.save(map);
		//reload from db
		NavigableMap<String, Json> rslt = loadFromDb("urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c");
		compareMaps(map,rslt);
	}
	
	@Test
	public void shouldSaveFullModelAndReturnLatestWithEdit() throws IOException {
		clearDb();
		// get a sample of signalk
		NavigableMap<String, Json> map = getJsonMap("./src/test/resources/samples/full/docs-data_model.json");
		SecurityService secure = new SecurityService();
		secure.addAttributes(map);
		//save and flush
		influx.save(map);
		//reload from db
		NavigableMap<String, Json> rslt = loadFromDb("urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c");
		compareMaps(map,rslt);
		//now run again with variation
		map.put("vessels.urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c.navigation.headingMagnetic.value",Json.make(6.55));
		secure.addAttributes(map);
		//save and flush
		influx.save(map);
		//reload
		rslt = influx.loadData(map,"select * from vessels group by skey,primary, uuid,grp order by time desc limit 1","signalk");
		compareMaps(map,rslt);
	}
	
	@Test
	public void shouldSaveFullModelSamples() throws IOException {
		File dir = new File("./src/test/resources/samples/full");
		for(File f:dir.listFiles()){
			if(f.isDirectory())continue;
			clearDb();
			logger.debug("Testing sample:"+f.getName());
			//flush primaryMap
			influx.loadPrimary();
			// get a sample of signalk
			NavigableMap<String, Json> map = getJsonMap(f.getAbsolutePath());
			SecurityService secure = new SecurityService();
			secure.addAttributes(map);
			//save and flush
			influx.save(map);
			//reload from db
			NavigableMap<String, Json> rslt = loadFromDb(map.get("self").asString(),map.get("version").asString());
			compareMaps(map,rslt);
		}
	}
	
	@Test
	public void shouldSaveDeltaSamples() throws Exception {
		File dir = new File("./src/test/resources/samples/delta");
		for(File f:dir.listFiles()){
			if(f.isDirectory())continue;
			clearDb();
			logger.debug("Testing sample:"+f.getName());
			// get a sample of signalk
			NavigableMap<String, Json> map = getJsonDeltaMap(f.getAbsolutePath());
			SecurityService secure = new SecurityService();
			secure.addAttributes(map);
			//save and flush
			influx.save(map);
			//reload from db
			NavigableMap<String, Json> rslt = loadFromDb();
			compareMaps(map,rslt);
		}
	}
	
	@Test
	public void testFullResources() throws IOException {
		clearDb();
		// get a hash of signalk
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/full_resources.json"));
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseFull(Json.read(body), map, "");
		
		SecurityService secure = new SecurityService();
		secure.addAttributes(map);
		
		influx.save(map);
		//reload from db
		NavigableMap<String, Json> rslt = loadFromDb();
		compareMaps(map,rslt);
	}
	
	

	@Test
	public void testConfigJson() throws IOException {
		clearDb();
		// get a hash of signalk
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/signalk-config.json"));
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseFull(Json.read(body), map, "");
		
		SecurityService secure = new SecurityService();
		secure.addAttributes(map);
		
		influx.save(map);
		
		//reload from db
		NavigableMap<String, Json> rslt = loadConfigFromDb();
		compareMaps(map,rslt);
	}
	
	
	

	@Test
	public void testPKTreeJson() throws Exception {
		clearDb();
		// get a hash of signalk
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/PK_tree.json"));
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseFull(Json.read(body), map, "");
		SecurityService secure = new SecurityService();
		secure.addAttributes(map);
		map.forEach((t, u) -> logger.debug(t + "=" + u));
		map.forEach((k, v) -> influx.save(map));
		//reload from db
		NavigableMap<String, Json> rslt = loadFromDb();
		compareMaps(map,rslt);
	}
	
	private void compareMaps(NavigableMap<String, Json> map, NavigableMap<String, Json> rslt) {
		
		
		//check we have self and version
		map.remove("self");
		map.remove("self._attr");
		map.remove("version");
		map.remove("version._attr");
		rslt.remove("self");
		rslt.remove("self._attr");
		rslt.remove("version");
		rslt.remove("version._attr");
		
		//are they the same
		logger.debug("Map size=" + map.size());
		logger.debug("Rslt size=" + rslt.size());
		
		map.forEach((t, u) -> {
			//logger.debug("map key:"+t+":"+u+"|"+rslt.get(t));
			if(!u.equals(rslt.get(t)))logger.debug("map > rslt entries differ: {}:{}|{}",t ,u, rslt.get(t));
		});
		rslt.forEach((t, u) -> {
			if(!u.equals(map.get(t)))logger.debug("rslt > map entries differ: {}:{}|{}",t ,u, map.get(t));
		
		});
		
		assertEquals("Maps differ",map,rslt);
		logger.debug("Entries are the same");
		
		assertEquals("Maps differ in size",map.size(),rslt.size());
	}
	

	private NavigableMap<String, Json> getJsonMap(String file) throws IOException {
		String body = FileUtils.readFileToString(new File(file));
		//convert to map
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseFull(Json.read(body), map, "");
		map.forEach((t, u) -> logger.debug(t + "=" + u));
		return map;
	}
	
	private NavigableMap<String, Json> getJsonDeltaMap(String file) throws Exception {
		String body = FileUtils.readFileToString(new File(file));
		//convert to map
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseDelta(Json.read(body), map);
		map.forEach((t, u) -> logger.debug(t + "=" + u));
		return map;
	}
	
	private NavigableMap<String, Json> loadFromDb(String self) {
		return loadFromDb(self,"1.0.0");
	}
	private NavigableMap<String, Json> loadFromDb(String self, String version) {
		NavigableMap<String, Json> rslt = loadFromDb();
		
		//add std entries
		rslt.put("self",Json.make(self));
		rslt.put("version",Json.make(version));
		
		logger.debug("self",Json.make(self));
		logger.debug("version",Json.make(version));
		return rslt;
	}
	
	private NavigableMap<String, Json> loadFromDb() {
		NavigableMap<String, Json> rslt = new ConcurrentSkipListMap<String, Json>();
		
		rslt = influx.loadData(rslt,"select * from vessels group by skey,primary, uuid,sourceRef,owner,grp order by time desc limit 1","signalk");
		rslt = influx.loadSources(rslt,"select * from sources group by skey,uuid,owner,grp order by time desc limit 1","signalk");
		rslt = influx.loadResources(rslt,"select * from resources group by skey,uuid,owner,grp order by time desc limit 1","signalk");
		rslt.forEach((t, u) -> logger.debug(t + "=" + u));
		return rslt;
	}
	
	private NavigableMap<String, Json> loadConfigFromDb() {
		NavigableMap<String, Json> rslt = new ConcurrentSkipListMap<String, Json>();
		
		rslt = influx.loadConfig(rslt,"select * from config group by skey,uuid,owner,grp order by time desc limit 1","signalk");
		
		rslt.forEach((t, u) -> logger.debug(t + "=" + u));
		return rslt;
	}
}
