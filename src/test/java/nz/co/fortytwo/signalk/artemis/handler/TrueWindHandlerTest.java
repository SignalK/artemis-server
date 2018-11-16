package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_directionTrue;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_speedTrue;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_speedOverGround;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.self;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.intercept.BaseMsgInterceptorTest;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;

public class TrueWindHandlerTest extends BaseMsgInterceptorTest {
	@Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    private TrueWindHandler handler;

    @Before
    public void before(){
    	handler = partialMockBuilder(TrueWindHandler.class)
	    	.addMockedMethod("send")
    			.createMock(); 
    }
	@Test
	public void shouldStoreKey() throws ActiveMQException {
		Json json = Json.read("{\"value\":3.2,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		String uuid = Config.getConfigProperty(ConfigConstants.UUID);
		handler.setUuid(uuid);
		ClientMessage out = getClientMessage(json.toString(), MediaType.APPLICATION_JSON, false);
		out.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270."+nav_speedOverGround+".values.unknown");
		handler.send(out,"vessels."+uuid+dot+env_wind_angleTrueGround+".values.internal",-1.8736811954966175d);
		handler.send(out,"vessels."+uuid+dot+env_wind_angleTrueWater+".values.internal",-1.8736811954966175d);
		//handler.send(out,"vessels."+uuid+dot+env_wind_directionTrue+".values.internal",4.409504111682969d);
		handler.send(out,"vessels."+uuid+dot+env_wind_speedTrue+".values.internal",3.2d);
		
		replayAll();
//		AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+env_wind_angleApparent+"%' OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+env_wind_speedApparent+"%'OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+nav_speedOverGround+"%'OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+nav_courseOverGroundTrue+"%'OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+env_wind_speedApparent+"%'");
		
		//self, we process
		json = Json.read("{\"value\":-1.5707963271535559,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		ClientMessage message = getClientMessage(json.toString(), MediaType.APPLICATION_JSON, false);
		message.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.angleApparent.values.unknown");
		handler.consume(message);
		
		//speed
		json = Json.read("{\"value\":2.9,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		
		message.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.speedApparent.values.unknown");
		handler.consume(message);
		
		//sog
		//json = Json.read("{\"value\":3.2,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		//message = getClientMessage(json.toString(), MediaType.APPLICATION_JSON, false);
		//message.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270."+nav_speedOverGround+".values.unknown");
		handler.consume(out);
		
		verifyAll();
	}
	
	@Test
	public void shouldStoreAnotherKey() throws ActiveMQException {
		Json json = Json.read("{\"value\":3.2,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		String uuid = Config.getConfigProperty(ConfigConstants.UUID);
		handler.setUuid(uuid);
		ClientMessage out = getClientMessage(json.toString(), MediaType.APPLICATION_JSON, false);
		out.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270."+nav_speedOverGround+".values.unknown");
		handler.send(out,"vessels."+uuid+dot+env_wind_angleTrueGround+".values.internal",-1.8736811954966175d);
		handler.send(out,"vessels."+uuid+dot+env_wind_angleTrueWater+".values.internal",-1.8736811954966175d);
		handler.send(out,"vessels."+uuid+dot+env_wind_speedTrue+".values.internal",3.2d);
		
		handler.send(out,"vessels."+uuid+dot+env_wind_angleTrueGround+".values.internal",-2.3561944903716743d);
		handler.send(out,"vessels."+uuid+dot+env_wind_angleTrueWater+".values.internal",-2.3561944903716743d);
		handler.send(out,"vessels."+uuid+dot+env_wind_speedTrue+".values.internal",4.525483400405457d);
		
		replayAll();
//		AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+env_wind_angleApparent+"%' OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+env_wind_speedApparent+"%'OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+nav_speedOverGround+"%'OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+nav_courseOverGroundTrue+"%'OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+env_wind_speedApparent+"%'");
		
		//self, we process
		json = Json.read("{\"value\":-1.5707963271535559,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		ClientMessage message = getClientMessage(json.toString(), MediaType.APPLICATION_JSON, false);
		message.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.angleApparent.values.unknown");
		handler.consume(message);
		
		//speed
		json = Json.read("{\"value\":2.9,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		
		message.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.speedApparent.values.unknown");
		handler.consume(message);
		
		//sog
		//json = Json.read("{\"value\":3.2,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		//message = getClientMessage(json.toString(), MediaType.APPLICATION_JSON, false);
		//message.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270."+nav_speedOverGround+".values.unknown");
		handler.consume(out);
		
		json = Json.read("{\"value\":1.5,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		
		out.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.speedApparent.values.unknown");
		handler.consume(out);
		
		verifyAll();
	}
	
	@Test
	public void shouldNotStoreKey() throws ActiveMQException {

		replayAll();
//		AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+env_wind_angleApparent+"%' OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+env_wind_speedApparent+"%'OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+nav_speedOverGround+"%'OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+nav_courseOverGroundTrue+"%'OR "
//		+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+self+dot+env_wind_speedApparent+"%'");
		
		//self, we process
		Json json = Json.read("{\"value\":-1.5707963271535559,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		ClientMessage message = getClientMessage(json.toString(), MediaType.APPLICATION_JSON, false);
		message.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.angleApparent.values.unknown");
		handler.consume(message);
		
		//speed
		json = Json.read("{\"value\":2.9,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		
		message.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.speedApparent.values.unknown");
		handler.consume(message);
		
		
		verifyAll();
	}
	
	@Test
	public void testTrueWindDir() {
		TrueWindHandler wp = new TrueWindHandler();
		// test 0 wind, 0deg, 0spd
		double [] windcalc = wp.calcTrueWindDirection(0, 0, 0);
		assertEquals(0.0,windcalc[1], 1.0);
		assertEquals(0.0, windcalc[0], 0.1);

		// test 10 wind, 90deg, 0spd
		windcalc = wp.calcTrueWindDirection(10, Math.toRadians(90), 0);
		assertEquals(Math.toRadians(90.0), windcalc[1], 1.0);
		assertEquals(10.0, windcalc[0], 0.1);
		
		// test 10 wind, 900deg, 10spd = 135deg 14.14
		windcalc = wp.calcTrueWindDirection(10, Math.toRadians(90), 10);
		assertEquals(Math.toRadians(135.0), windcalc[1], 1.0);
		assertEquals(14.14, windcalc[0], 0.1);
		
		// test 10 wind, 270deg, 10spd = 360-135, 14.14
		windcalc = wp.calcTrueWindDirection(10, Math.toRadians(270), 10);
		assertEquals(Math.toRadians(225.0), windcalc[1], 1.0);
		assertEquals(14.14, windcalc[0], 0.1);
		
		// test .3 wind, 80deg, 0.5spd = 146, 0.9
		windcalc = wp.calcTrueWindDirection(.3, Math.toRadians(80), .5);
		assertEquals(Math.toRadians(146.0), windcalc[1], 1.0);
		assertEquals(0.5, windcalc[0], 0.1);
		
		// test 10 wind, -90deg, 6.5spd = 146, 0.9
		windcalc = wp.calcTrueWindDirection(10, Math.toRadians(270), 6.5);
		assertEquals(Math.toRadians(360-123.0), windcalc[1], 1.0);
		assertEquals(11.9, windcalc[0], 0.1);

	}

}
