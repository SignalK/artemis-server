package nz.co.fortytwo.signalk.artemis.server;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map.Entry;

import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.security.User;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereResource;

import nz.co.fortytwo.signalk.artemis.service.SignalkManagedApiService;

public class AuthenticationInterceptor implements AtmosphereInterceptor {
	private static final String AUTH_PRINCIPAL ="AUTH_PRINCIPAL";
	private static Logger logger = LogManager.getLogger(AuthenticationInterceptor.class);
	private SecurityConfiguration conf;
	
	public AuthenticationInterceptor(SecurityConfiguration conf) {
		super();
		this.conf=conf;
	}
	
	@Override
	public void configure(AtmosphereConfig config) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void postInspect(AtmosphereResource r) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Action inspect(AtmosphereResource r) {
		if (logger.isDebugEnabled()) {
			for (Entry<String, String> entry : r.getRequest().headersMap().entrySet()) {
				logger.debug("[internal client] receive: " + entry.getKey() + ":" + entry.getValue());
			}
		}
		
		UsernamePasswordCredentials upc = (UsernamePasswordCredentials) r.session(true).getAttribute(AUTH_PRINCIPAL);
		boolean authenticated =  (upc!=null);
		
		if (authenticated) {
			if (logger.isDebugEnabled())
				logger.debug("Auth User:" + upc.getUserName());
			authenticated = true;
			r.getRequest().header("X-User", upc.getUserName());
			r.getRequest().header("X-Pass", upc.getPassword());
			return Action.CONTINUE; 
		}
		String authorization = r.getRequest().getHeader("Authorization");
		if (logger.isDebugEnabled())
			logger.debug("Authorization:" + authorization);

		if (!authenticated && authorization != null && authorization.startsWith("Basic")) {
			// Authorization: Basic base64 credentials
			String base64Credentials = authorization.substring("Basic".length()).trim();
			String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.forName("UTF-8"));
			// credentials = username:password
			final String[] values = credentials.split(":", 2);
			if (logger.isDebugEnabled())
				logger.debug("Authorizing:" + values[0]);
			User user = conf.getUser(values[0]);
			if (user.isValid(values[0], values[1])) {
				upc = new UsernamePasswordCredentials(values[0], values[1]);
				r.session(true).setAttribute(AUTH_PRINCIPAL,upc);
				if (logger.isDebugEnabled())
					logger.debug("Added credentials:" + upc);
				authenticated = true;
				r.getRequest().header("X-User", upc.getUserName());
				r.getRequest().header("X-Pass", upc.getPassword());
			}

		}
		if (logger.isDebugEnabled())
			logger.debug("Auth:" + authenticated);
		if (!authenticated) {
			r.getResponse().setStatus(401);
			r.getResponse().addHeader("WWW-Authenticate", "Basic realm=\"Signalk Realm\"");
			return Action.CANCELLED;
		}
		return Action.CONTINUE; 
	}
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
