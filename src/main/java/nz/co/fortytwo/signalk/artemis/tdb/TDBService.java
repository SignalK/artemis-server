package nz.co.fortytwo.signalk.artemis.tdb;

import java.util.Map;
import java.util.NavigableMap;

import mjson.Json;

public interface TDBService {

	public void setUpTDb();
	
	/**
	 * Sets the db to allow writing. Enable writing after the system time has been correctly set (esp RPi).
	 * @param write
	 */
	public void setWrite(boolean write);
	
	
	/**
	 * Get the current write state. Typically false at startup, or until the system time is set.
	 * @return
	 */
	public boolean getWrite();

	public void closeTDb();

	/**
	 * Loads the map with data from resources
	 * @param map
	 * @param query
	 * @return
	 */
	public NavigableMap<String, Json> loadResources(NavigableMap<String, Json> map, Map<String,String> query);

	public NavigableMap<String, Json> loadConfig(NavigableMap<String, Json> map, Map<String,String> query);

	/**
	 * Loads the map with data from vessels
	 * @param map
	 * @param table 
	 * @param queryStr
	 * @param db
	 * @return
	 */
	public NavigableMap<String, Json> loadData(NavigableMap<String, Json> map, String table, Map<String,String> query);

	/**
	 * Loads the map with data from sources
	 * @param map
	 * @param query
	 * @return
	 */
	public NavigableMap<String, Json> loadSources(NavigableMap<String, Json> map, Map<String,String> query);

	public void save(NavigableMap<String, Json> map);

	public void save(String k, Json v);

	public void save(String k, Json v, String srcRef, long tStamp);

	/**
	 * Loads the primary keys into a cache for fast access.
	 */
	public void loadPrimary();

	/**
	 * Returns true if the key is the primary(default source) key for this value, false otherwise
	 * @param key
	 * @param sourceRef
	 * @return
	 */
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

	/**
	 * Loads the map with data as it was at the provided ISO timestamp
	 * @param map
	 * @param table
	 * @param query
	 * @param time
	 * @return
	 */
	public NavigableMap<String, Json> loadDataSnapshot(NavigableMap<String, Json> map, String table, Map<String, String> query,
			String time);

	/**
	 * Loads the map with data as it was at the provided timestamp in millis from the Unix Epoch.
	 * @param rslt
	 * @param table
	 * @param map
	 * @param queryTime
	 * @return
	 */
	public NavigableMap<String, Json> loadDataSnapshot(NavigableMap<String, Json> rslt, String table, Map<String, String> map,
			long queryTime);

}