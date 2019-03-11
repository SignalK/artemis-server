package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_anchor_currentRadius;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_anchor_maxRadius;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_anchor_position_latitude;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_anchor_position_longitude;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_position_latitude;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_position_longitude;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.junit.Before;
import org.junit.Test;

import nz.co.fortytwo.signalk.artemis.intercept.BaseMsgInterceptorTest;

public class AnchorWatchHandlerTest extends BaseMsgInterceptorTest {
	
    private AnchorWatchHandler handler;

    @Before
    public void before(){
    	handler = partialMockBuilder(AnchorWatchHandler.class)
	    	.addMockedMethod("send")
    			.createMock(); 
    	handler.setUuid(uuid);
    }
	@Test
	public void shouldStoreKey() throws ActiveMQException {
		
		ClientMessage out = getMessage("{\"value\":173.24706,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_position_longitude, "internal");
		handler.send(out,"vessels."+uuid+dot+nav_anchor_currentRadius+".values.internal",29.681280090589777d);
		
		replayAll();
	
		//setup vars
		
		//anchor lat
		handler.consume(getMessage("{\"value\":-41.29366666666667,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_anchor_position_latitude,"unknown"));
		
		//anchor lon
		handler.consume(getMessage("{\"value\":173.24707333333333,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_anchor_position_longitude,"unknown"));
		
		//max_radius
		handler.consume(getMessage("{\"value\":256,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_anchor_maxRadius,"unknown"));
		
		//lat
		handler.consume(getMessage("{\"value\":-41.2934,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_position_latitude,"unknown"));
		
		//lon
		handler.consume(out);
		
		
		verifyAll();
	}
	
	
	@Test
	public void shouldStoreAnotherKey() throws ActiveMQException {
	
		ClientMessage out = getMessage("{\"value\":173.24706,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_position_longitude, "internal");
		ClientMessage out1 = getMessage("{\"value\":173.248,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_position_longitude, "internal");
		handler.send(out,"vessels."+uuid+dot+nav_anchor_currentRadius+".values.internal",29.681280090589777d);
		handler.send(out1,"vessels."+uuid+dot+nav_anchor_currentRadius+".values.internal",82.92607364299573d);
		
		replayAll();
	
		//setup vars
		
		//anchor lat
		handler.consume(getMessage("{\"value\":-41.29366666666667,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_anchor_position_latitude,"unknown"));
		
		//anchor lon
		handler.consume(getMessage("{\"value\":173.24707333333333,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_anchor_position_longitude,"unknown"));
		
		//max_radius
		handler.consume(getMessage("{\"value\":256,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_anchor_maxRadius,"unknown"));
		
		//lat
		handler.consume(getMessage("{\"value\":-41.2934,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_position_latitude,"unknown"));
		
		//lon
		handler.consume(out);
		handler.consume(out1);
		
		
		verifyAll();
	}
	
	@Test
	public void shouldNotStoreKey() throws ActiveMQException {
		replayAll();
		
		//setup vars
		
		//anchor lat
		handler.consume(getMessage("{\"value\":-41.29366666666667,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_anchor_position_latitude,"unknown"));
		
		//anchor lon
		handler.consume(getMessage("{\"value\":173.24707333333333,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_anchor_position_longitude,"unknown"));
		
		//max_radius
		handler.consume(getMessage("{\"value\":256,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_anchor_maxRadius,"unknown"));
		
		//lat
		handler.consume(getMessage("{\"value\":-41.2934,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",nav_position_latitude,"unknown"));
	
		verifyAll();
	}
	
	

}
