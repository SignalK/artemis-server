package nz.co.fortytwo.signalk.artemis.graal;

import java.lang.management.ManagementFactory;
import java.util.List;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GraalPool extends GenericObjectPool<ContextHolder> {

	private static Logger logger = LogManager.getLogger(GraalPool.class);
	protected static String engineName;
	public static String getEngineName() {
		return engineName;
	}
	protected static BasePooledObjectFactory<ContextHolder> factory;
	//protected static GenericObjectPool<ContextHolder> pool ;
	static {
		try {
			List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
			for(String arg :args) {
				if(arg.contains("EnableJVMCI")) {
					logger.info("Starting Graal polyglot engine");
					factory=new GraalPoolFactory();
				}
			}
			if(factory==null) {
				logger.info("Starting Nashorn javascript engine");
				factory=new NashornPoolFactory();
			}
			engineName = factory.getClass().getName();
		} catch (Exception e) {
			logger.error(e,e);
		} 
		
	}

	public GraalPool(){
		super(factory);
		setMaxTotal(3);
		try {
			addObject();
		} catch (Exception e) {
			logger.error(e,e);
		}
	}
	public GraalPool(PooledObjectFactory<ContextHolder> factory) {
		super(factory);
	}
}
