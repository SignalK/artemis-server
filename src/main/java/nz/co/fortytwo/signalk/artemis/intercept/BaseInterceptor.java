package nz.co.fortytwo.signalk.artemis.intercept;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.tdb.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.tdb.TDBService;
import nz.co.fortytwo.signalk.artemis.util.MessageSupport;

public class BaseInterceptor extends MessageSupport{
	private static Logger logger = LogManager.getLogger(BaseInterceptor.class);
	protected static TDBService influx = new InfluxDbService();
	
}
