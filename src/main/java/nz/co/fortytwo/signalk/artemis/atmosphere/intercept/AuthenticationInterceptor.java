package nz.co.fortytwo.signalk.artemis.atmosphere.intercept;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.SECURITY_SSL_ENABLE;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTH_COOKIE_NAME;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.updateCookie;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.validateToken;

import java.util.Map.Entry;

import javax.servlet.http.Cookie;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.AtmosphereInterceptorService;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereInterceptorAdapter;
import org.atmosphere.cpr.AtmosphereResource;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;

@AtmosphereInterceptorService
public class AuthenticationInterceptor extends AtmosphereInterceptorAdapter implements AtmosphereInterceptor {

	private static Logger logger = LogManager.getLogger(AuthenticationInterceptor.class);
	protected String scheme;

	public AuthenticationInterceptor() {
		super();
		scheme = Config.getConfigPropertyBoolean(SECURITY_SSL_ENABLE)?"https":"http";
	}

	@Override
	public Action inspect(AtmosphereResource r) {
		if (logger.isDebugEnabled()) {
			logger.debug("URL: {}", r.getRequest().getRequestURL());
			for (Entry<String, String> entry : r.getRequest().headersMap().entrySet()) {
				logger.debug("[{}] receive: {}:{}", r.getRequest().getRemoteAddr(), entry.getKey(), entry.getValue());
			}
		}
		if (r.getRequest().getPathInfo().startsWith("/signalk/v1/api/")){
			return Action.CONTINUE;
		}
		// check for secure path
		if (!(r.getRequest().getPathInfo().startsWith("/signalk/v1/logger")
				|| r.getRequest().getPathInfo().startsWith("/config")
				|| r.getRequest().getPathInfo().startsWith("/signalk/v1/apps")
				|| r.getRequest().getPathInfo().startsWith("/signalk/control")
				|| r.getRequest().getPathInfo().startsWith("/signalk/v1/security"))) {
			return Action.CONTINUE;
		}
		// assume we might have a cookie token
		for (Cookie c : r.getRequest().getCookies()) {
			if (logger.isDebugEnabled())
				logger.debug("Cookie: {}, {}", c.getName(), c.getValue());
			if (AUTH_COOKIE_NAME.equals(c.getName())) {
				String jwtToken = c.getValue();

				try {
					// Validate and update the token
					r.getResponse().addCookie(updateCookie(c, validateToken(jwtToken)));
					// add the roles to request
					r.getRequest().localAttributes().put(SignalKConstants.JWT_TOKEN, jwtToken);
					return Action.CONTINUE;
				} catch (Exception e) {
					logger.error(e.getMessage());
					c.setMaxAge(1);
					c.setPath("/");
					r.getResponse().addCookie(c);

				}
			}
		}

		
	
		if (logger.isDebugEnabled())
			logger.debug("Unauthenticated, redirect to {}", "/login.html?target="+r.getRequest().getPathInfo());

		r.getResponse().setStatus(HttpStatus.SC_MOVED_TEMPORARILY);
		r.getResponse().setHeader("Location", "/login.html?target="+r.getRequest().getPathInfo());

		return Action.RESUME;

	}

}
