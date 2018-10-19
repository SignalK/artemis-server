package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.STATIC_DIR;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTH_COOKIE_NAME;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_API;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;

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
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.AtmosphereResourceSession;
import org.atmosphere.cpr.AtmosphereResourceSessionFactory;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
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
	
	private static final long PING_PERIOD = 5000;
	
	private static Timer timer = new Timer();

	@Context
	protected BroadcasterFactory broadCasterFactory;

	public BaseApiService() {
		super();
		staticDir = new File(Config.getConfigProperty(STATIC_DIR));
	}

	protected void initSession(String tempQ) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("initSession: {}", tempQ);
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
		if (logger.isDebugEnabled())
			logger.debug("Incoming msg: {}, {}", correlation, body);
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
				if (logger.isDebugEnabled()) {
					logger.debug("websocket.onThrowable: {}", event);
					event.throwable().printStackTrace();
				}

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
		if(logger.isDebugEnabled())
			logger.debug("Added close listener");
	}

	public boolean setConsumer(AtmosphereResource resource, boolean resumeAfter) throws ActiveMQException {

		if (getConsumer().getMessageHandler() == null) {
			if (logger.isDebugEnabled())
				logger.debug("Adding consumer messageHandler : {}", getTempQ());

			if (!resumeAfter) {
				resource.setBroadcaster(broadCasterFactory.get());
				logger.debug("Adding broadcaster");
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
			logger.debug("Set handler");
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

	protected void addWebsocketCloseListener(AtmosphereResource resource) {
		resource.addEventListener( new WebSocketEventListenerAdapter() {
			
			@Override
			public void onResume(AtmosphereResourceEvent event) {
				if (logger.isDebugEnabled()) 
					logger.debug("onResume: {}",event);
				super.onResume(event);
				try {
					resource.close();
				} catch (IOException e) {
					logger.error(e,e);
				}
				addCloseListener(resource);
		
			}
		});
	}
	
	protected void getPath(String path, Cookie cookie, String time) throws Exception {
		String correlation = java.util.UUID.randomUUID().toString();
		initSession(correlation);

		path=StringUtils.defaultIfBlank(path,"*");
		if (logger.isDebugEnabled())
			logger.debug("get raw: {}",path);
		
		path = StringUtils.removeStart(path,SIGNALK_API);
		path = StringUtils.removeStart(path,"/");
		path = path.replace('/', '.');

		
		// handle /vessels.* etc
		path = Util.fixSelfKey(path);
		if (logger.isDebugEnabled())
			logger.debug("get path: {}",path);
		//String jwtToken = (String) resource.getRequest().getAttribute(SignalKConstants.JWT_TOKEN);
		if (logger.isDebugEnabled()) {//
			logger.debug("JwtToken: {}", getToken(cookie));
		}
		if(StringUtils.isNotBlank(time)) {
			sendMessage(Util.getJsonGetSnapshotRequest(path,getToken(cookie), time).toString(),correlation);
		}else {
			sendMessage(Util.getJsonGetRequest(path,getToken(cookie)).toString(),correlation);
		}
		
	}
	
	protected String getWebsocket(AtmosphereResource resource, String body, Cookie cookie) {
		try {
			String correlationId = "stream-" + resource.uuid(); // UUID.randomUUID().toString();

			// resource.suspend();
			
			
			if (logger.isDebugEnabled())
				logger.debug("Correlation: {}, Post: {}", correlationId, body);
			
			initSession(correlationId);
			if(setConsumer(resource, false)) {
				addCloseListener(resource);
				setConnectionWatcher(resource, PING_PERIOD);
			}

			sendMessage(addToken(body, cookie), correlationId);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			try {
				resource.getResponse().sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error(e.getMessage(), e);
			}
		}
		return "";
	}

	protected void setConnectionWatcher(AtmosphereResource resource, long period) {
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				AtmosphereResourceSessionFactory factory = resource.getAtmosphereConfig().framework().sessionFactory();
				AtmosphereResourceSession session = factory.getSession(resource);
				Long lastPing = (Long) session.getAttribute("lastPing");

				if (logger.isDebugEnabled())logger.debug("Get lastPing {}={}", resource.uuid(),lastPing );
				try {
					ping(resource);
					
					if (logger.isDebugEnabled())
						logger.debug("Checking broadcast age < {}", period*3);
					if (lastPing!=null && System.currentTimeMillis() - lastPing > period * 3) {
						
							if (logger.isDebugEnabled())
								logger.debug("Checking ping failed: {} , closing...",
										System.currentTimeMillis() - lastPing);
							resource.close();
							cancel();
							timer.purge();
					}
				}catch (Exception e) {
					logger.debug(e,e);
					cancel();
					timer.purge();
				}
			}

			
		};
		if(logger.isDebugEnabled())
			logger.debug("Created connection watcher");
		timer.schedule(task, period, period);
		if(logger.isDebugEnabled())
			logger.debug("Scheduled connection watcher");
	}

	protected void ping(AtmosphereResource resource) {
		if (logger.isDebugEnabled())
			logger.debug("Sending a ping to {}", resource.uuid());
		// send a ping
			WebSocket ws = resource.getAtmosphereConfig().websocketFactory().find(resource.uuid());
			ws.sendPing("XX".getBytes());
		
	}
}