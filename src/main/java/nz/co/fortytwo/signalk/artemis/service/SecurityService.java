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
	
//	public static final String OTHER_WRITE = "o_w";
//	public static final String OTHER_READ = "o_r";
//	public static final String ROLE_WRITE = "r_w";
//	public static final String ROLE_READ = "r_r";
	public static final String GROUP = "grp";
	public static final String OWNER = "owner";
	public static final String OWNER_DEFAULT = "admin";
	public static final String GROUP_DEFAULT = "admin";
	public static final String GROUP_CREW = "crew";
	public static final String GROUP_GUESTS = "guests";
	public static final String GROUP_FRIENDS = "friends";
	public static final String GROUP_OFFICIAL = "official";
	public static final String GROUP_PUBLIC = "public";
	
	
//	public static final boolean DEFAULT_ROLE_READ = true;
//	public static final boolean DEFAULT_ROLE_WRITE = false;
//	public static final boolean DEFAULT_OTHER_READ = false;
//	public static final boolean DEFAULT_OTHER_WRITE = false;
	
	public SecurityService(){
		secure.put("vessels.urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c.navigation",Json.object(OWNER, OWNER_DEFAULT, GROUP,GROUP_CREW));
		secure.put("vessels.urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c.navigation.position",Json.object(OWNER, OWNER_DEFAULT, GROUP,GROUP_GUESTS));
		//secure.put("vessels.urn:mrn:signalk:uuid:705f5f1a-efaf-44aa-9cb8-a0fd6305567c.navigation.speedOverGround",Json.object("owner", "admin", "role","crew", "attr","rwx------"));
		secure.put("config",Json.object(OWNER, OWNER_DEFAULT, GROUP,GROUP_DEFAULT));
		
	}
	public  NavigableMap<String, Json> addAttributes( NavigableMap<String, Json> map){
		logger.debug("Securing :{}",map);
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
				attrs= Json.object(OWNER, OWNER_DEFAULT, GROUP,GROUP_DEFAULT );
			}
			logger.debug("Secured :{}",  attrs);
			map.put(k+"._attr",attrs);
		});
		return map;
	}
}
