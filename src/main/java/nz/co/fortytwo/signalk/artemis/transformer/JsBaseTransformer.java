package nz.co.fortytwo.signalk.artemis.transformer;

import java.io.InputStream;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Context;

import nz.co.fortytwo.signalk.artemis.intercept.BaseInterceptor;

public class JsBaseTransformer extends BaseInterceptor {
	
	private static Logger logger = LogManager.getLogger(JsBaseTransformer.class);
	protected static GenericObjectPool<Context> pool = new GenericObjectPool<Context>(new JsPoolFactory());
	
	public JsBaseTransformer() {
		logger.debug("Starting Graal JS engine..");
		pool.setBlockWhenExhausted(true);
		pool.setMaxTotal(3);
	}
	
	protected static InputStream getIOStream(String path) {

		if(logger.isDebugEnabled())logger.debug("Return resource {}", path);
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

	}

	
}
