package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_USER_TOKEN;

import javax.ws.rs.core.MediaType;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMockSupport;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.server.BaseServerTest;
import nz.co.fortytwo.signalk.artemis.service.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;

public class BaseMsgInterceptorTest extends EasyMockSupport {

	private static Logger logger = LogManager.getLogger(BaseMsgInterceptorTest.class);
	protected String uuid;
		
	public BaseMsgInterceptorTest() {
		super();
		uuid = Config.getConfigProperty(ConfigConstants.UUID);
		InfluxDbService.setDbName(BaseServerTest.SIGNALK_TEST_DB);
		InfluxDbService.allowWrite=true;
	}

	protected ClientMessage getClientMessage(String body, String contentType, boolean reply) {
		ClientMessage message = new ClientMessageImpl((byte) 0, false, 0, System.currentTimeMillis(), (byte) 4, 1024);
		if(reply)message.putBooleanProperty(SignalKConstants.REPLY,reply);
		message.putStringProperty(Config.AMQ_CONTENT_TYPE, contentType);
		if(body!=null)
			message.getBodyBuffer().writeString(body);
		return message;
	}

	protected ClientMessage getMessage(String jsonStr, String key, String src) {
		return getMessage(jsonStr, key+".values."+src);
	}
	protected ClientMessage getMessage(String jsonStr, String key) {
		Json json = Json.read(jsonStr);
		ClientMessage message = getClientMessage(json.toString(), MediaType.APPLICATION_JSON, false);
		message.putStringProperty(Config.AMQ_INFLUX_KEY, "vessels."+uuid+"."+key);
//		message.putStringProperty(AMQ_USER_TOKEN, token);
//		try {
//			message.putStringProperty(Config.AMQ_USER_ROLES, SecurityUtils.getRoles(token).toString());
//		} catch (Exception e) {
//			logger.error(e,e);
//		}
		return message;
	}
}