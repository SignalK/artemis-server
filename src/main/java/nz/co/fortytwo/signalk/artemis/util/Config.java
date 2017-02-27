package nz.co.fortytwo.signalk.artemis.util;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;

/**
 * Listen to the config.* queues and cache the results for easy access.
 * 
 * @author robert
 *
 */
public class Config {

	public static final String ADMIN_USER = "config.server.admin.user";
	public static final String ADMIN_PWD = "config.server.admin.password";

	private static Logger logger = LogManager.getLogger(Config.class);

	private static ConfigListener listener;
	private static SortedMap<String, Object> map = new ConcurrentSkipListMap<>();

	private static Config config = null;

	static {
		try {
			map = Util.loadConfig(map);
			config=new Config();
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	protected Config(){
		listener = new ConfigListener(map, (String) map.get(ADMIN_USER),
				(String) map.get(ADMIN_PWD));
		logger.info("Config listener started for user:" + map.get(ADMIN_USER));
	}
	public static Config getInstance() {
		return config;
	}

	private Map<String, Object> getMap() {
		return map;
	}

	public static String getConfigProperty(String prop) {
		try {
			return (String) config.getMap().get(prop);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static Json getConfigJsonArray(String prop) {
		try {
			String arrayStr = (String) config.getMap().get(prop);
			if (StringUtils.isNotBlank(arrayStr) && arrayStr.length() > 2) {
				Json array = Json.read(arrayStr);
				return array;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static Integer getConfigPropertyInt(String prop) {
		try {
			if (config.getMap().get(prop) instanceof String) {
				return (Integer.valueOf((String) config.getMap().get(prop)));
			}
			if (config.getMap().get(prop) instanceof Number) {
				return ((Number) config.getMap().get(prop)).intValue();
			}

			return (Integer) config.getMap().get(prop);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static Double getConfigPropertyDouble(String prop) {
		try {
			if (config.getMap().get(prop) instanceof String) {
				return (Double.valueOf((String) config.getMap().get(prop)));
			}
			return (Double) config.getMap().get(prop);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static Boolean getConfigPropertyBoolean(String prop) {
		try {
			if (config.getMap().get(prop) instanceof Boolean) {
				return ((Boolean) config.getMap().get(prop));
			}
			return new Boolean((String) config.getMap().get(prop));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	private class ConfigListener implements Runnable {

		private String user;
		private String password;

		public ConfigListener(SortedMap<String, Object> map, String user, String password) {
			this.user = user;
			this.password = password;
		}

		@Override
		public void run() {
			ClientSession rxSession = null;
			ClientConsumer consumer = null;
			try {
				// start polling consumer.
				rxSession = Util.getVmSession(user, password);
				consumer = rxSession.createConsumer("vessels", "_AMQ_LVQ_NAME like 'config.%'", true);

				while (true) {
					ClientMessage msgReceived = consumer.receive(100000);
					if (msgReceived == null)
						continue;
					if (logger.isDebugEnabled())
						logger.debug("message = " + msgReceived.getMessageID() + ":" + msgReceived.getAddress() + ", "
								+ msgReceived.getBodyBuffer().readString());
					map.put(msgReceived.getAddress().toString(), msgReceived.getBodyBuffer().readNullableString());
				}

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (consumer != null) {
					try {
						consumer.close();
					} catch (ActiveMQException e) {
						logger.error(e);
					}
				}
				if (rxSession != null) {
					try {
						rxSession.close();
					} catch (ActiveMQException e) {
						logger.error(e);
					}
				}
			}

		}
	}

}
