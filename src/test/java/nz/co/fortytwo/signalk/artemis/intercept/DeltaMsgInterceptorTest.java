package nz.co.fortytwo.signalk.artemis.intercept;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.message.impl.CoreMessage;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
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
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;

public class DeltaMsgInterceptorTest  extends BaseMsgInterceptorTest {

	private static Logger logger = LogManager.getLogger(DeltaMsgInterceptor.class);
	private Json update = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"updates\":[{\"$source\":\"NMEA2000.N2000-01\",\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.revolutions\",\"value\":16.341667},{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	private Json put = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"put\":[{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}");
	private Json config = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"config\":[{\"$source\":\"NMEA2000.N2000-01\",\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	
	@Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    private DeltaMsgInterceptor interceptor;// 1

    @Before
    public void before(){
    	interceptor = partialMockBuilder(DeltaMsgInterceptor.class)
	    	.addMockedMethod("saveMap")
    			.createMock(); 
    }
	@Test
	public void shouldProcessUpdate() throws ActiveMQException {
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(update, new ConcurrentSkipListMap<String,Json>());
		map = security.addAttributes(map);
		interceptor.saveMap(map);
		
		replayAll();
		
		ClientMessage message = getClientMessage(update.toString(), Config.JSON_DELTA, false); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		verifyAll();
	}
	
	@Test
	public void shouldCreateValuesExtension() throws ActiveMQException {
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(update, new ConcurrentSkipListMap<String,Json>());
		for(String k:map.keySet()){
			logger.debug("Update Key: {}",k);
			assertTrue(k.contains(dot+values+dot));
		}

		map = SignalkMapConvertor.parseDelta(put, new ConcurrentSkipListMap<String,Json>());
		for(String k:map.keySet()){
			logger.debug("Put Key: {}",k);
			assertTrue(k.contains(dot+values+dot));
		}
		
	}
	@Test
	public void shouldProcessPut() throws ActiveMQException {
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(put, new ConcurrentSkipListMap<String,Json>());
		map = security.addAttributes(map);
		interceptor.saveMap(map);
		
		replayAll();
		
		ClientMessage message = getClientMessage(put.toString(), Config.JSON_DELTA, false); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		verifyAll();
	}
	@Test
	public void shouldProcessConfig() throws ActiveMQException {
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(config, new ConcurrentSkipListMap<String,Json>());
		map = security.addAttributes(map);
		interceptor.saveMap(map);
		
		replayAll();
		
		ClientMessage message = getClientMessage(config.toString(), Config.JSON_DELTA, false); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		verifyAll();
	}
	@Test
	public void shouldAvoidReply() throws ActiveMQException {
	
		replayAll();
		
		ClientMessage message = getClientMessage(update.toString(), Config.JSON_DELTA, true); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
	
		verifyAll();
	}
	
	@Test
	public void shouldAvoidFullFormat() throws ActiveMQException {
	
		replayAll();
		
		ClientMessage message = getClientMessage(update.toString(), Config.JSON_FULL, true); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		verifyAll();
	}
	@Test
	public void shouldAvoidVessels() throws ActiveMQException, IOException {
		
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(update, new ConcurrentSkipListMap<String,Json>());
		Json full = SignalkMapConvertor.mapToFull(map);
		ClientMessage message = getClientMessage(full.toString(), Config.JSON_DELTA, true); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		verifyAll();
	}
	

}
