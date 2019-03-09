package nz.co.fortytwo.signalk.artemis.transformer;

import java.lang.management.ManagementFactory;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.intercept.BaseInterceptor;

public class JsBaseTransformer extends BaseInterceptor {
	
	private static Logger logger = LogManager.getLogger(JsBaseTransformer.class);
	protected static String engineName;
	protected static GenericObjectPool<ContextHolder> pool ;
	static {
		try {
			List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
			for(String arg :args) {
				if(arg.contains("EnableJVMCI")) {
					pool = new GenericObjectPool<ContextHolder>(new GraalPoolFactory());
					engineName = GraalPoolFactory.class.getName();
				}
			}
			if(pool==null) {
				pool = new GenericObjectPool<ContextHolder>(new NashornPoolFactory());
				engineName = NashornPoolFactory.class.getName();
			}
			pool.setBlockWhenExhausted(true);
			pool.setMaxTotal(3);
			//preload at least one
			pool.addObject();
		} catch (Exception e) {
			logger.error(e,e);
		} 
		
	}
	
	public JsBaseTransformer() {
		logger.debug("Starting {} JS engine..", engineName);
		
	}
	
}
