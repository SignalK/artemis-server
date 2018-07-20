package nz.co.fortytwo.signalk.artemis.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.websocket.DefaultWebSocketProcessor;
import org.atmosphere.websocket.WebSocket;

@SuppressWarnings("serial")
public class SignalkWebSocketProcessor extends DefaultWebSocketProcessor {

	private static Logger logger = LogManager.getLogger(SignalkWebSocketProcessor.class);

	@Override
	public void onPong(WebSocket webSocket, byte[] payload, int offset, int length) {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Receive pong {}", webSocket.resource().uuid());

			setLastPing(webSocket.resource());
			
		} catch (Exception e) {
			logger.error(e, e);
		}
	}
	
	public void setLastPing(AtmosphereResource resource) {
		   resource.getAtmosphereConfig()
		   	.framework().sessionFactory()
		   	.getSession(resource)
		   	.setAttribute("lastPing", System.currentTimeMillis());
		  
		   if (logger.isDebugEnabled())
			   logger.debug("Set lastPing {}={}", resource.uuid(),System.currentTimeMillis());
		}

}
