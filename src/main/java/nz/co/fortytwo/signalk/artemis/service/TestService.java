package nz.co.fortytwo.signalk.artemis.service;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/courses")
public class TestService {
	private static Logger logger = LogManager.getLogger(TestService.class);

    public TestService() {
		super();
		logger.debug("TestService starting..");
	}

	@GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> fetchAll() {
    	logger.debug("fetching all");
        List<String> courses = new ArrayList<>();
        courses.add( "Configure Jersey with annotations");
        courses.add("Configure Jersey without web.xml");
        return courses;
    }

}