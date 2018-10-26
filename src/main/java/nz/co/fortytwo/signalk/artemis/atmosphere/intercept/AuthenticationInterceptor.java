package nz.co.fortytwo.signalk.artemis.atmosphere.intercept;

import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTH_COOKIE_NAME;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.updateCookie;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.validateToken;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FROMTIME;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.TIMEPERIOD;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.TOTIME;
import static nz.co.fortytwo.signalk.artemis.util.Util.mangleRequestParams;

import javax.servlet.http.Cookie;

import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;

import org.apache.http.HttpStatus;
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
			logger.debug("URL: {}", r.getRequest().getRequestURL());
			r.getRequest().headersMap().forEach((k,v) -> {
				logger.debug("[{}] receive header: {}:{}", r.getRequest().getRemoteAddr(), k, v);
			});
			r.getRequest().getParameterMap().forEach((k,v) -> {
					logger.debug("[{}] receive param: {}:{}", r.getRequest().getRemoteAddr(), k, v);
			});
		}

		// check for secure path
		if (!(r.getRequest().getPathInfo().startsWith("/signalk/v1/api/")
				|| r.getRequest().getPathInfo().startsWith("/signalk/v1/history")
				|| r.getRequest().getPathInfo().startsWith("/signalk/v1/logger")
				|| r.getRequest().getPathInfo().startsWith("/config")
				|| r.getRequest().getPathInfo().startsWith("/signalk/apps")
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
					// add some parameters to the request

					if (! r.getRequest().getRequestURI().contains(FROMTIME) && 
							! r.getRequest().getRequestURI().contains(TOTIME) &&
							! r.getRequest().getRequestURI().contains(TIMEPERIOD))
					{
						StringBuffer sb=(new StringBuffer(r.getRequest().getPathInfo())).append(mangleRequestParams(r.getRequest().getParameterMap()));
						r.getRequest().pathInfo(sb.toString());
					}
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
		
		StringBuffer uri = r.getRequest().getRequestURL(); // "/login.html";
		String url = uri.substring(0, uri.indexOf(r.getRequest().getRequestURI()));
				
		url = url + "/login.html?target="+r.getRequest().getPathInfo();
		url = url+mangleRequestParams(r.getRequest().getParameterMap());
		if (logger.isDebugEnabled())
			logger.debug("Unauthenticated, redirect to {}", url);

		r.getResponse().setStatus(HttpStatus.SC_MOVED_TEMPORARILY);
		r.getResponse().setHeader("Location", url);

		return Action.RESUME;
	}

}
