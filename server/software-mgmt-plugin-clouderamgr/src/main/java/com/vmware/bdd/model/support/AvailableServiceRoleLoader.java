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
package com.vmware.bdd.model.support;

import com.google.gson.GsonBuilder;
import com.vmware.bdd.utils.SerialUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 5:59 PM
 */
public class AvailableServiceRoleLoader {

   private static final Logger logger = Logger.getLogger(AvailableServiceRoleLoader.class);
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
         clouderaConfDir = "/tmp/cloudera-manager"; // for test only
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
      logger.info("loading " + upDisplayName + "...");
      if (elements.containsKey(upDisplayName)) {
         return (AvailableServiceRole) elements.get(upDisplayName);
      }

      String content = SerialUtils.dataFromFile(clouderaConfDir + File.separator + upDisplayName);
      AvailableServiceRole serviceRole = SerialUtils.getObjectByJsonString(AvailableServiceRole.class, content);
      elements.put(upDisplayName, serviceRole);
      return serviceRole;
   }

   public static void loadAll() throws IOException {
      File dir = new File(clouderaConfDir);
      for (File file : dir.listFiles()) {
         load(file.getName());
      }
   }

   public static String dump() {
      return (new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()).toJson(elements);
   }

   public static void clean() {
      elements.clear();
   }
}
