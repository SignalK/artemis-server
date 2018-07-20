package nz.co.fortytwo.signalk.artemis.atmosphere.intercept;

import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTHENTICATION_SCHEME;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.authenticateUser;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.isTokenBasedAuthentication;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.setForbidden;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.setUnauthorised;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.validateToken;

import java.util.Base64;
import java.util.Map.Entry;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.AtmosphereInterceptorService;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereInterceptorAdapter;
import org.atmosphere.cpr.AtmosphereResource;


@AtmosphereInterceptorService
public class AuthenticationInterceptor extends AtmosphereInterceptorAdapter implements AtmosphereInterceptor {

	private static Logger logger = LogManager.getLogger(AuthenticationInterceptor.class);

	public AuthenticationInterceptor() {
		super();
	}

	@Override
	public Action inspect(AtmosphereResource r) {
		if (logger.isDebugEnabled()) {
			for (Entry<String, String> entry : r.getRequest().headersMap().entrySet()) {
				logger.debug("[{}] receive: {}:{}", r.getRequest().getRemoteAddr(), entry.getKey(), entry.getValue());
			}
		}
		if (!(r.getRequest().getPathInfo().startsWith("/signalk/v1/api/config")
				||r.getRequest().getPathInfo().startsWith("/signalk/v1/logger")
				||r.getRequest().getPathInfo().startsWith("/config")
				||r.getRequest().getPathInfo().startsWith("/signalk/v1/security"))) {
			return Action.CONTINUE;
		}

		String authorizationHeader = r.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

		if(StringUtils.isBlank(authorizationHeader)) {
			setUnauthorised(r);
			return Action.RESUME;
		}
		// Validate the Authorization header
		
		logger.debug("authHeader: {}",authorizationHeader);
		
		if(StringUtils.startsWithIgnoreCase(authorizationHeader,"Basic ")) {
	
			try {
				String auth = new String(Base64.getDecoder().decode(authorizationHeader.substring(6)));
				String user = StringUtils.substringBefore(auth,":");
				String password = StringUtils.substringAfter(auth,":");
				
				String token = authenticateUser(user, password);
				r.getResponse().setHeader(HttpHeaders.AUTHORIZATION, AUTHENTICATION_SCHEME+" "+token);
				return Action.CONTINUE;
			} catch (Exception e) {
				logger.error(e,e);
				setUnauthorised(r);
				return Action.RESUME;
			}
		}
		
		if (!isTokenBasedAuthentication(authorizationHeader)) {
			setUnauthorised(r);
			return Action.RESUME;
		}
		
		// Extract the token from the Authorization header
		String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();

		try {
			// Validate and update the token
			r.getResponse().setHeader(HttpHeaders.AUTHORIZATION, AUTHENTICATION_SCHEME+" "+validateToken(token));
			return Action.CONTINUE;
		} catch (Exception e) {
			setForbidden(r);
			return Action.RESUME;
		}

	}


}
