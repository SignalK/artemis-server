package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.GET;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UNKNOWN;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.values;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.handler.MessageSupport;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class SignalkKvConvertor {

	private static Logger logger = LogManager.getLogger(SignalkKvConvertor.class);
	
	
	/**
	 * Convert Delta JSON to map. Returns null if the json is not an update,
	 * otherwise return a map
	 * 
	 * @param node
	 * @return
	 * @throws ActiveMQException 
	 * @throws Exception
	 */
	public static void parseDelta(MessageSupport sender,Message origMessage, Json node) throws ActiveMQException {

		if (node == null || node.isNull())
			return ;
		// avoid full signalk syntax
		if (node.has(vessels))
			return;

		if (Util.isDelta(node) && !Util.isSubscribe(node) && !node.has(GET)) {

			if (logger.isDebugEnabled())
				logger.debug("processing delta  {}", node);
			// process it

			// go to context
			String ctx = node.at(CONTEXT).asString();
			ctx = Util.fixSelfKey(ctx);
			ctx = StringUtils.removeEnd(ctx, ".");
			if (logger.isDebugEnabled())
				logger.debug("ctx: {}", node);

			if (node.has(UPDATES)) {
				for (Json update : node.at(UPDATES).asJsonList()) {
					parseUpdate(sender, origMessage,update, ctx);
				}
			}
			if (node.has(PUT)) {
				for (Json put : node.at(PUT).asJsonList()) {
					parsePut(sender, origMessage, put, ctx);
				}
			}
			if (node.has(CONFIG)) {
				for (Json update : node.at(CONFIG).asJsonList()) {
					parseUpdate(sender, origMessage, update, ctx);
				}
			}

		
		}
		return ;

	}

	protected static void parseUpdate(MessageSupport sender,  Message origMessage, Json update, String ctx) throws ActiveMQException {

		// grab values and add
		Json array = update.at(values);
		for (Json val : array.asJsonList()) {
			if (val == null || val.isNull() || !val.has(PATH))
				continue;

			Json e = val.dup();

			String key = dot + e.at(PATH).asString();
			if (key.equals(dot))
				key = "";
			// e.delAt(PATH);
			// temp.put(ctx+"."+key, e.at(value).getValue());
			String srcRef = SignalKConstants.UNKNOWN;
			if (update.has(sourceRef)) {
				srcRef = update.at(sourceRef).asString();
				e.set(sourceRef, srcRef);
			}

			if (update.has(timestamp)) {
				if (logger.isDebugEnabled())
					logger.debug("put timestamp: {}:{}", ctx + key, e);
				e.set(timestamp, update.at(timestamp).asString());
			} else {
				e.set(timestamp, Util.getIsoTimeString());
			}
			e.delAt(PATH);
			if (e.has(value)) {
				if (logger.isDebugEnabled())
					logger.debug("map.put: {}:{}", ctx + key, e);
				sender.sendKvMessage(origMessage,ctx + key + dot + values + dot + srcRef, e);
			}
		}

	}

	protected static void parsePut(MessageSupport sender, Message origMessage, Json put, String ctx) throws ActiveMQException {

		if (put == null || put.isNull() || !put.has(PATH))
			return;

		Json e = put.dup();

		String key = dot + e.at(PATH).asString();
		if (key.equals(dot))
			key = "";

		if (!e.has(sourceRef)) {
			e.set(sourceRef, UNKNOWN);
		}
		if (!e.has(timestamp)) {
			e.set(timestamp, Util.getIsoTimeString());
		}
		e.delAt(PATH);
		if (e.has(value)) {
			if (logger.isDebugEnabled())
				logger.debug("put: {}:{}", ctx + key, e);
			sender.sendKvMessage(origMessage,ctx + key + dot + values + dot + e.at(sourceRef).asString(), e);
		}

	}
}
