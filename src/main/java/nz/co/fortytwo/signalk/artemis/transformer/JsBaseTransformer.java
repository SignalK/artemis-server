package nz.co.fortytwo.signalk.artemis.transformer;

import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.intercept.BaseInterceptor;



//@SuppressWarnings("restriction")
public class JsBaseTransformer extends BaseInterceptor {
	
	private static Logger logger = LogManager.getLogger(JsBaseTransformer.class);
	
	public JsBaseTransformer() {
		logger.debug("Starting Graal JS engine..");
	}
	
	protected static InputStream getIOStream(String path) {

		if(logger.isDebugEnabled())logger.debug("Return resource {}", path);
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

	}

	
}
