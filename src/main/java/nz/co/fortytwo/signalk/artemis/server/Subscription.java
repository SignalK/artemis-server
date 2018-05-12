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
package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.activemq.artemis.core.postoffice.RoutingStatus;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

import io.netty.util.internal.ConcurrentSet;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.event.PathEvent;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;


/**
 * Holds subscription data, wsSessionId, path, period
 * If a subscription is made via REST before the websocket is started then the wsSocket will hold the sessionId.
 * This must be swapped for the wsSessionId when the websocket starts.
 * The subscription will be in an inactive state when submitted by REST if sessionId = sessionId
 * 
 * @author robert
 * 
 */
public class Subscription {
	private static Logger logger = LogManager.getLogger(Subscription.class);
	private static InfluxDbService influx=new InfluxDbService();
	String sessionId = null;
	String path = null;
	long period = -1;
	boolean active = true;
	private long minPeriod;
	private String format;
	private String policy;
	private Pattern pattern = null;
	private String vesselPath;
	Set<String> subscribedPaths = new ConcurrentSet<String>();
	private String routeId;
	private String destination;

	private TimerTask task;
	private Timer timer;
	private String table;
	private String uuid;

	public Subscription(String sessionId, String destination, String user, String password, String path, long period, long minPeriod, String format, String policy) throws Exception {
		this.sessionId = sessionId;

		this.path = Util.sanitizePath(path);
		String context = Util.getContext(path);
		this.table=StringUtils.substringBefore(context,".");
		this.uuid=StringUtils.substringAfter(context,".");
		pattern = Util.regexPath(StringUtils.substring(path,context.length()+1));
		this.period = period;
		this.minPeriod = minPeriod;
		this.format = format;
		this.policy = policy;
		this.destination=destination;

		
		task = new TimerTask() {
			
			@Override
			public void run() {
				if(logger.isDebugEnabled()){
					logger.debug("Running for client:"+destination+", "+getPath());
					logger.debug("Running for session:"+sessionId);
				}
				ServerSession s = ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
				if(logger.isDebugEnabled())
					logger.debug("Running server session:"+(s==null?s:s.getName()));
				
				if(s==null)	return;
				
				try {
					//get a map of the current subs values
					NavigableMap<String, Json> rslt = new ConcurrentSkipListMap<String, Json>();
					//select * from vessels where uuid='urn:mrn:imo:mmsi:209023000' AND skey=~/nav.*cou/ group by skey,uuid,sourceRef,owner,grp order by time desc limit 1
					if(logger.isDebugEnabled())
						logger.debug("select * from "+table+" where uuid='"+uuid+"' AND skey=~/"+pattern+"/ group by skey,uuid,sourceRef,owner,grp order by time desc limit 1");
					influx.loadData(rslt,"select * from "+table+" where uuid='"+uuid+"' AND skey=~/"+pattern+"/ group by skey,uuid,sourceRef,owner,grp order by time desc limit 1","signalk");
					if(logger.isDebugEnabled())logger.debug("rslt map = "+rslt);
						
					if(SignalKConstants.FORMAT_DELTA.equals(format)){
						Json json = SignalkMapConvertor.mapToDelta(rslt);
						if(logger.isDebugEnabled())logger.debug("Delta json = "+json);
						Util.sendReply(rslt.getClass().getSimpleName(),destination,format,json,s);
					}
					if(SignalKConstants.FORMAT_FULL.equals(format)){
						Json json = SignalkMapConvertor.mapToFull(rslt);
						if(logger.isDebugEnabled())logger.debug("Full json = "+json);
						Util.sendReply(rslt.getClass().getSimpleName(),destination,format,json,s);
					}
					s.commit();				
				} catch (Exception e) {
					logger.error(e.getMessage(),e);
				
				}
				
			}
		};
		
		//SignalKModelFactory.getInstance().getEventBus().register(this);
		//for(String p: ImmutableList.copyOf(SignalKModelFactory.getInstance().getKeys())){
		//	if(isSubscribed(p)){
		//		subscribedPaths.add(p);
		//	}
		//}
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
		return "Subscription [sessionId=" + sessionId + ", path=" + path + ", period=" + period + ", routeId=" + routeId+ ", format=" + format + ", active=" + active + ", destination=" + destination + "]";
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) throws Exception {
		this.active = active;
		if(logger.isDebugEnabled())logger.debug("Set active:"+active);

		if(!active && timer!=null){
			timer.cancel();
			timer=null;
		}

		if(active && timer==null){
			timer = new Timer(sessionId, true);
			timer.schedule(task, 0, getPeriod());
			if(logger.isDebugEnabled())logger.debug("Scheduled:"+getPeriod());
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
		return pattern.matcher(key).find();
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
	 * The list is filtered by the key if it is not null or empty in which case a full list is returned,
	 * @param key
	 * @return
	 */
	public List<String> getSubscribed(String key) {
		if(StringUtils.isBlank(key)){
			return ImmutableList.copyOf(subscribedPaths);
		}
		List<String> paths = new ArrayList<String>();
		for (String p : subscribedPaths) {
			if (p.startsWith(key)) {
				if(logger.isDebugEnabled())logger.debug("Adding path:" + p);
				paths.add(p);
			}
		}
		return paths;
	}

	/**
	 * Listens for node changes in the server and adds them if they match the subscription
	 * 
	 * @param pathEvent
	 */
	@Subscribe
	public void recordEvent(PathEvent pathEvent) {
		if (pathEvent == null)
			return;
		if (pathEvent.getPath() == null)
			return;
		if(path.endsWith(dot+source)
    			&& path.endsWith(dot+timestamp)
    			&& path.contains(dot+source+dot)
    			&& path.endsWith(dot+sourceRef)){
        	return;
        }
		if (logger.isDebugEnabled())
			logger.debug(this.hashCode() + " received event " + pathEvent.getPath());
		if(isSubscribed(pathEvent.getPath())){
			subscribedPaths.add(pathEvent.getPath());
		}
	}

	public void setRouteId(String routeId) {
		this.routeId=routeId;
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

}
