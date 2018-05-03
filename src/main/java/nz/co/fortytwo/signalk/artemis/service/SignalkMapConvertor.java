package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.label;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.type;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.values;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.util.Map.Entry;
import java.util.NavigableMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class SignalkMapConvertor {

	private static Logger logger = LogManager.getLogger(SignalkMapConvertor.class);
	
	public static NavigableMap<String, Json> recurseJsonFull(Json json, NavigableMap<String, Json> map, String prefix) {
		for (Entry<String, Json> entry : json.asJsonMap().entrySet()) {
			
				logger.debug("Recurse {} = {}",()->entry.getKey(),()->entry.getValue());
			if (entry.getValue().isPrimitive() 
					|| entry.getValue().isNull() 
					|| entry.getValue().isArray() 
					||entry.getValue().has(value)) {
				map.put(prefix + entry.getKey(), entry.getValue());
				continue;
			}  
			recurseJsonFull(entry.getValue(), map, prefix + entry.getKey() + ".");
		
		}
		return map;

	}

	/**
	 * Convert Delta JSON to map. Returns null if the json is not an update,
	 * otherwise return a map
	 * 
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public static NavigableMap<String, Json> parseDelta(Json node, NavigableMap<String, Json> temp) throws Exception {
		// avoid full signalk syntax
		if (node.has(vessels))
			return null;

		if (node.has(CONTEXT) && (node.has(UPDATES) || node.has(PUT))) {
			
			logger.debug("processing delta  {}",node);
			// process it

			// go to context
			String ctx = node.at(CONTEXT).asString();
			ctx = Util.fixSelfKey(ctx);
			logger.debug("ctx: {}", node);
			// Json pathNode = temp.addNode(path);
			Json updates = node.at(UPDATES);
			if (updates == null)
				updates = node.at(PUT);
			if (updates == null)
				return temp;

			for (Json update : updates.asJsonList()) {
				parseUpdate(temp, update, ctx);
			}

			
				logger.debug("processed delta  {}" ,temp);
			return temp;
		}
		return null;

	}

	protected static void parseUpdate(NavigableMap<String, Json> temp, Json update, String ctx) throws Exception {

		// grab values and add
		Json array = update.at(values);
		for (Json e : array.asJsonList()) {
			if (e == null || e.isNull() || !e.has(PATH))
				continue;
			String key = dot + e.at(PATH).asString();
			if(key.equals(dot))key="";
			e.delAt(PATH);
			// temp.put(ctx+"."+key, e.at(value).getValue());

			if (update.has(source)) {
				Json src = update.at(source);
				String srcRef=src.at(type).asString()+dot+src.at(label).asString();
				e.set(sourceRef, srcRef);
				//add sources
				recurseJsonFull(src,temp,sources+dot+srcRef+dot);
				
			}else{
				e.set(sourceRef, "self");
			}

			if (update.has(timestamp)) {
				logger.debug("put timestamp: {}:{}", ctx + key, e);
				e.set(timestamp, update.at(timestamp).asString());
			}else{
				e.set(timestamp,Util.getIsoTimeString());
			}
			
			if (e.has(value)) {
				logger.debug("put: {}:{}", ctx +  key, e);
				temp.put(ctx +  key, e);
			}
		}

	}

}
