package nz.co.fortytwo.signalk.artemis.service;

import java.util.Map;
import java.util.NavigableMap;

import mjson.Json;

public interface TDBService {

	public void setUpTDb();

	public void closeTDb();

	public NavigableMap<String, Json> loadResources(NavigableMap<String, Json> map, Map<String,String> query);

	public NavigableMap<String, Json> loadConfig(NavigableMap<String, Json> map, Map<String,String> query);

	/**
	 * @param map
	 * @param table 
	 * @param queryStr
	 * @param db
	 * @return
	 */
	public NavigableMap<String, Json> loadData(NavigableMap<String, Json> map, String table, Map<String,String> query);

	public NavigableMap<String, Json> loadSources(NavigableMap<String, Json> map, Map<String,String> query);

	public void save(NavigableMap<String, Json> map);

	public void save(String k, Json v);

	public void save(String k, Json v, String srcRef, long tStamp);

	public void loadPrimary();

	public Boolean isPrimary(String key, String sourceRef);

	/**
	 * Sets the primary key
	 * Returns true if the keys sourceRef was null or changed, false if the keys sourceRef was unchanged.
	 * 
	 * @param key
	 * @param sourceRef
	 * @return
	 */
	public Boolean setPrimary(String key, String sourceRef);

	void close();

}