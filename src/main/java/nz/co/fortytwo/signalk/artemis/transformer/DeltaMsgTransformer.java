package nz.co.fortytwo.signalk.artemis.transformer;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.JSON_DELTA;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.intercept.BaseInterceptor;
import nz.co.fortytwo.signalk.artemis.service.SignalkKvConvertor;
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

public class DeltaMsgTransformer extends BaseInterceptor implements Transformer {

	private static Logger logger = LogManager.getLogger(DeltaMsgTransformer.class);

	/**
	 * Reads Delta format JSON and creates a kv message pairs. Does nothing if json
	 * is not an update, and returns the original message
	 * 
	 * @param node
	 * @return
	 */
	@Override
	public Message transform(Message message) {

		// is this s delta message
		if (!JSON_DELTA.equals(message.getStringProperty(AMQ_CONTENT_TYPE)))
			return message;

		Json node = Util.readBodyBuffer(message.toCore());

		if (logger.isDebugEnabled())
			logger.debug("Delta msg: {}", node.toString());

		// deal with diff format
		if (isDelta(node)) {
			// try {
			try {
				processDelta(message, node);
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}

		}
		return message;
	}

	protected void processDelta(Message message, Json node) throws ActiveMQException {
		if (logger.isDebugEnabled())
			logger.debug("Saving delta: {}", node.toString());
	
		SignalkKvConvertor.parseDelta(this,message,node);
		
		node.clear(true);
	}

}
