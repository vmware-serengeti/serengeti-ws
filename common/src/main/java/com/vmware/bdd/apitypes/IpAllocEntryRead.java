/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

public class IpAllocEntryRead {
   private String ipAddress;

   private Long clusterId;

   private String clusterName;

   private Long nodeGroupId;

   private String nodeGroupName;

   private Long nodeId;

   private String nodeName;

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public Long getClusterId() {
      return clusterId;
   }

   public void setClusterId(Long clusterId) {
      this.clusterId = clusterId;
   }

   public String getClusterName() {
      return clusterName;
   }

   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   public Long getNodeGroupId() {
      return nodeGroupId;
   }

   public void setNodeGroupId(Long nodeGroupId) {
      this.nodeGroupId = nodeGroupId;
   }

   public String getNodeGroupName() {
      return nodeGroupName;
   }

   public void setNodeGroupName(String nodeGroupName) {
      this.nodeGroupName = nodeGroupName;
   }

   public Long getNodeId() {
      return nodeId;
   }

   public void setNodeId(Long nodeId) {
      this.nodeId = nodeId;
   }

   public String getNodeName() {
      return nodeName;
   }

   public void setNodeName(String nodeName) {
      this.nodeName = nodeName;
   }
}
