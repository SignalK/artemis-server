package nz.co.fortytwo.signalk.artemis.service;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/apps")
@Api (value = "/signalk/v1/apps")
public class AppsService extends BaseApiService {
	
	private static final int BUFFER_SIZE = 4096;
	private static Logger logger = LogManager.getLogger(AppsService.class);

	public AppsService() throws Exception {
		super();
		logger.debug("AppService starting..");

	}

	@GET
	@Path("list")
	@ApiOperation(value = "Return a list of installed webapps", notes = "Concatenates the package.json files from the installed apps as a json array ", response = String.class)
	@Produces(MediaType.APPLICATION_JSON)
	public Response list() {
		try {
			Json list = Json.array();
			for (File f : staticDir.listFiles()) {
				if (f.isFile())
					continue;
				File p = new File(f, "package.json");
				if (p.exists()) {
					String s = FileUtils.readFileToString(p);
					list.add(Json.read(s));
				}

			}
			return Response.status(HttpStatus.SC_OK).entity(list.toString()).build();

		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("install")
	@ApiOperation(value = "Install a webapp@version", notes = "Installs the webapp")
	public Response install(@QueryParam("appName") String appName, @QueryParam("appVersion") String appVersion) {
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

			return Response.seeOther(new URI("/config/logs.html")).build();

		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	@GET
	@ApiOperation(value = "Update a webapp@version", notes = "Removes any current version and install the new webapp@version")
	@Path("update")
	public Response update(@QueryParam("appName") String appName, @QueryParam("appVersion") String appVersion) {
		Response resp = remove(appName);
		if(HttpStatus.SC_OK != resp.getStatus()) {
			return resp;
		}
		return install(appName, appVersion);
	}

	@GET
	@ApiOperation(value = "Removes a webapp", notes = "Removes the webapp")
	@Path("remove")
	public Response remove(@QueryParam("appName") String appName) {
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
	@ApiOperation(value = "Search for a webapp", notes = "Returns a list of avaliable signalk webapps from npmjs.org.")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(@QueryParam("keyword") String keyword) {
		try (final AsyncHttpClient c = asyncHttpClient();) {
			Json json = Util.getUrlAsJson(c, "https://api.npms.io/v2/search?size=250&q=keywords:signalk-webapp");

			return Response.status(HttpStatus.SC_OK).entity(json.at("results").toString()).build();

		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	private void runNpmInstall(final File output, File destDir, String name, String version) throws Exception {
		destDir.mkdirs();
		FileUtils.writeStringToFile(output, "\nBeginning install for " + name + "@" + version, true);
		// npm --save install ' + `${name}@${version}

		File download = new File(staticDir,"download");
		download.mkdirs();
		//download
		File tmp = File.createTempFile(name, ".tgz", download);
		
		try (final AsyncHttpClient client = asyncHttpClient();
				FileOutputStream out = new FileOutputStream(tmp);) {

			Json json = Util.getUrlAsJson(client, "https://registry.npmjs.org/"+name);
			String url = json.at("versions").at(version).at("dist").at("tarball").asString();
			FileUtils.writeStringToFile(output, "\nDownloading " + url, true);
			
			
			AsyncCompletionHandler<Response> asyncHandler = new AsyncCompletionHandler<Response>() {

				   @Override
				   public State onBodyPartReceived(final HttpResponseBodyPart content)
				     throws Exception {
				       out.write(content.getBodyPartBytes());
				       return State.CONTINUE;
				   }
				
					@Override
					public Response onCompleted(org.asynchttpclient.Response response) throws Exception {
						return Response.status(HttpStatus.SC_OK).build();
					}
				};

				client.prepareGet(url).execute(asyncHandler).get();
				
			//unpack
			FileUtils.writeStringToFile(output, "\nUnpacking " + name + "@" + version, true);
			unpack(tmp, name,staticDir);
			
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
	                File f = new File(appDir,entry.getName());
	                boolean created = f.mkdir();
	                if (!created) {
	                	logger.debug("Unable to create directory '{}', during extraction of archive contents.\n",
	                            f.getAbsolutePath());
	                }
	            } else {
	                int count;
	                byte data[] = new byte[BUFFER_SIZE];
	                File f = new File(appDir,entry.getName());
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
}
