package nz.co.fortytwo.signalk.artemis.intercept;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMockSupport;

import nz.co.fortytwo.signalk.artemis.server.BaseServerTest;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;

public class BaseMsgInterceptorTest extends EasyMockSupport {

	private static Logger logger = LogManager.getLogger(BaseMsgInterceptorTest.class);
		
	public BaseMsgInterceptorTest() {
		super();
		InfluxDbService.setDbName(BaseServerTest.SIGNALK_TEST_DB);
	}

	protected ClientMessage getClientMessage(String body, String contentType, boolean reply) {
		ClientMessage message = new ClientMessageImpl((byte) 0, false, 0, System.currentTimeMillis(), (byte) 4, 1024);
		if(reply)message.putBooleanProperty(SignalKConstants.REPLY,reply);
		message.putStringProperty(Config.AMQ_CONTENT_TYPE, contentType);
		if(body!=null)
			message.getBodyBuffer().writeString(body);
		return message;
	}

}