/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.apitypes;

import java.util.List;

import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

public class DistroRead implements Comparable<DistroRead>{
   private String name;
   private String vendor = Constants.DEFAULT_VENDOR;
   private String version;
   private boolean hveSupported;
   private List<String> roles;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getVendor() {
      return vendor;
   }

   public void setVendor(String vendor) {
      this.vendor = vendor;
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

   public List<String> getRoles() {
      return roles;
   }

   public void setRoles(List<String> roles) {
      this.roles = roles;
   }

   @Override
   public int compareTo(DistroRead distroRead) {
      if (CommonUtil.isBlank(distroRead.getName())) {
         return 1;
      }
      return this.getName().compareTo(distroRead.getName());
   }
}
