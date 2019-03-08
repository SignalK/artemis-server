package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_INFLUX_KEY;
import static nz.co.fortytwo.signalk.artemis.util.Config.INTERNAL_KV;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.TDBService;
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
 * Read key value pairs from Config.INTERNAL_KV and store in influxdb
 * 
 * @author robert
 * 
 */

public class AlarmHandler extends BaseHandler {

	private static Logger logger = LogManager.getLogger(AlarmHandler.class);

	private static NavigableMap<String, Json> alarmMap = new ConcurrentSkipListMap<>();

	public AlarmHandler() {
		super();
		if (logger.isDebugEnabled())
			logger.debug("Initialising for : {} ", uuid);
		try {

			// load all keys with alarms
			NavigableMap<String, Json> map = loadAlarms(influx);
			for (Entry<String, Json> entry : map.entrySet()) {
				parseMeta(entry.getKey(), entry.getValue());
			}

			// start listening
			initSession(AMQ_INFLUX_KEY+" LIKE '"+vessels+"%'");
		} catch (Exception e) {
			logger.error(e, e);
		}
	}

	private void parseMeta(String key, Json json) {
		if (logger.isDebugEnabled())
			logger.debug("Adding alarm for  meta key: {} : {}", key, json);
		if (json.isObject() && json.has(meta)) {
			parseMetaByKey(key, json.at(meta));
		}
	}

	private NavigableMap<String, Json> loadAlarms(TDBService influx) {
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		Map<String, String> query = new HashMap<>();
		query.put(skey, meta);
		return influx.loadData(map, vessels, query);

	}

	@Override
	public void consume(Message message) {
		String key = message.getStringProperty(AMQ_INFLUX_KEY);
		if (logger.isDebugEnabled())
			logger.debug("Consuming : {} ", key);
		if (key.contains(meta)) {
			Json node = Util.readBodyBuffer(message.toCore());
			parseMetaByKey(key, node);
			return;
		}

		if (!alarmMap.containsKey(key)) {
			if (logger.isDebugEnabled())
				logger.debug("Skipping : {} ", key);
			return;
		}

		Json node = Util.readBodyBuffer(message.toCore());

		if (logger.isDebugEnabled())
			logger.debug("Checking alarm for key: {} : {}", key, node);
		check(message, key, alarmMap.get(key), node);
		node.clear(true);

	}

	private void parseMetaByKey(String key, Json node) {
		if (logger.isDebugEnabled())
			logger.debug("Adding alarm for key: {} : {}", key, node);

		String parentKey = StringUtils.substringBeforeLast(key, ".meta.");
		String metaKey = StringUtils.substringAfterLast(key, ".meta.");
		Json metaJson = alarmMap.get(parentKey);
		if (metaJson == null) {
			metaJson = Json.object();
		}
		Util.setJson(metaJson, metaKey, node);
		alarmMap.put(parentKey, metaJson);
		if (logger.isDebugEnabled())
			logger.debug("Added alarm for key: {} : {}", parentKey, metaJson);

	}

	protected void check(Message message, String key, Json alarmDef, Json node) {
		// only works for doubles!
		logger.debug("  Alarm key: {}, def: {}, val: {}", key, alarmDef,node);
		if (node.has(value) && node.at(value).isNumber()) {
			double val = node.at(value).asDouble();
			logger.debug("  Alarm val: {}", val);
			if (alarmDef.has(zones)) {
				Json zoneList = alarmDef.at(zones);
				logger.debug("zones: {}",zoneList);
				for (Json zone : zoneList) {
					double upper = zone.has("upper") ? zone.at("upper").asDouble() : Double.MAX_VALUE;
					double lower = zone.has("lower") ? zone.at("lower").asDouble() : Double.MIN_VALUE;
					logger.debug("limits: {}:{}",lower, upper);
					if (val >= lower && val < upper) {
						String state = zone.at("state").asString();
						if (logger.isDebugEnabled())
							logger.debug("  Alarm in zone: {}", state);
						// send notification
						// vessels.self.notifications+path
						int len = Util.getContext(key).length();
						key = key.substring(0, len) + dot + notifications + key.substring(len);
						try {
							if (normal.equals(state)) {
								sendJson(message, key, getNormalNotification());
							}

							if (warn.equals(state)) {
								// notification.at(value).set("method", alarmDef.at("warnMethod"));
								sendJson(message, key,
										getNotification(state, zone.at("message"), alarmDef.at("warnMethod")));
							}
							if (alarm.equals(state)) {
								// notification.at(value).set("method", alarmDef.at("alarmMethod"));
								sendJson(message, key,
										getNotification(state, zone.at("message"), alarmDef.at("alarmMethod")));
							}
							if (logger.isDebugEnabled())
								logger.debug("  Sending alarm: {}={}", key, state);

						} catch (Exception e) {
							logger.error(e, e);
						}

					}
				}
			}
		}

	}

	private Json getNotification(String state, Json message, Json warnMethod) {
		Json notification = Json.object();
		notification.set(value, Json.object());
		notification.at(value).set("state", state).set("message", message).set("method", warnMethod);

		return notification;
	}

	private Json getNormalNotification() {
		Json notification = Json.object();
		notification.set(value, Json.object());
		notification.at(value).set("state", Json.nil()).set("message", Json.nil()).set("method", Json.nil());

		return notification;
	}

}
