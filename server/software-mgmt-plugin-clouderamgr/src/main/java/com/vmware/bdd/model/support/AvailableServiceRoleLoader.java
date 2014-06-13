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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.SerialUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 5:59 PM
 */
public class AvailableServiceRoleLoader {

   private static Map<String, Object> elements = new HashMap<String, Object>();

   public static AvailableServiceRole getServiceRole(String name) throws IOException {
      if (elements.containsKey(name)) {
         return (AvailableServiceRole) elements.get(name);
      }
      return loadServiceRole(name);
   }

   /**
    * load given service and its parent recursively, refer to AvailableServiceRole.setParent(String name)
    * TODO: error handling
    * @param name
    * @return
    * @throws IOException
    */
   private static AvailableServiceRole loadServiceRole(String name) throws IOException {
      System.out.println("loading " + name + "...");
      String content = CommonUtil.readJsonFile(name + ".json");
      AvailableServiceRole serviceRole = SerialUtils.getObjectByJsonString(AvailableServiceRole.class, content);
      elements.put(name, serviceRole);
      return serviceRole;
   }

   public static String dump() {
      return (new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()).toJson(elements);
   }

   public static void clean() {
      elements.clear();
   }
}
