package nz.co.fortytwo.signalk.artemis.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.coveo.nashorn_modules.FilesystemFolder;
import com.coveo.nashorn_modules.Folder;
import com.coveo.nashorn_modules.Require;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class NMEAParserTest {

	private static Logger logger = LogManager.getLogger(NMEAParserTest.class);
	
	@SuppressWarnings("restriction")
	private NashornScriptEngine engine;
	private File jsRoot;
	private boolean rmcClock = false;

	private static ScheduledExecutorService globalScheduledThreadPool = Executors.newScheduledThreadPool(20);

	@SuppressWarnings("restriction")
	public NMEAParserTest() throws ScriptException {
		super();
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		//console.error
	    engine = (NashornScriptEngine) factory.getScriptEngine(new String[] { "--language=es6" });
	    
	 // Injection of __NASHORN_POLYFILL_TIMER__ in ScriptContext
	    engine.getContext().setAttribute("__NASHORN_POLYFILL_TIMER__", globalScheduledThreadPool, ScriptContext.ENGINE_SCOPE);

	    jsRoot = new File(getClass().getClassLoader().getResource("signalk-parser-nmea0183/index-es5.js").getPath()).getParentFile();
	    logger.debug("Javascript jsRoot: {}",jsRoot.getAbsolutePath());
	    Folder rootFolder = FilesystemFolder.create(jsRoot, "UTF-8");
		Require.enable(engine, rootFolder);
		logger.debug("Starting nashorn");
	}
	@Test
	public void test() throws FileNotFoundException, ScriptException, NoSuchMethodException {
		try{
			logger.debug("Starting test");
		engine.eval(new java.io.FileReader(new File(jsRoot.getParent(),"parser.js")));
        

        // expose object defined in the script to the Java application
      //  Object parser = engine.get("parser");

        // create an Invocable object by casting the script engine object
        Invocable inv = (Invocable) engine;
        
        File dir =new File(jsRoot,"hooks-es5");
        for(File f:dir.listFiles()){
        	if(!f.isFile())continue;
        	if(!f.getName().endsWith(".js"))continue;
        	//seatalk breaks
        	if(f.getName().startsWith("ALK"))continue;
        	logger.debug(f.getName());
        	inv.invokeFunction("loadHook",StringUtils.substringBefore(f.getName(),".") );
        }
        /*
        RMC Sentence
        http://www.gpsinformation.org/dale/nmea.htm#RMC
        $GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
        values:
         -      RMC          Recommended Minimum sentence C
        [0]     123519       Fix taken at 12:35:19 UTC
        [1]     A            Status A=active or V=Void.
        [2][3]  4807.038,N   Latitude 48 deg 07.038' N
        [4][5]  01131.000,E  Longitude 11 deg 31.000' E
        [6]     022.4        Speed over the ground in knots
        [7]     084.4        Track angle in degrees True
        [8]     230394       Date - 23rd of March 1994
        [9][10] 003.1,W      Magnetic Variation
         -      *6A          The checksum data, always begins with *
        */
        String rmc = new String("$GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A");
       // String input = new String("{\"id\":\"RMC\",\"sentence\":\"xx\", \"parts\": [123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W],\"tags\":{\"source\":\"xx\"}}");
        Object result = inv.invokeFunction("parse",rmc );
        
        logger.debug("Output1: "+result);
        
        rmc = new String("$GPRMC,123525,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A\n");
        // String input = new String("{\"id\":\"RMC\",\"sentence\":\"xx\", \"parts\": [123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W],\"tags\":{\"source\":\"xx\"}}");
         result = inv.invokeFunction("parse",rmc );
         
         logger.debug("Output2: "+result);
         
          rmc = new String("!AIVDM,1,1,,A,13aEOK?P00PD2wVMdLDRhgvL289?,0*26\n");
         // String input = new String("{\"id\":\"RMC\",\"sentence\":\"xx\", \"parts\": [123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W],\"tags\":{\"source\":\"xx\"}}");
           result = inv.invokeFunction("parse",rmc );
          
          logger.debug("Output3: "+result);
        // invoke the method named "hello" on the object defined in the script
        // with "Script Method!" as the argument
      //  inv.invokeMethod(parser, "parse", "$SDDBT,17.0,f,5.1,M,2.8,F*3E");
		}catch(Exception e){
			logger.error(e,e);
		}
	}

}
