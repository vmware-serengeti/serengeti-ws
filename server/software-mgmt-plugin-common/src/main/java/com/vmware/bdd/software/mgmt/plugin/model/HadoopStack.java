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
package com.vmware.bdd.software.mgmt.plugin.model;

import java.io.Serializable;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * Hadoop distribution information, for instance CDH5 5.0.1
 * @author line
 *
 */public class HadoopStack implements Comparable, Serializable {

   private static final long serialVersionUID = -4032296997440707531L;

   @Expose
   private String distro; // for CM, CDH4/CDH5

   @Expose
   private String fullVersion;

   @Expose
   private boolean hveSupported;

   @Expose
   private List<String> roles;

   @Expose
   private String vendor;

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

   public String getFullVersion() {
      return fullVersion;
   }

   public void setFullVersion(String fullVersion) {
      this.fullVersion = fullVersion;
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

   public String getVendor() {
      return vendor;
   }

   public void setVendor(String vendor) {
      this.vendor = vendor;
   }

   public void setDistro(String stackName, String stackVersion) {
      this.distro = stackName + "-" + stackVersion;
   }

   @Override
   public int compareTo(Object o) {
      assert o.getClass().equals(this.getClass());
      HadoopStack stack = (HadoopStack) o;
      if (this.getFullVersion().compareTo(stack.getFullVersion()) < 0) {
         return 1;
      } else if (this.getFullVersion().compareTo(stack.getFullVersion()) > 0) {
         return -1;
      } else {
         return 0;
      }
   }

}
