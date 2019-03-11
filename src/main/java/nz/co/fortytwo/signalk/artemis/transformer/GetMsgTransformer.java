package nz.co.fortytwo.signalk.artemis.transformer;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE_JSON_GET;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.ALL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_FULL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.GET;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.aircraft;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.aton;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sar;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.skey;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.intercept.BaseInterceptor;
import nz.co.fortytwo.signalk.artemis.tdb.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.tdb.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.MessageSupport;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.SignalkMapConvertor;
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
 * Converts SignalK GET interceptor
 * 
 * @author robert
 * 
 */

public class GetMsgTransformer extends MessageSupport implements Transformer {

	
	private static Logger logger = LogManager.getLogger(GetMsgTransformer.class);
	private static TDBService influx = new InfluxDbService();
	
	/**
	 * Reads Delta GET message and returns the result in full format. Does nothing if json
	 * is not a GET, and returns the original message
	 * 
	 * @param node
	 * @return
	 */

	@Override
	public Message transform(Message message) {
		if (!AMQ_CONTENT_TYPE_JSON_GET.equals(message.getStringProperty(AMQ_CONTENT_TYPE)))
			return message;
		
		Json node = Util.readBodyBuffer(message.toCore());
		String correlation = message.getStringProperty(Config.AMQ_CORR_ID);
		String destination = message.getStringProperty(Config.AMQ_REPLY_Q);
		
		// deal with diff format
		if (node.has(CONTEXT) && (node.has(GET))) {
			if (logger.isDebugEnabled())
				logger.debug("GET msg: {}", node.toString());
			String ctx = node.at(CONTEXT).asString();
			String jwtToken = null;
			if(node.has(SignalKConstants.TOKEN)&&!node.at(SignalKConstants.TOKEN).isNull()) {
				jwtToken=node.at(SignalKConstants.TOKEN).asString();
			}
			String root = StringUtils.substringBefore(ctx,dot);
			root = Util.sanitizeRoot(root);
			
			
			//limit to explicit series
			if (!vessels.equals(root) 
				&& !CONFIG.equals(root) 
				&& !sources.equals(root) 
				&& !resources.equals(root)
				&& !aircraft.equals(root)
				&& !sar.equals(root)
				&& !aton.equals(root)
				&& !ALL.equals(root)){
				try{
					sendReply(destination,FORMAT_FULL,correlation,Json.object(),jwtToken);
					return null;
				} catch (Exception e) {
					logger.error(e, e);
					
				}
			}
			String qUuid = StringUtils.substringAfter(ctx,dot);
			if(StringUtils.isBlank(qUuid))qUuid="*";
			ArrayList<String> fullPaths=new ArrayList<>();
			
			try {
				NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
				for(Json p: node.at(GET).asJsonList()){
					
					String path = p.at(PATH).asString();
					String time = p.has("time")? p.at("time").asString(): null;
					if (logger.isDebugEnabled())
						logger.debug("GET time : {}={}", time, StringUtils.isNotBlank(time)?Util.getMillisFromIsoTime(time):null);
					path=Util.sanitizePath(path);
					fullPaths.add(Util.sanitizeRoot(ctx+dot+path));
					
					path=Util.regexPath(path).toString();
					Map<String, String> queryMap = new HashMap<>();
					if(StringUtils.isNotBlank(qUuid))queryMap.put(skey,path);
					if(StringUtils.isBlank(path))queryMap.put("uuid",Util.regexPath(qUuid).toString());
					switch (root) {
					case CONFIG:
						influx.loadConfig(map, queryMap);
						if(map.size()==0 && queryMap.size()==0) {
							//send defaults
							Config.setDefaults(map);
						}
						break;
					case resources:
						influx.loadResources(map, queryMap);
						break;
					case sources:
						influx.loadSources(map, queryMap);
						break;
					case vessels:
						if(StringUtils.isNotBlank(time)) {
							influx.loadDataSnapshot(map, vessels, queryMap, time);
						}else {
							influx.loadData(map, vessels, queryMap);
						}
						break;
					case aircraft:
						if(StringUtils.isNotBlank(time)) {
							influx.loadDataSnapshot(map, aircraft, queryMap, time);
						}else {
							influx.loadData(map, aircraft, queryMap);
						}
						
						break;
					case sar:
						if(StringUtils.isNotBlank(time)) {
							influx.loadDataSnapshot(map, sar, queryMap, time);
						}else {
							influx.loadData(map, sar, queryMap);
						}
			
						break;
					case aton:
						if(StringUtils.isNotBlank(time)) {
							influx.loadDataSnapshot(map, aton, queryMap, time);
						}else {
							influx.loadData(map, aton, queryMap);
						}
						
						break;
					case ALL:
						if(StringUtils.isNotBlank(time)) {
							influx.loadDataSnapshot(map, vessels, null, time);
						}else {
							influx.loadData(map, vessels, null);
						}
						//loadAllDataFromInflux(map,aircraft);
						//loadAllDataFromInflux(map,sar);
						//loadAllDataFromInflux(map,aton);
					default:
					}
					
					
				}
				
				if (logger.isDebugEnabled())logger.debug("GET  token: {}, map : {}",jwtToken, map);
				
				Json json = SignalkMapConvertor.mapToFull(map);
				
				if (logger.isDebugEnabled())logger.debug("GET json : {}", json);
				
				String fullPath = StringUtils.getCommonPrefix(fullPaths.toArray(new String[]{}));
				//fullPath=StringUtils.remove(fullPath,".*");
				//fullPath=StringUtils.removeEnd(fullPath,".");
				
				// for REST we only send back the sub-node, so find it
				if (logger.isDebugEnabled())logger.debug("GET node : {}", fullPath);
				
				if (StringUtils.isNotBlank(fullPath) && !root.startsWith(CONFIG) && !root.startsWith(ALL))
					json = Util.findNodeMatch(json, fullPath);
				
				sendReply(destination,FORMAT_FULL,correlation,json,jwtToken);

			} catch (Exception e) {
				logger.error(e, e);
				
			}

		}
		return message;
	}

	

}
