package nz.co.fortytwo.signalk.artemis.transformer;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.message.impl.CoreMessage;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.intercept.BaseMsgInterceptorTest;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.util.Config;

public class FullMsgTransformerTest  extends BaseMsgInterceptorTest {

	private static Logger logger = LogManager.getLogger(FullMsgTransformerTest.class);
	private Json full;
	private Json config;
	private Json delta;
	
	
	@Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    private FullMsgTransformer transformer;// 1
    
    public FullMsgTransformerTest() {
		try {
			delta=Json.read(FileUtils.readFileToString(new File("./src/test/resources/samples/delta/docs-data_model.json")));
			full=Json.read(FileUtils.readFileToString(new File("./src/test/resources/samples/full/docs-data_model.json")));
			config=Json.read(FileUtils.readFileToString(new File("./src/test/resources/samples/signalk-config.json")));
		} catch (IOException e) {
			logger.error(e,e);
		}
	}
	
    @Before
    public void before(){
    	transformer = partialMockBuilder(FullMsgTransformer.class)
	    	.addMockedMethod("saveMap").createMock(); 
    }
	@Test
	public void shouldProcessVessels() throws ActiveMQException {
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseFull(full, new ConcurrentSkipListMap<String,Json>(),"");
		ClientMessage message = getClientMessage(full.toString(), Config.JSON_FULL, false); 
		transformer.sendKvMap(message, map);
		
		replayAll();
		
		verifyAll();
	}
	
	@Test
	public void shouldProcessConfig() throws ActiveMQException {
		NavigableMap<String, Json> map = SignalkMapConvertor.parseFull(config, new ConcurrentSkipListMap<String,Json>(),"");
		ClientMessage message = getClientMessage(full.toString(), Config.JSON_FULL, false); 
		transformer.sendKvMap(message, map);
		
		replayAll();
		
		
		verifyAll();
	}
	
	@Test
	public void shouldAvoidDeltaFormat() throws ActiveMQException {
	
		replayAll();
		NavigableMap<String, Json> map = SignalkMapConvertor.parseFull(config, new ConcurrentSkipListMap<String,Json>(),"");
		ClientMessage message = getClientMessage(full.toString(), Config.JSON_DELTA, false); 
		transformer.sendKvMap(message, map);
		
		verifyAll();
	}
	@Test
	public void shouldAvoidContext() throws Exception {
		
		replayAll();
		NavigableMap<String, Json> map = SignalkMapConvertor.parseFull(config, new ConcurrentSkipListMap<String,Json>(),"");
		ClientMessage message = getClientMessage(full.toString(), Config.JSON_FULL, false); 
		transformer.sendKvMap(message, map);
		
		verifyAll();
	}
	

}
