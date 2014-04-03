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

package com.vmware.bdd.placement.util;

import java.util.ArrayList;

import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.entity.VirtualNode;
import com.vmware.bdd.utils.CommonUtil;

public class PlacementUtil {
   public static final String LOCAL_DATASTORE_TYPE = "LOCAL";
   public static final String SHARED_DATASTORE_TYPE = "SHARED";

   public static final String NIC_LABLE = "Network Adapter 1";
   public static final String OS_DISK = "OS.vmdk";
   public static final String SWAP_DISK = "SWAP.vmdk";
   public static final String DATA_DISK = "DATA.vmdk";
   public static final String DATA_DISK_1 = "DATA1.vmdk";
   public static final String DATA_DISK_2 = "DATA2.vmdk";

   public static final String LSI_CONTROLLER_EXTERNAL_ADDRESS_PREFIX =
         "VirtualLsiLogicController:0:";
   public static final int CONTROLLER_RESERVED_CHANNEL = 7;
   public static final String[] PARA_VIRTUAL_SCSI_EXTERNAL_ADDRESS_PREFIXES = {
      "ParaVirtualSCSIController:1:",
      "ParaVirtualSCSIController:2:",
      "ParaVirtualSCSIController:3:"
   };

   public static final String OUT_OF_SYNC_HOSTS = "outOfSyncHosts";
   public static final String NO_NETWORKS_HOSTS = "noNetworkHosts";
   public static final String NETWORK_NAMES = "networkNames";
   public static final String NO_DATASTORE_HOSTS = "noDatastoreHosts";
   public static final String NO_DATASTORE_HOSTS_NODE_GROUP = "noDatastoreHostsNodeGroup";

   public static String getVmName(String clusterName, String groupName,
         int index) {
      return clusterName + "-" + groupName + "-" + index;
   }

   public static int getIndex(BaseNode node) {
      return Integer.parseInt(node.getVmName().split("-")[2]);
   }

   public static String[] wildCardToRegex(String[] wildCards) {
      String[] regexs = new String[wildCards.length];
      for (int i = 0; i < wildCards.length; i++) {
         regexs[i] =
               CommonUtil.escapePattern(wildCards[i]).replaceAll("\\*", ".*")
                     .replaceAll("\\?", ".[1]");
      }

      return regexs;
   }

   public static ArrayList<String> getBaseNodeNames(VirtualNode vNode) {
      ArrayList<String> names = new ArrayList<String>();

      for (BaseNode node : vNode.getBaseNodes()) {
         names.add(node.getVmName());
      }

      return names;
   }

   public static int getNextValidParaVirtualScsiIndex(int paraVirtualScsiIndex) {
      paraVirtualScsiIndex ++;
      int diskIndex = paraVirtualScsiIndex /
         PARA_VIRTUAL_SCSI_EXTERNAL_ADDRESS_PREFIXES.length;
      // controller reserved channel, *:7, cannot be used by custom disk
      if (diskIndex == CONTROLLER_RESERVED_CHANNEL) {
         paraVirtualScsiIndex +=
            PlacementUtil.PARA_VIRTUAL_SCSI_EXTERNAL_ADDRESS_PREFIXES.length;
      }
      return paraVirtualScsiIndex;
   }

   public static String getParaVirtualAddress(int paraVirtualScsiIndex) {
      int arrayIndex = paraVirtualScsiIndex %
         PARA_VIRTUAL_SCSI_EXTERNAL_ADDRESS_PREFIXES.length;
      int diskIndex = paraVirtualScsiIndex /
         PARA_VIRTUAL_SCSI_EXTERNAL_ADDRESS_PREFIXES.length;
      return PARA_VIRTUAL_SCSI_EXTERNAL_ADDRESS_PREFIXES[arrayIndex] + diskIndex;
   }
}
