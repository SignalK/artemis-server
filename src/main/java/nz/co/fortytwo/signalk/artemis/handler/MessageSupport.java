package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.Config.ADMIN_PWD;
import static nz.co.fortytwo.signalk.artemis.util.Config.ADMIN_USER;
import static nz.co.fortytwo.signalk.artemis.util.Config.INCOMING_RAW;
import static nz.co.fortytwo.signalk.artemis.util.Config.INTERNAL_KV;
import static nz.co.fortytwo.signalk.artemis.util.Config.OUTGOING_REPLY;
import static nz.co.fortytwo.signalk.artemis.util.Config.getConfigProperty;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self_str;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.version;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.core.message.impl.CoreMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class MessageSupport {

	private static Logger logger = LogManager.getLogger(MessageSupport.class);

	protected ThreadLocal<ClientProducer> producer = new ThreadLocal<>();

	protected ThreadLocal<ClientSession> txSession = new ThreadLocal<>();

	// protected ClientConsumer consumer;

	// protected static TDBService influx = new InfluxDbService();

	protected String uuid;

	public MessageSupport() {
		uuid = Config.getConfigProperty(ConfigConstants.UUID);
	}

	public void initSession() throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("initSession: {}");
		getTxSession();
		getProducer();

	}

	protected void send(Message message, String key, double d) throws ActiveMQException {
		if (logger.isDebugEnabled())
			logger.debug("Sending: key: {}, value: {}", key, d);
		Json json = Json.object().set(value, d).set(timestamp, Util.getIsoTimeString());
		if (logger.isDebugEnabled())
			logger.debug("Sending: key: {}, value: {}", key, json);
		sendKvMessage(message, key, json);

	}

	protected void sendJson(Message message, String key, Json json) throws ActiveMQException {
		if (!json.isNull()) {
			json.set(timestamp, Util.getIsoTimeString());
		}
		if (logger.isDebugEnabled())
			logger.debug("Sending json: key: {}, value: {}", key, json);

		sendKvMessage(message, key, json);

	}

	protected void sendKvJson(Message message, Json json) {
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<String, Json>();
		SignalkMapConvertor.parseDelta(json, map);
		sendKvMap(message, map);

	}

	public void sendKvMap(Message message, NavigableMap<String, Json> map) {
		// remove "self" and "version"
		map.remove(self_str);
		map.remove(version);

		map.forEach((k, j) -> {
			try {
				sendKvMessage(message, k, j);
			} catch (Exception e) {
				logger.error(e, e);
			}
		});

	}

	public void sendReply(String destination, String format, String correlation, Json json, String token)
			throws Exception {

		if (json == null || json.isNull())
			json = Json.object();

		ClientMessage txMsg = getTxSession().createMessage(false);

		// txMsg.putStringProperty(Config.JAVA_TYPE, type);
		if (correlation != null)
			txMsg.putStringProperty(Config.AMQ_CORR_ID, correlation);
		if (token != null)
			txMsg.putStringProperty(Config.AMQ_USER_TOKEN, token);
		txMsg.putStringProperty(Config.AMQ_SUB_DESTINATION, destination);
		txMsg.putBooleanProperty(Config.SK_SEND_TO_ALL, false);
		txMsg.putStringProperty(SignalKConstants.FORMAT, format);
		txMsg.putBooleanProperty(SignalKConstants.REPLY, true);
		txMsg.putStringProperty(Config.AMQ_CORR_ID, correlation);
		txMsg.setExpiration(System.currentTimeMillis() + 5000);
		txMsg.getBodyBuffer().writeString(json.toString());
		if (logger.isDebugEnabled())
			logger.debug("Destination: {}, Msg body = {}", OUTGOING_REPLY+dot +destination, json.toString());
		getProducer().send(OUTGOING_REPLY+dot + destination, txMsg);

	}

	protected String sendMessage(String queue, String body, String correlation, String jwtToken) throws ActiveMQException {
		if (logger.isDebugEnabled())
			logger.debug("Incoming msg: {}, {}", correlation, body);
		ClientMessage message = null;

		message = getTxSession().createMessage(false);

		message.getBodyBuffer().writeString(body);
		message.putStringProperty(Config.AMQ_REPLY_Q, queue);
		if (correlation != null) {
			message.putStringProperty(Config.AMQ_CORR_ID, correlation);
		}
		if (jwtToken != null) {
			message.putStringProperty(Config.AMQ_USER_TOKEN, jwtToken);
		}
		send(new SimpleString(Config.INCOMING_RAW), message);
		return correlation;
	}
	
	private void send(SimpleString queue, ClientMessage message) throws ActiveMQException {
		getProducer().send(queue, message);
	
	}

	protected void sendRawMessage(Message origMessage, String body) throws ActiveMQException {
		ClientMessage txMsg = getTxSession().createMessage(false);
		txMsg.copyHeadersAndProperties(origMessage);

		txMsg.removeProperty(Config.AMQ_CONTENT_TYPE);

		txMsg.setExpiration(System.currentTimeMillis() + 5000);
		txMsg.getBodyBuffer().writeString(body);
		if (logger.isDebugEnabled())
			logger.debug("Msg body incoming.raw: {}", body);

		getProducer().send(Config.INCOMING_RAW, txMsg);

	}

	public void sendKvMessage(Message origMessage, String k, Json j) throws ActiveMQException {
		ClientMessage txMsg = getTxSession().createMessage(false);
		txMsg.copyHeadersAndProperties(origMessage);

		txMsg.putStringProperty(Config.AMQ_INFLUX_KEY, k);
		txMsg.setRoutingType(RoutingType.MULTICAST);
		txMsg.setExpiration(System.currentTimeMillis() + 5000);
		txMsg.getBodyBuffer().writeString(j.toString());
		if (logger.isDebugEnabled())
			logger.debug("Msg body signalk.kv: {} = {}", k, j.toString());

		getProducer().send(Config.INTERNAL_KV, txMsg);

	}

	public ClientSession getTxSession() {

		if (txSession == null)
			txSession = new ThreadLocal<>();

		if (txSession.get() == null) {
			if (logger.isDebugEnabled())
				logger.debug("Start amq session");
			try {
				txSession.set(Util.getVmSession(getConfigProperty(ADMIN_USER), getConfigProperty(ADMIN_PWD)));
			} catch (Exception e) {
				logger.error(e, e);
			}
		}
		return txSession.get();
	}

	public ClientProducer getProducer() {

		if (producer == null)
			producer = new ThreadLocal<>();

		if (producer.get() == null && getTxSession() != null && !getTxSession().isClosed()) {
			if (logger.isDebugEnabled())
				logger.debug("Start producer");

			try {
				producer.set(getTxSession().createProducer());
			} catch (ActiveMQException e) {
				logger.error(e, e);
			}

		}
		return producer.get();

	}

	public void stop() {

		if (producer!=null && producer.get() != null) {
			try {
				producer.get().close();
			} catch (ActiveMQException e) {
				logger.error(e, e);
			}
		}
		if (txSession!=null && txSession.get() != null) {
			try {
				txSession.get().close();
			} catch (ActiveMQException e) {
				logger.error(e, e);
			}
		}

	}

	protected void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
