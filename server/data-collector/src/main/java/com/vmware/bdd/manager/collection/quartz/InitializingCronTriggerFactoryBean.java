/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import org.apache.log4j.Logger;
import org.quartz.CronExpression;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;

import com.vmware.bdd.manager.collection.CollectionDriver;
import com.vmware.bdd.manager.collection.CollectionDriverManager;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.PropertiesUtil;

public class InitializingCronTriggerFactoryBean extends CronTriggerFactoryBean {

   static final Logger logger = Logger
         .getLogger(InitializingCronTriggerFactoryBean.class);
   private CollectionDriverManager collectionDriverManager;

   public InitializingCronTriggerFactoryBean(
         CollectionDriverManager collectionDriverManager,
         String defaultCronExpression) {
      this.collectionDriverManager = collectionDriverManager;
      if (this.collectionDriverManager == null
            || this.collectionDriverManager.getDriver() == null) {
         setCronExpression(defaultCronExpression);
         return;
      }
      String cronExpression = getCronExpressionFromConfiguration();
      if (!CommonUtil.isBlank(cronExpression)
            && CronExpression.isValidExpression(cronExpression)) {
         setCronExpression(cronExpression);
      } else {
         CollectionDriver driver = this.collectionDriverManager.getDriver();
         if (driver != null) {
            setCronExpression(driver.getDefaultCronExpression());
         } else {
            logger.warn("In the constructor of class InitializingCronTriggerFactoryBean, driver is null.");
         }
      }
   }

   private String getCronExpressionFromConfiguration() {
      String cronExpression = "";
      CollectionDriver driver = collectionDriverManager.getDriver();
      if (driver != null) {
         cronExpression =
               new PropertiesUtil(
                     CollectionDriverManager.getConfigurationFile())
                     .getProperty(driver.getCronExpressionName());
      }
      return cronExpression;
   }

}
