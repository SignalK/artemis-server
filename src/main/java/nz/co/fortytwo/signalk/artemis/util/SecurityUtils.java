package nz.co.fortytwo.signalk.artemis.util;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE_AIS;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_USER_TOKEN;
import static nz.co.fortytwo.signalk.artemis.util.Config.MSG_SRC_TYPE_EXTERNAL_IP;
import static nz.co.fortytwo.signalk.artemis.util.Config.MSG_SRC_TYPE_INTERNAL_IP;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE_JSON_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE_JSON_FULL;
import static nz.co.fortytwo.signalk.artemis.util.Config.MSG_SRC_BUS;
import static nz.co.fortytwo.signalk.artemis.util.Config.MSG_SRC_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE_N2K;
import static nz.co.fortytwo.signalk.artemis.util.Config.MSG_SRC_TYPE_SERIAL;
import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE__0183;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_MSG_TOKEN;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.servlet.http.Cookie;
import javax.ws.rs.core.HttpHeaders;

import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.joda.time.DateTime;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.PasswordStorage.CannotPerformOperationException;

public final class SecurityUtils {
	
	private static Logger logger = LogManager.getLogger(SecurityUtils.class);

	private static java.nio.file.Path target = Paths.get("./conf/security-conf.json");
	private static final String PASSWORD = "password";
	private static final String LAST_CHANGED = "lastPasswordChange";
	public static final String USERS = "users";
	public static final String ROLES = "roles";
	public static final String REALM = "signalk";
	public static final String AUTHENTICATION_SCHEME = "Bearer";
	public static final String AUTH_COOKIE_NAME = "SK_TOKEN";
	public static final int EXPIRY = 24*60;
	private static final String HASH = "hash";
	private static SecretKey key = MacProvider.generateKey();

	private static Json securityConf;

	private static ConcurrentHashMap<String, String> tokenStore=new ConcurrentHashMap<>();
	private static String[] systemUsers = {"ais", "serial", "n2k","tcp_internal","tcp_external"};

	private static List<String> invalidTokens = Collections.synchronizedList(new ArrayList<String>());
	
	public static void validateTokenStore() throws Exception  {
		for(String name:tokenStore.keySet()) {
			tokenStore.put(name, validateToken(tokenStore.get(name)));
		}
		//also clean invalidTokens
		Iterator<String> it = invalidTokens.iterator();
		while (it.hasNext()) {
			String token = (String) it.next();
			try {
				//will fail if invalid/expired
				Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
			}catch (Exception e) {
				it.remove();
			}
		}
	}
	
	private static Json getUser(Json conf, String name) throws Exception {
		List<Json> list = conf.at("users").asJsonList();
		for(Json user:list) {
			if(StringUtils.equals(user.at("name").asString(),name)) {
				return user;
			}
		}
		return null;
	}
	
	private static Json addUser(Json conf, String name, String password, String email, Json roles) throws Exception {
		
		List<Json> list = conf.at("users").asJsonList();
		list.add(Json.object()
				.set("name", name)
				.set("role",roles)
				.set("password",password)
				.set("email", email)
				);
		return conf;
	}
	
	public static void setForbidden(AtmosphereResource r) {
		r.getResponse().setHeader(HttpHeaders.AUTHORIZATION, "Basic realm=\""+REALM+"\"");
		r.getResponse().setStatus(HttpStatus.SC_FORBIDDEN, "Forbidden");
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
		// Throw an Exception if the token is invalid
		if(invalidTokens.contains(token)){
			logger.debug("Invalid token: {}", token);
			throw new SecurityException("Token is logged out");
		}
		Claims body = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
		
		//renew if near expiry
		if((System.currentTimeMillis()-body.getExpiration().getTime())< (body.getExpiration().getTime()*0.1)) {
			return issueToken(body.getSubject(), Json.read(body.get(ROLES,String.class)));
		}
		return token;
	}
	
	public static String getSubject(String token) throws Exception {
		// Check if the token was issued by the server and if it's not expired
		return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody().getSubject();
		
	}
	
	public static Json getRoles(String token) {
		if(StringUtils.isBlank(token))return Json.read("[\"public\"]");
		try {
			return Json.read(Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody().get(ROLES, String.class));
		}catch (Exception e) {
			return Json.read("[\"public\"]");
		}
	}
	

	public static String authenticateUser(String username, String password) throws Exception {
		//load user json
		Json conf = getSecurityConfAsJson();
		Json users=conf.at(USERS);
		logger.debug("Users: {}",users);
		for(Json user : users.asJsonList()) {
			logger.debug("Checking: {}",user);
			if(username.equals(user.at("name").asString()) 
					&& PasswordStorage.verifyPassword(password, user.at(HASH).asString())){
				Json roles = user.at(ROLES);
				return issueToken(username, roles);
			}
		}
		throw new SecurityException("Username or password invalid");
	}

	public static String issueToken(String username, Json roles) {
		// Issue a token (can be a random String persisted to a database or a JWT token)
		// The issued token must be associated to a user
		Claims claims = Jwts.claims();
		claims.put(ROLES, roles.toString());
		
		String compactJws = Jwts.builder()
				.setSubject(username)
				.setClaims(claims)
				.setIssuedAt(DateTime.now().toDate())
				.setExpiration(DateTime.now().plusMinutes(EXPIRY).toDate())
				.signWith(SignatureAlgorithm.HS512, key)
				.compact();
		// Return the issued token
		if(logger.isDebugEnabled())logger.debug("Issue token: {}",compactJws);
		return compactJws;
	}

	public static void save(String body) throws IOException {
		Json conf = Json.read(body);
		for( Json user : conf.at(USERS).asJsonList()) {
			//hash any new passwords
			String pass = user.at(PASSWORD).asString();
			if(StringUtils.isNotBlank(pass)) {
				try {
					user.set(HASH,PasswordStorage.createHash(pass));
					user.set(PASSWORD, "");
					user.set(LAST_CHANGED, Util.getIsoTimeString());
				} catch (CannotPerformOperationException e) {
					logger.error(e,e);
				}
			}
		}
		FileUtils.writeStringToFile(target.toFile(), conf.toString());
		securityConf=null;
	}
	public static byte[] getSecurityConfAsBytes() throws IOException {
		return FileUtils.readFileToByteArray(target.toFile());
	}
	
	public static Json getSecurityConfAsJson() throws IOException {
		if(securityConf==null) {
			securityConf=Json.read(FileUtils.readFileToString(target.toFile()));
		}
		return securityConf;
	}



	public static Cookie updateCookie(Cookie c, String token) {
		if(c==null)
			c = new Cookie(AUTH_COOKIE_NAME, token);
		else
			c.setValue(token);
		c.setMaxAge(3600);
		c.setHttpOnly(false);
		c.setPath("/");
		return c;
	}
	
	

	public static ArrayList<String> getDeniedReadPaths(String rolesStr) throws Exception {
		if(StringUtils.isBlank(rolesStr)) return  new ArrayList<>();
		return getDeniedReadPaths(Json.read(rolesStr));
	}
	public static ArrayList<String> getDeniedReadPaths(Json roles) throws Exception {
		ArrayList<String> denied = new ArrayList<>();
		for(Json r : roles.asJsonList()) {
			for(Json d : getSecurityConfAsJson().at(ROLES).at(r.asString()).at("denied")) {
				if(d.at("read").asBoolean()) {
					denied.add(d.at("name").asString());
				}
			}
		}
		
		return denied;
		
	}
	
	public static ArrayList<String> getDeniedWritePaths(String rolesStr) throws Exception {
		if(StringUtils.isBlank(rolesStr)) return new ArrayList<>();
		return getDeniedWritePaths(Json.read(rolesStr));
	}
	public static ArrayList<String> getDeniedWritePaths(Json roles) throws Exception {
		ArrayList<String> denied = new ArrayList<>();
		for(Json r : roles.asJsonList()) {
			for(Json d : getSecurityConfAsJson().at(ROLES).at(r.asString()).at("denied")) {
				if(d.at("write").asBoolean()) {
					denied.add(d.at("name").asString());
				}
			}
		}
		
		return denied;
		
	}
	public static ArrayList<String> getAllowedReadPaths(String rolesStr) throws Exception {
		if(StringUtils.isBlank(rolesStr)) return new ArrayList<>();
		return getAllowedReadPaths(Json.read(rolesStr));
	}
	public static ArrayList<String> getAllowedReadPaths(Json roles) throws Exception {
		ArrayList<String> allowed = new ArrayList<>();
		for(Json r : roles.asJsonList()) {
			for(Json d : getSecurityConfAsJson().at(ROLES).at(r.asString()).at("allowed")) {
				if(d.at("read").asBoolean()) {
					allowed.add(Util.fixSelfKey(d.at("name").asString()));
				}
			}
		}
		
		return allowed;
		
	}
	
	public static ArrayList<String> getAllowedWritePaths(String rolesStr) throws Exception {
		if(StringUtils.isBlank(rolesStr)) return new ArrayList<>();
		return getAllowedWritePaths(Json.read(rolesStr));
	}
	public static ArrayList<String> getAllowedWritePaths(Json roles) throws Exception {
		ArrayList<String> allowed = new ArrayList<>();
		for(Json r : roles.asJsonList()) {
			for(Json d : getSecurityConfAsJson().at(ROLES).at(r.asString()).at("allowed")) {
				if(d.at("write").asBoolean()) {
					allowed.add(Util.fixSelfKey(d.at("name").asString()));
				}
			}
		}
		
		return allowed;
		
	}

	public static void trimMap( final NavigableMap<String, Json> map, String roles) throws Exception {
	
		ArrayList<String> denied = SecurityUtils.getDeniedReadPaths(roles);
		ArrayList<String> allowed = SecurityUtils.getAllowedReadPaths(roles);
		trimMap(map, allowed, denied);
	
	}
	public static void trimMap(NavigableMap<String, Json> rslt, ArrayList<String> allowed, ArrayList<String> denied) {
		// check allowed first
		try {
			if (allowed == null || allowed.size() == 0)
				return;
			for (String key : allowed) {
				if (key.equals("all")) {
					return;
				}
			}

			rslt.entrySet().removeIf(entry->{
				for (String a : allowed) {
					if (entry.getKey().startsWith(a)) {
						return false;
					}
				}
				return true;
			});
			
			return;
			
		} catch (Exception e) {
			logger.error(e, e);
		}

		// check denied
		try {
			if (denied == null || denied.size() == 0)
				return;
			for (String key : denied) {
				if (key.equals("all")) {
					rslt.clear();
					return;
				}
				rslt.entrySet().removeIf(entry->{
					for (String a : allowed) {
						if (entry.getKey().startsWith(a)) {
							return true;
						}
					}
					return false;
				});
				return;
			}
		} catch (Exception e) {
			logger.error(e, e);
		}

	}

	public static void injectTokenIntoMessage(ICoreMessage message, Json node) {
		if(node.has(SK_MSG_TOKEN)) {
			message.putStringProperty(AMQ_USER_TOKEN, node.at(SK_MSG_TOKEN).asString());
		}
		
	}

	public static void injectToken(Message msg) throws Exception {
		
		//AIS, 0183
		String msgType = msg.getStringProperty(AMQ_CONTENT_TYPE);
		//EXTERNAL_IP, SERIAL
		String msgSrc = msg.getStringProperty(MSG_SRC_TYPE);
		//ip, /dev/ttyUSB0
		String msgBus = msg.getStringProperty(MSG_SRC_BUS);
		if(logger.isDebugEnabled())logger.debug("Inject token: {} ,{},{}",msgType, msgSrc, msgBus);
		switch (msgType) {
		case AMQ_CONTENT_TYPE_AIS:
			if(StringUtils.equals(MSG_SRC_TYPE_SERIAL,msgSrc) || StringUtils.equals(AMQ_CONTENT_TYPE_N2K,msgSrc)) {
				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("ais"));
			}
			if(StringUtils.equals(MSG_SRC_TYPE_INTERNAL_IP,msgSrc)) {
				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("tcp_internal"));
			}
			break;
		case AMQ_CONTENT_TYPE__0183:
			if(StringUtils.equals(MSG_SRC_TYPE_SERIAL,msgSrc)) {
				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("serial"));
			}

			if(StringUtils.equals(MSG_SRC_TYPE_INTERNAL_IP,msgSrc)) {
				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("tcp_internal"));
			}
			if(StringUtils.equals(MSG_SRC_TYPE_EXTERNAL_IP,msgSrc)) {
				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("tcp_external"));
			}
			break;
		case AMQ_CONTENT_TYPE_N2K:
			if(StringUtils.equals(AMQ_CONTENT_TYPE_N2K,msgSrc)) {
				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("n2k"));
			}
			if(StringUtils.equals(MSG_SRC_TYPE_INTERNAL_IP,msgSrc)) {
				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("tcp_internal"));
			}
			break;
		case AMQ_CONTENT_TYPE_JSON_FULL:
			//signalk without auth over serial
			if(StringUtils.equals(MSG_SRC_TYPE_SERIAL,msgSrc)) {
				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("serial"));
			}
			//signalk over internal
//			if(StringUtils.equals(INTERNAL_IP,msgSrc)) {
//				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("tcp_internal"));
//			}
//			if(StringUtils.equals(EXTERNAL_IP,msgSrc)) {
//				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("tcp_external"));
//			}
			break;
		case AMQ_CONTENT_TYPE_JSON_DELTA:
			//signalk without auth over serial
			if(StringUtils.equals(MSG_SRC_TYPE_SERIAL,msgSrc)) {
				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("serial"));
			}
			//signalk over internal
//			if(StringUtils.equals(INTERNAL_IP,msgSrc)) {
//				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("tcp_internal"));
//			}
//			if(StringUtils.equals(EXTERNAL_IP,msgSrc)) {
//				msg.putStringProperty(AMQ_USER_TOKEN, tokenStore.get("tcp_external"));
//			}
			break;
			
		
		default:
			break;
		}
		if(logger.isDebugEnabled())logger.debug("Injected token: {} : {} ,{},{}",msg.getStringProperty(AMQ_USER_TOKEN),msgType, msgSrc, msgBus);
	} 

	public static void checkSystemUsers() throws Exception {
		Json conf = getSecurityConfAsJson();
		Json roles = conf.at(ROLES);

		if(roles.at("serial")==null) {
			conf.at(ROLES).set("serial",Json.read("{ \"allowed\": [ { \"read\": true, \"name\": \"all\", \"write\": true } ], \"denied\": [] }"));
		}
		if(roles.at("n2k")==null) {
			conf.at(ROLES).set("n2k",Json.read("{ \"allowed\": [ { \"read\": true, \"name\": \"all\", \"write\": true } ], \"denied\": [] }"));
		}
		if(roles.at("ais")==null) {
			conf.at(ROLES).set("ais",Json.read("{ \"allowed\": [ { \"read\": true, \"name\": \"vessels.self\", \"write\": false } ], \"denied\": [] }"));
		}
		if(roles.at("tcp_internal")==null) {
			conf.at(ROLES).set("tcp_internal",Json.read("{ \"allowed\": [ { \"read\": true, \"name\": \"all\", \"write\": true } ], \"denied\": [] }"));
		}
		if(roles.at("tcp_external")==null) {
			conf.at(ROLES).set("tcp_external",Json.read("{ \"allowed\": [], \"denied\": [ { \"read\": true, \"name\": \"all\", \"write\": true } ] }"));
		}
		
		for(String name: systemUsers) {
			if (getUser(conf,name) == null) {
				addUser(conf,name, java.util.UUID.randomUUID().toString(), "", Json.array().add(name));
			}
			String token = issueToken(name,getUser(conf, name).at(ROLES) );
			tokenStore.put(name, token);
		}
		SecurityUtils.save(conf.toString());
	}

	public static void invalidateToken(String token) {
		invalidTokens.add(token);
	}

}
