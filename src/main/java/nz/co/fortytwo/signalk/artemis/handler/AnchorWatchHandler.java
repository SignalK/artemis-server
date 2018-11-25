package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_INFLUX_KEY;
import static nz.co.fortytwo.signalk.artemis.util.Config.INTERNAL_KV;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.Message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;


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

/**
 * Read key value pairs from Config.INTERNAL_KV and watch anchor radius
 * 
 * @author robert
 * 
 */

public class AnchorWatchHandler extends BaseHandler{
	
	private static Logger logger = LogManager.getLogger(AnchorWatchHandler.class);

	
	private Double maxRadius;
	private Double currentLon;
	private Double currentLat;
	private Double anchorLon;
	private Double anchorLat;
	
	public AnchorWatchHandler() {
		super();
		try {
			
			if (logger.isDebugEnabled())
				logger.debug("Initialising for : {} ",uuid );
			//get db defaults
			initFromDb();
			
			initSession(AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+nav_anchor_maxRadius+"%' OR "
						+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+nav_position_latitude+"%'OR "
						+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+nav_position_longitude+"%'OR "
						+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+nav_anchor_position_latitude+"%'OR "
						+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+nav_anchor_position_longitude+"%'");
		} catch (Exception e) {
			logger.error(e,e);
		}
	}


	
	protected void initFromDb() {
		NavigableMap<String, Json> map=new ConcurrentSkipListMap<String, Json>();
		Map<String, String> query= new HashMap<>();
		query.put(skey, nav_anchor);
		influx.loadData(map, vessels, query);
		for(Entry<String, Json> entry:map.entrySet()) {
			if(entry.getKey().contains(nav_anchor_maxRadius)) maxRadius=Util.asDouble(entry.getValue().at(value));
			if(entry.getKey().contains(nav_anchor_position_latitude))anchorLat=Util.asDouble(entry.getValue().at(value));
			if(entry.getKey().contains(nav_position_longitude))anchorLon=Util.asDouble(entry.getValue().at(value));
		}
	}


	@Override
	public void consume(Message message) {
		
			String key = message.getStringProperty(AMQ_INFLUX_KEY);
			Json node = Util.readBodyBuffer(message.toCore());
			
			if (logger.isDebugEnabled())
				logger.debug("Processing key: {} : {}", key, node);
			if(key.contains(nav_anchor_maxRadius)) maxRadius=Util.asDouble(node.at(value));
			if(key.contains(nav_position_longitude))currentLon=Util.asDouble(node.at(value));
			if(key.contains(nav_position_latitude))currentLat=Util.asDouble(node.at(value));
			if(key.contains(nav_anchor_position_longitude))anchorLon=Util.asDouble(node.at(value));
			if(key.contains(nav_anchor_position_latitude))anchorLat=Util.asDouble(node.at(value));
			if(maxRadius!=null && maxRadius>0
					&& currentLon!=null 
					&& currentLat!=null 
					&& anchorLat!=null 
					&& anchorLon!=null) {
				check(message);
			}
	}
	
	protected void check(Message message){
		try {
			
				//anchor watch is on
				//workout distance
				double distance = Util.haversineMeters(currentLat,currentLon,anchorLat, anchorLon);
				logger.debug("Updating anchor distance:"+distance);
				send(message,vessels+dot+uuid+dot+ nav_anchor_currentRadius+".values.internal",distance);
		} catch (Exception e) {
			logger.error(e);
		}
	}
	


	
	

}
