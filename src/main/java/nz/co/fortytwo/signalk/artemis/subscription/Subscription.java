/*
 * 
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 * 
 * This file is part of the signalk-server-java project
 * 
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.co.fortytwo.signalk.artemis.subscription;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.skey;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;

import io.netty.util.internal.ConcurrentSet;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.service.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Holds subscription data, wsSessionId, path, period If a subscription is made
 * via REST before the websocket is started then the wsSocket will hold the
 * sessionId. This must be swapped for the wsSessionId when the websocket
 * starts. The subscription will be in an inactive state when submitted by REST
 * if sessionId = sessionId
 * 
 * @author robert
 * 
 */
public class Subscription {
	private static Logger logger = LogManager.getLogger(Subscription.class);
	private static TDBService influx = new InfluxDbService();

	String sessionId = null;
	String path = null;
	long period = -1;
	private String startTime = null;
	private long playbackPeriod = -1;
	boolean active = true;
	private long minPeriod;
	private String format;
	private String policy;
	private Pattern pathPattern = null;
	private Pattern uuidPattern = null;
	private String vesselPath;
	Set<String> subscribedPaths = new ConcurrentSet<String>();
	private String routeId;
	private String destination;

	private TimerTask task;
	
	private String table;
	private String uuid;
	private Map<String, String> map = new HashMap<>();
	private String correlation;

	public Subscription(String sessionId, String destination, String user, String password, String path, long period,
			long minPeriod, String format, String policy, String correlation, String startTime, double playbackRate) throws Exception {
		this.sessionId = sessionId;

		this.path = Util.sanitizePath(path);
		String context = Util.getContext(path);
		this.table = StringUtils.substringBefore(context, ".");
		this.uuid = StringUtils.substringAfter(context, ".");
		uuidPattern = Util.regexPath(uuid);
		pathPattern = Util.regexPath(StringUtils.substring(path, context.length() + 1));
		this.period = period;
		this.minPeriod = minPeriod;
		this.format = format;
		this.policy = policy;
		this.destination = destination;
		this.setCorrelation(correlation);
		
		if(StringUtils.isNotBlank(startTime)) {
			this.startTime=startTime;
			this.playbackPeriod=(long) (period/playbackRate);
		}
		
		map.put("uuid", uuidPattern.toString());
		map.put(skey, pathPattern.toString());
		
		SubscriptionManagerFactory.getInstance().createTempQueue(destination);
		task = new TimerTask() {

			private long queryTime=StringUtils.isNotBlank(startTime)?Util.getMillisFromIsoTime(startTime):System.currentTimeMillis();

			@Override
			public void run() {
				if (logger.isDebugEnabled()) {
					logger.debug("Running for client:{}, {}" , destination, getPath());
					logger.debug("Running for session:{}" , sessionId);
				}

				try {
					if (!active) {
						cancel();
						return;
					}
					
					// get a map of the current subs values
					NavigableMap<String, Json> rslt = new ConcurrentSkipListMap<String, Json>();
					// select * from vessels where
					// uuid='urn:mrn:imo:mmsi:209023000' AND skey=~/nav.*cou/
					// group by skey,uuid,sourceRef,owner,grp order by time desc
					// limit 1
					if(StringUtils.isNotBlank(startTime)) {
						influx.loadDataSnapshot(rslt, table, map, queryTime);
						if(queryTime<System.currentTimeMillis()) {
							queryTime=queryTime+period;
						}
					}else {
						influx.loadData(rslt, table, map);
					}
					if (logger.isDebugEnabled())
						logger.debug("rslt map = {}" , rslt);
					Json json = null;
					if (SignalKConstants.FORMAT_DELTA.equals(format)) {
						json = SignalkMapConvertor.mapToUpdatesDelta(rslt);
						if (logger.isDebugEnabled())
							logger.debug("Delta json = {}", json);
					}
					if (SignalKConstants.FORMAT_FULL.equals(format)) {
						json = SignalkMapConvertor.mapToFull(rslt);
						if (logger.isDebugEnabled())
							logger.debug("Full json = {}", json);
					}
					try{
						SubscriptionManagerFactory.getInstance().send(rslt.getClass().getSimpleName(), destination, format, correlation, json);
					}catch(ActiveMQException amq){
						logger.error(amq,amq);
						setActive(false);
					}

				} catch (Exception e) {
					logger.error(e.getMessage(), e);

				}

			}
		};
		//now send hello
		if (logger.isDebugEnabled())
			logger.debug("Sending hello: {}", Config.getHelloMsg());
		try{
			SubscriptionManagerFactory.getInstance().send(ConcurrentSkipListMap.class.getSimpleName(), destination, format, correlation,  Config.getHelloMsg());
		}catch(ActiveMQException amq){
			logger.error(amq,amq);
			setActive(false);
		}
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + (int) (period ^ (period >>> 32));
		result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (period != other.period)
			return false;
		if (sessionId == null) {
			if (other.sessionId != null)
				return false;
		} else if (!sessionId.equals(other.sessionId))
			return false;
		return true;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = Util.sanitizePath(path);
	}

	public long getPeriod() {
		return period;
	}

	public void setPeriod(long period) {
		this.period = period;
	}

	@Override
	public String toString() {
		return "Subscription [sessionId=" + sessionId + ", path=" + path + ", period=" + period + ", routeId=" + routeId
				+ ", format=" + format + ", active=" + active + ", destination=" + destination + "]";
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) throws Exception {
		this.active = active;
		if (logger.isDebugEnabled())
			logger.debug("Set active:{}, {}", active, this);

		if (active) {
			if(StringUtils.isNotBlank(startTime)) {
				SubscriptionManagerFactory.getInstance().schedule(task, playbackPeriod);
			}else {
				SubscriptionManagerFactory.getInstance().schedule(task, getPeriod());
			}
			if (logger.isDebugEnabled())
				logger.debug("Scheduled:{}" , getPeriod());
		}

	}

	public boolean isSameRoute(Subscription sub) {
		if (period != sub.period)
			return false;
		if (sessionId == null) {
			if (sub.sessionId != null)
				return false;
		} else if (!sessionId.equals(sub.sessionId))
			return false;
		if (path != sub.path)
			return false;
		if (path == null) {
			if (sub.path != null)
				return false;
		} else if (!path.equals(sub.path))
			return false;
		if (format == null) {
			if (sub.format != null)
				return false;
		} else if (!format.equals(sub.format))
			return false;
		if (policy == null) {
			if (sub.policy != null)
				return false;
		} else if (!policy.equals(sub.policy))
			return false;
		return true;
	}

	public boolean isSameVessel(Subscription sub) {
		if (getVesselPath() != null && getVesselPath().equals(sub.getVesselPath()))
			return true;
		return false;
	}

	/**
	 * Gets the context path, eg vessels.motu, vessel.*, or vessels.2933??
	 * 
	 * @param path
	 * @return
	 */
	public String getVesselPath() {
		if (vesselPath == null) {
			if (!path.startsWith(vessels))
				return null;
			int pos = path.indexOf(".") + 1;
			// could be just 'vessels'
			if (pos < 1)
				return null;
			pos = path.indexOf(".", pos);
			// could be just one .\dot. vessels.123456789
			if (pos < 0)
				return path;
			vesselPath = path.substring(0, pos);
		}
		return vesselPath;
	}

	/**
	 * Returns true if this subscription is interested in this path
	 * 
	 * @param key
	 * @return
	 */
	public boolean isSubscribed(String key) {
		return pathPattern.matcher(key).find();
	}

	public long getMinPeriod() {
		return minPeriod;
	}

	public String getFormat() {
		return format;
	}

	public String getPolicy() {
		return policy;
	}

	/**
	 * Returns a list of paths that this subscription is currently providing.
	 * The list is filtered by the key if it is not null or empty in which case
	 * a full list is returned,
	 * 
	 * @param key
	 * @return
	 */
	public List<String> getSubscribed(String key) {
		if (StringUtils.isBlank(key)) {
			return ImmutableList.copyOf(subscribedPaths);
		}
		List<String> paths = new ArrayList<String>();
		for (String p : subscribedPaths) {
			if (p.startsWith(key)) {
				if (logger.isDebugEnabled())
					logger.debug("Adding path:{}", p);
				paths.add(p);
			}
		}
		return paths;
	}

	

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public String getRouteId() {
		return routeId;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getCorrelation() {
		return correlation;
	}

	public void setCorrelation(String correlation) {
		this.correlation = correlation;
	}


	

}
