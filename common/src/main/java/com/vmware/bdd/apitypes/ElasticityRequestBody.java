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

/**
 * This class is a DTO of the cluster set/resetParam command.
 */
public class ElasticityRequestBody {
   private Integer minComputeNodeNum; // min compute nodes that stay powered on
   private Integer maxComputeNodeNum; // max compute nodes that stay powered on
   private Integer activeComputeNodeNum;
   private Boolean enableAuto;
   private Priority ioPriority;

   public static enum ElasticityMode {
      AUTO, MANUAL
   }

   public Integer getMinComputeNodeNum() {
      return minComputeNodeNum;
   }

   public void setMinComputeNodeNum(Integer minComputeNodeNum) {
      this.minComputeNodeNum = minComputeNodeNum;
   }

   public Integer getMaxComputeNodeNum() {
      return maxComputeNodeNum;
   }

   public void setMaxComputeNodeNum(Integer maxComputeNodeNum) {
      this.maxComputeNodeNum = maxComputeNodeNum;
   }

   public Integer getActiveComputeNodeNum() {
      return activeComputeNodeNum;
   }

   public void setActiveComputeNodeNum(Integer activeComputeNodeNum) {
      this.activeComputeNodeNum = activeComputeNodeNum;
   }

   public Priority getIoPriority() {
      return ioPriority;
   }

   public void setIoPriority(Priority ioPriority) {
      this.ioPriority = ioPriority;
   }

   public Boolean getEnableAuto() {
      return enableAuto;
   }

   public void setEnableAuto(Boolean enableAuto) {
      this.enableAuto = enableAuto;
   }
}