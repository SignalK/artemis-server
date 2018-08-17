package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.GET;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.util.Config;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;
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
 * Converts SignalK delta format to map format
 * 
 * @author robert
 * 
 */

public class DeltaMsgInterceptor extends BaseInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(DeltaMsgInterceptor.class);

	/**
	 * Reads Delta format JSON and inserts in the influxdb. Does nothing if json is
	 * not an update, and returns the original message
	 * 
	 * @param node
	 * @return
	 */

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if (isResponse(packet))
			return true;

		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage message = realPacket.getMessage();

			if (!Config.JSON_DELTA.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))
				return true;

			Json node = Util.readBodyBuffer(message);
			if (node.has(GET))
				return true;

			if (logger.isDebugEnabled())
				logger.debug("Delta msg: {}", node.toString());

			// deal with diff format
			if (isDelta(node)) {
				try {
					NavigableMap<String, Json> map = processDelta(node);
					if (!influx.getWrite()) {
						// set the time if we can
						// vessels.urn:mrn:signalk:uuid:80a3bcf0-d1a5-467e-9cd9-35c1760bb2d3.navigation.datetime.values.NMEA0183.SERIAL.value
						for (String key : map.keySet()) {
							if (key.startsWith(vessels_dot_self_dot + nav_datetime)) {
								Json time = map.get(key);
								if (time != null && !time.isNull()) {
									// set system time
									// sudo date -s 2018-08-11T17:52:51+12:00
									String cmd = "sudo date -s " + time.asString();
									logger.info("Executing date setting command:" + cmd);
									Runtime.getRuntime().exec(cmd.split(" "));

									logger.info("Executed date setting command:" + cmd);

									influx.setWrite(true);
								}
							}
						}
					}
					saveMap(map);
					return true;
				} catch (Exception e) {
					logger.error(e, e);
					throw new ActiveMQException(ActiveMQExceptionType.INTERNAL_ERROR, e.getMessage(), e);
				}
			}

		}
		return true;

	}

	protected NavigableMap<String, Json> processDelta(Json node) {
		if (logger.isDebugEnabled())
			logger.debug("Saving delta: {}", node.toString());
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		SignalkMapConvertor.parseDelta(node, map);
		return map;

	}

}
