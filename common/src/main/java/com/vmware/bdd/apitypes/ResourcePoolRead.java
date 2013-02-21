/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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

import com.google.gson.Gson;

public class ResourcePoolRead {
   private String rpName;
   private String vcCluster;
   private String rpVsphereName;
   private int totalRAMInMB;
   private int usedRAMInMB;
   private int totalCPUInMHz;
   private int usedCPUInMHz;
   private NodeRead[] nodes;

   public String getRpName() {
      return rpName;
   }

   public void setRpName(String rpName) {
      this.rpName = rpName;
   }

   public String getVcCluster() {
      return vcCluster;
   }

   public void setVcCluster(String vcCluster) {
      this.vcCluster = vcCluster;
   }

   public String getRpVsphereName() {
      return rpVsphereName;
   }

   public void setRpVsphereName(String rpVsphereName) {
      this.rpVsphereName = rpVsphereName;
   }

   public int getTotalRAMInMB() {
      return totalRAMInMB;
   }

   public void setTotalRAMInMB(int totalRAMInMB) {
      this.totalRAMInMB = totalRAMInMB;
   }

   public int getUsedRAMInMB() {
      return usedRAMInMB;
   }

   public void setUsedRAMInMB(int usedRAMInMB) {
      this.usedRAMInMB = usedRAMInMB;
   }

   public int getTotalCPUInMHz() {
      return totalCPUInMHz;
   }

   public void setTotalCPUInMHz(int totalCPUInMHz) {
      this.totalCPUInMHz = totalCPUInMHz;
   }

   public int getUsedCPUInMHz() {
      return usedCPUInMHz;
   }

   public void setUsedCPUInMHz(int usedCPUInMHz) {
      this.usedCPUInMHz = usedCPUInMHz;
   }

   public NodeRead[] getNodes() {
      return nodes;
   }

   public void setNodes(NodeRead[] nodes) {
      this.nodes = nodes;
   }

   public String toString() {
      return (new Gson()).toJson(this);
   }
   
   public String findPath(){
      return this.getVcCluster() + "/" + this.getRpVsphereName();
   }
}
