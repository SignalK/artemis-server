package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.STATIC_DIR;

/*
 * 
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 * 
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import nz.co.fortytwo.signalk.artemis.util.Config;

/*
 * Processes REST requests for Signal K installs from Github
 * 
 * 
 * @author robert
 */
@Path("/signalk/apps")
public class GitService extends BaseApiService {

	private static final String SLASH = "/";
	private static Logger logger = LogManager.getLogger(GitService.class);
	private File staticDir = null;

	private static String github = "https://github.com/";

	public GitService() {
		super();
		staticDir = new File(Config.getConfigProperty(STATIC_DIR));
	}

	/**
	 * Process a signalk install
	 * 
	 * @param request
	 * @param response
	 * @param signalkModel
	 * @return
	 * @throws IOException
	 */
	
	@GET
	@Path("install")
	@Produces(MediaType.TEXT_PLAIN)
	public Response processInstall(@QueryParam("project") String project, @QueryParam("npm") boolean npm)
			throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("We are processing the path = {}, npm={}", project, npm);

		// check valid request.
		if (StringUtils.isBlank(project)) {
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
		}
		// now we should have a valid github project name
		try {
			Thread t = new Thread() {

				@Override
				public void run() {
					try {
						
						if (install(project) && npm) {
							int p = project.lastIndexOf("/");
							String name=(p>0)? project.substring(p+1):project;
							runNpmInstall(getLogOutputFile("output.log"), new File(staticDir, SLASH + name));
							runNpmBuild(getLogOutputFile("output.log"), new File(staticDir, SLASH + name));
						}
					} catch (Exception e) {
						logger.error(e,e);
					}
				}
			};
			t.start();
			
			return Response.seeOther(new URI("/config/logs.html")).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
		}

	}

	/**
	 * Process a signalk install
	 * 
	 * @param request
	 * @param response
	 * @param signalkModel
	 * @return
	 * @throws IOException
	 */
	
	@GET
	@Path("upgrade")
	@Produces(MediaType.TEXT_PLAIN)
	public Response processUpgrade(@QueryParam("project") String project, @QueryParam("npm") boolean npm)
			throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("We are processing the path = {}, npm={}", project, npm);

		// check valid request.
		if (StringUtils.isBlank(project)) {
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
		}
		// now we should have a valid github project name
		try {
			Thread t = new Thread() {

				@Override
				public void run() {
					try {
						int p = project.lastIndexOf("/");
						String name=(p>0)? project.substring(p+1):project;
						if (upgrade(name) && npm) {
							runNpmInstall(getLogOutputFile("output.log"), new File(staticDir, SLASH + name));
							runNpmBuild(getLogOutputFile("output.log"), new File(staticDir, SLASH + name));
						}
					} catch (Exception e) {
						logger.error(e,e);
					}
				}
			};
			t.start();
			
			return Response.seeOther(new URI("/config/logs.html")).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
		}

	}

	protected boolean install(String path) throws Exception {
		// staticDir.mkdirs();
		Git result = null;
		try {

			// make log name
			String logFile = "output.log";
			File output = getLogOutputFile(logFile);
			
			int p = path.lastIndexOf("/");
			String name=(p>0)? path.substring(p+1):path;
			File destDir = new File(staticDir, SLASH + name);
			destDir.mkdirs();
			String gitPath = github + path + ".git";
			logger.debug("Cloning from " + gitPath + " to " + destDir.getAbsolutePath());
			FileUtils.writeStringToFile(output, "Cloning from " + gitPath + " to " + destDir.getAbsolutePath() + "\n",
					false);
			try {
				result = Git.cloneRepository().setURI(gitPath).setDirectory(destDir).call();
				result.fetch().setRemote(gitPath);
				logger.debug("Cloned " + gitPath + " repository: " + result.getRepository().getDirectory());
				FileUtils.writeStringToFile(output,
						"DONE: Cloned " + gitPath + " repository: " + result.getRepository().getDirectory(), true);
			} catch (Exception e) {
				FileUtils.writeStringToFile(output, e.getMessage(), true);
				FileUtils.writeStringToFile(output, ExceptionUtils.getStackTrace(e), true);
				logger.debug("Error updating " + gitPath + " repository: " + e.getMessage(), e);
				return false;
			}

			return true;

		} finally {
			if (result != null)
				result.close();
		}

	}

	private File getLogOutputFile(String logFile) {
		File installLogDir = new File(staticDir, "logs");
		installLogDir.mkdirs();
		// make log name
		File output = new File(installLogDir, logFile);
		return output;
	}

	private void runNpmInstall(final File output, File destDir) throws Exception {
		FileUtils.writeStringToFile(output, "\nBeginning npm install", true);
		ProcessBuilder pb = new ProcessBuilder("npm", "install");
		Map<String, String> env = System.getenv();
		if (env.containsKey("PATH")) {
			pb.environment().put("PATH", env.get("PATH"));
		}
		if (env.containsKey("Path")) {
			pb.environment().put("Path", env.get("Path"));
		}
		if (env.containsKey("path")) {
			pb.environment().put("path", env.get("path"));
		}
		pb.directory(destDir);
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(output));
		final Process p = pb.start();

		try {
			p.waitFor();
			FileUtils.writeStringToFile(output, "\nDONE: Npm ended sucessfully", true);
		} catch (Exception e) {
			try {
				logger.error(e);
				FileUtils.writeStringToFile(output, "\nNpm ended badly:" + e.getMessage(), true);
				FileUtils.writeStringToFile(output, "\n" + ExceptionUtils.getStackTrace(e), true);
			} catch (IOException e1) {
				logger.error(e1);
			}
		}

	}
	
	private void runNpmBuild(final File output, File destDir) throws Exception {
		FileUtils.writeStringToFile(output, "\nBeginning npm build", true);
		ProcessBuilder pb = new ProcessBuilder("npm", "run","build");
		Map<String, String> env = System.getenv();
		if (env.containsKey("PATH")) {
			pb.environment().put("PATH", env.get("PATH"));
		}
		if (env.containsKey("Path")) {
			pb.environment().put("Path", env.get("Path"));
		}
		if (env.containsKey("path")) {
			pb.environment().put("path", env.get("path"));
		}
		pb.directory(destDir);
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(output));
		final Process p = pb.start();

		try {
			p.waitFor();
			FileUtils.writeStringToFile(output, "\nDONE: Npm ended sucessfully", true);
		} catch (Exception e) {
			try {
				logger.error(e);
				FileUtils.writeStringToFile(output, "\nNpm ended badly:" + e.getMessage(), true);
				FileUtils.writeStringToFile(output, "\n" + ExceptionUtils.getStackTrace(e), true);
			} catch (IOException e1) {
				logger.error(e1);
			}
		}

	}

	protected boolean upgrade(String path) throws Exception {
		// staticDir.mkdirs();
		Repository repository = null;
		try {
			String logFile = "output.log";
			File output = getLogOutputFile(logFile);
			//
			File destDir = new File(staticDir, SLASH + path);
			destDir.mkdirs();

			String gitPath = github + path + ".git";
			logger.debug("Updating from " + gitPath + " to " + destDir.getAbsolutePath());
			FileUtils.writeStringToFile(output, "Updating from " + gitPath + " to " + destDir.getAbsolutePath() + "\n",
					false);
			Git git = null;
			try {
				FileRepositoryBuilder builder = new FileRepositoryBuilder();
				repository = builder.setGitDir(new File(destDir, "/.git")).readEnvironment() // scan
																								// environment
																								// GIT_*
																								// variables
						.findGitDir() // scan up the file system tree
						.build();
				git = new Git(repository);
				PullResult result = git.pull().call();
				FileUtils.writeStringToFile(output, result.getMergeResult().toString(), true);
				logger.debug("DONE: Updated " + gitPath + " repository: " + result.getMergeResult().toString());

				// now run npm install
				// runNpmInstall(output, destDir);
			} catch (Exception e) {
				FileUtils.writeStringToFile(output, e.getMessage(), true);
				FileUtils.writeStringToFile(output, ExceptionUtils.getStackTrace(e), true);
				logger.debug("Error updating " + gitPath + " repository: " + e.getMessage(), e);
				return false;
			} finally {
				if (git != null)
					git.close();
				if (repository != null)
					repository.close();
			}
			return true;

		} finally {
			if (repository != null)
				repository.close();
		}

	}

}
