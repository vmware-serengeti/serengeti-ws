/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.specpolicy;

import org.apache.commons.configuration.ConversionException;
import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.GroupType;
import com.vmware.bdd.apitypes.InstanceType;

public class ExpandUtils {
   private static final Logger logger = Logger.getLogger(ExpandUtils.class);
   private static int[][] templateStorage;
   static {
      initTemplateValues();
   }

   private static void initTemplateValues() {
      templateStorage = new int[GroupType.values().length][InstanceType.values().length];
      int value;

      value = setTemplateStorage("storage.mastergroup.extralarge", GroupType.MANAGEMENTGROUP.ordinal(), 
            InstanceType.EXTRA_LARGE.ordinal(), 200);
      logger.debug("extra large storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.large", GroupType.MANAGEMENTGROUP.ordinal(),
            InstanceType.LARGE.ordinal(), 100);
      logger.debug("large storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.medium", GroupType.MANAGEMENTGROUP.ordinal(), 
            InstanceType.MEDIUM.ordinal(), 50);
      logger.debug("medium storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.small", GroupType.MANAGEMENTGROUP.ordinal(), 
            InstanceType.SMALL.ordinal(), 25);
      logger.debug("small storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.extralarge", GroupType.WORKGROUP.ordinal(), 
            InstanceType.EXTRA_LARGE.ordinal(), 400);
      logger.debug("extra large storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.large", GroupType.WORKGROUP.ordinal(),
            InstanceType.LARGE.ordinal(), 200);
      logger.debug("large storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.medium", GroupType.WORKGROUP.ordinal(),
            InstanceType.MEDIUM.ordinal(),100);
      logger.debug("medium storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.small", GroupType.WORKGROUP.ordinal(), 
            InstanceType.SMALL.ordinal(), 50);
      logger.debug("small storage of worker group  is " + value + "GB.");
   }

   private static int setTemplateStorage(String propertyName, int groupType,
         int instanceType, int defaultVal) {
      int value = 0;
      try {
         value = Configuration.getInt(propertyName, defaultVal);
      } catch (ConversionException e) {
         value = defaultVal;
      }
      templateStorage[groupType][instanceType] = value;
      return value;
   }

   public static int getStorage(InstanceType instance, GroupType groupType) {
      return templateStorage[groupType.ordinal()][instance.ordinal()];
   }
}
