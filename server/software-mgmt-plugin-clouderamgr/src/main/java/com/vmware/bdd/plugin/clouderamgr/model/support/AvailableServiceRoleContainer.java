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
package com.vmware.bdd.plugin.clouderamgr.model.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vmware.bdd.plugin.clouderamgr.utils.Constants;
import com.vmware.bdd.plugin.clouderamgr.utils.SerialUtils;
import org.apache.log4j.Logger;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 5:59 PM
 */
public class AvailableServiceRoleContainer {

   private static final Logger logger = Logger.getLogger(AvailableServiceRoleContainer.class);
   private static Map<String, Object> elements = new HashMap<String, Object>();
   private static String clouderaConfDir = null;

   static {
      String homeDir = System.getProperties().getProperty("serengeti.home.dir");
      if (homeDir != null && homeDir.length() > 0) {
         StringBuilder builder = new StringBuilder();
         builder.append(homeDir).append(File.separator).append("conf")
               .append(File.separator).append("cloudera-manager");
         clouderaConfDir = builder.toString();
      } else {
         // for test only
         clouderaConfDir = AvailableServiceRoleContainer.class.getClassLoader().getResource("cloudera-manager").getPath();
      }
   }

   /**
    * load given service and its parent recursively, refer to AvailableServiceRole.setParent(String name)
    * @param displayName
    * @return
    * @throws IOException
    */
   public static synchronized AvailableServiceRole load(String displayName) throws IOException {
      String upDisplayName = displayName.toUpperCase();
      if (elements.containsKey(upDisplayName)) {
         return (AvailableServiceRole) elements.get(upDisplayName);
      }

      logger.info("loading " + upDisplayName + "...");
      String content = null;
      AvailableServiceRole serviceRole = null;
      try {
         content = SerialUtils.dataFromFile(clouderaConfDir + File.separator + upDisplayName);
         serviceRole = SerialUtils.getObjectByJsonString(AvailableServiceRole.class, content);

      } catch (IOException e) {
         logger.error(e.getMessage());
         throw new IOException("Failed to load " + upDisplayName);
      }
      elements.put(upDisplayName, serviceRole);
      return serviceRole;
   }

   public static void loadAll() throws IOException {
      File dir = new File(clouderaConfDir);
      for (File file : dir.listFiles()) {
         load(file.getName());
      }
   }

   /**
    * get all supported services of the given CDH major version,
    * set majorVersion=-1 to ignore this limitation
    * @param majorVersion
    * @return
    * @throws IOException
    */
   public static Set<String> allServices(int majorVersion) throws IOException {
      loadAll();
      Set<String> services = new HashSet<String>();
      for (Object o : elements.values()) {
         AvailableServiceRole element = (AvailableServiceRole) o;
         if (element.isService() && isValid(majorVersion, element)) {
            services.add(element.getDisplayName());
         }
      }
      return services;
   }

   private static boolean isValid(int majorVersion, AvailableServiceRole element) {
      if (majorVersion == Constants.VERSION_UNBOUNDED) {
         return true;
      }
      if (majorVersion < element.getVersionCdhMin()) {
         return false;
      }
      if (element.getVersionCdhMax() != Constants.VERSION_UNBOUNDED  && majorVersion > element.getVersionCdhMax()) {
         return false;
      }
      return true;
   }

   public static String getSupportedConfigs(int majorVersion) throws IOException {
      loadAll();
      Map<String, Object> configs = new HashMap<String, Object>();
      for (Object o : elements.values()) {
         AvailableServiceRole element = (AvailableServiceRole) o;
         if ((element.isService() || element.isRole()) && isValid(majorVersion, element)) {
            configs.put(element.getDisplayName(), element.getAvailableConfigurations().keySet());
         }
      }
      return (new Gson()).toJson(configs);
   }

   /**
    * get all supported roles of the given CDH major version,
    * set majorVersion=-1 to ignore this limitation
    * @param majorVersion
    * @return
    * @throws IOException
    */
   public static Set<String> allRoles(int majorVersion) throws IOException {
      loadAll();
      Set<String> roles = new HashSet<String>();
      for (Object o : elements.values()) {
         AvailableServiceRole element = (AvailableServiceRole) o;
         if (element.isRole() && isValid(majorVersion, element)) {
            roles.add(element.getDisplayName());
         }
      }
      return roles;
   }

   public static String dump() {
      return (new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()).toJson(elements);
   }

   public static void clean() {
      elements.clear();
   }
}
