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
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FsShell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
/**
 * @author Jarred Li
 *
 */
@Test
@ContextConfiguration(locations = { "classpath:org/springframework/data/hadoop/impala/mapreduce/MapReduceCommandsTest-context.xml" })
public class MapReduceCommandsTest extends AbstractTestNGSpringContextTests{

	@Autowired
	MapReduceCommands mrCmds;
	
	private String hadoopExampleJarFile = "src/test/resources/hadoop-examples-1.0.3.jar";
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public void setUp() throws Exception {
		mrCmds.init();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public void tearDown() throws Exception {
		mrCmds = null;
	}

	/**
	 * Test method for {@link org.springframework.data.hadoop.impala.mapreduce.MapReduceCommands#init()}.
	 */
	@Test
	public void testInit() {
		mrCmds.init();
	}

	/**
	 * Test method for {@link org.springframework.data.hadoop.impala.mapreduce.MapReduceCommands#submit(java.lang.String)}.
	 * @throws Exception 
	 */
	@Test
	public void testSubmit() throws Exception {
		Configuration jobConfig = new Configuration(false);

		Configuration hadoopConfig = mrCmds.getHadoopConfiguration();
		
		FsShell shell = new FsShell(hadoopConfig);
		List<String> argv = new ArrayList<String>();
		argv.add("-rmr");
		argv.add("/tmp/wc-input");
		shell.run(argv.toArray(new String[0]));
		
		argv = new ArrayList<String>();
		argv.add("-put");
		File f = new File("src/test/resources/wordcount-conf.xml");		
		argv.add(f.getAbsolutePath());
		argv.add("/tmp/wc-input/");
		shell.run(argv.toArray(new String[0]));
		
		argv = new ArrayList<String>();
		argv.add("-rmr");
		argv.add("/tmp/wc-output");
		shell.run(argv.toArray(new String[0]));
		
		String hadoopFsName = hadoopConfig.get("fs.default.name");
		String hadoopJT = hadoopConfig.get("mapred.job.tracker");
		File jarFile = new File(hadoopExampleJarFile);
		
		jobConfig.set("fs.default.name", hadoopFsName);
		jobConfig.set("mapred.job.tracker", hadoopJT);
		jobConfig.set("mapred.jar", jarFile.getAbsolutePath());
		jobConfig.set("mapred.input.dir", "/tmp/wc-input");
		jobConfig.set("mapred.output.dir", "/tmp/wc-output");
		jobConfig.set("mapreduce.map.class", "org.apache.hadoop.examples.WordCount.TokenizerMapper");
		jobConfig.set("mapreduce.reduce.class", "org.apache.hadoop.examples.WordCount.IntSumReducer");
		
		String tmpFile = "/tmp/impala-test-wordcount-conf.xml";
		try {
			jobConfig.writeXml(new FileOutputStream(new File(tmpFile)));
		} catch (Exception e) {
			Assert.fail("fail to write temp MR configuration file");
		}
		
		mrCmds.submit(tmpFile);
	}

	@Test
	public void testJar() throws Exception{
		Configuration hadoopConfig = mrCmds.getHadoopConfiguration();
		FsShell shell = new FsShell(hadoopConfig);
		List<String> argv = new ArrayList<String>();
		argv.add("-rmr");
		argv.add("/tmp/wc-input2");
		shell.run(argv.toArray(new String[0]));
		
		argv = new ArrayList<String>();
		argv.add("-put");
		File f = new File("src/test/resources/wordcount-conf.xml");		
		argv.add(f.getAbsolutePath());
		argv.add("/tmp/wc-input2/");
		shell.run(argv.toArray(new String[0]));
		
		argv = new ArrayList<String>();
		argv.add("-rmr");
		argv.add("/tmp/wc-output2");
		shell.run(argv.toArray(new String[0]));
		
		File jarFile = new File(hadoopExampleJarFile);		
		mrCmds.jar(jarFile.getAbsolutePath(), "org.apache.hadoop.examples.WordCount","/tmp/wc-input2 /tmp/wc-output2");
	}
}