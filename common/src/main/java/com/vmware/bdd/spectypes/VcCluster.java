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
package com.vmware.bdd.spectypes;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

/**
 * JSON object for VC cluster definition
 * 
 * @author line
 * 
 */
public class VcCluster {
   @Expose
   private String name;
   @Expose
   private List<String> vc_rps;

   public VcCluster(String name, List<String> rps) {
      this.name = name;
      this.vc_rps = rps;
      if (vc_rps == null) {
         vc_rps = new ArrayList<String>();
      }
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public List<String> getVc_rps() {
      return vc_rps;
   }

   public void addVCResourcePools(List<String> rps) {
      if (rps == null) {
         return;
      }
      vc_rps.addAll(rps);
   }

   public void addVCResourcePool(String rp) {
      if (rp == null) {
         return;
      }
      vc_rps.add(rp);
   }
   
   public String toString() {
      Gson gson = new Gson();
      return gson.toJson(this);
   }
}
