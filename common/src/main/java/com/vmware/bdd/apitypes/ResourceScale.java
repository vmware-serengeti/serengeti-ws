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
package com.vmware.bdd.apitypes;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */
public class ResourceScale {

   private String clusterName;
   private String nodeGroupName;
   private int cpuNumber;
   private long memory; //

   public ResourceScale(){
      
   }
   public ResourceScale(String clusterName, String nodeGroupName, int cpuNumber, long memory){
      this.clusterName = clusterName;
      this.nodeGroupName = nodeGroupName;
      this.cpuNumber = cpuNumber;
      this.memory = memory;
   }
   
   /**
    * @return the clusterName
    */
   @RestRequired
   public String getClusterName() {
      return clusterName;
   }

   /**
    * @param clusterName
    *           the clusterName to set
    */
   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   /**
    * @return the nodeGroupName
    */
   @RestRequired
   public String getNodeGroupName() {
      return nodeGroupName;
   }

   /**
    * @param nodeGroupName
    *           the nodeGroupName to set
    */
   public void setNodeGroupName(String nodeGroupName) {
      this.nodeGroupName = nodeGroupName;
   }

   /**
    * @return the cpuNumber
    */
   public int getCpuNumber() {
      return cpuNumber;
   }

   /**
    * @param cpuNumber
    *           the cpuNumber to set
    */
   public void setCpuNumber(int cpuNumber) {
      this.cpuNumber = cpuNumber;
   }

   /**
    * @return the memory
    */
   public long getMemory() {
      return memory;
   }

   /**
    * @param memory
    *           the memory to set
    */
   public void setMemory(long memory) {
      this.memory = memory;
   }

   public String toString(){
      StringBuilder strBuilder = new StringBuilder();
      strBuilder.append("clusterName=");
      strBuilder.append(clusterName);
      strBuilder.append(",nodeGroup=");
      strBuilder.append(nodeGroupName);
      strBuilder.append(",cpuNumber=");
      strBuilder.append(cpuNumber);
      strBuilder.append(",memory=");
      strBuilder.append(memory);
      return strBuilder.toString();
   }

}
