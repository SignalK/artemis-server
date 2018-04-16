package nz.co.fortytwo.signalk.artemis.service;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;

public class SecurityService {
	
	
	private static Logger logger = LogManager.getLogger(InfluxDbService.class);
	private ConcurrentSkipListMap<String,Json> secure = new ConcurrentSkipListMap<>();
	
	public static final String OTHER_WRITE = "o_w";
	public static final String OTHER_READ = "o_r";
	public static final String ROLE_WRITE = "r_w";
	public static final String ROLE_READ = "r_r";
	public static final String ROLE = "role";
	public static final String OWNER = "owner";
	public static final String DEFAULT_OWNER = "guest";
	public static final String DEFAULT_ROLE = "guest";
	public static final boolean DEFAULT_ROLE_READ = true;
	public static final boolean DEFAULT_ROLE_WRITE = false;
	public static final boolean DEFAULT_OTHER_READ = false;
	public static final boolean DEFAULT_OTHER_WRITE = false;
	
	public SecurityService(){
		secure.put("vessels.urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c.navigation",Json.object(OWNER, "admin", ROLE,"crew", ROLE_READ,true, ROLE_WRITE, true, OTHER_READ,false, OTHER_WRITE, false));
		secure.put("vessels.urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c.navigation.position",Json.object(OWNER, "admin", ROLE,"crew",  ROLE_READ,true, ROLE_WRITE, true, OTHER_READ,true, OTHER_WRITE, false));
		//secure.put("vessels.urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c.navigation.speedOverGround",Json.object("owner", "admin", "role","crew", "attr","rwx------"));
		secure.put("config",Json.object(OWNER, "admin", ROLE,"admin",  ROLE_READ,true, ROLE_WRITE, true, OTHER_READ,false, OTHER_WRITE, false));
		
	}
	public  NavigableMap<String, Json> addAttributes( NavigableMap<String, Json> map){
		map.forEach((k,v)->{
			if(k.endsWith("_attr"))return;
			logger.debug("Securing :{}={}",k,v);
			Json attrs = secure.get(k);
			String subk = k;
			while(attrs==null){
				subk = StringUtils.substringBeforeLast(subk,".");
				attrs = secure.get(subk);
				logger.debug("Found :{}={}", subk,  attrs);
				if(subk.indexOf(".") < 0)break;
			}
			 
			if(attrs==null){
				attrs= Json.object(OWNER, DEFAULT_OWNER, ROLE,DEFAULT_ROLE, ROLE_READ,DEFAULT_ROLE_READ, ROLE_WRITE, DEFAULT_ROLE_WRITE, OTHER_READ,DEFAULT_OTHER_READ, OTHER_WRITE, DEFAULT_OTHER_WRITE );
			}
			logger.debug("Secured :{}",  attrs);
			map.put(k+"._attr",attrs);
		});
		return map;
	}
}
