package nz.co.fortytwo.signalk.artemis.intercept;

import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.tdb.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.tdb.TDBService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.MessageSupport;

public class BaseInterceptor extends MessageSupport{
	private static Logger logger = LogManager.getLogger(BaseInterceptor.class);
	protected TDBService influx = new InfluxDbService();
	
	protected boolean ignoreMessage(String queue, String msgType, ICoreMessage message) {
		if (!StringUtils.equals(message.getAddress(), queue))
			return true;
		if (!msgType.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))
			return true;
		return false;
	}
}
