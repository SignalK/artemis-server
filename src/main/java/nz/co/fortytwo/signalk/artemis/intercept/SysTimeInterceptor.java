package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.Config.INCOMING_RAW;
import static nz.co.fortytwo.signalk.artemis.util.Config.INTERNAL_KV;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_datetime;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.io.IOException;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
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
 * Reads key and if its self.navigation.datetime it sets the server time
 * 
 * @author robert
 * 
 */

public class SysTimeInterceptor extends BaseInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(SysTimeInterceptor.class);

	public SysTimeInterceptor() {
		super();
		if(Config.getConfigProperty(ConfigConstants.CLOCK_SOURCE).equals("system")) {
			influx.setWrite(true);
		}
	}

	/**
	 * Reads key and if its self.navigation.datetime it sets the server time and makes the influxdb writable.
	 * Used when the RPi has no rtc, and we get system time from GPS.
	 * 
	 * @param node
	 * @return
	 */

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if (influx.getWrite())return true;
		
		if (packet instanceof SessionSendMessage) {
			ICoreMessage message = ((SessionSendMessage) packet).getMessage();
			
			if(!StringUtils.equals(message.getAddress(), INTERNAL_KV))return true;
			String timeKey = vessels+dot+Config.getConfigProperty(ConfigConstants.UUID)+dot + nav_datetime;
			
			if (!timeKey.equals(message.getStringProperty(Config.AMQ_INFLUX_KEY)))
				return true;
			
				Json time = Util.readBodyBuffer(message);
				if (time != null && !time.isNull()) {
					// set system time
					// sudo date -s 2018-08-11T17:52:51+12:00
					String cmd = "sudo date -s " + time.at(value).asString();
					logger.info("Executing date setting command: {}", cmd);
					try {
						Runtime.getRuntime().exec(cmd.split(" "));
						logger.info("Executed date setting command: {}", cmd);
						influx.setWrite(true);
					} catch (IOException e) {
						logger.error(e,e);
					}
				}

		}
		return true;

	}


}
