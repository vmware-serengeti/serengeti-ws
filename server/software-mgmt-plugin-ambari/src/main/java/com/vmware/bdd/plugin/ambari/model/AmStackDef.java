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
package com.vmware.bdd.plugin.ambari.model;

import java.io.Serializable;
import java.util.List;

import com.google.gson.annotations.Expose;

public class AmStackDef implements Serializable {

   private static final long serialVersionUID = 5585914234249234047L;

   @Expose
   private String name;

   @Expose
   private String version;

   @Expose
   private boolean hveSupported;

   @Expose
   private List<String> components;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public boolean isHveSupported() {
      return hveSupported;
   }

   public void setHveSupported(boolean hveSupported) {
      this.hveSupported = hveSupported;
   }

   public List<String> getComponents() {
      return components;
   }

   public void setComponents(List<String> components) {
      this.components = components;
   }
}
