package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.Config.INCOMING_RAW;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
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
 * Converts SignalK delta format 'source' entry to 'sourceRef' 
 * 
 * @author robert
 * 
 */

public class FullSourceInterceptor extends BaseInterceptor implements Interceptor {

	
	private static Logger logger = LogManager.getLogger(FullSourceInterceptor.class);
	
	/**
	 * Reads full format JSON and converts source to $source. Does nothing if json
	 * is not full format, and returns the original message
	 * 
	 * @param node
	 * @return
	 */

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		
		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage message = realPacket.getMessage();
			if(!StringUtils.equals(message.getAddress(), INCOMING_RAW))return true;
			if (!Config.JSON_FULL.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))
				return true;
			
			String srcBus = message.getStringProperty(Config.MSG_SRC_BUS);
			String msgSrcType = message.getStringProperty(Config.MSG_SRC_TYPE);
			Json node = Util.readBodyBuffer(message);
			
			if (logger.isDebugEnabled())
				logger.debug("Delta msg: {}", node.toString());

			// deal with full format
			try {
				convertFullSrcToRef(node, srcBus,msgSrcType);
				message.getBodyBuffer().clear();
				message.getBodyBuffer().writeString(node.toString());
				node.clear(true);
				return true;
			} catch (Exception e) {
				logger.error(e, e);
				throw new ActiveMQException(ActiveMQExceptionType.INTERNAL_ERROR, e.getMessage(), e);
			}
		}
		return true;

	}

	

	
	
	
	

}
