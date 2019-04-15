package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.values;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.handler.DeltaMsgHandler;
import nz.co.fortytwo.signalk.artemis.intercept.BaseMsgInterceptorTest;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalkMapConvertor;

public class DeltaMsgHandlerTest  extends BaseMsgInterceptorTest {

	private static Logger logger = LogManager.getLogger(DeltaMsgHandlerTest.class);
	private Json update = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"updates\":[{\"$source\":\"NMEA2000.N2000-01\",\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.revolutions\",\"value\":16.341667},{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	private Json put = Json.read("{\"context\":\"resources\",\"put\":[{\"path\":\"waypoints.urn:mrn:signalk:uuid:23a93b18-63ef-4b9d-942c-7e010b789c07\",\"value\":{\"feature\":{\"geometry\":{\"coordinates\":[-76.97297721516502,35.04369923349947],\"type\":\"Point\"},\"id\":\"\",\"type\":\"Feature\",\"properties\":{\"name\":\"test\",\"cmt\":\"test\"}},\"position\":{\"latitude\":35.04369923349947,\"longitude\":-76.97297721516502}},\"timestamp\":\"2019-04-11T20:54:39.584Z\"}]}");
	private Json post = Json.read("{\"context\":\"vessels\",\"post\":[{\"path\":\"\",\"value\":{},\"timestamp\":\"2018-08-08T02:48:28.885Z\"}]}");
	private Json postBad = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"post\":[{\"path\":\"propulsion.0\",\"value\":45500.0,\"timestamp\":\"2018-08-08T02:48:28.885Z\"}]}");
	private Json post1 = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"post\":[{\"path\":\"propulsion\",\"value\":45500.0,\"timestamp\":\"2018-08-08T02:48:28.885Z\"}]}");
	private Json postBad1 = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"post\":[{\"path\":\"propulsion.0\",\"value\":45500.0,\"timestamp\":\"2018-08-08T02:48:28.885Z\"}]}");
	private Json config = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"config\":[{\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"server.demo.start\",\"value\":true}]}]}");
	
    private DeltaMsgHandler handler;// 1

    @Before
    public void before() throws NoSuchMethodException, SecurityException{
    	handler = partialMockBuilder(DeltaMsgHandler.class)
	    	.addMockedMethod("sendKvMessage")
	    	.addMockedMethod("sendReply")
	    	.addMockedMethod(BaseHandler.class.getDeclaredMethod("initSession",String.class, String.class,RoutingType.class))
    			.createMock(); 
    }
	@Test
	public void shouldProcessUpdate() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(update.toString(), Config.AMQ_CONTENT_TYPE_JSON_DELTA, false); 
		handler.sendKvMessage( anyObject(message.getClass()), anyString(), anyObject(Json.class));
		expectLastCall().times(4);
		replayAll();
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(update, new ConcurrentSkipListMap<String,Json>());
		handler.sendKvMap(message, map);
		
		handler.consume(message);
		
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
		
		
		ClientMessage message = getClientMessage(put.toString(), Config.AMQ_CONTENT_TYPE_JSON_DELTA, false); 
		handler.sendKvMessage( same(message), anyString(), anyObject(Json.class));
		expectLastCall().times(2);
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(put, new ConcurrentSkipListMap<String,Json>());
		handler.sendKvMap(message, map);
		handler.consume(message);
		
		verifyAll();
	}
	
	@Test
	public void shouldProcessPost() throws ActiveMQException {
		
		
		ClientMessage message = getClientMessage(post.toString(), Config.AMQ_CONTENT_TYPE_JSON_DELTA, false); 
		handler.sendKvMessage( same(message), anyString(), anyObject(Json.class));
		expectLastCall().times(1);
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(post, new ConcurrentSkipListMap<String,Json>());
		handler.sendKvMap(message, map);
		handler.consume(message);
		
		verifyAll();
	}
	@Test
	public void shouldProcessPost1() throws ActiveMQException {
		
		
		ClientMessage message = getClientMessage(post1.toString(), Config.AMQ_CONTENT_TYPE_JSON_DELTA, false); 
		handler.sendKvMessage( same(message), anyString(), anyObject(Json.class));
		expectLastCall().times(1);
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(post1, new ConcurrentSkipListMap<String,Json>());
		handler.sendKvMap(message, map);
		handler.consume(message);
		
		verifyAll();
	}
	
	@Test
	public void shouldNotProcessPost() throws Exception {
		
		
		ClientMessage message = getClientMessage(postBad.toString(), Config.AMQ_CONTENT_TYPE_JSON_DELTA, false); 
		handler.sendReply(anyString(), anyString(), anyString(), anyObject(Json.class), anyString());
		expectLastCall().times(1);
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(postBad, new ConcurrentSkipListMap<String,Json>());
		handler.sendKvMap(message, map);
		handler.consume(message);
		
		verifyAll();
	}
	@Test
	public void shouldNotProcessPost1() throws Exception {
		
		
		ClientMessage message = getClientMessage(postBad1.toString(), Config.AMQ_CONTENT_TYPE_JSON_DELTA, false); 
		handler.sendReply(anyString(), anyString(), anyString(), anyObject(Json.class), anyString());
		expectLastCall().times(1);
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(postBad1, new ConcurrentSkipListMap<String,Json>());
		handler.sendKvMap(message, map);
		handler.consume(message);
		
		verifyAll();
	}
	
	@Test
	public void shouldProcessConfig() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(config.toString(), Config.AMQ_CONTENT_TYPE_JSON_DELTA, false); 
		handler.sendKvMessage(same(message), anyString(), anyObject(Json.class));
		expectLastCall().times(2);
		replayAll();
		
		NavigableMap<String, Json> map = SignalkMapConvertor.parseDelta(config, new ConcurrentSkipListMap<String,Json>());
		logger.debug("Config: {}",map);
		handler.sendKvMap(message, map);
		handler.consume(message);
		
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
		ClientMessage message = getClientMessage(full.toString(), Config.AMQ_CONTENT_TYPE_JSON_FULL, false); 
		handler.sendKvMap(message, map);
		
		handler.consume(message);
		
		verifyAll();
	}
	

}
