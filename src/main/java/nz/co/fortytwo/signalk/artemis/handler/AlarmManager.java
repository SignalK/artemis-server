package nz.co.fortytwo.signalk.artemis.handler;


import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;


public class AlarmManager {
	private static Logger logger = LogManager.getLogger(AlarmManager.class);
	protected static TDBService influx = new InfluxDbService();
	private String vesselUuid=Config.getConfigProperty(ConfigConstants.UUID);
	
	public AlarmManager() {
		logger.debug("Alarm manager created for vessel:{}",vesselUuid);
	}
	public void calculate(String uuid) {
		logger.debug("Alarm check fired ");
		influx.setWrite(true);
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("skey",Util.regexPath(nav+dot+"position").toString());
		queryMap.put("uuid",Util.regexPath(uuid).toString());
		influx.loadData(map, vessels, queryMap);
		logger.debug("Position: {}", map);
	}
}
