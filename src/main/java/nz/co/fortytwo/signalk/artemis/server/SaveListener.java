package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;

import java.io.File;
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
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class SaveListener implements Runnable {

	private static Logger logger = LogManager.getLogger(SaveListener.class);
	private SortedMap<String, Json> map = new ConcurrentSkipListMap<>();
	private Thread t;
	private String user;
	private String password;
	
	private boolean running = true;
	private String address;
	private String queue;
	private String fileName;
	private long interval;
	private boolean save=false;
	

	public SaveListener(String user, String password, String address, String queue, String fileName, long interval) {
		this.interval = interval;
		this.fileName = fileName;
		this.queue = queue;
		this.address = address;
		this.user = user;
		this.password = password;
	}

	public void startSave() throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("Checking for " + queue + " listener..");
		if (t != null && t.isAlive())
			return;
		if (logger.isDebugEnabled())
			logger.debug("Starting " + queue + " listener..");
		t = new Thread(this);
		t.setDaemon(true);
		t.start();
	}

	public void stop() {
		// TODO Auto-generated method stub
		running = false;
	}

	@Override
	public void run() {
		ClientSession rxSession = null;

		try {
			// start polling consumer.
			rxSession = Util.getVmSession(user, password);
			rxSession.start();

			while (running) {
				try {
					Thread.sleep(interval);
					ClientConsumer consumer = rxSession.createConsumer(address,
							"_AMQ_LVQ_NAME = '" + queue + "' or _AMQ_LVQ_NAME like '" + queue + ".%'", true);
					try{
						while (running) {
						
							ClientMessage msgReceived = consumer.receive(10);
	
							if (msgReceived == null)
								break;
							// if we have changes, we read until there are more and
							// trigger a save later.
							Json json = Util.readBodyBuffer(msgReceived);
							if (logger.isDebugEnabled())
								logger.debug(queue + " listener: message = " + msgReceived.getMessageID() + ":"
										+ msgReceived.getAddress() + ", " + json);
							Json obj = null;
							if (json.isPrimitive()||json.isNull()) {
								obj = Json.object().set(value, json);
							} else {
								obj = json;
							}
							if (StringUtils.isNotBlank(msgReceived.getStringProperty(source))) {
								logger.debug("source:" + msgReceived.getStringProperty(source));
								obj.set(source, msgReceived.getStringProperty(source));
							}
	
							if (StringUtils.isNotBlank(msgReceived.getStringProperty(sourceRef))) {
								logger.debug("sourceRef:" + msgReceived.getStringProperty(sourceRef));
								obj.set(sourceRef, msgReceived.getStringProperty(sourceRef));
							}
							if (StringUtils.isNotBlank(msgReceived.getStringProperty(timestamp))) {
								logger.debug("timestamp:" + msgReceived.getStringProperty(timestamp));
								obj.set(timestamp, msgReceived.getStringProperty(timestamp));
							}
							Json prev = map.put(msgReceived.getAddress().toString(), obj);
							if(!obj.equals(prev))save=true;
						
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}finally {
						if (consumer != null) {
							try {
								consumer.close();
							} catch (ActiveMQException e) {
								logger.error(e);
							}
						}
					}

					
					if(map.size()>0 && save){
						if (logger.isDebugEnabled()) {
							logger.debug(queue + " listener: saving " + queue);
							logger.debug(" contents: " + map);
						}
						Config.saveMap(map, new File(fileName));
						save=false;
					}

				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}

			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			
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