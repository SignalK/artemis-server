package nz.co.fortytwo.signalk.artemis.transformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.graal.GraalPool;
import nz.co.fortytwo.signalk.artemis.intercept.BaseInterceptor;

public class JsBaseTransformer extends BaseInterceptor {
	
	private static Logger logger = LogManager.getLogger(JsBaseTransformer.class);
	protected static String engineName;
	protected static GraalPool pool = new GraalPool();
	
	public JsBaseTransformer() {
		logger.debug("Starting {} JS engine..", pool.getEngineName());
		
	}
	
}
