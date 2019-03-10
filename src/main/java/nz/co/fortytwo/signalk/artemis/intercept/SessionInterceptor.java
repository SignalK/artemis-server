package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.Config.INCOMING_RAW;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.server.ArtemisServer;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * A way to acquire the session in a message
 */
public class SessionInterceptor extends BaseInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(SessionInterceptor.class);

	@Override
	public boolean intercept(final Packet packet, final RemotingConnection connection) throws ActiveMQException {
		
		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;
			
			Message msg = realPacket.getMessage();
			if(!StringUtils.equals(msg.getAddress(), INCOMING_RAW))return true;
			
			if(msg.getStringProperty(Config.MSG_SRC_BUS)==null)
				msg.putStringProperty(Config.MSG_SRC_BUS, connection.getRemoteAddress());
			if(msg.getStringProperty(Config.MSG_SRC_TYPE)==null)
				//TODO: this is not correct for web api calls.
				msg.putStringProperty(Config.MSG_SRC_TYPE, Config.EXTERNAL_IP);
			if(msg.getStringProperty(Config.AMQ_SESSION_ID)==null) {
				for (ServerSession s : ArtemisServer.getActiveMQServer().getSessions(connection.getID().toString())) {
					if (s.getConnectionID().equals(connection.getID())) {
						if (logger.isDebugEnabled())
							logger.debug("Session is: {}, name: {}",s.getConnectionID(),s.getName());
						msg.putStringProperty(Config.AMQ_SESSION_ID, s.getName());
					} else {
						if (logger.isDebugEnabled())
							logger.debug("Session not found for: {}, name: {}",s.getConnectionID() ,s.getName());
					}
				}
			}
			//make sure we get tokens for serial, tcp, etc
			if(msg.getStringProperty(Config.AMQ_USER_TOKEN)==null)
				try {
					if (logger.isDebugEnabled())
						logger.debug("Injecting AMQ_USER_TOKEN in {}",msg);
					SecurityUtils.injectToken(msg);
				} catch (Exception e1) {
					logger.error(msg);
					logger.error(Util.readBodyBufferToString(msg.toCore()));
					logger.error(e1,e1);
				}
			//should not have roles here
			msg.removeProperty(Config.AMQ_USER_ROLES);
			msg.putStringProperty(Config.AMQ_USER_ROLES, SecurityUtils.getRoles(msg.getStringProperty(Config.AMQ_USER_TOKEN)).toString());
		

		} else {
			if (logger.isDebugEnabled())
				logger.debug("Packet is:{}, contents:{}",packet.getClass(), packet.toString());
		}
		
		return true;
	}

}
