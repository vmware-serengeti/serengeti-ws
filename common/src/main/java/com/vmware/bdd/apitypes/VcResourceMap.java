/***************************************************************************
 *   Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.vmware.bdd.apitypes;

import java.util.List;
import java.util.Map;

/**
 * Author: Blake Zhang
 * Date: 7/7/15
 */
public class VcResourceMap {
   private List<DatacenterMap> datacenters;

   public List<DatacenterMap> getDatacenters() {
      return datacenters;
   }

   public void setDatacenters(List<DatacenterMap> datacenters) {
      this.datacenters = datacenters;
   }

   public class DatacenterMap {
      private String src;

      private String tgt;

      private List<VcClusterMap> clusters;

      public String getSrc() {
         return src;
      }

      public void setSrc(String src) {
         this.src = src;
      }

      public String getTgt() {
         return tgt;
      }

      public void setTgt(String tgt) {
         this.tgt = tgt;
      }

      public List<VcClusterMap> getClusters() {
         return clusters;
      }

      public void setClusters(List<VcClusterMap> clusters) {
         this.clusters = clusters;
      }
   }

   public class VcClusterMap {
      private String src;

      private String tgt;

      private Map<String, String> hosts;

      public String getSrc() {
         return src;
      }

      public void setSrc(String src) {
         this.src = src;
      }

      public String getTgt() {
         return tgt;
      }

      public void setTgt(String tgt) {
         this.tgt = tgt;
      }

      public Map<String, String> getHosts() {
         return hosts;
      }

      public void setHosts(Map<String, String> hosts) {
         this.hosts = hosts;
      }
   }
}
