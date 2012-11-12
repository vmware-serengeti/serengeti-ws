/*
 * Copyright 2011-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.hadoop.impala.r;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Provider of R commands.
 * 
 * @author Costin Leau
 */
@Component
public class RCommands implements CommandMarker {

	private static final String PREFIX = "r ";
	private static final String BIN = "bin" + File.separator + "Rscript";
	private String home = "";
	private String cmd = home + BIN;
	private File wkdir = new File(".");

	private ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(
			new FileSystemResourceLoader());

	public RCommands() {
		String os = System.getProperty("os.name").toLowerCase();

		// set default R paths - best effort
		if (os.contains("win")) {
			home = "C:\\Program Files\\R\\";
			File f = new File(home);
			if (f.exists() && f.isDirectory()) {
				File[] listFiles = f.listFiles();
				if (listFiles != null && listFiles.length > 0) {
					home = listFiles[0].getAbsolutePath() + "\\";
				}
			}
		}
		else if (os.contains("mac")) {
			home = "/Library/Frameworks/R.framework/Resources/";
		}
		else if (os.contains("sunos")) {
			home = "/usr/lib/";
		}
		else {
			// else linux/unix
			home = "/usr/";
		}

		cmd = home + BIN;
	}

	private String info() {
		StringBuilder sb = new StringBuilder();
		sb.append("R [");
		//String version = "unknown";
		//sb.append(version);
		//sb.append("][");
		sb.append("home=");
		sb.append(home);
		sb.append("][workDir=");
		sb.append(wkdir.getAbsolutePath());
		sb.append("]");

		return sb.toString();
	}

	@CliCommand(value = { PREFIX + "cfg" }, help = "Configures R")
	public String config(@CliOption(key = { "home" }, mandatory = false, help = "R home directory") String home, 
			@CliOption(key = { "workDir" }, mandatory = false, help = "working directory") String wkdir)
			throws Exception {

		if (StringUtils.hasText(home)) {
			if (new File(home).exists()) {
				if (!home.endsWith(File.separator)) {
					home += File.separator;
				}
				this.home = home;
				cmd = home + BIN;
			}
			else {
				return "Cannot find path [" + home + "]";
			}
		}

		if (StringUtils.hasText(wkdir)) {
			File f = new File(wkdir);
			if (f.exists()) {
				this.wkdir = f;
			}
			else {
				return "Cannot find path [" + wkdir + "]";
			}
		}
		
		return info();
	}

	@CliCommand(value = { PREFIX + "script" }, help = "Executes a R script")
	public String script(@CliOption(key = { "", "location" }, mandatory = true, help = "Script location") String location,
						 @CliOption(key = { "args" }, mandatory = false, help = "Script arguments") String args) {

		if (!new File(cmd).exists()) {
			return "Cannot find R command [" + cmd + "]";
		}

		Resource resource = resourceResolver.getResource(fixLocation(location));
		if (!resource.exists()) {
			return "No resource found at " + location;
		}

		String uri = location;

		try {
			uri = resource.getFile().getAbsolutePath();
		} catch (IOException ex) {
			// ignore - we'll use location
		}

		Process process;
		List<String> cmds = new ArrayList<String>();

		cmds.add(cmd);
		cmds.add(uri);

		if (StringUtils.hasText(args)) {
			cmds.addAll(Arrays.asList(StringUtils.tokenizeToStringArray(args, " ")));
		}

		BufferedReader output = null;
		try {
			process = new ProcessBuilder(cmds).directory(wkdir).redirectErrorStream(true).start();
			output = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = output.readLine()) != null) {
				System.out.println(line);
			}
			int code = process.waitFor();
			return "R script executed with exit code - " + code;
		} catch (Exception ex) {
			return "R script execution failed - " + ex;
		} finally {
			IOUtils.closeQuietly(output);
		}
	}

	private static String fixLocation(String location) {
		if (StringUtils.hasText(location) && !location.contains(":")) {
			return "file:" + location;
		}
		return location;
	}
}