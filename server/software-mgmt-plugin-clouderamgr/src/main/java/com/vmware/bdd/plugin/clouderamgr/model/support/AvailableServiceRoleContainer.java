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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vmware.bdd.plugin.clouderamgr.utils.Constants;
import com.vmware.bdd.plugin.clouderamgr.utils.SerialUtils;
import com.vmware.bdd.utils.Version;

import org.apache.log4j.Logger;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 5:59 PM
 */
public class AvailableServiceRoleContainer {

   private static final Logger logger = Logger.getLogger(AvailableServiceRoleContainer.class);
   private static Map<String, AvailableServiceRole> elements = new HashMap<String, AvailableServiceRole>();
   private static String clouderaConfDir = null;

   static {
      String homeDir = System.getProperties().getProperty("serengeti.home.dir");
      if (homeDir != null && homeDir.length() > 0) {
         StringBuilder builder = new StringBuilder();
         builder.append(homeDir).append(File.separator).append("conf")
               .append(File.separator).append("ClouderaManager").append(File.separator).append("available-services-and-roles");
         clouderaConfDir = builder.toString();
      } else {
         // for test only
         clouderaConfDir = AvailableServiceRoleContainer.class.getClassLoader().getResource("available-services-and-roles").getPath();
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
         return elements.get(upDisplayName);
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
   public static Set<String> allServices(String distroVersion) throws IOException {
      loadAll();
      Set<String> services = new HashSet<String>();
      for (AvailableServiceRole element : elements.values()) {
         if (element.isService() && isSupported(distroVersion, element)) {
            services.add(element.getDisplayName());
         }
      }
      return services;
   }

   public static boolean isSupported(String distroVersion, AvailableServiceRole element) {
      if (Constants.VERSION_UNBOUNDED.equals(distroVersion)) {
         return true;
      }

      boolean isGreaterThanMinVersion = false;
      boolean isLessThanMaxVersion = false;

      String cdhMinVersion = element.getVersionCdhMin();
      if (Version.compare(distroVersion, cdhMinVersion) >= 0) {
         isGreaterThanMinVersion = true;
      }

      String cdhMaxVersion = element.getVersionCdhMax();
      if (Constants.VERSION_UNBOUNDED.equals(cdhMaxVersion) || Version.compare(distroVersion, cdhMaxVersion) <= 0) {
         isLessThanMaxVersion = true;
      }

      if (isGreaterThanMinVersion && isLessThanMaxVersion) {
         return true;
      } else {
         return false;
      }
   }

   public static String getSupportedConfigs(String distroVersion) throws IOException {
      loadAll();
      Map<String, Object> configs = new HashMap<String, Object>();
      for (AvailableServiceRole element : elements.values()) {
         if ((element.isService() || element.isRole()) && isSupported(distroVersion, element)) {
            configs.put(element.getDisplayName(), element.getAvailableConfigurations().keySet());
         }
      }
      Gson gson =
            new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                  .setPrettyPrinting().create();
      return gson.toJson(configs);
   }

   /**
    * get all supported roles of the given CDH major version,
    * set majorVersion=-1 to ignore this limitation
    * @param majorVersion
    * @return
    * @throws IOException
    */
   public static Set<String> allRoles(String distroVersion) throws IOException {
      loadAll();
      Set<String> roles = new HashSet<String>();
      for (AvailableServiceRole element : elements.values()) {
         if (element.isRole() && isSupported(distroVersion, element)) {
            roles.add(element.getDisplayName());
         }
      }
      return roles;
   }

   public static Map<String, String> nameToDisplayName() throws IOException {
      loadAll();
      Map<String, String> nameMap = new HashMap<String, String>();
      for (AvailableServiceRole element : elements.values()) {
         nameMap.put(element.getName(), element.getDisplayName());
      }
      return nameMap;
   }

   public static String dump() {
      return (new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()).toJson(elements);
   }

   public static boolean circularDependencyCheck() {
      /*
      Map<AvailableServiceRole, BitSet> serviceToId = new HashMap<AvailableServiceRole, BitSet>();

      int index = 0;
      for (AvailableServiceRole service : elements.values()) {
         if (service.isService()) {
            BitSet bitSet = new BitSet();
            bitSet.set(index, true);
            serviceToId.put(service, bitSet);
            index++;
         }
      }

      Set<AvailableServiceRole> leftServices = serviceToId.keySet();
      while (!leftServices.isEmpty()) {
         Map<AvailableServiceRole, BitSet> accessedServices = new HashMap<AvailableServiceRole, BitSet>();
         AvailableServiceRole currentService = leftServices.iterator().next();

      }
      */
      return true;
   }
}
