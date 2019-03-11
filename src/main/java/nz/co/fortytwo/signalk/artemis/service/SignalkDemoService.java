package nz.co.fortytwo.signalk.artemis.service;

import java.io.File;
import java.io.IOException;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.MessageSupport;

public class SignalkDemoService extends MessageSupport implements Runnable {

	private static Logger logger = LogManager.getLogger(SignalkDemoService.class);
	private int c;
	private long start;

	public SignalkDemoService() throws Exception {
	}

	@Override
	public void initSession() throws Exception {
		try {
			super.initSession();

			
		} catch (Exception e) {
			logger.error(e, e);
			throw e;
		}
	}

	private void processDemo(File streamFile, int delay) throws IOException, ActiveMQException, InterruptedException {
		LineIterator itr = FileUtils.lineIterator(streamFile);
		while (itr.hasNext()) {
			ClientMessage txMsg = getTxSession().createMessage(true);
			txMsg.getBodyBuffer().writeString(itr.next());
			txMsg.putStringProperty(Config.MSG_SRC_BUS, "/dev/DEMO");
			txMsg.putStringProperty(Config.MSG_SRC_TYPE, Config.MSG_SRC_TYPE_SERIAL);
			getProducer().send(new SimpleString(Config.INCOMING_RAW), txMsg);
			c++;
			if(delay>0) {
			  Thread.currentThread().sleep(delay);
			}
			if(c%10000==0) {
				logger.info("Processed {} messages/sec",((double)c)/((double)(System.currentTimeMillis()-start)/1000));
				start=System.currentTimeMillis();
				c=0;
			}
		}
	}

	@Override
	public void run() {
		
		if(!Config.getConfigPropertyBoolean(ConfigConstants.DEMO))return;
		start=System.currentTimeMillis();
		String streamUrl = Config.getConfigProperty(ConfigConstants.STREAM_URL);
		logger.info("Starting demo {}..",streamUrl);
		
		File streamFile = new File(streamUrl);
		if(!streamFile.exists()) {
			logger.error("Demo file does not exist: {}",streamUrl);
			return;
		}
		
		
		int delay=Config.getConfigPropertyInt(ConfigConstants.DEMO_DELAY);
		
		while (Config.getConfigPropertyBoolean(ConfigConstants.DEMO)) {
			
			try {
				processDemo(streamFile, delay);
			} catch (Exception e) {
				logger.error(e,e);
			} 
			
			logger.info("Finished demo file, restarting..");
		}

		
	}

}
