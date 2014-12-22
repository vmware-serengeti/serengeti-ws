/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.manager.collection.quartz;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.springframework.batch.core.JobParameters;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.manager.JobManager;
import com.vmware.bdd.manager.collection.CollectionDriver;
import com.vmware.bdd.manager.collection.CollectionDriverManager;
import com.vmware.bdd.manager.collection.FakeCollectionDriverManager;
import com.vmware.bdd.service.collection.ICollectionInitializerService;
import com.vmware.bdd.service.collection.impl.FakeCollectionInitializerService;
import com.vmware.bdd.service.collection.job.JobConstants;
import com.vmware.bdd.utils.PropertiesUtil;

public class TestDataCollectionQuartzJob {

   private static File file;
   private static String FILE_PATH = "/tmp/test.properties";

   @MockClass(realClass = PropertiesUtil.class)
   public static class MockPropertiesUtil {

      PropertiesUtil propertiesUtil = new PropertiesUtil(file);

      @Mock
      public String getProperty(String key) {
         return "true";
      }

      @Mock
      public PropertiesUtil setProperty(String key, String value) {
         Properties prop = new Properties();
         prop.setProperty(key, value);
         propertiesUtil.setProperties(prop);
         return propertiesUtil;
      }
   }

   private static void setUp() {
      file = new File(FILE_PATH);
      if (!file.exists()) {
         try {
            file.createNewFile();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   @BeforeMethod(groups = { "TestDataCollectionQuartzJob" })
   public void setMockup() {
      setUp();
      Mockit.setUpMock(MockPropertiesUtil.class);
   }

   @AfterMethod
   public void tearDown() {
      Mockit.tearDownMocks();
      cleanUpData();
   }

   private void cleanUpData() {
      if (file.exists()) {
         file.delete();
      }
   }

   @Test(groups = { "TestDataCollectionQuartzJob" })
   public void testExecute() throws Exception {
      Scheduler scheduler = mock(Scheduler.class);
      JobManager jobManager = mock(JobManager.class);
      CollectionDriver collectionDriver = mock(CollectionDriver.class);
      ICollectionInitializerService collectionInitializerService =
            Mockito.mock(FakeCollectionInitializerService.class);
      CollectionDriverManager collectionDriverManager =
            new FakeCollectionDriverManager(
                  "com.vmware.bdd.manager.ph.PhoneHomeCollectionDriver",
                  collectionInitializerService, collectionDriver, file);
      DataCollectionQuartzJob dataCollectionQuartzJob =
            new DataCollectionQuartzJob();
      dataCollectionQuartzJob
            .setCollectionDriverManager(collectionDriverManager);
      dataCollectionQuartzJob.setJobManager(jobManager);
      dataCollectionQuartzJob.setScheduler(scheduler);
      dataCollectionQuartzJob.execute();
      verify(jobManager).runJob(eq(JobConstants.COLLECT_DATA_JOB_NAME),
            isA(JobParameters.class));
      verify(collectionDriver, times(1)).getCollectionSwitchName();
   }
}
