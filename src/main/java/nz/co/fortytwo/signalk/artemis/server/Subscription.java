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

import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

import io.netty.util.internal.ConcurrentSet;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.model.event.PathEvent;
//import nz.co.fortytwo.signalk.model.impl.SignalKModelFactory;

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
	private String user;
	private String password;
	private TimerTask task;
	private Timer timer;
	ClientSession txSession;
	ClientProducer producer;
	//private String outputType;

	public Subscription(String sessionId, String destination, String user, String password, String path, long period, long minPeriod, String format, String policy) throws Exception {
		this.sessionId = sessionId;

		this.path = Util.sanitizePath(path);
		pattern = Util.regexPath(this.path);
		this.period = period;
		this.minPeriod = minPeriod;
		this.format = format;
		this.policy = policy;
		this.destination=destination;
		this.user=user;
		this.password=password;
		
		
		task = new TimerTask() {
			
			@Override
			public void run() {
				ClientSession rxSession = null;
				ClientConsumer consumer = null;
				try {
					//start polling consumer.
					rxSession = Util.getVmSession(user, password);
					consumer = rxSession.createConsumer("vessels",
							"_AMQ_LVQ_NAME like '"+getPath()+".%'", true);
					
					ClientMessage msgReceived = null;
					HashMap< String, HashMap<String, HashMap<String,List<ClientMessage>>>> msgs = new HashMap<>();
					while ((msgReceived = consumer.receive(10)) != null) {
						if(logger.isDebugEnabled())logger.debug("message = "  + msgReceived.getMessageID()+":" + msgReceived.getAddress() );
						String ctx = Util.getContext(msgReceived.getAddress().toString());
						HashMap< String,HashMap<String,List<ClientMessage>>> ctxMap = msgs.get(ctx);
						if(ctxMap==null){
							ctxMap=new HashMap<>();
							msgs.put(ctx, ctxMap);
						}
						HashMap<String,List<ClientMessage>> tsMap = ctxMap.get(msgReceived.getStringProperty(timestamp));
						if(tsMap==null){
							tsMap=new HashMap<>();
							ctxMap.put(msgReceived.getStringProperty(timestamp), tsMap);
						}
						List<ClientMessage> srcMap = tsMap.get(msgReceived.getStringProperty(source));
						if(srcMap==null){
							srcMap=new ArrayList<>();
							tsMap.put(msgReceived.getStringProperty(source), srcMap);
						}
						srcMap.add( msgReceived);
					}
					Json deltas = Util.generateDelta(msgs);
					for( Json delta : deltas.asJsonList() ){
						ClientMessage txMsg = txSession.createMessage(true);
						txMsg.getBodyBuffer().writeString(delta.toString());
						txMsg.putStringProperty(Config.JAVA_TYPE, delta.getClass().getSimpleName());
						producer.send(new SimpleString("outgoing.reply."+destination),txMsg);
						if(logger.isDebugEnabled())logger.debug("json = "+delta);
					}
					consumer.close();
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally {
					if(consumer!=null){
						try {
							consumer.close();
						} catch (ActiveMQException e) {
							logger.error(e);
						}
					}
					if(rxSession!=null){
						try {
							rxSession.close();
						} catch (ActiveMQException e) {
							logger.error(e);
						}
					}
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
		if(active && (txSession==null || txSession.isClosed())){
			txSession = Util.getVmSession(user, password);
		}
		if(active && (producer==null || producer.isClosed())){
			producer = txSession.createProducer();
		}
		if(!active && timer!=null){
			timer.cancel();
			timer=null;
		}
		if(!active){
			if(producer!=null)producer.close();
			if(txSession!=null)txSession.close();
		}
		if(active && timer==null){
			timer = new Timer(sessionId, true);
			timer.schedule(task, 0, getPeriod());
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
