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

package com.vmware.bdd.placement.exception;

import java.util.List;
import java.util.Map;

import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.util.PlacementUtil;

public class PlacementException extends BddException {

   private static final long serialVersionUID = -4830110305699128931L;

   public PlacementException() {
   }

   public PlacementException(Throwable cause, String errorId, Object... detail) {
      super(cause, "PLACEMENT", errorId, detail);
   }

   public static PlacementException PLACEMENT_ERROR(PlacementException cause, List<BaseNode> placedNodes,
         Map<String, List<String>> filteredHosts) {
      StringBuilder placedNodesStr = new StringBuilder();
      if (!placedNodes.isEmpty()) placedNodesStr.append("\n");
      for (BaseNode baseNode : placedNodes) {
         placedNodesStr.append(BddException.getErrorMessage("PLACEMENT.NODE_PLACED_ON_HOST", baseNode.getVmName(), baseNode.getTargetHost()));
         placedNodesStr.append(" ");
      }
      StringBuilder filteredHostsStr = new StringBuilder();
      List<String> outOfSyncHosts = filteredHosts.get(PlacementUtil.OUT_OF_SYNC_HOSTS);
      if (null != outOfSyncHosts) {
         filteredHostsStr.append("\n");
         filteredHostsStr.append(BddException.getErrorMessage("PLACEMENT.OUT_OF_SYNC_HOSTS", outOfSyncHosts.toString()));
      }
      List<String> networkNames = filteredHosts.get(PlacementUtil.NETWORK_NAMES);
      List<String> noNetworkHosts = filteredHosts.get(PlacementUtil.NO_NETWORKS_HOSTS);
      if (null != noNetworkHosts) {
         filteredHostsStr.append("\n");
         filteredHostsStr.append(BddException.getErrorMessage("PLACEMENT.NO_NETWORK_HOSTS", networkNames.toString(), noNetworkHosts.toString()));
      }
      return new PlacementException(cause, "PLACEMENT_ERROR", cause.getMessage(), placedNodesStr.toString(), filteredHostsStr.toString());
   }

   public static PlacementException OUT_OF_STORAGE_ON_HOST(String host) {
      return new PlacementException(null, "OUT_OF_STORAGE", host);
   }

   public static PlacementException OUT_OF_RP(List<String> baseNodeNames) {
      return new PlacementException(null, "OUT_OF_RP", baseNodeNames.toString());
   }

   public static PlacementException OUT_OF_VC_HOST(List<String> baseNodeNames) {
      return new PlacementException(null, "OUT_OF_HOST",
            baseNodeNames.toString());
   }

   public static PlacementException DO_NOT_HAVE_SHARED_VC_CLUSTER(
         List<String> nodeGroupNames) {
      return new PlacementException(null, "DO_NOT_HAVE_SHARED_VC_CLUSTER",
            nodeGroupNames.toString());
   }

   public static PlacementException OUT_OF_VC_CLUSTER(String cluster) {
      return new PlacementException(null, "NONE_CLUSTER", cluster);
   }

   public static PlacementException INVALID_RACK_INFO(String cluster,
         String nodeGroup) {
      return new PlacementException(null, "INVALID_RACK_INFO", cluster,
            nodeGroup);
   }

   public static PlacementException VC_CLUSTER_NOT_FOUND(String vcCluster) {
      return new PlacementException(null, "VC_CLUSTER_NOT_FOUND", vcCluster);
   }

   public static PlacementException OUT_OF_RACK(List<String> candidateRacks, List<String> baseNodeNames) {
      return new PlacementException(null, "OUT_OF_RACK", candidateRacks.toString(), baseNodeNames.toString());
   }

   public static PlacementException INSTANCE_PER_HOST_VIOLATION(List<String> baseNodeNames) {
      return new PlacementException(null, "INSTANCE_PER_HOST_VIOLATION", baseNodeNames.toString());
   }
}
