/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.api.model.support;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.utils.SerialUtils;
import com.vmware.bdd.utils.Version;

public class AvailableConfigurationContainer {

   private static final Logger logger = Logger.getLogger(AvailableConfigurationContainer.class);
   private static List<AvailableConfiguration> availableConfigurations = new ArrayList<AvailableConfiguration>();
   private static String ambariConfDir = null;

   static {
      String homeDir = System.getProperties().getProperty("serengeti.home.dir");
      if (homeDir != null && homeDir.length() > 0) {
         StringBuilder builder = new StringBuilder();
         builder.append(homeDir).append(File.separator).append("conf")
         .append(File.separator).append("Ambari").append(File.separator).append("available-configurations");
         ambariConfDir = builder.toString();
      } else {
         // for test only
         ambariConfDir = AvailableConfigurationContainer.class.getClassLoader().getResource("available-configurations").getPath();
      }
   }

   public static List<AvailableConfiguration> load(String fileName) throws IOException {

      logger.info("loading " + fileName + "...");
      String content = null;
      try {
         content = SerialUtils.dataFromFile(ambariConfDir + File.separator + fileName);
         AvailableConfiguration availableConfiguration = SerialUtils.getObjectByJsonString(AvailableConfiguration.class, content);
         availableConfigurations.add(availableConfiguration);

      } catch (IOException e) {
         logger.error(e.getMessage());
         throw new IOException("Failed to load " + fileName);
      }
      return availableConfigurations;
   }

   public static void loadAll() throws IOException {
      availableConfigurations.clear();
      File dir = new File(ambariConfDir);
      for (File file : dir.listFiles()) {
         load(file.getName());
      }
   }


   public static boolean isSupported(String distroVersion, AvailableConfiguration availableConfiguration) {
      if (Constants.VERSION_UNBOUNDED.equals(distroVersion)) {
         return true;
      }

      boolean isGreaterThanMinVersion = false;
      boolean isLessThanMaxVersion = false;

      String hdpMinVersion = availableConfiguration.getVersionHdpMin();
      if (Version.compare(distroVersion, hdpMinVersion) >= 0) {
         isGreaterThanMinVersion = true;
      }

      String hdpMaxVersion = availableConfiguration.getVersionHdpMax();
      if (Constants.VERSION_UNBOUNDED.equals(hdpMaxVersion) || Version.compare(distroVersion, hdpMaxVersion) <= 0) {
         isLessThanMaxVersion = true;
      }

      if (isGreaterThanMinVersion && isLessThanMaxVersion) {
         return true;
      } else {
         return false;
      }
   }

   public static synchronized Map<String, Object> getSupportedConfigs(String distroVersion) throws IOException {
      loadAll();
      Map<String, Object> supportedConfigs = new HashMap<String, Object>();
      for (AvailableConfiguration availableConfiguration : availableConfigurations) {
         if (isSupported(distroVersion, availableConfiguration)) {
            for (AvailableConfigurationInfo availableConfigurationInfo : availableConfiguration.getConfigurations()) {
               String configType = availableConfigurationInfo.getType();
               String propertyName = availableConfigurationInfo.getName();
               Set<String> propertyNames;
               if (supportedConfigs.containsKey(configType)) {
                  propertyNames = (Set<String>) supportedConfigs.get(configType);
               } else {
                  propertyNames = new HashSet<String>();
               }
               propertyNames.add(propertyName);
               supportedConfigs.put(configType, propertyNames);
            }
         }
      }
      return supportedConfigs;
   }
}