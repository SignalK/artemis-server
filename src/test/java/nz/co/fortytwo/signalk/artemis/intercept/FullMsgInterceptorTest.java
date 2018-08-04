package nz.co.fortytwo.signalk.artemis.intercept;

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
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.util.Config;

public class FullMsgInterceptorTest  extends BaseMsgInterceptorTest {

	private static Logger logger = LogManager.getLogger(FullMsgInterceptorTest.class);
	private Json full;
	private Json config;
	private Json delta;
	
	
	@Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    private FullMsgInterceptor interceptor;// 1
    
    public FullMsgInterceptorTest() {
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
    	interceptor = partialMockBuilder(FullMsgInterceptor.class)
	    	.addMockedMethod("saveMap").createMock(); 
    }
	@Test
	public void shouldProcessVessels() throws ActiveMQException {
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseFull(full, new ConcurrentSkipListMap<String,Json>(),"");
		map = security.addAttributes(map);
		interceptor.saveMap(map);
		
		replayAll();
		
		ClientMessage message = getClientMessage(full.toString(), Config.JSON_FULL, false); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		verifyAll();
	}
	
	@Test
	public void shouldProcessConfig() throws ActiveMQException {
		NavigableMap<String, Json> map = SignalkMapConvertor.parseFull(config, new ConcurrentSkipListMap<String,Json>(),"");
		map = security.addAttributes(map);
		interceptor.saveMap(map);
		
		replayAll();
		
		ClientMessage message = getClientMessage(config.toString(), Config.JSON_FULL, false); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		verifyAll();
	}
	@Test
	public void shouldAvoidReply() throws ActiveMQException {
	
		replayAll();
		
		ClientMessage message = getClientMessage(full.toString(), Config.JSON_FULL, true); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
	
		verifyAll();
	}
	
	@Test
	public void shouldAvoidDeltaFormat() throws ActiveMQException {
	
		replayAll();
		
		ClientMessage message = getClientMessage(delta.toString(), Config.JSON_DELTA, true); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		verifyAll();
	}
	@Test
	public void shouldAvoidContext() throws Exception {
		
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(full, new ConcurrentSkipListMap<String,Json>());
		Json full = SignalkMapConvertor.mapToFull(map);
		ClientMessage message = getClientMessage(delta.toString(), Config.JSON_FULL, true); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		verifyAll();
	}
	

}
