package nz.co.fortytwo.signalk.artemis.filter;

import java.io.IOException;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.jersey.core.util.Priority;

import io.jsonwebtoken.Jwts;
import nz.co.fortytwo.signalk.artemis.server.AuthenticationService;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Secured;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements  ContainerRequestFilter {

	private static Logger logger = LogManager.getLogger(AuthenticationFilter.class);
	
    private static final String REALM = "signalk";
    private static final String AUTHENTICATION_SCHEME = "Bearer";

    public AuthenticationFilter() {
		logger.info("Started {}",getClass().getName());
	}
    
    @Override
	public void filter(ContainerRequestContext ctx)
			throws IOException {
    	logger.debug("Filtering {}",ctx.getUriInfo().getPath());
	
        // Get the Authorization header from the request
        String authorizationHeader =
        		ctx.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Validate the Authorization header
        if (!isTokenBasedAuthentication(authorizationHeader)) {
        	ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Cannot access")
                    .build());
        }

        // Extract the token from the Authorization header
        String token = authorizationHeader
                            .substring(AUTHENTICATION_SCHEME.length()).trim();

        try {

            // Validate the token
            validateToken(token);
            //return Action.CONTINUE;
        } catch (Exception e) {
        	ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Cannot access")
                    .build());
        }
        
    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {

        // Check if the Authorization header is valid
        // It must not be null and must be prefixed with "Bearer" plus a whitespace
        // The authentication scheme comparison must be case-insensitive
        return authorizationHeader != null && authorizationHeader.toLowerCase()
                    .startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }


    private void validateToken(String token) throws Exception {
        // Check if the token was issued by the server and if it's not expired
    	assert Jwts.parser()
    		.setSigningKey(AuthenticationService.getKey())
    		.parseClaimsJws(token)
    		.getBody()
    		.getSubject()
    		.equals(Config.getConfigProperty(Config.ADMIN_USER));
        // Throw an Exception if the token is invalid
    }

	
	
}