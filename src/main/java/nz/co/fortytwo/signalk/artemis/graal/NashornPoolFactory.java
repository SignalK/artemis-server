package nz.co.fortytwo.signalk.artemis.graal;

import java.io.IOException;
import java.io.InputStream;

import javax.script.Invocable;
import javax.script.ScriptException;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;


public class NashornPoolFactory extends BasePooledObjectFactory<ContextHolder> {
	private static Logger logger = LogManager.getLogger(NashornPoolFactory.class);
	protected static NashornScriptEngine engine = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine();
	
	
    @Override
    public ContextHolder create() {
        try {
			return new ContextHolder((Invocable)engine, initEngine().createBindings());
		} catch (Exception e) {
			logger.error(e,e);
			return null;
		}
    }

    /**
     * Use the default PooledObject implementation.
     */
    @Override
    public PooledObject<ContextHolder> wrap(ContextHolder context) {
        return new DefaultPooledObject<ContextHolder>(context);
    }

    /**
     * When an object is returned to the pool, clear the buffer.
     */
    @Override
    public void passivateObject(PooledObject<ContextHolder> pooledObject) {
        //pooledObject.getObject().leave();
    }

    private NashornScriptEngine initEngine() throws IOException, NoSuchMethodException, ScriptException  {
		logger.info("Create js context");
		if(logger.isDebugEnabled())logger.debug("Load parser: {}", "signalk-parser-nmea0183/dist/bundle.js");
		engine.eval(IOUtils.toString(getIOStream("signalk-parser-nmea0183/dist/bundle.js")));
		 
		if(logger.isDebugEnabled())logger.debug("Parser: {}",engine.get("parser"));
		
		String hooks = IOUtils.toString(getIOStream("signalk-parser-nmea0183/hooks-es5/supported.txt"), Charsets.UTF_8);
		if(logger.isDebugEnabled())logger.debug("Hooks: {}",hooks);

		String[] files = hooks.split("\n");
		
		for (String f : files) {
			// seatalk breaks
//			if (f.startsWith("ALK"))
//				continue;
			if(logger.isDebugEnabled())logger.debug(f);
			engine.invokeMethod(engine.get("parser"), "loadHook", f.trim());
		}
		return engine;
	}

    @Override
	public void destroyObject(PooledObject<ContextHolder> p) throws Exception {
		//p.getObject().close();
		super.destroyObject(p);
	}

	private static InputStream getIOStream(String path) {

		if(logger.isDebugEnabled())logger.debug("Return resource {}", path);
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

	}

    // for all other methods, the no-op implementation
    // in BasePooledObjectFactory will suffice
}