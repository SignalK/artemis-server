package nz.co.fortytwo.signalk.artemis.util;

import javax.crypto.SecretKey;
import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.joda.time.DateTime;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import nz.co.fortytwo.signalk.artemis.server.AuthenticationInterceptor;

public class SecurityUtils {
	
	private static Logger logger = LogManager.getLogger(AuthenticationInterceptor.class);

	public static final String ROLES = "roles";
	public static final String REALM = "signalk";
	public static final String AUTHENTICATION_SCHEME = "Bearer";
	private static SecretKey key = MacProvider.generateKey();
	
	public static void setForbidden(AtmosphereResource r) {
		r.getResponse().setHeader(HttpHeaders.AUTHORIZATION, "Basic realm=\""+REALM+"\"");
		r.getResponse().setStatus(HttpStatus.SC_FORBIDDEN);
	}

	public static void setUnauthorised(AtmosphereResource r) {
		r.getResponse().setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\""+REALM+"\"");
		r.getResponse().setStatus(HttpStatus.SC_UNAUTHORIZED);
		
	}

	public static boolean isTokenBasedAuthentication(String authorizationHeader) {

		// Check if the Authorization header is valid
		// It must not be null and must be prefixed with "Bearer" plus a whitespace
		// The authentication scheme comparison must be case-insensitive
		return authorizationHeader != null
				&& authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
	}

	public static String validateToken(String token) throws Exception {
		// Check if the token was issued by the server and if it's not expired
		Claims body = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
		assert body.getSubject().equals(Config.getConfigProperty(Config.ADMIN_USER));
		// Throw an Exception if the token is invalid
		//renew if near expiry
		if((System.currentTimeMillis()-body.getExpiration().getTime())< (body.getExpiration().getTime()*0.1)) {
			return issueToken(body.getSubject());
		}
		return token;
	}
	
	public static String getSubject(String token) throws Exception {
		// Check if the token was issued by the server and if it's not expired
		return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody().getSubject();
		
	}
	
	public static String getRoles(String token) throws Exception {
		return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody().get(ROLES, String.class);
	}
	

	public static String authenticateUser(String username, String password) throws Exception {
		if (!Config.getConfigProperty(Config.ADMIN_USER).equals(username)
				|| !Config.getConfigProperty(Config.ADMIN_PWD).equals(password)) {
			throw new SecurityException("Username or password invalid");
		}
		return issueToken(username);	
	}

	public static String issueToken(String username) {
		// Issue a token (can be a random String persisted to a database or a JWT token)
		// The issued token must be associated to a user

		String compactJws = Jwts.builder()
				.setSubject(username)
				.setExpiration(DateTime.now().plusHours(1).toDate())
				.signWith(SignatureAlgorithm.HS512, key)
				.compact();
		// Return the issued token
		return compactJws;
	}


}
