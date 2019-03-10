package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.aircraft;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.aton;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.meta;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sar;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sentence;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.skey;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.values;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.version;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.LogLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Series;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class InfluxDbService implements TDBService {

	private static final String STR_VALUE = "strValue";
	private static final String LONG_VALUE = "longValue";
	private static final String DOUBLE_VALUE = "doubleValue";
	private static final String NULL_VALUE = "nullValue";
	private static Logger logger = LogManager.getLogger(InfluxDbService.class);
	private InfluxDB influxDB;
	private static String dbName = "signalk";
	
	public static final String PRIMARY_VALUE = "primary";
	public static final ConcurrentSkipListMap<String, String> primaryMap = new ConcurrentSkipListMap<>();
	public static boolean allowWrite=false;
	
	public InfluxDbService() {
		setUpTDb();
	}

	public InfluxDbService(String dbName) {
		InfluxDbService.dbName=dbName;
		setUpTDb();
	}

	/* (non-Javadoc)
	 * @see nz.co.fortytwo.signalk.artemis.service.TDBService#setUpInfluxDb()
	 */
	@Override
	public void setUpTDb() {
		//try 7 times
		int c=0;
		while(c<8) {
			c++;
			try {
				influxDB = InfluxDBFactory.connect("http://localhost:8086", "admin", "admin");
				c=99;
			}catch(InfluxDBIOException e) {
				logger.error(e, e);
				try {
					wait(5000);
				} catch (InterruptedException e1) {
					logger.error(e1,e1);
				}
			}
		}
		if (!influxDB.databaseExists(dbName))
			influxDB.createDatabase(dbName);
		influxDB.setDatabase(dbName);
		influxDB.setLogLevel(LogLevel.BASIC);
//		influxDB.enableBatch(BatchOptions.DEFAULTS.exceptionHandler((failedPoints, throwable) -> {
//			logger.error("FAILED:"+failedPoints);
//			logger.error(throwable);
//		}));
		
		influxDB.enableBatch(10000, 250, TimeUnit.MILLISECONDS);
		influxDB.setRetentionPolicy("autogen");
		if(primaryMap.size()==0)loadPrimary();
		
	}

	@Override
	public void setWrite(boolean write) {
		logger.info("Set write: {}", write);
		allowWrite=write;
//		try {
//			Config.saveConfig();
//		} catch (IOException e) {
//			logger.error(e,e);
//		}
	}
	
	@Override
	public boolean getWrite() {
		return allowWrite;
	}
	
	/* (non-Javadoc)
	 * @see nz.co.fortytwo.signalk.artemis.service.TDBService#closeInfluxDb()
	 */
	@Override
	public void closeTDb() {
		influxDB.close();
	}
	
	@Override
	public NavigableMap<String, Json> loadResources(NavigableMap<String, Json> map, Map<String, String> query) {
		//"select * from "+table+" where uuid='"+uuid+"' AND skey=~/"+pattern+"/ group by skey,primary, uuid,sourceRef,owner,grp order by time desc limit 1"
		String queryStr="select * from resources "+getWhereString(query)+" group by skey, primary, uuid, sourceRef order by time desc limit 1";
		return loadResources(map, queryStr);
	}

	private String getWhereString(Map<String, String> query) {
		StringBuilder builder = new StringBuilder();
		if(query==null || query.size()==0)return "";
		String joiner=" where ";
		for(Entry<String, String> e: query.entrySet()){
			builder.append(joiner)
				.append(e.getKey())
				.append("=~/")
				.append(e.getValue())
				.append("/");
			joiner=" and ";
		}
		return builder.toString();
	}
	
	private String getWhereString(Map<String, String> query, String time) {
		return getWhereString(query, Util.getMillisFromIsoTime(time));
	}
	private String getWhereString(Map<String, String> query, long time) {
		StringBuilder builder = new StringBuilder();
		//1536567258000 = ms
		//1537401592000000000=ns
		builder.append(" where time < "+time+ "ms");
		if(query!=null || query.size()>0) {
			for(Entry<String, String> e: query.entrySet()){
				builder.append(" and ")
					.append(e.getKey())
					.append("=~/")
					.append(e.getValue())
					.append("/");
			}
		}
		return builder.toString();
	}

	@Override
	public NavigableMap<String, Json> loadConfig(NavigableMap<String, Json> map, Map<String, String> query) {
		String queryStr="select * from config "+getWhereString(query)+" group by skey order by time desc limit 1";
		return loadConfig(map, queryStr);
	}

	@Override
	public NavigableMap<String, Json> loadData(NavigableMap<String, Json> map, String table, Map<String, String> query) {
		String queryStr="select * from "+table+getWhereString(query)+" group by skey,primary, uuid,sourceRef order by time desc limit 1";
		return loadData(map, queryStr);
	}
	
	@Override
	public NavigableMap<String, Json> loadDataSnapshot(NavigableMap<String, Json> map, String table, Map<String, String> query, long time) {
		String queryStr="select * from "+table+getWhereString(query, time)+" group by skey,primary, uuid,sourceRef order by time desc limit 1";
		return loadData(map, queryStr);
	}
	@Override
	public NavigableMap<String, Json> loadDataSnapshot(NavigableMap<String, Json> map, String table, Map<String, String> query, String time) {
		String queryStr="select * from "+table+getWhereString(query, time)+" group by skey, primary, uuid, sourceRef order by time desc limit 1";
		return loadData(map, queryStr);
	}

	@Override
	public NavigableMap<String, Json> loadSources(NavigableMap<String, Json> map, Map<String, String> query) {
		String queryStr="select * from sources "+getWhereString(query)+" group by sourceRef, skey order by time desc limit 1";
		return loadSources(map, queryStr);
	}
	
	public NavigableMap<String, Json> loadResources(NavigableMap<String, Json> map, String queryStr) {
		if (logger.isDebugEnabled())logger.debug("Query: {}", queryStr);
		Query query = new Query(queryStr, dbName);
		QueryResult result = influxDB.query(query);
		
		if (result == null || result.getResults() == null)
			return map;
		result.getResults().forEach((r) -> {
			if (r == null ||r.getSeries()==null)
				return;
			r.getSeries().forEach((s) -> {
				if (logger.isDebugEnabled())logger.debug(s);
				if(s==null)return;
				
				Map<String, String> tagMap = s.getTags();
				String key = s.getName() + dot + s.getTags().get(skey);
				
				Json val = getJsonValue(s,0);
				if(key.contains(".values.")){
					//handle values
					if (logger.isDebugEnabled())logger.debug("values: {}",val);
					String parentKey = StringUtils.substringBeforeLast(key,".values.");
					String valKey = StringUtils.substringAfterLast(key,".values.");
					String subkey = StringUtils.substringAfterLast(valKey,".value.");
					
					//make parent Json
					Json parent = getParent(map,parentKey);
						
					//add attributes
					if (logger.isDebugEnabled())logger.debug("Primary value: {}",tagMap.get("primary"));
					boolean primary = Boolean.valueOf((String)tagMap.get("primary"));
					if(primary){
						extractPrimaryValue(parent,s,subkey,val, false);
					}else{
						valKey=StringUtils.substringBeforeLast(valKey,".");
						Json valuesJson = Util.getJson(parent,values );
						
						Json subJson = valuesJson.at(valKey);
						if(subJson==null){
							subJson = Json.object();
							valuesJson.set(valKey,subJson);
						}
						extractValue(subJson,s,subkey, val, false);
					}
					
					return;
				}
				if( (key.endsWith(".value")||key.contains(".value."))){
					if (logger.isDebugEnabled())logger.debug("value: {}",val);
					String subkey = StringUtils.substringAfterLast(key,".value.");
				
					key = StringUtils.substringBeforeLast(key,".value");
					
					//make parent Json
					Json parent = getParent(map,key);
					if (logger.isDebugEnabled())logger.debug("Primary value: {}",tagMap.get("primary"));
					boolean primary = Boolean.valueOf((String)tagMap.get("primary"));
					if(primary){
						extractPrimaryValue(parent,s,subkey,val, false);
					}else{
						extractValue(parent,s,subkey, val, false);
					}
					//extractValue(parent,s, subkey, val);
					
					map.put(key,parent);
					return;
				}
				
				map.put(key,val);
				
				
			});
		});
		return map;
	}

	public NavigableMap<String, Json> loadConfig(NavigableMap<String, Json> map, String queryStr) {
		Query query = new Query(queryStr, dbName);
		QueryResult result = influxDB.query(query);
		
		if (result == null || result.getResults() == null) {
			return map;
		}
		result.getResults().forEach((r) -> {
			if (r.getSeries() == null ||r.getSeries()==null)
				return;
			r.getSeries().forEach((s) -> {
				if (logger.isDebugEnabled())logger.debug(s);
				if(s==null)return;
				
				Map<String, String> tagMap = s.getTags();
				String key = s.getName() + dot + tagMap.get(skey);
				
				Json val = getJsonValue(s,0);
				
				map.put(key,val);
				
				
			});
		});
		return map;
	}

	
	public NavigableMap<String, Json> loadData(NavigableMap<String, Json> map, String queryStr){
		if (logger.isDebugEnabled())logger.debug("queryStr: {}",queryStr);
		Query query = new Query(queryStr, dbName);
		QueryResult result = influxDB.query(query);
		//NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		if (logger.isDebugEnabled())logger.debug(result);
		if(result==null || result.getResults()==null)return map;
		result.getResults().forEach((r)-> {
			if (logger.isDebugEnabled())logger.debug(r);
			if(r==null||r.getSeries()==null)return;
			r.getSeries().forEach(
				(s)->{
					if (logger.isDebugEnabled())logger.debug(s);
					if(s==null)return;
					
					Map<String, String> tagMap = s.getTags();
					String key = s.getName()+dot+tagMap.get("uuid")+dot+tagMap.get(skey);
					
					Json val = getJsonValue(s,0);
					
					//add timestamp and sourceRef
					if(key.endsWith(".sentence")){
						if (logger.isDebugEnabled())logger.debug("sentence: {}",val);
						//make parent Json
						String parentKey = StringUtils.substringBeforeLast(key,".");
						
						Json parent = getParent(map,parentKey);
				
						parent.set(sentence,val);
						
						return;
						
					}
					if(key.contains(".meta.")){
						//add meta to parent of value
						if (logger.isDebugEnabled())logger.debug("meta: {}",val);
						String parentKey = StringUtils.substringBeforeLast(key,".meta.");
						String metaKey = StringUtils.substringAfterLast(key,".meta.");
						
						//make parent Json
						Json parent = getParent(map,parentKey);
					
						//add attributes
						Util.setJson(parent,"meta."+metaKey, val);
						
						return;
					}
					if(key.contains(".values.")){
						//handle values
						if (logger.isDebugEnabled())logger.debug("key: {}, values: {}",key,val);
						String parentKey = StringUtils.substringBeforeLast(key,".values.");
						String valKey = StringUtils.substringAfterLast(key,".values.");
						String subkey = StringUtils.substringAfterLast(valKey,".value.");
						
						if (logger.isDebugEnabled())logger.debug("parentKey: {}, valKey: {}, subKey: {}",parentKey,valKey, subkey);
						//make parent Json
						Json parent = getParent(map,parentKey);
							
						//add attributes
						if (logger.isDebugEnabled())logger.debug("Primary value: {}",tagMap.get("primary"));
						boolean primary = Boolean.valueOf((String)tagMap.get("primary"));
						if(primary){
							extractPrimaryValue(parent,s,subkey,val,true);
						}else{
							
							valKey=StringUtils.substringBeforeLast(valKey,".");
							Json valuesJson = Util.getJson(parent,values );
							Json subJson = valuesJson.at(valKey);
							if(subJson==null){
								subJson = Json.object();
								valuesJson.set(valKey,subJson);
							}
							
							extractValue(subJson,s,subkey, val, true);
						}
						
						return;
					}
					if((key.endsWith(".value")||key.contains(".value."))){
						if (logger.isDebugEnabled())logger.debug("value: {}",val);
						String subkey = StringUtils.substringAfterLast(key,".value.");
					
						key = StringUtils.substringBeforeLast(key,".value");
						
						//make parent Json
						Json parent = getParent(map,key);
						if (logger.isDebugEnabled())logger.debug("Primary value: {}",tagMap.get("primary"));
						boolean primary = Boolean.valueOf((String)tagMap.get("primary"));
						if(primary){
							extractPrimaryValue(parent,s,subkey,val,true);
						}else{
							extractValue(parent,s,subkey, val, true);
						}
						//extractValue(parent,s, subkey, val);
						
						map.put(key,parent);
						return;
					}
					
					map.put(key,val);
					
					
				});
			});
		return map;
	}

	

	private Json getParent(NavigableMap<String, Json> map, String parentKey) {
		
		//make parent Json
		Json parent = map.get(parentKey);
		if(parent==null){
			parent = Json.object();
			map.put(parentKey,parent);
		}
		return parent;
	}

	public NavigableMap<String, Json> loadSources(NavigableMap<String, Json> map, String queryStr) {
		Query query = new Query(queryStr, dbName);
		QueryResult result = influxDB.query(query);
		// NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		
		if (logger.isDebugEnabled())logger.debug(result);
		if (result == null || result.getResults() == null)
			return map;
		result.getResults().forEach((r) -> {
			if (logger.isDebugEnabled())logger.debug(r);
			if (r.getSeries() == null)return;
			r.getSeries().forEach((s) -> {
				
				if (logger.isDebugEnabled())logger.debug("Load source: {}",s);
				if (s == null)return;
				
				String key = s.getName() + dot + s.getTags().get("sourceRef")+dot + s.getTags().get(skey);
				if (logger.isDebugEnabled())logger.debug("Load source map: {} = {}",()->key,()->getJsonValue(s,0));
				map.put(key, getJsonValue(s,0));
				

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
		Object obj = getValue(LONG_VALUE, s, 0);
		if (obj != null)
			return Json.make(Math.round((Double) obj));
		obj = getValue(NULL_VALUE, s, 0);
		if (obj != null) {
			if(obj instanceof String && Boolean.valueOf(obj.toString())){
				return Json.nil();
			}
			if(obj instanceof Boolean && (boolean)obj){
				return Json.nil();
			}
		}
		
		obj = getValue(DOUBLE_VALUE, s, 0);
		if (obj != null)
			return Json.make(obj);

		obj = getValue(STR_VALUE, s, 0);
		if (obj != null) {
			if (obj.equals("true")) {
				return Json.make(true);
			}
			if (obj.equals("false")) {
				return Json.make(false);
			}
			if (obj.toString().trim().startsWith("[") && obj.toString().endsWith("]")) {
				return Json.read(obj.toString());
			}
			
			return Json.make(obj);

		}
		return Json.nil();

	}
	

	/* (non-Javadoc)
	 * @see nz.co.fortytwo.signalk.artemis.service.TDBService#save(java.util.NavigableMap)
	 */
	@Override
	public void save(NavigableMap<String, Json> map) {
		if (logger.isDebugEnabled())logger.debug("Save map:  {}" ,map);
		for(Entry<String, Json> e: map.entrySet()){
			save(e.getKey(), e.getValue());
		}
		influxDB.flush();
	}

	/* (non-Javadoc)
	 * @see nz.co.fortytwo.signalk.artemis.service.TDBService#save(java.lang.String, mjson.Json, mjson.Json)
	 */
	@Override
	public void save(String k, Json v) {
		if (logger.isDebugEnabled())logger.debug("Save json:  {}={}" , k , v);
		//avoid _attr
		if(k.contains("._attr")){
			return;
		}
		if(k.contains("jwtToken")){
			return;
		}
		String srcRef = null;
		if(v.isObject() && v.has(sourceRef)) {
			srcRef=v.at(sourceRef).asString();
		}else {
			srcRef=StringUtils.substringAfter(k,dot+values+dot);
			if(srcRef.contains(dot+meta+dot))srcRef=StringUtils.substringBefore(srcRef, dot+meta);
		}
		if(StringUtils.isBlank(srcRef))srcRef="self";
		long tStamp = (v.isObject() && v.has(timestamp) ? Util.getMillisFromIsoTime(v.at(timestamp).asString())
				: System.currentTimeMillis());
		save(k,v,srcRef, tStamp);
	}
	
	/* (non-Javadoc)
	 * @see nz.co.fortytwo.signalk.artemis.service.TDBService#save(java.lang.String, mjson.Json, java.lang.String, long, mjson.Json)
	 */
	@Override
	public void save(String k, Json v, String srcRef ,long tStamp) {
		if (v.isPrimitive()|| v.isBoolean()) {
			
			if (logger.isDebugEnabled())logger.debug("Save primitive:  {}={}", k, v);
			saveData(k, srcRef, tStamp, v.getValue());	
			return;
		}
		if (v.isNull()) {
			
			if (logger.isDebugEnabled())logger.debug("Save null: {}={}",k , v);
			saveData(k, srcRef, tStamp, null);
			return;
		}
		if (v.isArray()) {
			
			if (logger.isDebugEnabled())logger.debug("Save array: {}={}", k , v);
			saveData(k, srcRef, tStamp, v);
			return;
		}
		if (v.has(sentence)) {
			saveData(k + dot + sentence, srcRef, tStamp, v.at(sentence));
		}
		if (v.has(meta)) {
			for (Entry<String, Json> i : v.at(meta).asJsonMap().entrySet()) {
				
				if (logger.isDebugEnabled())logger.debug("Save meta: {}={}",()->i.getKey(), ()->i.getValue());
				saveData(k + dot + meta + dot + i.getKey(), srcRef, tStamp, i.getValue());
			}
		}
		
		if (v.has(values)) {
		
			for (Entry<String, Json> i : v.at(values).asJsonMap().entrySet()) {
				
				if (logger.isDebugEnabled())logger.debug("Save values: {}={}",()->i.getKey() ,()-> i.getValue());
				String sRef = StringUtils.substringBefore(i.getKey(),dot+value);
				Json vs = i.getValue();
				long ts = (vs.isObject() && vs.has(timestamp) ? Util.getMillisFromIsoTime(vs.at(timestamp).asString())
						: tStamp);
				save(k,i.getValue(),sRef, ts);
				
			}
		}
		
		if (v.has(value)&& v.at(value).isObject()) {
			for (Entry<String, Json> i : v.at(value).asJsonMap().entrySet()) {
				
				if (logger.isDebugEnabled())logger.debug("Save value object: {}={}" , ()->i.getKey(),()->i.getValue());
				saveData(k + dot + value + dot + i.getKey(), srcRef, tStamp, i.getValue());
			}
			return;
		}

		
		if (logger.isDebugEnabled())logger.debug("Save value: {} : {}", k, v);
		saveData(k + dot + value, srcRef, tStamp, v.at(value));

		return;
	}


	
	private void extractPrimaryValue(Json parent, Series s, String subKey, Json val, boolean useValue) {
		String srcref = s.getTags().get("sourceRef");
		if (logger.isDebugEnabled())logger.debug("extractPrimaryValue: {}:{}",s, srcref);
		
		Json node = parent;
		Object ts = getValue("time", s, 0);
		if (ts != null) {
			// make predictable 3 digit nano ISO format
			ts = Util.getIsoTimeString(DateTime.parse((String)ts, ISODateTimeFormat.dateTimeParser()).getMillis());
			node.set(timestamp, Json.make(ts));
		}
		
		if (srcref != null) {
			node.set(sourceRef, Json.make(srcref));
		}

		if(useValue){
			// check if its an object value
			if (StringUtils.isNotBlank(subKey)) {
				Json valJson = Util.getJson(parent,value );
				valJson.set(subKey, val);
			} else {
				node.set(value, val);
			}

		}else{
			// check if its an object value
			if (StringUtils.isNotBlank(subKey)) {
				parent.set(subKey, val);
			} else {
				node.set(value, val);
			}

		}
		if (logger.isDebugEnabled())logger.debug("extractPrimaryValueObj: {}",parent);
	}
	
	private void extractValue(Json parent, Series s, String subkey, Json val, boolean useValue) {
		String srcref = s.getTags().get("sourceRef");
		if (logger.isDebugEnabled())logger.debug("extractValue: {}:{}",s, srcref);
		Json node = parent;
		

		Object ts = getValue("time", s, 0);
		if (ts != null) {
			// make predictable 3 digit nano ISO format
			ts = Util.getIsoTimeString(DateTime.parse((String)ts, ISODateTimeFormat.dateTimeParser()).getMillis());
			node.set(timestamp, Json.make(ts));
		}
			
		// true for vessels, eg any node with .values.
		if(useValue){
			if (logger.isDebugEnabled())logger.debug("extractValue: {}:{}",parent.getParentKey(), val);
			if (StringUtils.isNotBlank(subkey)) {
				Json valJson = Util.getJson(parent,value );
				valJson.set(subkey, val);
				if (logger.isDebugEnabled())logger.debug("extractValue with subkey: {}",node);
			} else {
				node.set(value, val);
				if (logger.isDebugEnabled())logger.debug("extractValue without subkey: {}",node);
			}

		}else{
			//for source
			if (StringUtils.isNotBlank(subkey)) {
				Json valJson = Util.getJson(parent,value );
				valJson.set(subkey, val);
			} else {
				node.set(value, val);
			}

		}
		
		if (logger.isDebugEnabled())logger.debug("extractValue, parent: {}",parent);
	}


	protected void saveData(String key, String sourceRef, long millis, Object val) {
			if(!allowWrite) {
				String clock = null;
				try {
					clock = Config.getConfigProperty(ConfigConstants.CLOCK_SOURCE);
				}catch (Exception e) {
					//ignore
				}
				if(StringUtils.equals(clock,"system")) {
					//if (logger.isInfoEnabled())logger.info("write enabled for {} : {}",()->val.getClass().getSimpleName(),()->key);
					setWrite(true);
				}else {
					if (logger.isInfoEnabled())logger.info("write not enabled for {} : {}",()->val.getClass().getSimpleName(),()->key);
					return;
				}
			}
			if(val!=null)
				if (logger.isDebugEnabled())logger.debug("save {} : {}",()->val.getClass().getSimpleName(),()->key);
			else{
				if (logger.isDebugEnabled())logger.debug("save {} : {}",()->null,()->key);
			}
			
			if(self_str.equals(key))return;
			if(version.equals(key))return;
			//String[] path = StringUtils.split(key, '.');
			//StringUtils.substringBetween(key, dot, dot)
			try {
				int p1 = key.indexOf(dot);
				if(p1<0) {
					p1=key.length();
				}
				int p2 = key.indexOf(dot,p1+1);
				int p3 = p2+1;
				if(p2<0) {
					p2=key.length();
					p3=p2;
				}
				
				Builder point = null;
				switch (key.substring(0, p1)) {	
				case resources:
					point = Point.measurement(key.substring(0, p1)).time(millis, TimeUnit.MILLISECONDS)
							.tag("sourceRef", sourceRef)
							.tag("uuid", key.substring(p1+1, p2))
							.tag(InfluxDbService.PRIMARY_VALUE, isPrimary(key,sourceRef).toString())
							.tag(skey, key.substring(p3));
					influxDB.write(addPoint(point, getFieldType(val), val));
					break;
				case sources:
					point = Point.measurement(key.substring(0, p1)).time(millis, TimeUnit.MILLISECONDS)
							.tag("sourceRef", key.substring(p1+1, p2))
							.tag(skey, key.substring(p3));
					influxDB.write(addPoint(point, getFieldType(val), val));
					break;
				case CONFIG:
					point = Point.measurement(key.substring(0, p1)).time(millis, TimeUnit.MILLISECONDS)
							//.tag("uuid", key.substring(p1+1, p2))
							.tag(skey, key.substring(p1+1));
					influxDB.write(addPoint(point, getFieldType(val), val));
					//also update the config map
					Config.setProperty(key, Json.make(val));
					break;
				case vessels:
					writeToInflux(key, p1, p2, p3, millis, sourceRef, getFieldType(val), val);
					break;
				case aircraft:
					writeToInflux(key, p1, p2, p3,millis, sourceRef, getFieldType(val), val);
					break;
				case sar:
					writeToInflux(key, p1, p2, p3,millis, sourceRef, getFieldType(val), val);
					break;
				case aton:
					writeToInflux(key, p1, p2, p3, millis, sourceRef, getFieldType(val), val);
					break;
				default:
					break;
				}
			}catch (Exception e) {
				logger.error(" Failed on key {} : {}",key, e.getMessage(), e);
				throw e;
			}
		
		
	}

	private void writeToInflux(String key, int p1, int p2, int p3, long millis, String sourceRef, String field,  Object val) {
		Boolean primary = isPrimary(key,sourceRef);
		Builder point = Point.measurement(key.substring(0, p1))
				.time(millis, TimeUnit.MILLISECONDS)
				.tag("sourceRef", sourceRef)
				.tag("uuid", key.substring(p1+1, p2))
				.tag(InfluxDbService.PRIMARY_VALUE, primary.toString())
				.tag(skey, key.substring(p3));
		
		influxDB.write(addPoint(point, field, val));
		
	}

	/* (non-Javadoc)
	 * @see nz.co.fortytwo.signalk.artemis.service.TDBService#loadPrimary()
	 */
	@Override
	public void loadPrimary(){
		primaryMap.clear();
		logger.info("Adding primaryMap");
		QueryResult result = influxDB.query(new Query("select * from vessels where primary='true' group by skey,primary,uuid,sourceRef order by time desc limit 1",dbName));
		if(result==null || result.getResults()==null)return ;
		result.getResults().forEach((r)-> {
			if(logger.isTraceEnabled())logger.trace(r);
			if(r==null||r.getSeries()==null)return;
			r.getSeries().forEach(
				(s)->{
					if(logger.isTraceEnabled())logger.trace(s);
					if(s==null)return;
					
					Map<String, String> tagMap = s.getTags();
					String key = s.getName()+dot+tagMap.get("uuid")+dot+StringUtils.substringBeforeLast(tagMap.get(skey),dot+value);
					primaryMap.put(StringUtils.substringBefore(key,dot+values+dot), tagMap.get("sourceRef"));
					if(logger.isTraceEnabled())logger.trace("Primary map: {}={}",key,tagMap.get("sourceRef"));
				});
		});
	}
	/* (non-Javadoc)
	 * @see nz.co.fortytwo.signalk.artemis.service.TDBService#isPrimary(java.lang.String, java.lang.String)
	 */
	@Override
	public Boolean isPrimary(String key, String sourceRef) {
		//truncate the .values. part of the key
		String mapRef = primaryMap.get(StringUtils.substringBefore(key,dot+values+dot));
		
		if(mapRef==null){
			//no result = primary
			setPrimary(key, sourceRef);
			if (logger.isDebugEnabled())logger.debug("isPrimary: {}={} : {}",key,sourceRef, true);
			return true;
		}
		if(StringUtils.equals(sourceRef, mapRef)){
			if (logger.isDebugEnabled())logger.debug("isPrimary: {}={} : {}, {}",key,sourceRef, mapRef, true);
			return true;
		}
		if (logger.isDebugEnabled())logger.debug("isPrimary: {}={} : {}, {}",key,sourceRef, mapRef, false);
		return false;
	}
	
	/* (non-Javadoc)
	 * @see nz.co.fortytwo.signalk.artemis.service.TDBService#setPrimary(java.lang.String, java.lang.String)
	 */
	@Override
	public Boolean setPrimary(String key, String sourceRef) {
		if (logger.isDebugEnabled())logger.debug("setPrimary: {}={}",key,sourceRef);
		return !StringUtils.equals(sourceRef, primaryMap.put(StringUtils.substringBefore(key,dot+values+dot),sourceRef));
	}

	private Point addPoint(Builder point, String field, Object jsonValue) {
		Object value = null;
		if (logger.isDebugEnabled())logger.debug("addPoint: {} : {} : {}",field,jsonValue.getClass(),jsonValue);
		if(jsonValue==null)return point.addField(field,true).build();
		if(jsonValue instanceof Json){
			if(((Json)jsonValue).isNull()){
				return point.addField(field,true).build();
			}else if(((Json)jsonValue).isString()){
				value=((Json)jsonValue).asString();
			}else if(((Json)jsonValue).isArray()){
				value=((Json)jsonValue).toString();
			}else if(((Json)jsonValue).isObject()){
					value=((Json)jsonValue).toString();
			}else{
				value=((Json)jsonValue).getValue();
			}
		}else{
			value=jsonValue;
		}
		if(value instanceof Boolean)return point.addField(field,((Boolean)value).toString()).build();
		if(value instanceof Double)return point.addField(field,(Double)value).build();
		if(value instanceof Float)return point.addField(field,(Double)value).build();
		if(value instanceof BigDecimal)return point.addField(field,((BigDecimal)value).doubleValue()).build();
		if(value instanceof Long)return point.addField(field,(Long)value).build();
		if(value instanceof Integer)return point.addField(field,((Integer)value).longValue()).build();
		if(value instanceof BigInteger)return point.addField(field,((BigInteger)value).longValue()).build();
		if(value instanceof String)return point.addField(field,(String)value).build();
		if (logger.isDebugEnabled())logger.debug("addPoint: unknown type: {} : {}",field,value);
		return null;
	}

	private String getFieldType(Object value) {
		
		if(value==null){
			if (logger.isDebugEnabled())logger.debug("getFieldType: {} : {}",value,value);
			return NULL_VALUE;
		}
		if (logger.isDebugEnabled())logger.debug("getFieldType: {} : {}",value.getClass().getName(),value);
		if(value instanceof Json){
			if(((Json)value).isNull())return NULL_VALUE;
			if(((Json)value).isArray())return STR_VALUE;
			if(((Json)value).isObject())return STR_VALUE;
			value=((Json)value).getValue();
		}
		if(value instanceof Double)return DOUBLE_VALUE;
		if(value instanceof BigDecimal)return DOUBLE_VALUE;
		if(value instanceof Long)return LONG_VALUE;
		if(value instanceof BigInteger)return LONG_VALUE;
		if(value instanceof Integer)return LONG_VALUE;
		if(value instanceof String)return STR_VALUE;
		if(value instanceof Boolean)return STR_VALUE;
		
		if (logger.isDebugEnabled())logger.debug("getFieldType:unknown type: {} : {}",value.getClass().getName(),value);
		return null;
	}

	/* (non-Javadoc)
	 * @see nz.co.fortytwo.signalk.artemis.service.TDBService#close()
	 */
	@Override
	public void close() {
		influxDB.close();
	}

	public InfluxDB getInfluxDB() {
		return influxDB;
	}

	public static String getDbName() {
		return dbName;
	}

	public static void setDbName(String dbName) {
		InfluxDbService.dbName = dbName;
	}

	


}
