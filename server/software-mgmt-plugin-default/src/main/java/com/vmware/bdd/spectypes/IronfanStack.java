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
package com.vmware.bdd.spectypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;

public class IronfanStack extends HadoopStack {

   /**
    *
    */
   private static final long serialVersionUID = 7572021110483416556L;

   private String packagesExistStatus;

   private Map<String, String> hadoopDistroMap = new HashMap<String, String>();

   private List<String> packageRepos;

   @JsonIgnore
   public String getPackagesExistStatus() {
      return packagesExistStatus;
   }

   public void setPackagesExistStatus(String packagesExistStatus) {
      this.packagesExistStatus = packagesExistStatus;
   }

   @JsonIgnore
   public Map<String, String> getHadoopDistroMap() {
      return hadoopDistroMap;
   }

   public void setHadoopDistroMap(Map<String, String> hadoopDistroMap) {
      this.hadoopDistroMap = hadoopDistroMap;
   }

   @JsonIgnore
   public List<String> getPackageRepos() {
      return packageRepos;
   }

   public void setPackageRepos(List<String> packageRepos) {
      this.packageRepos = packageRepos;
   }
}
