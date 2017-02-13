package nz.co.fortytwo.signalk.artemis.util;


import static nz.co.fortytwo.signalk.util.SignalKConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;

public class Util {
	
	private static Pattern selfMatch = Pattern.compile("\\.self\\.");
	private static Pattern selfEndMatch = Pattern.compile("\\.self$");
	

	public static String fixSelfKey(String key) {
		key = selfMatch.matcher(key).replaceAll(dot + self + dot);
		key = selfEndMatch.matcher(key).replaceAll(dot + self);
		return key;
	}
	
	public static String sanitizePath(String newPath) {
		newPath = newPath.replace('/', '.');
		if (newPath.startsWith(dot))
			newPath = newPath.substring(1);
		
		if ((vessels+dot+self_str).equals(newPath)){
			newPath = vessels_dot_self;
		}
		newPath = newPath.replace(vessels+dot+self_str+dot, vessels_dot_self_dot);
		return newPath;
	}
	
	public static Pattern regexPath(String newPath) {
		// regex it
		String regex = newPath.replaceAll(".", "[$0]").replace("[*]", ".*").replace("[?]", ".");
		return Pattern.compile(regex);
	}

	public static ClientSession getVmSession(String user, String password) throws Exception {
		ClientSessionFactory nettyFactory = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
				.createSessionFactory();
		return nettyFactory.createSession(user, password, false, true, true, false, 10);
	}

	public static ClientSession getLocalhostClientSession(String user, String password) throws Exception {
		Map<String, Object> connectionParams = new HashMap<String, Object>();
		connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
		connectionParams.put(TransportConstants.PORT_PROP_NAME, 61617);

		ClientSessionFactory nettyFactory = ActiveMQClient
				.createServerLocatorWithoutHA(
						new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams))
				.createSessionFactory();
		return nettyFactory.createSession(user, password, false, true, true, false, 10);
	}
}
