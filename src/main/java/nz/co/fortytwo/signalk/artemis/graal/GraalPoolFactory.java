package nz.co.fortytwo.signalk.artemis.graal;

import java.io.IOException;
import java.io.InputStream;

import javax.script.ScriptEngine;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class GraalPoolFactory extends BasePooledObjectFactory<ContextHolder> {
	private static Logger logger = LogManager.getLogger(GraalPoolFactory.class);
	protected static Engine engine = Engine.newBuilder().build();
	private static Source bundle0183;
	private static Source bundleN2k;
	
    @Override
    public ContextHolder create() {
        try {
			return new ContextHolder(initEngine());
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

    private Context initEngine() throws IOException  {
		logger.info("create js context");
		
		//protected static GraalJSScriptEngine engine = (GraalJSScriptEngine) new GraalJSScriptEngine().getScriptEngine();
		
		Context context = Context.newBuilder("js").allowHostAccess(true).engine(engine).build();
	
		if(bundleN2k==null) {
			logger.info("Load n2kMapper: {}", "n2k-signalk/dist/bundle.js");
			bundleN2k = Source.newBuilder("js", Thread.currentThread().getContextClassLoader().getResource("n2k-signalk/dist/bundle.js")).build();
		}
		 Value n2kCtx = context.eval(bundleN2k);
		 if(logger.isDebugEnabled())logger.debug("n2kMapper: {}",n2kCtx.getMemberKeys());
		
		if(bundle0183==null) {
			logger.info("Load 0183 parser: {}", "signalk-parser-nmea0183/dist/bundle.js");
			bundle0183 = Source.newBuilder("js", Thread.currentThread().getContextClassLoader().getResource("signalk-parser-nmea0183/dist/bundle.js")).build();
		}
		 Value jsCtx = context.eval(bundle0183);
		 
		if(logger.isDebugEnabled())logger.debug("0183 Parser: {}",jsCtx.getMemberKeys());
		
		String hooks = IOUtils.toString(getIOStream("signalk-parser-nmea0183/hooks-es5/supported.txt"), Charsets.UTF_8);
		if(logger.isDebugEnabled())logger.debug("Hooks: {}",hooks);

		String[] files = hooks.split("\n");
		
		for (String f : files) {
			// seatalk breaks
//			if (f.startsWith("ALK"))
//				continue;
			if(logger.isDebugEnabled())logger.debug(f);
			context.getBindings("js").getMember("parser").invokeMember("loadHook", f.trim());
		}
		logger.info("js context complete");
		return context;
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