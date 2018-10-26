package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.STATIC_DIR;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTH_COOKIE_NAME;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQIllegalStateException;
import org.apache.activemq.artemis.api.core.ActiveMQNonExistentQueueException;
import org.apache.activemq.artemis.api.core.ActiveMQQueueExistsException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.BroadcasterFactory;
import org.jgroups.util.UUID;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.subscription.SubscriptionManagerFactory;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseApiService {

	protected static ClientProducer producer;

	protected static ClientSession txSession;

	protected String tempQ;

	protected ClientConsumer consumer;

	private static Logger logger = LogManager.getLogger(BaseApiService.class);

	protected long lastBroadcast = System.currentTimeMillis();

	protected File staticDir = null;

	@Context
	protected BroadcasterFactory broadCasterFactory;

	public BaseApiService() {
		super();
		staticDir = new File(Config.getConfigProperty(STATIC_DIR));
	}

	protected void initSession(String tempQ) throws Exception {
		if (getTempQ() == null) {
			this.tempQ = tempQ;
		}
		getTxSession();
		getProducer();
		getConsumer();
	}

	protected File getLogOutputFile(String logFile) {
		File installLogDir = new File(staticDir, "logs");
		installLogDir.mkdirs();
		// make log name
		File output = new File(installLogDir, logFile);
		return output;
	}

	protected String getToken(HttpServletRequest req) {
		// assume we might have a cookie token
		for (javax.servlet.http.Cookie c : req.getCookies()) {
			if (logger.isDebugEnabled())
				logger.debug("Cookie: {}, {}", c.getName(), c.getValue());
			if (AUTH_COOKIE_NAME.equals(c.getName())) {
				return c.getValue();
			}
		}
		return null;
	}
	
	protected String addToken(String body, HttpServletRequest req) {
		String jwtToken = getToken(req);
		if(StringUtils.isNotBlank(jwtToken)){
			//add it to the body
			return Json.read(body).set(SignalKConstants.TOKEN, jwtToken).toString();
		}
		return body;
	}

	protected String getToken(Cookie cookie) {
		if (cookie == null)
			return null;
		// assume we might have a cookie token

		if (logger.isDebugEnabled())
			logger.debug("Cookie: {}, {}", cookie.getName(), cookie.getValue());
		if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
			return cookie.getValue();
		}

		return null;
	}

	protected String addToken(String body, Cookie cookie) {

		String jwtToken = getToken(cookie);
		if (StringUtils.isNotBlank(jwtToken)) {
			// add it to the body
			return Json.read(body).set(SignalKConstants.TOKEN, jwtToken).toString();
		}
		return body;
	}

	protected String sendMessage(String body) throws ActiveMQException {
		return sendMessage(body, UUID.randomUUID().toString());
	}

	protected String sendMessage(String body, String correlation) throws ActiveMQException {
		ClientMessage message = null;
		synchronized (txSession) {
			message = txSession.createMessage(false);
		}
		message.getBodyBuffer().writeString(body);
		message.putStringProperty(Config.AMQ_REPLY_Q, getTempQ());
		if (correlation != null) {
			message.putStringProperty(Config.AMQ_CORR_ID, correlation);
		}
		send(new SimpleString(Config.INCOMING_RAW), message);
		return correlation;
	}

	private synchronized void send(SimpleString queue, ClientMessage message) throws ActiveMQException {
		synchronized (txSession) {
			getProducer().send(queue, message);
		}

	}

	@Override
	protected void finalize() throws Throwable {
		closeSession();
		super.finalize();
	}

	protected void addCloseListener(AtmosphereResource resource) {

		resource.addEventListener(new AtmosphereResourceEventListenerAdapter() {

			@Override
			public void onThrowable(AtmosphereResourceEvent event) {
				try {
					event.getResource().close();
				} catch (NullPointerException e) {
					logger.error("NullPointerException: {}", e.getMessage());
				} catch (IllegalStateException | IOException e) {
					logger.error(e, e);
				}
				if (logger.isDebugEnabled())
					logger.debug("websocket.onThrowable: {}", event);

			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onDisconnect(AtmosphereResourceEvent event) {
				if (logger.isDebugEnabled())
					logger.debug("websocket.onDisconnect: {}", event);
				try {
					event.getResource().close();
				} catch (IllegalStateException | IOException e) {
					logger.error(e, e);
				}
				super.onDisconnect(event);
			}

			@Override
			public void onClose(AtmosphereResourceEvent event) {
				if (logger.isDebugEnabled())
					logger.debug("websocket.onClose: {}", event);
				closeSession();
				try {
					SubscriptionManagerFactory.getInstance().removeByTempQ(getTempQ());
				} catch (Exception e) {
					logger.error(e, e);
				}
				super.onClose(event);
			}

			@Override
			public void onBroadcast(AtmosphereResourceEvent event) {
				lastBroadcast = System.currentTimeMillis();
				if (logger.isDebugEnabled())
					logger.debug("websocket.onBroadcast: {}, {}", resource.uuid(), event);
				super.onBroadcast(event);
			}

			@Override
			public void onHeartbeat(AtmosphereResourceEvent event) {
				// lastBroadcast=System.currentTimeMillis();
				if (logger.isDebugEnabled())
					logger.debug("websocket.onHeartbeat, {}, {}", resource.uuid(), event);
				// super.onHeartbeat(event);
			}

		});

	}

	public boolean setConsumer(AtmosphereResource resource, boolean resumeAfter) throws ActiveMQException {

		if (getConsumer().getMessageHandler() == null) {
			if (logger.isDebugEnabled())
				logger.debug("Adding consumer messageHandler : {}", getTempQ());

			if (!resumeAfter) {
				resource.setBroadcaster(broadCasterFactory.get());
			}

			getConsumer().setMessageHandler(new MessageHandler() {

				@Override
				public void onMessage(ClientMessage message) {
					try {
						if (logger.isDebugEnabled())
							logger.debug("onMessage for client {}", message);
						String recv = Util.readBodyBufferToString(message);
						message.acknowledge();
						if (StringUtils.isBlank(recv))
							recv = "{}";

						if (logger.isDebugEnabled())
							logger.debug("onMessage for client at {}, {}", getTempQ(), recv);

						if (resumeAfter) {
							resource.write(recv);
							resource.resume();
						} else {
							resource.getBroadcaster().broadcast(recv, resource);
						}

						if (logger.isDebugEnabled())
							logger.debug("Sent to resource: {}", resource);

					} catch (ActiveMQException e) {
						logger.error(e, e);
					}
				}
			});
			return true;
		}
		return false;
	}

	private void closeSession() {

		if (getConsumer() != null) {
			if (logger.isDebugEnabled())
				logger.debug("Close consumer: {}", tempQ);
			try {
				synchronized (txSession) {
					getConsumer().close();
					try {
						if (txSession != null && !txSession.isClosed()
								&& txSession.queueQuery(new SimpleString(getTempQ())).getConsumerCount() == 0) {
							if (logger.isDebugEnabled())
								logger.debug("Delete queue: {}", tempQ);
							txSession.deleteQueue(getTempQ());
						}
					} catch (ActiveMQNonExistentQueueException | ActiveMQIllegalStateException e) {
						if (logger.isDebugEnabled())
							logger.debug(e.getMessage());
					}
				}
			} catch (ActiveMQException e) {
				logger.warn(e, e);
			}

		}

	}

	public ClientSession getTxSession() {

		if (txSession == null) {
			if (logger.isDebugEnabled())
				logger.debug("Start amq session: {}", getTempQ());
			try {
				txSession = Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
						Config.getConfigProperty(Config.ADMIN_PWD));
			} catch (Exception e) {
				logger.error(e, e);
			}
		}
		return txSession;
	}

	public ClientProducer getProducer() {
		if (producer == null && getTxSession() != null && !getTxSession().isClosed()) {
			if (logger.isDebugEnabled())
				logger.debug("Start producer: {}", getTempQ());
			try {
				synchronized (txSession) {
					producer = getTxSession().createProducer();
				}
			} catch (ActiveMQException e) {
				logger.error(e, e);
			}
		}
		return producer;

	}

	public ClientConsumer getConsumer() {

		if (consumer == null && getTxSession() != null && !getTxSession().isClosed()) {
			if (logger.isDebugEnabled())
				logger.debug("Start consumer: {}", getTempQ());
			try {
				try {
					synchronized (txSession) {
						getTxSession().createTemporaryQueue("outgoing.reply." + getTempQ(), RoutingType.ANYCAST,
								getTempQ());
					}
				} catch (ActiveMQQueueExistsException e) {
					if (logger.isDebugEnabled())
						logger.debug(e.getMessage());
				}
				synchronized (txSession) {
					consumer = getTxSession().createConsumer(getTempQ());
					getTxSession().start();
				}
			} catch (ActiveMQException e) {
				logger.error(e, e);
			}
		}
		return consumer;

	}

	public String getTempQ() {
		return tempQ;
	}

}