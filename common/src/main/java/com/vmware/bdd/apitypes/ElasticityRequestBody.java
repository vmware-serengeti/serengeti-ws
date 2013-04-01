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
 * This class is a DTO of the cluster setElasticity command.
 */
public class ElasticityRequestBody {
   private boolean enableAutoElasticity;
   private Integer minComputeNodeNum; // min compute nodes that stay powered on
   private boolean calledByReset;
   private Integer activeComputeNodeNum;
   private String nodeGroupName;
   private ElasticityOperation elasticityOperation;

   public static enum ElasticityMode {
      AUTO, MANUAL
   }

   public static enum ElasticityOperation {
      OP_SET_AUTO, OP_SET_MANUAL, OP_RESET
   }

   public boolean getEnableAutoElasticity() {
      return enableAutoElasticity;
   }

   public void setEnableAutoElasticity(boolean enableAutoElasticity) {
      this.enableAutoElasticity = enableAutoElasticity;
   }

   public Integer getMinComputeNodeNum() {
      return minComputeNodeNum;
   }

   public void setMinComputeNodeNum(Integer minComputeNodeNum) {
      this.minComputeNodeNum = minComputeNodeNum;
   }

   public boolean getCalledByReset() {
      return calledByReset;
   }

   public void setCalledByReset(boolean calledByReset) {
      this.calledByReset = calledByReset;
   }

   public Integer getActiveComputeNodeNum() {
      return activeComputeNodeNum;
   }

   public void setActiveComputeNodeNum(Integer activeComputeNodeNum) {
      this.activeComputeNodeNum = activeComputeNodeNum;
   }

   public String getNodeGroupName() {
      return nodeGroupName;
   }

   public void setNodeGroupName(String nodeGroupName) {
      this.nodeGroupName = nodeGroupName;
   }

   public ElasticityOperation getElasticityOperation() {
      return elasticityOperation;
   }

   public void setElasticityOperation(ElasticityOperation elasticityOperation) {
      this.elasticityOperation = elasticityOperation;
   }
}
