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
package com.vmware.bdd.model;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:56 PM
 */
public class CmRoleDef {
   @Expose
   private String name;

   @Expose
   private String type;

   @Expose
   private String nodeRef; // TODO, relate to node.nodeId

   @Expose
   private Map<String, String> configs;

   public Map<String, String> getConfigs() {
      return configs;
   }

   public void setConfigs(Map<String, String> configs) {
      this.configs = configs;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getNodeRef() {
      return nodeRef;
   }

   public void setNodeRef(String nodeRef) {
      this.nodeRef = nodeRef;
   }

   public void addConfig(String key, String value) {
      if (configs == null) {
         configs = new HashMap<String, String>();
      }
      configs.put(key, value);
   }
}
