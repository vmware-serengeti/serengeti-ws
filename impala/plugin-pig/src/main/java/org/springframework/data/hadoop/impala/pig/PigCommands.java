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
package org.springframework.data.hadoop.impala.pig;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.hadoop.conf.Configuration;
import org.apache.pig.ExecType;
import org.apache.pig.backend.executionengine.ExecJob;
import org.apache.pig.tools.pigstats.ScriptState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.hadoop.pig.PigContextFactoryBean;
import org.springframework.data.hadoop.pig.PigScript;
import org.springframework.data.hadoop.pig.PigServerFactoryBean;
import org.springframework.data.hadoop.pig.PigTemplate;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Provider of pig commands.
 * 
 * @author Costin Leau
 */
@Component
public class PigCommands implements CommandMarker {

	private static final String PREFIX = "pig ";

	@Autowired
	private Configuration hadoopConfiguration;

	private PigContextFactoryBean pigContextFactory;
	private PigServerFactoryBean pigFactory;
	private PigTemplate pigTemplate;

	private ExecType execType;
	private String jobTracker, jobName, jobPriority;
	private Boolean validateEachStatement;
	private String propertiesLocation;

	private ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(
			new FileSystemResourceLoader());

	
	public void init() throws Exception {
		pigContextFactory = new PigContextFactoryBean();
		pigContextFactory.setConfiguration(hadoopConfiguration);

		if (StringUtils.hasText(jobTracker)) {
			pigContextFactory.setJobTracker(jobTracker);
		}

		if (execType != null) {
			pigContextFactory.setExecType(execType);
		}
		Properties props = loadProperties();
		if (props != null) {
			pigContextFactory.setProperties(props);
		}
		pigContextFactory.afterPropertiesSet();

		pigFactory = new PigServerFactoryBean();
		pigFactory.setPigContext(pigContextFactory.getObject());
		if (validateEachStatement != null) {
			pigFactory.setValidateEachStatement(validateEachStatement);
		}
		if (StringUtils.hasText(jobName)) {
			pigFactory.setJobName(jobName);
		}
		if (StringUtils.hasText(jobPriority)) {
			pigFactory.setJobPriority(jobPriority);
		}

		pigTemplate = new PigTemplate(pigFactory.getObject());
	}

	private Properties loadProperties() throws Exception {
		if (StringUtils.hasText(propertiesLocation)) {
			PropertiesFactoryBean propsFactory = new PropertiesFactoryBean();
			propsFactory.setLocations(resourceResolver.getResources(propertiesLocation));
			propsFactory.afterPropertiesSet();
			return propsFactory.getObject();
		}

		return null;
	}

	@CliCommand(value = { PREFIX + "cfg" }, help = "Configures Pig")
	public String config(@CliOption(key = { "props" }, mandatory = false, help = "Properties file location") String location, 
			@CliOption(key = { "jobTracker" }, mandatory = false, help = "Job tracker") String jobTracker, 
			@CliOption(key = { "execType" }, mandatory = false, help = "Execution type") ExecType execType, 
			@CliOption(key = { "jobName" }, mandatory = false, help = "Job name") String jobName, 
			@CliOption(key = { "jobPriority" }, mandatory = false, help = "Job priority") String jobPriority, 
			@CliOption(key = { "validateEachStatement" }, mandatory = false, help = "Validation of each statement") Boolean validateEachStatement)
			throws Exception {

		this.jobTracker = jobTracker;
		this.jobName = jobName;
		this.jobPriority = jobPriority;
		this.validateEachStatement = validateEachStatement;
		this.execType = execType;
		this.propertiesLocation = fixLocation(location);

		// reset template
		pigTemplate = null;
		return info();
	}

	public String info() {
		StringBuilder sb = new StringBuilder();
		sb.append("Pig [");
		String pigVersion = ScriptState.class.getPackage().getImplementationVersion();
		// for some reason this seems to fail 
		if (!StringUtils.hasText(pigVersion)) {
			pigVersion = ScriptState.get().getPigVersion();
		}

		sb.append((StringUtils.hasText(pigVersion) ? pigVersion : "unknown"));
		sb.append("]");
		
		sb.append("[fs=");		
		String fs = hadoopConfiguration.get("fs.default.name");
		if(fs != null && fs.length() > 0){
			sb.append(fs);
		}
		sb.append("]");
		
		sb.append("[jt=");
		String jt = hadoopConfiguration.get("mapred.job.tracker");
		if(jt != null && jt.length() > 0){
			sb.append(jt);
		}		
		sb.append("]");
		
		sb.append("[execType=");
		sb.append((execType != null ? execType.name() : ExecType.MAPREDUCE.name()));
		sb.append("]");
		// TODO: potentially add a check to see whether HDFS is running

		return sb.toString();
	}

	@CliCommand(value = { PREFIX + "script" }, help = "Executes a Pig script")
	public String script(@CliOption(key = { "", "location" }, mandatory = true, help = "Script location") String location) {
		String jobTracker = hadoopConfiguration.get("mapred.job.tracker");
		if (jobTracker == null || jobTracker.length() == 0) {
			return "You must set Job Tracker URL before run Pig script";
		}
		Resource resource = resourceResolver.getResource(fixLocation(location));

		if (!resource.exists()) {
			return "Cannot resolve " + location;
		}

		String uri = location;

		try {
			uri = resource.getFile().getAbsolutePath();
		} catch (IOException ex) {
			// ignore - we'll use location
		}

		try {
			if (pigTemplate == null) {
				init();
			}

			List<ExecJob> results = pigTemplate.executeScript(new PigScript(resource));
			ExecJob result = results.get(0);
			Exception exception = result.getException();
			StringBuilder sb = new StringBuilder(result.getStatus().name());
			if (exception != null) {
				sb.append(" ;Cause=");
				sb.append(exception.getMessage());
			}
			return "Script [" + uri + "] executed succesfully. Returned status " + sb.toString();
		} catch (Exception ex) {
			return "Script [" + uri + "] failed - " + ex;
		}
	}	


	private static String fixLocation(String location) {
		if (StringUtils.hasText(location) && !location.contains(":")) {
			return "file:" + location;
		}
		return location;
	}
}