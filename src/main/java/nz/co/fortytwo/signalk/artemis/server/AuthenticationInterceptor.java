package nz.co.fortytwo.signalk.artemis.server;

import java.util.Base64;
import java.util.Map.Entry;

import javax.crypto.SecretKey;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.AtmosphereInterceptorService;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereResource;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Secured;

@Secured
@AtmosphereInterceptorService
public class AuthenticationInterceptor implements AtmosphereInterceptor {
	private static final String AUTH_PRINCIPAL = "AUTH_PRINCIPAL";
	private static Logger logger = LogManager.getLogger(AuthenticationInterceptor.class);
	private SecurityConfiguration conf;
	private static final String REALM = "signalk";
	private static final String AUTHENTICATION_SCHEME = "Bearer";
	private static SecretKey key = MacProvider.generateKey();

	public AuthenticationInterceptor() {
		super();
	}

	public AuthenticationInterceptor(SecurityConfiguration conf) {
		super();
		this.conf = conf;
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
				logger.debug("[{}] receive: {}:{}", r.getRequest().getRemoteAddr(), entry.getKey(), entry.getValue());
			}
		}
		if (!r.getRequest().getPathInfo().startsWith("/config")) {
			return Action.CONTINUE;
		}

		String authorizationHeader = r.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

		if(StringUtils.isBlank(authorizationHeader)) {
			r.getResponse().setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"signalk\"");
			r.getResponse().setStatus(HttpStatus.SC_UNAUTHORIZED);
			return Action.RESUME;
		}
		// Validate the Authorization header
		
		logger.debug("authHeader: {}",authorizationHeader);
		
		if(authorizationHeader.contains("Basic ")) {
	
			try {
				String auth = new String(Base64.getDecoder().decode(StringUtils.remove(authorizationHeader, "Basic ")));
				String user = StringUtils.substringBefore(auth,":");
				String password = StringUtils.substringAfter(auth,":");
				
				authenticate(user, password);
				return Action.CONTINUE;
			} catch (Exception e) {
				logger.error(e,e);
				r.getResponse().setHeader(HttpHeaders.AUTHORIZATION, "Basic realm=\"signalk\"");
				r.getResponse().setStatus(HttpStatus.SC_UNAUTHORIZED);
				return Action.RESUME;
			}
		}
		
		if (!isTokenBasedAuthentication(authorizationHeader)) {
			r.getResponse().setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"signalk\"");
			r.getResponse().setStatus(HttpStatus.SC_UNAUTHORIZED);

			return Action.RESUME;
		}
		
		// Extract the token from the Authorization header
		String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();

		try {
			// Validate the token
			validateToken(token);
			return Action.CONTINUE;
		} catch (Exception e) {
			r.getResponse().setHeader(HttpHeaders.AUTHORIZATION, "Basic realm=\"signalk\"");
			r.getResponse().setStatus(HttpStatus.SC_FORBIDDEN);

			return Action.RESUME;
		}


		
	}

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

	public Response authenticateUser(String username, String password) {

		try {

			// Authenticate the user using the credentials provided
			authenticate(username, password);

			// Issue a token for the user
			String token = issueToken(username);

			// Return the token on the response
			return Response.ok().header(HttpHeaders.AUTHORIZATION, AUTHENTICATION_SCHEME.toLowerCase()+" "+token).build();

		} catch (Exception e) {
			return Response.status(Response.Status.FORBIDDEN).build();
		}
	}

	private void authenticate(String username, String password) throws Exception {
		// Authenticate against a database, LDAP, file or whatever
		if (!Config.getConfigProperty(Config.ADMIN_USER).equals(username)
				|| !Config.getConfigProperty(Config.ADMIN_PWD).equals(password)) {
			// Throw an Exception if the credentials are invalid
			throw new SecurityException("Username or password invalid");
		}

	}

	private String issueToken(String username) {
		// Issue a token (can be a random String persisted to a database or a JWT token)
		// The issued token must be associated to a user

		String compactJws = Jwts.builder().setSubject("Joe").signWith(SignatureAlgorithm.HS512, key).compact();
		// Return the issued token
		return compactJws;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
}
