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
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.JsonSerializer;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class SignalkMapConvertor {

	private static Logger logger = LogManager.getLogger(SignalkMapConvertor.class);
	
	public static NavigableMap<String, Json> parseFull(Json json, NavigableMap<String, Json> map, String prefix) {
		if(map==null)map = new ConcurrentSkipListMap<>();
		if(json==null|| json.isNull()) return map;
		
		for (Entry<String, Json> entry : json.asJsonMap().entrySet()) {
			
			if (logger.isDebugEnabled())
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

		if (node.has(CONTEXT) && (node.has(UPDATES) || node.has(PUT) || node.has(CONFIG))) {
			
			if (logger.isDebugEnabled())
				logger.debug("processing delta  {}",node);
			// process it

			// go to context
			String ctx = node.at(CONTEXT).asString();
			ctx = Util.fixSelfKey(ctx);
			ctx = StringUtils.removeEnd(ctx,".");
			if (logger.isDebugEnabled())
				logger.debug("ctx: {}", node);

			if(node.has(UPDATES)){
				for (Json update : node.at(UPDATES).asJsonList()) {
					parseUpdate(temp, update, ctx);
				}
			}
			if(node.has(PUT)){
				for (Json put : node.at(PUT).asJsonList()) {
					parsePut(temp, put, ctx);
				}
			}
			if(node.has(CONFIG)){
				for (Json update : node.at(CONFIG).asJsonList()) {
					parseUpdate(temp, update, ctx);
				}
			}

			
			if (logger.isDebugEnabled())
				logger.debug("processed delta  {}" ,temp);
			return temp;
		}
		return null;

	}

	protected static void parseUpdate(NavigableMap<String, Json> temp, Json update, String ctx)  {

		// grab values and add
		Json array = update.at(values);
		for (Json val : array.asJsonList()) {
			if (val == null || val.isNull() || !val.has(PATH))
				continue;
			
			Json e = val.dup();
			
			String key = dot + e.at(PATH).asString();
			if(key.equals(dot))key="";
			//e.delAt(PATH);
			// temp.put(ctx+"."+key, e.at(value).getValue());
			String srcRef=null;
			if (update.has(sourceRef)) {
				srcRef=update.at(sourceRef).asString();
				e.set(sourceRef, srcRef);
			}
			

			if (update.has(timestamp)) {
				if (logger.isDebugEnabled())
					logger.debug("put timestamp: {}:{}", ctx + key, e);
				e.set(timestamp, update.at(timestamp).asString());
			}else{
				e.set(timestamp,Util.getIsoTimeString());
			}
			e.delAt(PATH);
			if (e.has(value)) {
				if (logger.isDebugEnabled())
					logger.debug("put: {}:{}", ctx +  key, e);
				temp.put(ctx +  key+dot+values+dot+srcRef, e);
			}
		}

	}
	
	protected static void parsePut(NavigableMap<String, Json> temp, Json put, String ctx)  {

			if (put == null || put.isNull() || !put.has(PATH))
				return;
			
			Json e = put.dup();
			
			String key = dot + e.at(PATH).asString();
			if(key.equals(dot))key="";
			
			if (!e.has(sourceRef)) {
				e.set(sourceRef,UNKNOWN);
			}
			if (!e.has(timestamp)) {
				e.set(timestamp,Util.getIsoTimeString());
			}
			e.delAt(PATH);
			if (e.has(value)) {
				if (logger.isDebugEnabled())
					logger.debug("put: {}:{}", ctx +  key, e);
				temp.put(ctx +  key+dot+values+dot+e.at(sourceRef).asString(), e);
			}
		

	}
	
	public static Json mapToFull(NavigableMap<String, Json> map) throws IOException{
		if(map==null)return Json.nil();
		Json root = Json.object();
		root.set(self_str,Json.make(Config.getConfigProperty(ConfigConstants.UUID)));
		root.set(version,Json.make(Config.getConfigProperty(ConfigConstants.VERSION)));
		map.entrySet().forEach((entry)->{
			if(entry.getKey().endsWith(attr))return;
			Util.setJson(root,entry.getKey(),entry.getValue());
		});
		return root;
	}

	public static Json mapToUpdatesDelta(NavigableMap<String, Json> map)  {
		Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> deltaMap = mapToDeltaMap(map);
		return generateDelta(deltaMap,UPDATES);
	}
	
	public static Json mapToPutDelta(NavigableMap<String, Json> map)  {
		Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> deltaMap = mapToDeltaMap(map);
		return generateDelta(deltaMap,PUT);
	}
	
	public static Json mapToConfigDelta(NavigableMap<String, Json> map)  {
		Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> deltaMap = mapToDeltaMap(map);
		return generateDelta(deltaMap,CONFIG);
	}
	
	public static Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> mapToDeltaMap(NavigableMap<String, Json> map)  {
		
		//ClientMessage msgReceived = null;
		Map<String, Map<String, Map<String, Map<String,List<Entry<String,Json>>>>>> msgs = new HashMap<>();
		if(map==null)return msgs;
		for (Entry<String,Json>entry: map.entrySet()) {
			Json eValue = entry.getValue();
			String eKey = entry.getKey();
			
			if(eKey.startsWith(sources))continue;
			if(eKey.endsWith(attr))continue;
			
			if (logger.isDebugEnabled())
				logger.debug("message = {} : {}",eKey, eValue);
			String ctx = Util.getContext(eKey);
			Map<String, Map<String, Map<String,List<Entry<String,Json>>>>> ctxMap = msgs.get(ctx);
			if (ctxMap == null) {
				ctxMap = new HashMap<>();
				msgs.put(ctx, ctxMap);
			}
			
			String tsVal = (eValue.isObject() && eValue.has(timestamp))? eValue.at(timestamp).asString():"none";
			if (logger.isDebugEnabled())
				logger.debug("$timestamp: {}",tsVal);
			Map<String, Map<String,List<Entry<String,Json>>>> tsMap = ctxMap.get(tsVal);
			if (tsMap == null) {
				tsMap = new HashMap<>();
				ctxMap.put(tsVal, tsMap);
			}
			
			String srVal = (eValue.isObject() && eValue.has(sourceRef))? eValue.at(sourceRef).asString():"none";
			if (logger.isDebugEnabled())
				logger.debug("$source: {}",srVal);
			Map<String,List<Entry<String,Json>>> srcMap = tsMap.get(srVal);
			if (srcMap == null) {
				srcMap = new HashMap<>();
				tsMap.put(srVal, srcMap);
			}
			eKey = StringUtils.substringAfter(eKey,ctx+dot);
			eKey = StringUtils.substringBefore(eKey,dot + values + dot);
	
			List<Entry<String,Json>> list = srcMap.get(eKey);
			if (list == null) {
				list = new ArrayList<>();
				srcMap.put(eKey, list);
			}
			if (logger.isDebugEnabled())
				logger.debug("Add entry: {}:{}",eKey,entry);
			list.add(entry);
		}
		return msgs;
	}
	
	/**
	 * Input is a list of message wrapped in a stack of hashMaps, eg Key=context
	 * Key=timestamp key=source List(messages) The method iterates through and
	 * creates the deltas as a Json array, one Json delta per context.
	 * 
	 * @param msgs
	 * @param deltatype 
	 * @return
	 */
	public static Json generateDelta(Map<String, Map<String, Map<String, Map<String,List<Entry<String,Json>>>>>> msgs, String deltatype) {
		logger.debug("Delta map: {}",msgs);
		Json delta = Json.object();
	
		if (msgs==null || msgs.size() == 0)
			return delta;

		Json updatesArray = Json.array();
		delta.set(deltatype, updatesArray);

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
					valObj.set(values, valuesArray);
					valObj.set(timestamp, ts);
					valObj.set(sourceRef, src);

					// now the values
					for (Entry<String,List<Entry<String,Json>>> msg : msgs.get(ctx).get(ts).get(src).entrySet()) {
						logger.debug("item: {}", msg.getKey());
						List<Entry<String,Json>> list = msg.getValue();
						
						for( Entry<String,Json> v :list){
							String vKey = v.getKey();
							Json vJson = v.getValue();
							logger.debug("Key: {}, value: {}", vKey,vJson);
							
							vKey = StringUtils.substringAfter(vKey,ctx+dot);
							vKey = StringUtils.substringBefore(vKey,dot + values + dot);
							
							Json val = Json.object(PATH, vKey);
							
							if (vJson.has(timestamp)){
								vJson.delAt(timestamp);
							}
							if (vJson.has(sourceRef)){
								vJson.delAt(sourceRef);
							}
							if(vJson.has(value)){
								val.set(value, vJson.at(value));
							}else{
								val.set(value, vJson);
							}
							logger.debug("Added Key: {}, value: {}", vKey,vJson);
							valuesArray.add(val);
						}
					}
				}
				// add context
			}
			delta.set(CONTEXT, ctx);
		}

		return delta;
	}

}
