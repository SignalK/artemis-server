package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;
import static org.asynchttpclient.Dsl.asyncHttpClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereService;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;
@AtmosphereService(
		dispatch = true,
		interceptors = {AtmosphereResourceLifecycleInterceptor.class, TrackMessageSizeInterceptor.class},
		path = "/signalk/v1/apps",
		servlet = "org.glassfish.jersey.servlet.ServletContainer")
@Path("/signalk/v1/apps")
@Tag(name = "Webapp management API")
public class AppsService extends BaseApiService {
	
	private static final int BUFFER_SIZE = 4096;
	private static Logger logger = LogManager.getLogger(AppsService.class);

	public AppsService() throws Exception {
		super();
		logger.debug("AppService starting..");

	}

	@GET
	@Path("list")
	@Operation(summary = "Return a list of installed webapps", 
		description = "Concatenates the package.json files from the installed apps as a json array ")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "Successful retrieval of apps list"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Produces(MediaType.APPLICATION_JSON)
	
	public Response list(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie) {
		try {

			return Response.status(HttpStatus.SC_OK).entity(getAppList().toString()).build();

		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("install")
	@Operation(summary = "Install a webapp@version", description = "Installs the webapp")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "Successful install of appName@appVersion"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	public Response install(
			@Context UriInfo uriInfo, 
			@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter(description = "Name of webapp as found on npmjs.com", example="@signalk/freeboard-sk") @QueryParam("appName") String appName, 
			@Parameter(description = "Version of webapp as found on npmjs.com", example="1.0.0") @QueryParam("appVersion") String appVersion) {
		try {
			Thread t = new Thread() {

				@Override
				public void run() {
					try {
						runNpmInstall(getLogOutputFile("output.log"), staticDir, appName, appVersion);
					} catch (Exception e) {
						logger.error(e, e);
					}
				}
			};
			t.start();

			return Response.seeOther(uriInfo.getRequestUriBuilder().scheme(scheme).replacePath("/config/logs.html").replaceQuery(null).build()).build();

		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}


	@GET
	@Operation(summary = "Removes a webapp", description = "Removes the webapp")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "Successful removal of appName"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Path("remove")
	public Response remove(
			@Context UriInfo uriInfo, 
			@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter(description = "Name of webapp", example="@signalk/freeboard-sk") @QueryParam("appName") String appName) {
		try {
			File appDir = new File(staticDir, appName);
			if(appDir.isFile()) {
				return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Can only remove signalk web-apps").build();
			}
			if(!appDir.exists()) {
				return Response.status(HttpStatus.SC_BAD_REQUEST).entity("No such web-app: "+appName).build();
			}
			if(appDir.delete()) {
				return Response.status(HttpStatus.SC_OK).entity(appName+" removed").build();
			}
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(appName+" removal failed").build();
			

		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("search")
	@Operation(summary = "Search for a webapp", description = "Returns a list of availible signalk webapps from npmjs.org.")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "Successful retrieval of data"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(
			@Context UriInfo uriInfo, 
			@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter(description = "Npm tag, default 'signalk-webapp'", example="signalk-webapp")@QueryParam("keyword") String keyword) {
		try (final AsyncHttpClient c = asyncHttpClient();) {
			Json json = Util.getUrlAsJson(c, "https://api.npms.io/v2/search?size=250&q=keywords:"+StringUtils.defaultString(keyword, "signalk-webapp"));
			Json out = Json.array();
			for(Json pkg:json.at("results").asJsonList()) {
				pkg=pkg.at("package");
				if(isPlugin(pkg))continue;
				
				pkg.delAt("maintainers");
				pkg.delAt("keywords");
				pkg.delAt("scope");
				pkg.delAt("publisher");
				pkg.delAt("links");
				out.add(pkg);
			}
			return Response.status(HttpStatus.SC_OK).entity(out.toString()).build();

		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}


	private boolean isPlugin(Json pkg) {
		if(pkg.has("keywords")){
			for(Json k:pkg.at("keywords").asJsonList()) {
				if(StringUtils.equals("signalk-node-server-plugin", k.asString())) {
					return true;
				}
			}
		}
		return false;
	}

	private void runNpmInstall(final File output, File destDir, String name, String version) throws Exception {
		destDir.mkdirs();
		logger.debug("Beginning install for \" + name + \"@\" + version");
		FileUtils.writeStringToFile(output, "\nBeginning install for " + name + "@" + version, true);
		// npm --save install ' + `${name}@${version}

		File download = new File(staticDir,"download");
		download.mkdirs();
		//download
		File tmp = File.createTempFile(name, ".tgz", download);
		
		try (final AsyncHttpClient client = asyncHttpClient();) {

			Json json = Util.getUrlAsJson(client, "https://registry.npmjs.org/"+name);
			String url = json.at("versions").at(version).at("dist").at("tarball").asString();
			FileUtils.writeStringToFile(output, "\nDownloading " + url, true);
			
			FileUtils.copyURLToFile(
					  new URL(url), 
					  tmp, 
					  30000, 
					  30000);
				
			//unpack
			logger.debug("Unpacking " + name + "@" + version);
			FileUtils.writeStringToFile(output, "\nUnpacking " + name + "@" + version, true);
			unpack(tmp, name,staticDir);
	        logger.debug("Install completed successfully!");
			FileUtils.writeStringToFile(output, "\nDONE: Install ended sucessfully", true);
		} catch (Exception e) {
			try {
				logger.error(e);
				FileUtils.writeStringToFile(output, "\nInstall ended badly:" + e.getMessage(), true);
				FileUtils.writeStringToFile(output, "\n" + ExceptionUtils.getStackTrace(e), true);
			} catch (IOException e1) {
				logger.error(e1);
			}
		}

	}

	private void unpack(File tmp, String name, File staticDir) throws FileNotFoundException, IOException {
		 //npm apps will be tarred into 'node_modules/app_name/dist/, need to remove node_modules/
		try (FileInputStream in = new FileInputStream(tmp);
				 GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
				 TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
		        
			TarArchiveEntry entry;
			File appDir = new File(staticDir, name);
			appDir.mkdirs();
			//TODO: delete existing ?
			
	        while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
	            /** If the entry is a directory, create the directory. **/
	            if (entry.isDirectory()) {
	                File f = new File(appDir,truncate(entry.getName()));
	                boolean created = f.mkdir();
	                if (!created) {
	                	logger.debug("Unable to create directory '{}', during extraction of archive contents.\n",
	                            f.getAbsolutePath());
	                }
	            } else {
	                int count;
	                byte data[] = new byte[BUFFER_SIZE];
	                File f = new File(appDir,truncate(entry.getName()));
	                f.getParentFile().mkdirs();
	                try (FileOutputStream fos = new FileOutputStream(f);
	                		BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE)) {
	                    while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
	                        dest.write(data, 0, count);
	                    }
	                }
	            }
	        }

	        logger.debug("Untar completed successfully!");
	    }
		
	}

	private String truncate(String name) {
		return StringUtils.removeStart(name, "node_modules/");
	}
}
