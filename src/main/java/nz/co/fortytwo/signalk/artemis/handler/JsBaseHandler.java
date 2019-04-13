package nz.co.fortytwo.signalk.artemis.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.graal.GraalPool;

public abstract class JsBaseHandler extends BaseHandler {
	
	private static Logger logger = LogManager.getLogger(JsBaseHandler.class);
	protected static String engineName;
	protected static GraalPool pool = new GraalPool();
	
	public JsBaseHandler() {
		logger.debug("Starting {} JS engine..", pool.getEngineName());
		
	}
	
}
