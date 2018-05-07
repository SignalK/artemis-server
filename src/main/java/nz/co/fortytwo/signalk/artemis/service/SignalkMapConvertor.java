package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;
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

import java.io.IOException;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQPropertyConversionException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.JsonSerializer;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class SignalkMapConvertor {

	private static Logger logger = LogManager.getLogger(SignalkMapConvertor.class);
	
	public static NavigableMap<String, Json> parseFull(Json json, NavigableMap<String, Json> map, String prefix) {
		if(map==null)map = new ConcurrentSkipListMap<>();
		if(json==null|| json.isNull()) return map;
		
		for (Entry<String, Json> entry : json.asJsonMap().entrySet()) {
			
				logger.debug("Recurse {} = {}",()->entry.getKey(),()->entry.getValue());
			if (entry.getValue().isPrimitive() 
					|| entry.getValue().isNull() 
					|| entry.getValue().isArray() 
					||entry.getValue().has(value)) {
				map.put(prefix + entry.getKey(), entry.getValue());
				continue;
			}  
			parseFull(entry.getValue(), map, prefix + entry.getKey() + ".");
		
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
	public static NavigableMap<String, Json> parseDelta(Json node, NavigableMap<String, Json> temp){
		
		if(temp==null)temp = new ConcurrentSkipListMap<>();
		if(node==null|| node.isNull()) return temp;
		// avoid full signalk syntax
		if (node.has(vessels))
			return null;

		if (node.has(CONTEXT) && (node.has(UPDATES) || node.has(PUT))) {
			
			logger.debug("processing delta  {}",node);
			// process it

			// go to context
			String ctx = node.at(CONTEXT).asString();
			ctx = Util.fixSelfKey(ctx);
			ctx = StringUtils.removeEnd(ctx,".");
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

	protected static void parseUpdate(NavigableMap<String, Json> temp, Json update, String ctx)  {

		// grab values and add
		Json array = update.at(values);
		for (Json e : array.asJsonList()) {
			if (e == null || e.isNull() || !e.has(PATH))
				continue;
			String key = dot + e.at(PATH).asString();
			if(key.equals(dot))key="";
			//e.delAt(PATH);
			// temp.put(ctx+"."+key, e.at(value).getValue());
			String srcRef=null;
			if (update.has(source)) {
				Json src = update.at(source);
				if(src.has(label)){
					srcRef=src.at(type).asString()+dot+src.at(label).asString();
				}else if(src.has(talker)){
					srcRef=src.at(type).asString()+dot+src.at(talker).asString();
				}
				
				//add sources
				parseFull(src,temp,sources+dot+srcRef+dot);
			}else{
				srcRef="self";
			}
			e.set(sourceRef, srcRef);

			if (update.has(timestamp)) {
				logger.debug("put timestamp: {}:{}", ctx + key, e);
				e.set(timestamp, update.at(timestamp).asString());
			}else{
				e.set(timestamp,Util.getIsoTimeString());
			}
			
			if (e.has(value)) {
				logger.debug("put: {}:{}", ctx +  key, e);
				temp.put(ctx +  key+dot+values+dot+srcRef, e.dup().delAt(PATH));
			}
		}

	}
	
	public static Json mapToFull(NavigableMap<String, Json> map) throws IOException{
		if(map==null)return Json.nil();
		JsonSerializer ser = new JsonSerializer();
		return Json.read(ser.write(map));
	}

	public static Json mapToDelta(NavigableMap<String, Json> map)  {
		if(map==null)return Json.nil();
		//ClientMessage msgReceived = null;
		Map<String, Map<String, Map<String, Map<String,List<Entry<String,Json>>>>>> msgs = new HashMap<>();
		for (Entry<String,Json>entry: map.entrySet()) {
			Json eValue = entry.getValue();
			String eKey = entry.getKey();
			
			if(eKey.startsWith(sources))continue;
			if(eKey.endsWith(attr))continue;
			
			logger.debug("message = {} : {}",eKey,eValue);
			String ctx = Util.getContext(eKey);
			Map<String, Map<String, Map<String,List<Entry<String,Json>>>>> ctxMap = msgs.get(ctx);
			if (ctxMap == null) {
				ctxMap = new HashMap<>();
				msgs.put(ctx, ctxMap);
			}
			logger.debug("$timestamp: {}",eValue.at(timestamp));
			Map<String, Map<String,List<Entry<String,Json>>>> tsMap = ctxMap.get(eValue.at(timestamp).asString());
			if (tsMap == null) {
				tsMap = new HashMap<>();
				ctxMap.put(eValue.at(timestamp).asString(), tsMap);
			}
			logger.debug("$source: {}",eValue.at(sourceRef));
			Map<String,List<Entry<String,Json>>> srcMap = tsMap.get(eValue.at(sourceRef).asString());
			if (srcMap == null) {
				srcMap = new HashMap<>();
				tsMap.put(eValue.at(sourceRef).asString(), srcMap);
			}
			eKey = eKey.substring(ctx.length()+1);
			if (eKey.contains(dot + values + dot))
				eKey = eKey.substring(0, eKey.indexOf(dot + values + dot));
			List<Entry<String,Json>> list = srcMap.get(eKey);
			if (list == null) {
				list = new ArrayList<>();
				srcMap.put(eKey, list);
			}
			logger.debug("Add entry: {}:{}",eKey,entry);
			list.add(entry);
		}
		return generateDelta(msgs);
	}
	
	/**
	 * Input is a list of message wrapped in a stack of hashMaps, eg Key=context
	 * Key=timestamp key=source List(messages) The method iterates through and
	 * creates the deltas as a Json array, one Json delta per context.
	 * 
	 * @param msgs
	 * @return
	 */
	public static Json generateDelta(Map<String, Map<String, Map<String, Map<String,List<Entry<String,Json>>>>>> msgs) {
		logger.debug("Delta map: {}",msgs);
		Json deltaArray = Json.array();
		// add values
		if (msgs.size() == 0)
			return deltaArray;
		// each timestamp
		Json delta = Json.object();
		deltaArray.add(delta);

		Json updatesArray = Json.array();
		delta.set(UPDATES, updatesArray);

		for (String ctx : msgs.keySet()) {

			for (String ts : msgs.get(ctx).keySet()) {
				logger.debug("timestamp: {}", ts);
				for (String src : msgs.get(ctx).get(ts).keySet()) {
					// new values object
					logger.debug("sourceRef: {}", src);
					// make wrapper object
					Json valObj = Json.object();
					updatesArray.add(valObj);

					Json valuesArray = Json.array();
					valObj.at(values, valuesArray);
					valObj.set(timestamp, ts);
					valObj.set(sourceRef, src);

					// now the values
					for (Entry<String,List<Entry<String,Json>>> msg : msgs.get(ctx).get(ts).get(src).entrySet()) {
						logger.debug("item: {}", msg.getKey());
						List<Entry<String,Json>> list = msg.getValue();
						
						for( Entry<String,Json> v :list){
							logger.debug("Key: {}, value: {}", v.getKey(),v.getValue());
							String vKey = v.getKey();
							vKey = vKey.substring(ctx.length()+1);
							if (vKey.contains(dot + values + dot))
								vKey = vKey.substring(0, vKey.indexOf(dot + values + dot));
							Json val = Json.object(PATH, vKey);
							
							if (v.getValue().isObject()){
								v.getValue().delAt(timestamp);
								v.getValue().delAt(sourceRef);
								val.set(value, v.getValue().at(value));
							}else{
								val.set(value, v.getValue());
							}
							valuesArray.add(val);
						}
					}
				}
				// add context
			}
			delta.set(CONTEXT, ctx);
		}

		return deltaArray;
	}

}
