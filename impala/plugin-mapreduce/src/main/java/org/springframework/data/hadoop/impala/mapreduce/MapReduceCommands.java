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
package org.springframework.data.hadoop.impala.mapreduce;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.impala.common.ConfigurationAware;
import org.springframework.data.hadoop.impala.common.util.SecurityUtil;
import org.springframework.data.hadoop.impala.common.util.SecurityUtil.ExitTrappedException;
import org.springframework.shell.core.ExecutionProcessor;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.event.ParseResult;
import org.springframework.stereotype.Component;


/**
 * Commands to submit and interact with MapReduce jobs
 * 
 * @author Jarred Li
 * @author Author of <code>org.apache.hadoop.util.RunJar</code>
 */
@Component
public class MapReduceCommands extends ConfigurationAware implements ExecutionProcessor{

	private JobClient jobClient;

	private static final String PREFIX = "mr job ";

	@Autowired
	private SecurityUtil securityUtil;

	@Override
	public ParseResult beforeInvocation(ParseResult invocationContext) {
		invocationContext = super.beforeInvocation(invocationContext);
		String jobTracker = getHadoopConfiguration().get("mapred.job.tracker");
		if (jobTracker != null && jobTracker.length() > 0) {
			if (jobClient == null) {
				init();
			}
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				org.apache.hadoop.mapreduce.JobSubmissionFiles.JOB_DIR_PERMISSION.fromShort((short) 0700);
				org.apache.hadoop.mapreduce.JobSubmissionFiles.JOB_FILE_PERMISSION.fromShort((short) 0644);
			}
			return invocationContext;
		}
		else{
			LOG.severe("You must set Job Tracker URL before run Map Reduce commands");
			throw new RuntimeException("You must set Job Tracker URL before run Map Reduce commands");
		}
	}

	public void init() {
		try {
			jobClient = new JobClient(new JobConf(getHadoopConfiguration()));
		} catch (IOException ex) {
			LOG.severe("Cannot create job client" + ex.getMessage());
		}
	}

	@Override
	protected String failedComponentName() {
		return "Map/Reduce";
	}

	@Override
	protected boolean configurationChanged() throws Exception {
		if (jobClient != null) {
			LOG.info("Hadoop configuration changed, re-initializing MR...");
		}
		init();
		return true;
	}
	

	@CliCommand(value = PREFIX + "submit", help = "Submit a Map Reduce job defined in the job file")
	public void submit(@CliOption(key = { "jobfile" }, mandatory = true, help = "the configuration file for MR job") final String jobFile) {
		List<String> argv = new ArrayList<String>();
		argv.add("-submit");
		argv.add(jobFile);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "status", help = "Query Map Reduce job status.")
	public void status(@CliOption(key = { "jobid" }, mandatory = true, help = "the job Id") final String jobid) {
		List<String> argv = new ArrayList<String>();
		argv.add("-status");
		argv.add(jobid);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "counter", help = "Print the counter value of the MR job")
	public void counter(
			@CliOption(key = { "jobid" }, mandatory = true, help = "the job Id") final String jobid, 
			@CliOption(key = { "groupname" }, mandatory = true, help = "the job Id") final String groupName,
			@CliOption(key = { "countername" }, mandatory = true, help = "the job Id") final String counterName) {
		List<String> argv = new ArrayList<String>();
		argv.add("-counter");
		argv.add(jobid);
		argv.add(groupName);
		argv.add(counterName);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "kill", help = "Kill the Map Reduce job")
	public void kill(@CliOption(key = { "jobid" }, mandatory = true, help = "the job Id") final String jobid) {
		List<String> argv = new ArrayList<String>();
		argv.add("-kill");
		argv.add(jobid);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "events", help = "Print the events' detail received by jobtracker for the given range")
	public void events(
			@CliOption(key = { "jobid" }, mandatory = true, help = "the job Id") final String jobid, 
			@CliOption(key = { "from" }, mandatory = true, help = "from event number") final String from, 
			@CliOption(key = { "number" }, mandatory = true, help = "total number of events") final String number) {
		List<String> argv = new ArrayList<String>();
		argv.add("-events");
		argv.add(jobid);
		argv.add(from);
		argv.add(number);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "history", help = "Print job details, failed and killed job details")
	public void history(@CliOption(key = { "all" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Whether print all information") final boolean all, 
			@CliOption(key = { "" }, mandatory = true, help = "job output directory") final String outputDir) {
		List<String> argv = new ArrayList<String>();
		argv.add("-history");
		if (all) {
			argv.add("all");
		}
		argv.add(outputDir);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "list", help = "List the Map Reduce jobs")
	public void list(@CliOption(key = { "all" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Whether list all jobs") final boolean all) {
		List<String> argv = new ArrayList<String>();
		argv.add("-list");
		if (all) {
			argv.add("all");
		}
		run(argv.toArray(new String[0]));
	}


	@CliCommand(value = "mr task kill", help = "Kill the Map Reduce task")
	public void killTask(@CliOption(key = { "taskid" }, mandatory = true, help = "the task Id") final String taskid) {
		List<String> argv = new ArrayList<String>();
		argv.add("-kill-task");
		argv.add(taskid);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = "mr task fail", help = "Fail the Map Reduce task")
	public void failTask(@CliOption(key = { "taskid" }, mandatory = true, help = "the task Id") final String taskid) {
		List<String> argv = new ArrayList<String>();
		argv.add("-fail-task");
		argv.add(taskid);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "set priority", help = "Change the priority of the job")
	public void setPriority(
			@CliOption(key = { "jobid" }, mandatory = true, help = "the job Id") final String jobid, 
			@CliOption(key = { "priority" }, mandatory = true, help = "the job priority") final JobPriority priority) {
		List<String> argv = new ArrayList<String>();
		argv.add("-set-priority");
		argv.add(jobid);
		argv.add(priority.getValue());
		run(argv.toArray(new String[0]));
	}

	public enum JobPriority {
		VERY_HIGH("VERY_HIGH"), HIGH("HIGH"), NORML("NORMAL"), LOW("LOW"), VERY_LOW("VERY_LOW");

		private String val;

		JobPriority(String v) {
			this.val = v;
		}

		public String getValue() {
			return val;
		}
	}

	@CliCommand(value = "mr jar", help = "Run Map Reduce job in the jar")
	public void jar(
			@CliOption(key = { "jarfile" }, mandatory = true, help = "jar file name") final String jarFileName, 
			@CliOption(key = "mainclass", mandatory = true, help = "main class name") final String mainClassName, 
			@CliOption(key = "args", mandatory = false, help = "input path") final String args) {
		securityUtil.forbidSystemExitCall();
		try {
			runJar(jarFileName, mainClassName, args);
		} catch (ExitTrappedException e) {
			//LOG.info("The MR job call System.exit. This is prevented.");
		} catch (Throwable t) {
			LOG.severe("run MR job failed. Failed Message:" + t.getMessage());
		} finally {
			securityUtil.enableSystemExitCall();
		}
	}


	/**
	 * @param jarFileName
	 * @param mainClassName
	 * @param args
	 * @throws Throwable 
	 */
	public void runJar(final String jarFileName, final String mainClassName, final String args) throws Throwable {
		File file = new File(jarFileName);
		File tmpDir = new File(new Configuration().get("hadoop.tmp.dir"));
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			tmpDir = new File(System.getProperty("java.io.tmpdir"), "impala");
		}
		tmpDir.mkdirs();
		if (!tmpDir.isDirectory()) {
			LOG.severe("Mkdirs failed to create " + tmpDir);
		}

		try {
			final File workDir = File.createTempFile("hadoop-unjar", "", tmpDir);
			workDir.delete();
			workDir.mkdirs();
			if (!workDir.isDirectory()) {
				LOG.severe("Mkdirs failed to create " + workDir);
				return;
			}

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						FileUtil.fullyDelete(workDir);
					} catch (IOException e) {
					}
				}
			});

			unJar(file, workDir);

			ArrayList<URL> classPath = new ArrayList<URL>();

			//This is to add hadoop configuration dir to classpath so that 
			//user's configuration can be accessed when running the jar
			File hadoopConfigurationDir = new File(workDir + Path.SEPARATOR + "impala-hadoop-configuration");
			writeHadoopConfiguration(hadoopConfigurationDir, this.getHadoopConfiguration());
			classPath.add(hadoopConfigurationDir.toURL());
			//classPath.add(new File(System.getenv("HADOOP_CONF_DIR")).toURL());

			classPath.add(new File(workDir + Path.SEPARATOR).toURL());
			classPath.add(file.toURL());
			classPath.add(new File(workDir, "classes" + Path.SEPARATOR).toURL());
			File[] libs = new File(workDir, "lib").listFiles();
			if (libs != null) {
				for (int i = 0; i < libs.length; i++) {
					classPath.add(libs[i].toURL());
				}
			}
			ClassLoader loader = new URLClassLoader(classPath.toArray(new URL[0]), this.getClass().getClassLoader());
			Thread.currentThread().setContextClassLoader(loader);
			Class<?> mainClass = Class.forName(mainClassName, true, loader);
			Method main = mainClass.getMethod("main", new Class[] { Array.newInstance(String.class, 0).getClass() });
			String[] newArgs = args.split(" ");
			main.invoke(null, new Object[] { newArgs });
		} catch (Exception e) {
			if (e instanceof InvocationTargetException) {
				if (e.getCause() instanceof ExitTrappedException) {
					throw (ExitTrappedException) e.getCause();
				}
			}
			else {
				throw e;
			}
		}
	}

	/**
	 * wirte the Hadoop configuration to one directory, 
	 * file name is "core-site.xml", "hdfs-site.xml" and "mapred-site.xml".
	 * 
	 * @param configDir the directory that the file be written
	 * @param config Hadoop configuration
	 * 
	 */
	public void writeHadoopConfiguration(File configDir, Configuration config) {
		configDir.mkdirs();
		try {
			FileOutputStream fos = new FileOutputStream(new File(configDir + Path.SEPARATOR + "core-site.xml"));
			config.writeXml(fos);
			fos = new FileOutputStream(new File(configDir + Path.SEPARATOR + "hdfs-site.xml"));
			config.writeXml(fos);
			fos = new FileOutputStream(new File(configDir + Path.SEPARATOR + "mapred-site.xml"));
			config.writeXml(fos);
		} catch (Exception e) {
			LOG.severe("Save user's configuration failed. Message:" + e.getMessage());
		}

	}

	private void unJar(File jarFile, File toDir) throws Throwable {
		JarFile jar = new JarFile(jarFile);
		try {
			Enumeration entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = (JarEntry) entries.nextElement();
				if (!entry.isDirectory()) {
					InputStream in = jar.getInputStream(entry);
					try {
						File file = new File(toDir, entry.getName());
						if (!file.getParentFile().mkdirs()) {
							if (!file.getParentFile().isDirectory()) {
								throw new IOException("Mkdirs failed to create " + file.getParentFile().toString());
							}
						}
						OutputStream out = new FileOutputStream(file);
						try {
							byte[] buffer = new byte[8192];
							int i;
							while ((i = in.read(buffer)) != -1) {
								out.write(buffer, 0, i);
							}
						} finally {
							out.close();
						}
					} finally {
						in.close();
					}
				}
			}
		} catch (Throwable t) {
			throw t;
		} finally {
			jar.close();
		}
	}

	private void run(String[] argv) {
		try {
			jobClient.run(argv);
		} catch (Throwable t) {
			LOG.severe("run MR job failed. Failed Message:" + t.getMessage());
		}
	}


}
