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
package nz.co.fortytwo.signalk.artemis.scheduled;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.tdb.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.tdb.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.MessageSupport;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Fired periodically. Sends the current time to internal.kv..
 * 
 * @author robert
 * 
 */
public class TimeUpdater extends MessageSupport implements Runnable {

	private static Logger logger = LogManager.getLogger(TimeUpdater.class);
	protected static TDBService influx = new InfluxDbService();
	private Json time = null;
	private ClientMessage txMsg = null;
	private String vesselUuid=Config.getConfigProperty(ConfigConstants.UUID);

	public TimeUpdater(){
		logger.debug("TimeUpdater created for vessel:{}",vesselUuid);
		time = Json.read("{\"value\": \"2018-07-17T23:50:16.000Z\",\"timestamp\": \"2018-07-17T23:50:16.000Z\",\"$source\": \"self.internal\"}");
	}
	private void initMessage() throws Exception {
		txMsg = getTxSession().createMessage(true);
		txMsg.putStringProperty(Config.MSG_SRC_BUS, "self.internal");
		txMsg.putStringProperty(Config.MSG_SRC_TYPE, Config.MSG_SRC_TYPE_INTERNAL_PROCESS);
		txMsg.putStringProperty(Config.AMQ_CONTENT_TYPE, Config.AMQ_CONTENT_TYPE_JSON);
		String token = SecurityUtils.authenticateUser("admin", "admin");
		txMsg.putStringProperty(Config.AMQ_USER_ROLES, SecurityUtils.getRoles(token).toString());
	}
	
	public void calculate() throws Exception {
		if(logger.isDebugEnabled())logger.debug("TimeUpdater fired ");
		String t = Util.getIsoTimeString();
		time.set("value", t);
		time.set("timestamp",t);
		if(logger.isDebugEnabled())logger.debug("The time for vessel:{} is {}",vesselUuid,t );
		if(txMsg==null)initMessage();
		sendKvMessage(txMsg, "vessels."+vesselUuid+".navigation.datetime", time);
		}
	

	@Override
	public void run() {
		try {
			calculate();
		} catch (Exception e) {
			logger.error(e,e);
		}
	}

}
