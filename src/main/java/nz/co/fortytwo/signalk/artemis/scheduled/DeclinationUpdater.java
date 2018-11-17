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
package nz.co.fortytwo.signalk.artemis.scheduled;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.TSAGeoMag;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Fired periodically. If LAT and LON is available, it calculates declination, compares to existing and saves if changed..
 * 
 * @author robert
 * 
 */
public class DeclinationUpdater implements Runnable {

	private static Logger logger = LogManager.getLogger(DeclinationUpdater.class);
	protected static TDBService influx = new InfluxDbService();
	private TSAGeoMag geoMag = new TSAGeoMag();
	private String vesselUuid=Config.getConfigProperty(ConfigConstants.UUID);

	public DeclinationUpdater() {
		logger.debug("Declination  calculator created for vessel:{}",vesselUuid);
	}
	public void calculate(String uuid) {
		logger.debug("Declination  calculation fired ");
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put(skey,Util.regexPath(nav+dot+"position").toString());
		queryMap.put("uuid",Util.regexPath(uuid).toString());
		influx.loadData(map, vessels, queryMap);
		logger.debug("Position: {}", map);
		Double lat = null;
		Double lon = null;
		for( Entry<String, Json> entry : map.entrySet()) {
			if(entry.getKey().endsWith("position")) {
				lat = entry.getValue().at(value).at(LATITUDE).asDouble();
				lon = entry.getValue().at(value).at(LONGITUDE).asDouble();
			}
			
		}

		if (lat != null && lon != null) {
			if (logger.isDebugEnabled())
				logger.debug("Declination  for " + lat + ", " + lon);

			double declination = Math.toRadians(geoMag.getDeclination(lat, lon, DateTime.now().getYear(), 0.0d));

			declination = Util.round(declination, 6);
			if (logger.isDebugEnabled()) {
				logger.debug("Declination (rad)= " + declination);
				logger.debug("Declination (deg)= " + Math.toDegrees(declination));
			}
			map.clear();
			queryMap.clear();
			queryMap.put(skey,Util.regexPath(nav+dot+"magneticVariation").toString());
			queryMap.put("uuid",Util.regexPath(uuid).toString());
			influx.loadData(map, vessels, queryMap);
			logger.debug(map);
			Double decl = null;
			if(map.get(vessels+dot+ uuid+dot+nav+dot+"magneticVariation")!=null) {
					decl = map.get(vessels+dot+ uuid+dot+nav+dot+"magneticVariation").at(value).asDouble();
					logger.debug("Existing Declination: {}", map);
			}
			if(decl==null || (decl!=null && declination==Util.round(decl, 6))) {
				logger.debug("Declination changed: {}", declination);
				influx.save(vessels+dot+uuid+dot + nav+dot+"magneticVariation", Json.object().set(value,declination));
				influx.save(vessels+dot+uuid+dot + nav+dot+"magneticVariationAgeOfService", Json.object().set(value,System.currentTimeMillis()));
				((InfluxDbService)influx).getInfluxDB().flush();
			}
		}
	}

	@Override
	public void run() {
		calculate(vesselUuid);
		
	}

}
