package nz.co.fortytwo.signalk.artemis.server;

import javax.crypto.SecretKey;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import nz.co.fortytwo.signalk.artemis.util.Config;

@Path("/authentication")
public class AuthenticationService {

	private static SecretKey key = MacProvider.generateKey();
	
    public static SecretKey getKey() {
		return key;
	}

	@POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response authenticateUser(@FormParam("username") String username, 
                                     @FormParam("password") String password) {

        try {

            // Authenticate the user using the credentials provided
            authenticate(username, password);

            // Issue a token for the user
            String token = issueToken(username);

            // Return the token on the response
            return Response.ok(token).build();

        } catch (Exception e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }      
    }

    private void authenticate(String username, String password) throws Exception {
        // Authenticate against a database, LDAP, file or whatever
    	if(!Config.getConfigProperty(Config.ADMIN_USER).equals(username)
    			&& !Config.getConfigProperty(Config.ADMIN_PWD).equals(password)){
    		// Throw an Exception if the credentials are invalid
    		throw new SecurityException("Username or password invalid");
    	}
        
    }

    private String issueToken(String username) {
        // Issue a token (can be a random String persisted to a database or a JWT token)
        // The issued token must be associated to a user
    	 

    	String compactJws = Jwts.builder()
    	  .setSubject("Joe")
    	  .signWith(SignatureAlgorithm.HS512, key)
    	  .compact();
        // Return the issued token
    	return compactJws;
    }
}