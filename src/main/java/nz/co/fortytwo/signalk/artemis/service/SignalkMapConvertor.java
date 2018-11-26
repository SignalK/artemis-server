package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.GET;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UNKNOWN;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.attr;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.meta;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sentence;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.values;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.version;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class SignalkMapConvertor {

	private static Logger logger = LogManager.getLogger(SignalkMapConvertor.class);


	public static NavigableMap<String, Json> parseFull(Json json, NavigableMap<String, Json> map, String prefix) {
		if (map == null)
			map = new ConcurrentSkipListMap<>();
		if (json == null || json.isNull())
			return map;

		for (Entry<String, Json> entry : json.asJsonMap().entrySet()) {

			String key = entry.getKey();
			Json val = entry.getValue();

			if (logger.isDebugEnabled())
				logger.debug("Recurse {} = {}", () -> key, () -> val);
			//primitive we write out
			if (val.isPrimitive() || val.isNull() || val.isArray()) {
				map.put(prefix + key, val);
				continue;
			}
			//value object we save in .values.sourceref.
			if (val.has(value)) {
				String srcRef = null;
				Json tmpVal = Json.object(value,val.at(value));
				if (val.has(sourceRef)) {
					srcRef = val.at(sourceRef).asString();
					tmpVal.set(sourceRef, srcRef);
				} else {
					srcRef = UNKNOWN;
					tmpVal.set(sourceRef, srcRef);
				}

				if (val.has(timestamp)) {
					if (logger.isDebugEnabled())
						logger.debug("put timestamp: {}:{}", key, val);
					tmpVal.set(timestamp, val.at(timestamp).asString());
				} else {
					tmpVal.set(timestamp, Util.getIsoTimeString());
				}
				if(prefix.contains(dot+values)) {
					map.put(prefix + key , val);
					if (logger.isDebugEnabled())
						logger.debug("put: {}:{}", prefix + key, val);
				}else {
					map.put(prefix + key + dot + values + dot + srcRef, tmpVal);
					if (logger.isDebugEnabled())
						logger.debug("put: {}:{}", prefix + key + dot + values + dot + srcRef, tmpVal);
				}
				//sourceRef is wrong for meta
				if(val.has(meta)) parseFull(val.at(meta), map, prefix + key + dot + values+dot+ srcRef+dot+meta+dot);
				if(val.has(values)) parseFull(val.at(values), map, prefix + key + dot + values+dot);
				continue;
			}
			
			parseFull(val, map, prefix + key + ".");

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
	public static NavigableMap<String, Json> parseDelta(Json node, NavigableMap<String, Json> temp) {

		if (temp == null)
			temp = new ConcurrentSkipListMap<>();
		if (node == null || node.isNull())
			return temp;
		// avoid full signalk syntax
		if (node.has(vessels))
			return null;

		if (Util.isDelta(node) && !Util.isSubscribe(node) && !node.has(GET)) {

			if (logger.isDebugEnabled())
				logger.debug("processing delta  {}", node);
			// process it

			// go to context
			String ctx = node.at(CONTEXT).asString();
			ctx = Util.fixSelfKey(ctx);
			ctx = StringUtils.removeEnd(ctx, ".");
			if (logger.isDebugEnabled())
				logger.debug("ctx: {}", node);

			if (node.has(UPDATES)) {
				for (Json update : node.at(UPDATES).asJsonList()) {
					parseUpdate(temp, update, ctx);
				}
			}
			if (node.has(PUT)) {
				for (Json put : node.at(PUT).asJsonList()) {
					parsePut(temp, put, ctx);
				}
			}
			if (node.has(CONFIG)) {
				for (Json update : node.at(CONFIG).asJsonList()) {
					parseUpdate(temp, update, ctx);
				}
			}

			if (logger.isDebugEnabled())
				logger.debug("processed delta  {}", temp);
			return temp;
		}
		return null;

	}

	protected static void parseUpdate(NavigableMap<String, Json> temp, Json update, String ctx) {

		// grab values and add
		Json array = update.at(values);
		for (Json val : array.asJsonList()) {
			if (val == null || val.isNull() || !val.has(PATH))
				continue;

			Json e = val.dup();

			String key = dot + e.at(PATH).asString();
			if (key.equals(dot))
				key = "";
			// e.delAt(PATH);
			// temp.put(ctx+"."+key, e.at(value).getValue());
			String srcRef = SignalKConstants.UNKNOWN;
			if (update.has(sourceRef)) {
				srcRef = update.at(sourceRef).asString();
				e.set(sourceRef, srcRef);
			}

			if (update.has(timestamp)) {
				if (logger.isDebugEnabled())
					logger.debug("put timestamp: {}:{}", ctx + key, e);
				e.set(timestamp, update.at(timestamp).asString());
			} else {
				e.set(timestamp, Util.getIsoTimeString());
			}
			e.delAt(PATH);
			if (e.has(value)) {
				if (logger.isDebugEnabled())
					logger.debug("map.put: {}:{}", ctx + key, e);
				temp.put(ctx + key + dot + values + dot + srcRef, e);
			}
		}

	}

	protected static void parsePut(NavigableMap<String, Json> temp, Json put, String ctx) {

		if (put == null || put.isNull() || !put.has(PATH))
			return;

		Json e = put.dup();

		String key = dot + e.at(PATH).asString();
		if (key.equals(dot))
			key = "";

		if (!e.has(sourceRef)) {
			e.set(sourceRef, UNKNOWN);
		}
		if (!e.has(timestamp)) {
			e.set(timestamp, Util.getIsoTimeString());
		}
		e.delAt(PATH);
		if (e.has(value)) {
			if (logger.isDebugEnabled())
				logger.debug("put: {}:{}", ctx + key, e);
			temp.put(ctx + key + dot + values + dot + e.at(sourceRef).asString(), e);
		}

	}

	public static Json mapToFull(NavigableMap<String, Json> map) throws Exception {

		Json root = Json.object();
		if (map == null)
			return root;

		//NavigableMap<String, Json> allowedMap = new ConcurrentSkipListMap<>();
		
		root.set(self_str, Json.make(Config.getConfigProperty(ConfigConstants.UUID)));
		root.set(version, Json.make(Config.getConfigProperty(ConfigConstants.VERSION)));
		for (Entry<String, Json> entry : map.entrySet()) {
			if (entry.getKey().endsWith(attr))
				continue;

			Json val = entry.getValue();
			
			String path = StringUtils.substringBefore(entry.getKey(), dot + values + dot);
			
			if(!entry.getKey().contains(meta)) {
				String ref = StringUtils.substringAfter(entry.getKey(), dot + values + dot);
				if(ref.contains(dot+meta+dot))ref=StringUtils.substringBefore(ref, dot+meta+dot);
				if(StringUtils.isNotBlank(ref)){
					if(!StringUtils.startsWith(path, sources)) { 
						if (logger.isDebugEnabled())
							logger.debug("Add source: {} to value: {}", ref, val);
						val.set(sourceRef, ref);
					}
				}
			}else {
				path= path + dot +meta+ dot+StringUtils.substringAfter(entry.getKey(), dot+meta+dot);
			}
			if (logger.isDebugEnabled())
				logger.debug("Add key: {}, value: {}", entry.getKey(), val.toString());
			if (val.isObject() && val.has(sentence)) {
				Util.setJson(root, path + dot + sentence, val.at(sentence).dup());
				continue;
			}
			Util.setJson(root, path, val.dup());
		}
		return root;
	}

	public static Json mapToUpdatesDelta(NavigableMap<String, Json> map) {
		
		Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> deltaMap = mapToDeltaMap(map);
		try {
			return generateDelta(deltaMap, UPDATES);
		}finally {
			map.clear();
			deltaMap.clear();
		}
	}

	public static Json mapToPutDelta(NavigableMap<String, Json> map) {
		Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> deltaMap = mapToDeltaMap(map);
		return generateDelta(deltaMap, PUT);
	}

	public static Json mapToConfigDelta(NavigableMap<String, Json> map) {
		Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> deltaMap = mapToDeltaMap(map);
		return generateDelta(deltaMap, CONFIG);
	}

	public static Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> mapToDeltaMap(
			NavigableMap<String, Json> map) {

		// ClientMessage msgReceived = null;
		Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> msgs = new HashMap<>();
		if (map == null)
			return msgs;
		for (Entry<String, Json> entry : map.entrySet()) {
			Json eValue = entry.getValue();
			String eKey = entry.getKey();

			if (eKey.startsWith(sources))
				continue;
			if (eKey.endsWith(attr))
				continue;

			if (logger.isDebugEnabled())
				logger.debug("message = {} : {}", eKey, eValue);
			String ctx = Util.getContext(eKey);
			Map<String, Map<String, Map<String, List<Entry<String, Json>>>>> ctxMap = msgs.get(ctx);
			if (ctxMap == null) {
				ctxMap = new HashMap<>();
				msgs.put(ctx, ctxMap);
			}

			String tsVal = (eValue.isObject() && eValue.has(timestamp)) ? eValue.at(timestamp).asString() : "none";
			if (logger.isDebugEnabled())
				logger.debug("$timestamp: {}", tsVal);
			Map<String, Map<String, List<Entry<String, Json>>>> tsMap = ctxMap.get(tsVal);
			if (tsMap == null) {
				tsMap = new HashMap<>();
				ctxMap.put(tsVal, tsMap);
			}

			String srVal = (eValue.isObject() && eValue.has(sourceRef)) ? eValue.at(sourceRef).asString() : "none";
			if (logger.isDebugEnabled())
				logger.debug("$source: {}", srVal);
			Map<String, List<Entry<String, Json>>> srcMap = tsMap.get(srVal);
			if (srcMap == null) {
				srcMap = new HashMap<>();
				tsMap.put(srVal, srcMap);
			}
			eKey = StringUtils.substringAfter(eKey, ctx + dot);
			eKey = StringUtils.substringBefore(eKey, dot + values + dot);

			List<Entry<String, Json>> list = srcMap.get(eKey);
			if (list == null) {
				list = new ArrayList<>();
				srcMap.put(eKey, list);
			}
			if (logger.isDebugEnabled())
				logger.debug("Add entry: {}:{}", eKey, entry);
			list.add(entry);
		}
		map=null;
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
	public static Json generateDelta(Map<String, Map<String, Map<String, Map<String, List<Entry<String, Json>>>>>> msgs,
			String deltatype) {
		if (logger.isDebugEnabled())
			logger.debug("Delta map: {}", msgs);
		Json delta = Json.object();
		
		if (msgs == null || msgs.size() == 0)
			return delta;
		try {
			Json updatesArray = Json.array();
			delta.set(deltatype, updatesArray);
	
			for (String ctx : msgs.keySet()) {
	
				for (String ts : msgs.get(ctx).keySet()) {
					if (logger.isDebugEnabled())
						logger.debug("timestamp: {}", ts);
					for (String src : msgs.get(ctx).get(ts).keySet()) {
						// new values object
						if (logger.isDebugEnabled())
							logger.debug("sourceRef: {}", src);
						// make wrapper object
						Json valObj = Json.object();
						updatesArray.add(valObj);
	
						Json valuesArray = Json.array();
						valObj.set(values, valuesArray);
						valObj.set(timestamp, ts);
						valObj.set(sourceRef, src);
	
						// now the values
						for (Entry<String, List<Entry<String, Json>>> msg : msgs.get(ctx).get(ts).get(src).entrySet()) {
							if (logger.isDebugEnabled())
								logger.debug("item: {}", msg.getKey());
							List<Entry<String, Json>> list = msg.getValue();
	
							for (Entry<String, Json> v : list) {
								String vKey = v.getKey();
								Json vJson = v.getValue();
								if (logger.isDebugEnabled())
									logger.debug("Key: {}, value: {}", vKey, vJson);
	
								vKey = StringUtils.substringAfter(vKey, ctx + dot);
								vKey = StringUtils.substringBefore(vKey, dot + values + dot);
	
								Json val = Json.object(PATH, vKey);
	
								if (vJson != null && vJson.isObject()) {
									if (vJson.has(timestamp)) {
										vJson.delAt(timestamp);
									}
									if (vJson.has(sourceRef)) {
										vJson.delAt(sourceRef);
									}
									if (vJson.has(value)) {
										val.set(value, vJson.at(value));
									}
								} else {
									val.set(value, vJson);
								}
								if (logger.isDebugEnabled())
									logger.debug("Added Key: {}, value: {}", vKey, vJson);
								valuesArray.add(val);
							}
						}
					}
					// add context
				}
				delta.set(CONTEXT, ctx);
			}
			msgs=null;
			return delta.dup();
		}finally {
			delta.clear(true);
		}
	}

}
