package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.meta;
import static nz.co.fortytwo.signalk.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sentence;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.util.SignalKConstants.values;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Series;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class InfluxDbService {

	private static Logger logger = LogManager.getLogger(InfluxDbService.class);
	private InfluxDB influxDB;
	private String dbName = "signalk";
	private boolean DEBUG = false;

	public InfluxDbService() {
		if (logger.isDebugEnabled()) {
			DEBUG = true;
		}
		setUpInfluxDb();
	}

	public void setUpInfluxDb() {
		influxDB = InfluxDBFactory.connect("http://localhost:8086", "admin", "admin");
		if (!influxDB.databaseExists(dbName))
			influxDB.createDatabase(dbName);
		influxDB.setDatabase(dbName);
		// String rpName = "aRetentionPolicy";
		// influxDB.createRetentionPolicy(rpName, dbName, "30d", "30m", 2,
		// true);
		// influxDB.setRetentionPolicy(rpName);
		// influxDB.enableBatch(BatchOptions.DEFAULTS);
		influxDB.enableBatch(BatchOptions.DEFAULTS.exceptionHandler((failedPoints, throwable) -> {
			logger.error(throwable);
		}));
	}

	public void closeInfluxDb() {
		influxDB.close();
	}

	public void recurseJsonFull(Json json, NavigableMap<String, Json> map, String prefix) {
		for (Entry<String, Json> entry : json.asJsonMap().entrySet()) {
			if (DEBUG)
				logger.debug(entry.getKey() + "=" + entry.getValue());
			if (entry.getValue().isPrimitive() || entry.getValue().isNull() || entry.getValue().isArray()) {
				map.put(prefix + entry.getKey(), entry.getValue());
				continue;
			} else if (entry.getValue().has(value)) {
//				if (entry.getValue().has(values)) {
//					recurseJsonFull(entry.getValue().at(values), map, prefix + entry.getKey() + ".values.");
//					entry.getValue().at(values).delAt(values);
//				}
				map.put(prefix + entry.getKey(), entry.getValue());
				
			} else {
				recurseJsonFull(entry.getValue(), map, prefix + entry.getKey() + ".");
			}

		}

	}

	/**
	 * Convert Delta JSON to map. Returns null if the json is not an update,
	 * otherwise return a map
	 * 
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public NavigableMap<String, Json> parseDelta(Json node, NavigableMap<String, Json> temp) throws Exception {
		// avoid full signalk syntax
		if (node.has(vessels))
			return null;

		if (node.has(CONTEXT) && (node.has(UPDATES) || node.has(PUT))) {
			if (DEBUG)
				logger.debug("processing delta  " + node);
			// process it

			// go to context
			String ctx = node.at(CONTEXT).asString();
			ctx = Util.fixSelfKey(ctx);
			// Json pathNode = temp.addNode(path);
			Json updates = node.at(UPDATES);
			if (updates == null)
				updates = node.at(PUT);
			if (updates == null)
				return temp;

			for (Json update : updates.asJsonList()) {
				parseUpdate(temp, update, ctx);

			}

			if (DEBUG)
				logger.debug("processed delta  " + temp);
			return temp;
		}
		return null;

	}

	protected void parseUpdate(NavigableMap<String, Json> temp, Json update, String ctx) throws Exception {

		// grab values and add
		Json array = update.at(values);
		for (Json e : array.asJsonList()) {
			if (e == null || e.isNull() || !e.has(PATH))
				continue;
			String key = e.at(PATH).asString();
			// temp.put(ctx+"."+key, e.at(value).getValue());
			if (e.has(value)) {
				temp.put(ctx + dot + key, e);
			}

			if (update.has(source)) {
				// TODO:generate a proper src ref.
				e.set(source, update.at(source).dup());
			}

			if (update.has(timestamp)) {
				String ts = update.at(timestamp).asString();
				// TODO: should validate the timestamp
				e.set(timestamp, ts);
			}
		}

	}

	public NavigableMap<String, Json> loadConfig() {
		Query query = new Query("select * from config group by skey order by time desc limit 1", dbName);
		QueryResult result = influxDB.query(query);
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		if (result == null || result.getResults() == null)
			return map;
		result.getResults().forEach((r) -> {
			if (r.getSeries() == null)
				return;
			r.getSeries().forEach((s) -> {
				if (DEBUG)
					logger.debug(s);
				if (s == null)
					return;
				String key = s.getName() + dot + s.getTags().get("skey");

				Object obj = getValue("longValue", s, 0);
				if (obj != null)
					map.put(key, Json.make(Math.round((Double) obj)));

				obj = getValue("doubleValue", s, 0);
				if (obj != null)
					map.put(key, Json.make(obj));

				obj = getValue("strValue", s, 0);
				if (obj != null) {
					if (obj.equals("true")) {
						map.put(key, Json.make(true));
					} else if (obj.equals("false")) {
						map.put(key, Json.make(false));
					} else if (obj.toString().startsWith("[") && obj.toString().endsWith("]")) {
						map.put(key, Json.read(obj.toString()));
					} else {
						map.put(key, Json.make(obj));
					}
				}

			});
		});
		return map;
	}

	public NavigableMap<String, Json> loadData(NavigableMap<String, Json> map, String queryStr, String db){
		Query query = new Query(queryStr, db);
		QueryResult result = influxDB.query(query);
		//NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		if(DEBUG)logger.debug(result);
		if(result==null || result.getResults()==null)return map;
		result.getResults().forEach((r)-> {
			if(DEBUG)logger.debug(r);
			if(r==null)return;
			r.getSeries().forEach(
				(s)->{
					if(DEBUG)logger.debug(s);
					if(s==null)return;
					String key = s.getName()+dot+s.getTags().get("uuid")+dot+s.getTags().get("skey");
					Json val = getJsonValue(s,0);
					boolean processed = false;
					//add timestamp and sourceRef
					
					
					if(key.endsWith(".sentence")){
							
						String senKey = StringUtils.substringBeforeLast(key,".");
						
						//make parent Json
						Json parent = map.get(senKey);
						if(parent==null){
							parent = Json.object();
							map.put(senKey,parent);
						}
						parent.set(sentence,val);
						processed=true;
						
					}
					if(key.contains(".meta.")){
						//add meta to parent of value
						String parentKey = StringUtils.substringBeforeLast(key,".meta.");
						String metaKey = StringUtils.substringAfterLast(key,".meta.");
						
						//make parent Json
						Json parent = map.get(parentKey);
						if(parent==null){
							parent = Json.object();
							map.put(parentKey,parent);
						}
						
						//add attributes
						addAtPath(parent,"meta."+metaKey, val);
						processed=true;
					}
					if(key.contains(".values.")){
						//add meta to parent of value
						String parentKey = StringUtils.substringBeforeLast(key,".values.");
						String metaKey = StringUtils.substringAfterLast(key,".values.");
						String attr = StringUtils.substringAfterLast(metaKey,".value.");
						metaKey=StringUtils.substringBeforeLast(metaKey,".");
						//make parent Json
						Json parent = map.get(parentKey);
						if(parent==null){
							parent = Json.object();
							map.put(parentKey,parent);
						}
						Json valuesJson = parent.at(values);
						if(valuesJson==null){
							valuesJson = Json.object();
							parent.set(values,valuesJson);
						}
						Json attrJson = valuesJson.at(metaKey);
						if(attrJson==null){
							attrJson = Json.object();
							valuesJson.set(metaKey,attrJson);
						}
						
						//add attributes
						extractValue(attrJson,s,attr,val,null);
						processed=true;
					}
					if(!processed && (key.endsWith(".value")||key.contains(".value."))){
						String attr = StringUtils.substringAfterLast(key,".value.");
					
						key = StringUtils.substringBeforeLast(key,".value");
						
						//make parent Json
						Json parent = map.get(key);
						if(parent==null)parent = Json.object();
						
						extractValue(parent,s, attr, val);
						
						map.put(key,parent);
						processed=true;
					}
					if(!processed) map.put(key,val);
					
				});
			});
		return map;
	}

	public NavigableMap<String, Json> loadSources(NavigableMap<String, Json> map, String queryStr, String db) {
		Query query = new Query(queryStr, db);
		QueryResult result = influxDB.query(query);
		// NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		if (DEBUG)
			logger.debug(result);
		if (result == null || result.getResults() == null)
			return map;
		result.getResults().forEach((r) -> {
			if (DEBUG)
				logger.debug(r);
			if (r.getSeries() == null)
				return;
			r.getSeries().forEach((s) -> {
				if (DEBUG)
					logger.debug(s);
				if (s == null)
					return;
				String key = s.getName() + dot + s.getTags().get("skey");

				Object obj = getValue("longValue", s, 0);
				if (obj != null)
					map.put(key, Json.make(Math.round((Double) obj)));

				obj = getValue("doubleValue", s, 0);
				if (obj != null)
					map.put(key, Json.make(obj));

				obj = getValue("strValue", s, 0);
				if (obj != null) {
					if (obj.equals("true")) {
						map.put(key, Json.make(true));
					} else if (obj.equals("false")) {
						map.put(key, Json.make(false));
					} else if (obj.toString().startsWith("[") && obj.toString().endsWith("]")) {
						map.put(key, Json.read(obj.toString()));
					} else {
						map.put(key, Json.make(obj));
					}
				}

			});
		});
		return map;
	}

	private Object getValue(String field, Series s, int row) {
		int i = s.getColumns().indexOf(field);
		if (i < 0)
			return null;
		return (s.getValues().get(row)).get(i);
	}

	private Json getJsonValue(Series s, int row) {
		Object obj = getValue("longValue", s, 0);
		if (obj != null)
			return Json.make(Math.round((Double) obj));

		obj = getValue("doubleValue", s, 0);
		if (obj != null)
			return Json.make(obj);

		obj = getValue("strValue", s, 0);
		if (obj != null) {
			if (obj.equals("true")) {
				return Json.make(true);
			}
			if (obj.equals("false")) {
				return Json.make(false);
			}
			if (obj.toString().startsWith("[") && obj.toString().endsWith("]")) {
				return Json.read(obj.toString());
			}
			return Json.make(obj);

		}
		return Json.nil();

	}

	public void save(NavigableMap<String, Json> map) {
		map.forEach((k, v) -> save(k, v));
		influxDB.flush();
	}

	public void save(String k, Json v) {
		String srcRef = (v.isObject() && v.has(sourceRef) ? v.at(sourceRef).asString() : "self");
		long tStamp = (v.isObject() && v.has(timestamp) ? Util.getMillisFromIsoTime(v.at(timestamp).asString())
				: System.currentTimeMillis());

		if (v.isPrimitive()) {
			if (DEBUG)
				logger.debug("Save primitive: " + k + "=" + v.toString());
			saveValue(k, v, srcRef, tStamp);
			return;
		}
		if (v.isNull()) {
			if (DEBUG)
				logger.debug("Save null: " + k + "=" + v.toString());
			saveData(k, srcRef, tStamp, (String) null);
			saveData(k, srcRef, tStamp, (Double) null);
			saveData(k, srcRef, tStamp, (Long) null);
			return;
		}
		if (v.isArray()) {
			if (DEBUG)
				logger.debug("Save array: " + k + "=" + v.toString());
			saveData(k, srcRef, tStamp, v.toString());
			return;
		}
		if (v.has(sentence)) {
			saveData(k + dot + sentence, srcRef, tStamp, v.at(sentence).asString());
		}
		if (v.has(meta)) {
			for (Entry<String, Json> i : v.at(meta).asJsonMap().entrySet()) {
				if (DEBUG)
					logger.debug("Save meta: " + i.getKey() + "=" + i.getValue());
				saveValue(k + dot + meta + dot + i.getKey(), i.getValue(), srcRef, tStamp);
			}
		}
		
		if (v.has(values)) {
			for (Entry<String, Json> i : v.at(values).asJsonMap().entrySet()) {
				if (DEBUG)
					logger.debug("Save values: " + i.getKey() + "=" + i.getValue());
				save(k + dot + values + dot + i.getKey(),i.getValue());
			}
		}

		if (v.at(value).isObject()) {
			for (Entry<String, Json> i : v.at(value).asJsonMap().entrySet()) {
				if (DEBUG)
					logger.debug("Save value object: " + i.getKey() + "=" + i.getValue());
				saveValue(k + dot + value + dot + i.getKey(), i.getValue(), srcRef, tStamp);
			}
			return;
		}

		if (DEBUG)
			logger.debug("Save value: " + k + "=" + v.toString());
		saveValue(k + dot + value, v.at(value), srcRef, tStamp);

		return;
	}

	private void addAtPath(Json parent, String path, Json val) {
		String[] pathArray = StringUtils.split(path, ".");
		Json node = parent;
		for (int x = 0; x < pathArray.length; x++) {
			// add last
			if (x == (pathArray.length - 1)) {
				if (DEBUG)
					logger.debug("finish:" + pathArray[x]);
				node.set(pathArray[x], val);
				break;
			}
			// get next node
			Json next = node.at(pathArray[x]);
			if (next == null) {
				next = Json.object();
				node.set(pathArray[x], next);
				if (DEBUG)
					logger.debug("add:" + pathArray[x]);
			}
			node = next;
		}

	}

	private void extractValue(Json parent, Series s, String attr, Json val) {
		Object sr = getValue("sourceRef", s, 0);
		extractValue(parent,s,attr,val,sr);
	}
	
	private void extractValue(Json parent, Series s, String attr, Json val, Object srcref) {
		Object ts = getValue("time", s, 0);
		if (ts != null) {
			// map.put(StringUtils.substringBeforeLast(key,".value")+dot+timestamp,Json.make(ts.toString()));
			parent.set(timestamp, Json.make(ts));
		}
		if (srcref != null) {
			// map.put(StringUtils.substringBeforeLast(key,".value")+dot+sourceRef,Json.make(sr.toString()));
			parent.set(sourceRef, Json.make(srcref));
		}
		
		// check if its an object value
		if (StringUtils.isNotBlank(attr)) {
			Json valJson = parent.at(value);
			if (valJson == null) {
				valJson = Json.object();
				parent.set(value, valJson);
			}
			valJson.set(attr, val);
		} else {
			parent.set(value, val);
		}

	}

	private void saveValue(String k, Json v, String srcRef, long tStamp) {
		if (v.isNumber()) {
			if (DEBUG)
				logger.debug("Save Number: " + k + "=" + v.getValue().getClass());
			if (v.getValue() instanceof Double) {
				saveData(k, srcRef, tStamp, (Double) v.getValue());
				return;
			}
			if (v.getValue() instanceof Float) {
				saveData(k, srcRef, tStamp, (Double) v.getValue());
				return;
			}
			if (v.getValue() instanceof BigDecimal) {
				saveData(k, srcRef, tStamp, ((BigDecimal) v.getValue()).doubleValue());
				return;
			}
			if (v.getValue() instanceof Long) {
				saveData(k, srcRef, tStamp, (Long) v.getValue());
				return;
			}
			if (v.getValue() instanceof Integer) {
				saveData(k, srcRef, tStamp, (Long) v.getValue());
				return;
			}
			return;
		}
		if (v.isArray()) {
			saveData(k, srcRef, tStamp, v.toString());
			return;
		}
		saveData(k, srcRef, tStamp, v.asString());
	}

	protected void saveData(String key, String sourceRef, long millis, Double value) {
		if (DEBUG)
			logger.debug("save Double:" + key);
		String[] path = StringUtils.split(key, '.');
		switch (path[0]) {
		case vessels:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", sourceRef)
					.tag("uuid", path[1]).tag("skey", String.join(".", ArrayUtils.subarray(path, 2, path.length)))
					.addField("doubleValue", value).build());
			break;
		case resources:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", sourceRef)
					.tag("uuid", path[1]).tag("skey", String.join(".", ArrayUtils.subarray(path, 1, path.length)))
					.addField("doubleValue", value).build());
			break;
		case sources:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", path[1])
					.tag("skey", String.join(".", ArrayUtils.subarray(path, 1, path.length)))
					.addField("doubleValue", value).build());
			break;
		case CONFIG:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", sourceRef)
					.tag("uuid", path[1]).tag("skey", String.join(".", ArrayUtils.subarray(path, 1, path.length)))
					.addField("doubleValue", value).build());
			break;
		default:
			break;
		}
	}

	protected void saveData(String key, String sourceRef, long millis, Long value) {
		if (DEBUG)
			logger.debug("save long:" + key);
		String[] path = StringUtils.split(key, '.');
		switch (path[0]) {
		case vessels:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", sourceRef)
					.tag("uuid", path[1]).tag("skey", String.join(".", ArrayUtils.subarray(path, 2, path.length)))
					.addField("longValue", value).build());
			break;
		case resources:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", sourceRef)
					.tag("uuid", path[1]).tag("skey", String.join(".", ArrayUtils.subarray(path, 1, path.length)))
					.addField("longValue", value).build());
			break;
		case sources:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", path[1])
					.tag("skey", String.join(".", ArrayUtils.subarray(path, 1, path.length)))
					.addField("longValue", value).build());
			break;
		case CONFIG:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", sourceRef)
					.tag("uuid", path[1]).tag("skey", String.join(".", ArrayUtils.subarray(path, 1, path.length)))
					.addField("longValue", value).build());
			break;

		default:
			break;
		}
	}

	protected void saveData(String key, String sourceRef, long millis, String value) {
		if (DEBUG)
			logger.debug("save String:" + key);
		String[] path = StringUtils.split(key, '.');
		switch (path[0]) {
		case vessels:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", sourceRef)
					.tag("uuid", path[1]).tag("skey", String.join(".", ArrayUtils.subarray(path, 2, path.length)))
					.addField("strValue", value).build());
			break;
		case resources:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", sourceRef)
					.tag("uuid", path[1]).tag("skey", String.join(".", ArrayUtils.subarray(path, 1, path.length)))
					.addField("strValue", value).build());
			break;
		case sources:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", path[1])
					.tag("skey", String.join(".", ArrayUtils.subarray(path, 1, path.length)))
					.addField("strValue", value).build());
			break;
		case CONFIG:
			influxDB.write(Point.measurement(path[0]).time(millis, TimeUnit.MILLISECONDS).tag("sourceRef", sourceRef)
					.tag("uuid", path[1]).tag("skey", String.join(".", ArrayUtils.subarray(path, 1, path.length)))
					.addField("strValue", value).build());
			break;
		default:
			break;
		}
	}

	public void close() {
		influxDB.close();
	}
}
