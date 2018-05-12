package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

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
	private static InfluxDbService influx = new InfluxDbService();
	private static SecurityService security = new SecurityService();

	/**
	 * Reads Delta GET message and returns the result in full format. Does nothing if json
	 * is not a GET, and returns the original message
	 * 
	 * @param node
	 * @return
	 */

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if (packet.isResponse())
			return true;
		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage message = realPacket.getMessage();
			if(message.getBooleanProperty(SignalKConstants.REPLY))return true;
			
			if (!Config.JSON_DELTA.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))
				return true;
			// if(logger.isDebugEnabled())logger.debug("Processing: " +
			// message);
			Json node = Util.readBodyBuffer(message);

			String sessionId = message.getStringProperty(Config.AMQ_SESSION_ID);
			String destination = message.getStringProperty(Config.AMQ_REPLY_Q);
			ServerSession s = ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
			
			// deal with diff format
			if (node.has(CONTEXT) && (node.has(GET))) {
				if (logger.isDebugEnabled())
					logger.debug("GET msg: " + node.toString());
				String ctx = node.at(CONTEXT).asString();
				String table = StringUtils.substringBefore(ctx,dot);
				//limit to explicit series
				if (!vessels.equals(table) 
					&& !CONFIG.equals(table) 
					&& !sources.equals(table) 
					&& !resources.equals(table)
					&& !aircraft.equals(table)
					&& !sar.equals(table)
					&& !aton.equals(table)){
					try{
						Util.sendReply(String.class.getSimpleName(),destination,FORMAT_FULL,Json.nil(),s);
						return true;
					} catch (Exception e) {
						logger.error(e, e);
						throw new ActiveMQException(ActiveMQExceptionType.INTERNAL_ERROR, e.getMessage(), e);
					}
				}
				String uuid = StringUtils.substringAfter(ctx,dot);
				try {
					NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
					for(Json p: node.at(GET).asJsonList()){
						String path = p.at(PATH).asString();
						StringBuffer sql=new StringBuffer();
						path=Util.regexPath(path).toString();
						switch (StringUtils.substringBefore(table, ".")) {
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
						default:
							sql.append("select * from "+table);
							if(StringUtils.isNotBlank(uuid) && StringUtils.isNotBlank(path))sql.append(" where skey=~/"+path+"/ and uuid=~/"+Util.regexPath(uuid).toString()+"/");
							if(StringUtils.isNotBlank(uuid) && StringUtils.isBlank(path))sql.append(" where uuid=~/"+Util.regexPath(uuid).toString()+"/");
							if(StringUtils.isBlank(uuid) && StringUtils.isNotBlank(path))sql.append(" where skey=~/"+path+"/");
							sql.append(" group by uuid, primary,skey,owner, grp order by time desc limit 1");
							influx.loadData(map, sql.toString(),"signalk");
						}
						
						if (logger.isDebugEnabled())
							logger.debug("GET sql : {}", sql);
					}
					
					if (logger.isDebugEnabled())
						logger.debug("GET map : {}", map);
					Json json = SignalkMapConvertor.mapToFull(map);
					
					
					Util.sendReply(map.getClass().getSimpleName(),destination,FORMAT_FULL,json,s);

					return true;
				} catch (Exception e) {
					logger.error(e, e);
					throw new ActiveMQException(ActiveMQExceptionType.INTERNAL_ERROR, e.getMessage(), e);
				}

			}
		}
		return true;

	}
	
	

}
