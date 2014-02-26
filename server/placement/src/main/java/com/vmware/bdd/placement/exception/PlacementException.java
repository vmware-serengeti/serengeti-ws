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
import com.vmware.bdd.placement.util.PlacementUtil;

public class PlacementException extends BddException {

   private static final long serialVersionUID = -4830110305699128931L;

   public PlacementException() {
   }

   public PlacementException(Throwable cause, String errorId, Object... detail) {
      super(cause, "PLACEMENT", errorId, detail);
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

   public static PlacementException OUT_OF_VC_HOST_WITH_FILTERING(List<String> baseNodeNames,
         Map<String, List<String>> filteredHosts) {
      StringBuilder str = new StringBuilder();
      List<String> outOfSyncHosts = filteredHosts.get(PlacementUtil.OUT_OF_SYNC_HOSTS);
      if (null != outOfSyncHosts) {
         str.append(BddException.getErrorMessage("PLACEMENT.OUT_OF_SYNC_HOSTS", outOfSyncHosts.toString()));
      }
      List<String> noNetworkHosts = filteredHosts.get(PlacementUtil.NO_NETWORKS_HOSTS);
      if (null != noNetworkHosts) {
         if (null != outOfSyncHosts) str.append(" ");
         str.append(BddException.getErrorMessage("PLACEMENT.NO_NETWORK_HOSTS", noNetworkHosts.toString()));
      }
      return new PlacementException(null, "OUT_OF_HOST_WITH_FILTERING",
            baseNodeNames.toString(), str.toString());
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
}
