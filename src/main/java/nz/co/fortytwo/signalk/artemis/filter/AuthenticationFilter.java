package nz.co.fortytwo.signalk.artemis.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.jersey.core.util.Priority;

import io.jsonwebtoken.Jwts;
import nz.co.fortytwo.signalk.artemis.server.AuthenticationService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Secured;

@Secured
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthenticationFilter implements ContainerRequestFilter {

	private static Logger logger = LogManager.getLogger(AuthenticationFilter.class);

	private static final String REALM = "signalk";
	private static final String AUTHENTICATION_SCHEME = "Bearer";

	public AuthenticationFilter() {
		logger.info("Started {}", getClass().getName());
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		logger.info("Checking {}", requestContext);
		final SecurityContext securityContext = requestContext.getSecurityContext();
		if (securityContext == null || !securityContext.isUserInRole("privileged")) {

			requestContext.abortWith(
					Response.status(Response.Status.UNAUTHORIZED).entity("User cannot access the resource.").build());
		}
	}
	

//	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//			throws IOException, ServletException {
//		HttpServletRequest r = HttpServletRequest.class.cast(request);
//		HttpServletResponse resp = HttpServletResponse.class.cast(response);
//		logger.debug("Filtering {}", r.getPathInfo());
//
//		// Get the Authorization header from the request
//		String authorizationHeader = r.getHeader(HttpHeaders.AUTHORIZATION);
//
//		// Validate the Authorization header
//		if (!isTokenBasedAuthentication(authorizationHeader)) {
//			resp.setStatus(HttpStatus.SC_FORBIDDEN);
//			resp.encodeRedirectURL("/login");
//
//		}
//
//		// Extract the token from the Authorization header
//		String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
//
//		try {
//
//			// Validate the token
//			validateToken(token);
//			// return Action.CONTINUE;
//		} catch (Exception e) {
//			resp.setStatus(HttpStatus.SC_FORBIDDEN);
//			resp.encodeRedirectURL("/login");
//		}
//		chain.doFilter(request, response);
//	}

	private boolean isTokenBasedAuthentication(String authorizationHeader) {

		// Check if the Authorization header is valid
		// It must not be null and must be prefixed with "Bearer" plus a whitespace
		// The authentication scheme comparison must be case-insensitive
		return authorizationHeader != null
				&& authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
	}

	private void validateToken(String token) throws Exception {
		// Check if the token was issued by the server and if it's not expired
		assert Jwts.parser().setSigningKey(AuthenticationService.getKey()).parseClaimsJws(token).getBody().getSubject()
				.equals(Config.getConfigProperty(Config.ADMIN_USER));
		// Throw an Exception if the token is invalid
	}

//	@Override
//	public void destroy() {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void init(FilterConfig filterConfig) throws ServletException {
//		logger.info("init {}", getClass().getName());
//
//	}

}