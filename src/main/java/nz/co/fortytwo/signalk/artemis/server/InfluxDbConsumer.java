package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;

import java.io.File;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.impl.TimeUtil;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class InfluxDbConsumer implements Runnable {

	private static Logger logger = LogManager.getLogger(InfluxDbConsumer.class);
	//private SortedMap<String, Json> map = new ConcurrentSkipListMap<>();
	private Thread t;
	private String user;
	private String password;
	
	private boolean running = true;
	private String address;
	private String queue;
	private long interval;
	private InfluxDB influxDB;
	private String dbName = "signalk";
	

	public InfluxDbConsumer(String user, String password, String queue, long interval) {
		this.interval = interval;
		this.queue = queue;
		this.user = user;
		this.password = password;

		influxDB = InfluxDBFactory.connect("http://localhost:8086", "root", "root");
		//influxDB.createDatabase(dbName);
		influxDB.setDatabase(dbName);
		String rpName = "aRetentionPolicy";
		//influxDB.createRetentionPolicy(rpName, dbName, "30d", "30m", 2, true);
		//influxDB.setRetentionPolicy(rpName);
		influxDB.enableBatch(BatchOptions.DEFAULTS);
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
					ClientConsumer consumer = rxSession.createConsumer(address, queue, false);
					try{
						while (running) {
						
							ClientMessage msgReceived = consumer.receive(10);
	
							if (msgReceived == null)
								break;
							// if we have changes, we read until there are more
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
							long millis = System.currentTimeMillis();
							if (StringUtils.isNotBlank(msgReceived.getStringProperty(timestamp))) {
								logger.debug("timestamp:" + msgReceived.getStringProperty(timestamp));
								obj.set(timestamp, msgReceived.getStringProperty(timestamp));
								millis=TimeUtil.fromInfluxDBTimeFormat(msgReceived.getStringProperty(timestamp));
							}
							//save the object to influxdb
							String[] path = msgReceived.getAddress().split("\\.");
							influxDB.write(Point.measurement(path[0])
									.time(millis, TimeUnit.MILLISECONDS)
									.tag("sourceRef", msgReceived.getStringProperty(sourceRef))
									.tag("uuid",path[1])
									.tag("key",String.join(".",ArrayUtils.subarray(path,2,path.length)) )
									//.addField("value", json.)
									.build());
						
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