package nz.co.fortytwo.signalk.artemis.transformer;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.values;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.easymock.EasyMock.*;

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
import nz.co.fortytwo.signalk.artemis.intercept.BaseMsgInterceptorTest;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalkMapConvertor;

public class DeltaMsgTransformerTest  extends BaseMsgInterceptorTest {

	private static Logger logger = LogManager.getLogger(DeltaMsgTransformerTest.class);
	private Json update = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"updates\":[{\"$source\":\"NMEA2000.N2000-01\",\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.revolutions\",\"value\":16.341667},{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	private Json put = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"put\":[{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0,\"timestamp\":\"2018-08-08T02:48:28.885Z\"}]}");
	private Json config = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"config\":[{\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"server.demo.start\",\"value\":true}]}]}");
	
    private DeltaMsgTransformer transformer;// 1

    @Before
    public void before(){
    	transformer = partialMockBuilder(DeltaMsgTransformer.class)
	    	.addMockedMethod("sendKvMessage")
    			.createMock(); 
    }
	@Test
	public void shouldProcessUpdate() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(update.toString(), Config.JSON_DELTA, false); 
		transformer.sendKvMessage( anyObject(message.getClass()), anyString(), anyObject(Json.class));
		expectLastCall().times(4);
		replayAll();
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(update, new ConcurrentSkipListMap<String,Json>());
		transformer.sendKvMap(message, map);
		
		assertNotNull(transformer.transform(message));
		
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
		
		
		ClientMessage message = getClientMessage(put.toString(), Config.JSON_DELTA, false); 
		transformer.sendKvMessage( same(message), anyString(), anyObject(Json.class));
		expectLastCall().times(2);
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(put, new ConcurrentSkipListMap<String,Json>());
		transformer.sendKvMap(message, map);
		assertNotNull(transformer.transform(message));
		
		verifyAll();
	}
	
	@Test
	public void shouldProcessConfig() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(config.toString(), Config.JSON_DELTA, false); 
		transformer.sendKvMessage(same(message), anyString(), anyObject(Json.class));
		expectLastCall().times(2);
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(config, new ConcurrentSkipListMap<String,Json>());
		logger.debug("Config: {}",map);
		transformer.sendKvMap(message, map);
		assertNotNull(transformer.transform(message));
		
		verifyAll();
	}
	
	
//	@Test
//	public void shouldAvoidFullFormat() throws ActiveMQException {
//	
//		replayAll();
//		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(config, new ConcurrentSkipListMap<String,Json>());
//		ClientMessage message = getClientMessage(config.toString(), Config.JSON_FULL, false); 
//		transformer.sendKvMap(message, map);
//		
//
//		assertNotNull(transformer.transform(message));
//		
//		verifyAll();
//	}
	@Test
	public void shouldAvoidVessels() throws Exception {
		
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(update, new ConcurrentSkipListMap<String,Json>());
		Json full = SignalkMapConvertor.mapToFull(map);
		ClientMessage message = getClientMessage(full.toString(), Config.JSON_FULL, false); 
		transformer.sendKvMap(message, map);
		
		assertNotNull(transformer.transform(message));
		
		verifyAll();
	}
	

}
