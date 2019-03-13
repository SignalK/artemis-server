package nz.co.fortytwo.signalk.artemis.util;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.GET;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POST;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UNKNOWN;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.meta;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.values;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.util.Map.Entry;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;

public class SignalkKvConvertor {

	private static Logger logger = LogManager.getLogger(SignalkKvConvertor.class);

	public static void parseFull(MessageSupport sender, Message origMessage, Json json, String prefix)
			throws ActiveMQException {

		if (json == null || json.isNull())
			return;

		for (Entry<String, Json> entry : json.asJsonMap().entrySet()) {

			String key = entry.getKey();
			Json val = entry.getValue();

			if (logger.isDebugEnabled())
				logger.debug("Recurse {} = {}", () -> key, () -> val);
			// primitive we write out
			if (val.isPrimitive() || val.isNull() || val.isArray()) {
				sender.sendKvMessage(origMessage, prefix + key, val.dup());
				continue;
			}
			// value object we save in .values.sourceref.
			if (val.has(value)) {
				String srcRef = null;
				Json tmpVal = Json.object(value, val.at(value));
				if (val.has(sourceRef)) {
					srcRef = val.at(sourceRef).asString();
					tmpVal.set(sourceRef, srcRef);
				} else {
					srcRef = UNKNOWN;
					tmpVal.set(sourceRef, srcRef);
				}

				if (val.has(timestamp)) {
					if (logger.isDebugEnabled())
						logger.debug("put timestamp: {}:{}", key, val);
					tmpVal.set(timestamp, val.at(timestamp).asString());
				} else {
					tmpVal.set(timestamp, Util.getIsoTimeString());
				}
				if (prefix.contains(dot + values)) {
					sender.sendKvMessage(origMessage, prefix + key, val.dup());
					if (logger.isDebugEnabled())
						logger.debug("put: {}:{}", prefix + key, val);
				} else {
					sender.sendKvMessage(origMessage, prefix + key + dot + values + dot + srcRef, tmpVal.dup());
					if (logger.isDebugEnabled())
						logger.debug("put: {}:{}", prefix + key + dot + values + dot + srcRef, tmpVal.dup());
				}
				// sourceRef is wrong for meta
				if (val.has(meta))
					parseFull(sender, origMessage, val.at(meta),
							prefix + key + dot + values + dot + srcRef + dot + meta + dot);
				if (val.has(values))
					parseFull(sender, origMessage, val.at(values), prefix + key + dot + values + dot);
				continue;
			}

			parseFull(sender, origMessage, val, prefix + key + ".");

		}

	}

	/**
	 * Convert Delta JSON to kv and send to kv queue.
	 * 
	 * @param node
	 * @return
	 * @throws ActiveMQException
	 * @throws Exception
	 */
	public static void parseDelta(MessageSupport sender, Message origMessage, Json node) throws Exception {

		if (node == null || node.isNull())
			return;
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
					parseUpdate(sender, origMessage, update, ctx);
				}
			}
			if (node.has(POST)) {
				for (Json post : node.at(POST).asJsonList()) {
					parsePost(sender, origMessage, post, ctx);
				}
			}
			if (node.has(PUT)) {
				for (Json put : node.at(PUT).asJsonList()) {
					parsePut(sender, origMessage, put, ctx);
				}
			}
			if (node.has(CONFIG)) {
				for (Json update : node.at(CONFIG).asJsonList()) {
					parseUpdate(sender, origMessage, update, CONFIG);
				}
			}

		}
		return;

	}

	protected static void parseUpdate(MessageSupport sender, Message origMessage, Json update, String ctx)
			throws ActiveMQException {

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
				sender.sendKvMessage(origMessage, ctx + key + dot + values + dot + srcRef, e);
			}
		}

	}

	protected static void parsePost(MessageSupport sender, Message origMessage, Json post, String ctx)
			throws Exception {

		if (post == null || post.isNull() || !post.has(PATH))
			return;

		if (!Util.checkPostValid(ctx +post.at(PATH).asString())) {
			String correlation = origMessage.getStringProperty(Config.AMQ_CORR_ID);
			String destination = origMessage.getStringProperty(Config.AMQ_REPLY_Q);
			Json err = sender.error(sender.getRequestId(post), "COMPLETED", 403,
					"Cannot POST to " + post.at(PATH).asString());
			sender.sendReply(destination, FORMAT_DELTA, correlation, err, null);
			return;
		}
		//add uuid
		post.set(PATH, post.at(PATH)+dot+UUID.randomUUID().toString());
		parsePut(sender,origMessage, post,ctx);
	}

	protected static void parsePut(MessageSupport sender, Message origMessage, Json put, String ctx) throws Exception {

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
			sender.sendKvMessage(origMessage, ctx + key + dot + values + dot + e.at(sourceRef).asString(), e);
		}

	}
}
