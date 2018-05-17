package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.server.ArtemisServer;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.service.SecurityService;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
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
 * Converts SignalK GET interceptor
 * 
 * @author robert
 * 
 */

public class GetMsgInterceptor extends BaseInterceptor implements Interceptor {

	
	private static Logger logger = LogManager.getLogger(GetMsgInterceptor.class);
	
	/**
	 * Reads Delta GET message and returns the result in full format. Does nothing if json
	 * is not a GET, and returns the original message
	 * 
	 * @param node
	 * @return
	 */

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if(isResponse(packet))return true;
		
		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage message = realPacket.getMessage();
			
			if (!Config.JSON_DELTA.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))
				return true;
		
			Json node = Util.readBodyBuffer(message);
			String correlation = message.getStringProperty(Config.AMQ_CORR_ID);
			String sessionId = message.getStringProperty(Config.AMQ_SESSION_ID);
			String destination = message.getStringProperty(Config.AMQ_REPLY_Q);
			ServerSession s = ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
			
			// deal with diff format
			if (node.has(CONTEXT) && (node.has(GET))) {
				if (logger.isDebugEnabled())
					logger.debug("GET msg: " + node.toString());
				String ctx = node.at(CONTEXT).asString();
				String root = StringUtils.substringBefore(ctx,dot);
				root = Util.sanitizeRoot(root);
				
				
				//limit to explicit series
				if (!vessels.equals(root) 
					&& !CONFIG.equals(root) 
					&& !sources.equals(root) 
					&& !resources.equals(root)
					&& !aircraft.equals(root)
					&& !sar.equals(root)
					&& !aton.equals(root)
					&& !ALL.equals(root)){
					try{
						sendReply(String.class.getSimpleName(),destination,FORMAT_FULL,Json.object(),s,correlation);
						return true;
					} catch (Exception e) {
						logger.error(e, e);
						throw new ActiveMQException(ActiveMQExceptionType.INTERNAL_ERROR, e.getMessage(), e);
					}
				}
				String qUuid = StringUtils.substringAfter(ctx,dot);
				if(StringUtils.isBlank(qUuid))qUuid="*";
				ArrayList<String> fullPaths=new ArrayList<>();
				
				try {
					NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
					for(Json p: node.at(GET).asJsonList()){
						String path = p.at(PATH).asString();
						path=Util.sanitizePath(path);
						fullPaths.add(Util.sanitizeRoot(ctx+dot+path));
						StringBuffer sql=new StringBuffer();
						path=Util.regexPath(path).toString();
						switch (root) {
						case CONFIG:
							sql.append("select * from config");
							if(StringUtils.isNotBlank(path))sql.append(" where skey=~/"+path+"/");
							sql.append(" group by skey,owner,grp order by time desc limit 1");
							influx.loadConfig(map, sql.toString(),"signalk");
							break;
						case resources:
							sql.append("select * from resources");
							if(StringUtils.isNotBlank(path))sql.append(" where skey=~/"+path+"/");
							sql.append(" group by skey,owner,grp order by time desc limit 1");
							influx.loadResources(map, sql.toString(),"signalk");
							break;
						case sources:
							sql.append("select * from sources");
							if(StringUtils.isNotBlank(path))sql.append(" where skey=~/"+path+"/");
							sql.append(" group by skey,owner,grp order by time desc limit 1");
							influx.loadSources(map, sql.toString(),"signalk");
							break;
						case vessels:
							loadDataFromInflux(root,qUuid,path,map);
							break;
						case aircraft:
							loadDataFromInflux(root,qUuid,path,map);
							break;
						case sar:
							loadDataFromInflux(root,qUuid,path,map);
							break;
						case aton:
							loadDataFromInflux(root,qUuid,path,map);
							break;
						case ALL:
							loadAllDataFromInflux(map,vessels);
							//loadAllDataFromInflux(map,aircraft);
							//loadAllDataFromInflux(map,sar);
							//loadAllDataFromInflux(map,aton);
						default:
						}
						
						if (logger.isDebugEnabled())
							logger.debug("GET sql : {}", sql);
					}
					
					if (logger.isDebugEnabled())logger.debug("GET map : {}", map);
					
					Json json = SignalkMapConvertor.mapToFull(map);
					
					if (logger.isDebugEnabled())logger.debug("GET json : {}", json);
					
					String fullPath = StringUtils.getCommonPrefix(fullPaths.toArray(new String[]{}));
					//fullPath=StringUtils.remove(fullPath,".*");
					//fullPath=StringUtils.removeEnd(fullPath,".");
					
					// for REST we only send back the sub-node, so find it
					if (logger.isDebugEnabled())logger.debug("GET node : {}", fullPath);
					
					if (StringUtils.isNotBlank(fullPath) && !root.startsWith(CONFIG) && !root.startsWith(ALL))
						json = Util.findNodeMatch(json, fullPath);
					
					sendReply(map.getClass().getSimpleName(),destination,FORMAT_FULL,json,s,correlation);

					return true;
				} catch (Exception e) {
					logger.error(e, e);
					throw new ActiveMQException(ActiveMQExceptionType.INTERNAL_ERROR, e.getMessage(), e);
				}

			}
		}
		return true;

	}

	

	private NavigableMap<String, Json> loadDataFromInflux(String table, String qUuid, String path, NavigableMap<String, Json> map) {
		StringBuffer sql=new StringBuffer();
		sql.append("select * from "+table);
		if(StringUtils.isNotBlank(qUuid) && StringUtils.isNotBlank(path))sql.append(" where skey=~/"+path+"/ and uuid=~/"+Util.regexPath(qUuid).toString()+"/");
		if(StringUtils.isNotBlank(qUuid) && StringUtils.isBlank(path))sql.append(" where uuid=~/"+Util.regexPath(qUuid).toString()+"/");
		if(StringUtils.isBlank(qUuid) && StringUtils.isNotBlank(path))sql.append(" where skey=~/"+path+"/");
		sql.append(" group by uuid, primary, skey,owner, grp order by time desc limit 1");
		if (logger.isDebugEnabled())
			logger.debug("GET sql : {}", sql);
		influx.loadData(map, sql.toString(),"signalk");
		return map;
	}
	
	private NavigableMap<String, Json> loadAllDataFromInflux(NavigableMap<String, Json> map, String table) {
		StringBuffer sql=new StringBuffer();
		sql.append("select * from "+table);
		sql.append(" group by uuid, primary,skey,owner, grp order by time desc limit 1");
		if (logger.isDebugEnabled())
			logger.debug("GET sql : {}", sql);
		influx.loadData(map, sql.toString(),"signalk");
		return map;
	}

	
	
	

}
