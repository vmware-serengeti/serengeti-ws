/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
 ***************************************************************************/
package com.vmware.bdd.manager.collection.quartz;


import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import com.vmware.bdd.util.collection.CollectionConstants;
import org.apache.log4j.Logger;
import org.quartz.Scheduler;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

import com.vmware.bdd.manager.JobManager;
import com.vmware.bdd.manager.collection.CollectionDriver;
import com.vmware.bdd.manager.collection.CollectionDriverManager;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.utils.PropertiesUtil;

public class DataCollectionQuartzJob {

   static final Logger logger = Logger.getLogger(DataCollectionQuartzJob.class);
   private Scheduler scheduler;
   private JobManager jobManager;
   private CollectionDriverManager collectionDriverManager;

   public Scheduler getScheduler() {
      return scheduler;
   }

   public void setScheduler(Scheduler scheduler) {
      this.scheduler = scheduler;
   }

   public void execute() throws Exception {
      if (collectionDriverManager == null) {
         return;
      }
      CollectionDriver driver = collectionDriverManager.getDriver();
      if (driver != null) {
         String datacollectionEnable =
               new PropertiesUtil(
                     CollectionDriverManager.getConfigurationFile())
                     .getProperty(driver.getCollectionSwitchName());
         if (datacollectionEnable.trim().equalsIgnoreCase("true")) {
            logger.debug("Method execute of class DataCollectionQuartzJob is running.");
            Map<String, JobParameter> param =
                  new TreeMap<String, JobParameter>();
            param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(
                  new Date()));
            JobParameters jobParameters = new JobParameters(param);
            jobManager.runJob(CollectionConstants.COLLECT_DATA_JOB_NAME, jobParameters);
         }
      }
   }

   public JobManager getJobManager() {
      return jobManager;
   }

   public void setJobManager(JobManager jobManager) {
      this.jobManager = jobManager;
   }

   public CollectionDriverManager getCollectionDriverManager() {
      return collectionDriverManager;
   }

   public void setCollectionDriverManager(
         CollectionDriverManager collectionDriverManager) {
      this.collectionDriverManager = collectionDriverManager;
   }

}
