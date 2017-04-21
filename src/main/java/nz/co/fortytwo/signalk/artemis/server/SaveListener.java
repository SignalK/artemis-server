package nz.co.fortytwo.signalk.artemis.server;

import java.io.File;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class SaveListener implements Runnable {
	
	private static Logger logger = LogManager.getLogger(ArtemisServer.class);
	private static SortedMap<String, Json> map = new ConcurrentSkipListMap<>();
	private Thread t;
	private String user;
	private String password;
	private boolean saved = true;
	private boolean running = true;
	private String address;
	private String queue;
	private String fileName;
	private long interval;
	private long lastSave=0;

	public SaveListener(String user, String password, String address, String queue, String fileName, long interval) {
		this.interval=interval;
		this.fileName=fileName;
		this.queue=queue;
		this.address=address;
		this.user = user;
		this.password = password;
	}
	

	public void startSave() throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("Checking for "+queue+" listener..");
		if (t != null && t.isAlive())
			return;
		if (logger.isDebugEnabled())
			logger.debug("Starting "+queue+" listener..");
		t = new Thread(this);
		t.setDaemon(true);
		t.start();
	}

	public void stop() {
		// TODO Auto-generated method stub
		running=false;
	}

	@Override
	public void run() {
		ClientSession rxSession = null;
		ClientConsumer consumer = null;
		try {
			// start polling consumer.
			rxSession = Util.getVmSession(user, password);
			consumer = rxSession.createConsumer(address, "_AMQ_LVQ_NAME like '"+queue+".%'", true);

			while (running) {
				ClientMessage msgReceived = consumer.receive(1000);
				if ((System.currentTimeMillis()-lastSave)>interval){
					lastSave=System.currentTimeMillis();
					if(!saved){
						if (logger.isDebugEnabled())
							logger.debug(queue+" listener: saving "+queue);
						Config.saveMap(map,new File(fileName));
						saved=true;
					}
				}
				if(msgReceived==null)continue;
				//if we have changes, we read until there are more and trigger a save later.
				Json json = Util.readBodyBuffer(msgReceived);
				if (logger.isDebugEnabled())
					logger.debug("SaveListener: message = " + msgReceived.getMessageID() + ":" + msgReceived.getAddress() + ", " + json);
				map.put(msgReceived.getAddress().toString(), json);
				saved=false;
				
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