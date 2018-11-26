package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_INFLUX_KEY;
import static nz.co.fortytwo.signalk.artemis.util.Config.INTERNAL_KV;

import org.apache.activemq.artemis.api.core.Message;
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

public class InfluxDbHandler extends BaseHandler{
	
	private static Logger logger = LogManager.getLogger(InfluxDbHandler.class);
	
	public InfluxDbHandler() {
		super();
		try {
			initSession(null);
		} catch (Exception e) {
			logger.error(e,e);
		}
	}

	@Override
	public void consume(Message message) {
		logger.debug("Consuming {}",message);
			String key = message.getStringProperty(AMQ_INFLUX_KEY);
			Json node = Util.readBodyBuffer(message.toCore());
			
			if (logger.isDebugEnabled())
				logger.debug("Saving key: {} : {}", key, node);
			save(key, node);
			node.clear(true);
	}


	protected void save(String key, Json node) {
		influx.save(key, node.dup());
		
	}


	
	

}
