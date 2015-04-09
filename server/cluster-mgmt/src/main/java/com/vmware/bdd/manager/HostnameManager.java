/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.manager;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.ClusterNetConfigInfo;
import com.vmware.bdd.apitypes.NetConfigInfo;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NicEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.spectypes.NicSpec.NetTrafficDefinition;

public class HostnameManager{

   private static final Logger logger = Logger.getLogger(HostnameManager.class);

   public static String getHostnamePrefix() {
      String hostnamePrefix = "";
      String hostnamePrefixFromConf = Configuration.getString("serengeti.hostname_prefix").trim();
      if (hostnamePrefixFromConf != null && !hostnamePrefixFromConf.isEmpty()) {
         hostnamePrefix = hostnamePrefixFromConf + "-";
      }
      return hostnamePrefix;
   }

   public static String getHostnameHdfsSuffix() {
      String hdfsSuffix = "";
      String hdfsSuffixConf = Configuration.getString("serengeti.hostname_suffix_hdfs").trim();
      if (hdfsSuffixConf !=  null && !hdfsSuffixConf.isEmpty()) {
         hdfsSuffix = "-" + hdfsSuffixConf;
      } else {
         hdfsSuffix = "-hdfs";
      }
      return hdfsSuffix;
   }

   public static String getHostnameMapredSuffix() {
      String mapredSuffix = "";
      String mapredSuffixConf = Configuration.getString("serengeti.hostname_suffix_mapred").trim();
      if (mapredSuffixConf !=  null && !mapredSuffixConf.isEmpty()) {
         mapredSuffix = "-" + mapredSuffixConf;
      } else {
         mapredSuffix = "-mapred";
      }
      return mapredSuffix;
   }

   // TODO To consider multiple traffic definitions in feature
   public static String generateHostname(NodeEntity node, NicEntity nic) throws BddException {
      String vNodeName = node.getVmName();
      String hostname = "";
      for (NetTrafficDefinition trafficDef : nic.getNetTrafficDefs()) {
         hostname = generateHostnameWithTrafficType(trafficDef.getTrafficType(), vNodeName, null);
      }
      return hostname;
   }

   // TODO To consider multiple traffic definitions in feature
   public static String generateHostname(NetworkEntity networkEntity, BaseNode vNode) throws BddException {
      String vNodeName = vNode.getVmName();
      Map<NetTrafficType, List<String>> networkConfig = vNode.getCluster().getNetworkConfig();
      String hostname = "";
      for (Map.Entry<NetTrafficType, List<String>> networkConfigEntry : networkConfig.entrySet()) {
         if (networkConfigEntry.getValue().contains(networkEntity.getName()) && networkEntity.getIsGenerateHostname()) {
            hostname = generateHostnameWithTrafficType(networkConfigEntry.getKey(), vNodeName, vNode.getCluster().getHostnamePrefix());
            break;
         }
      }
      return hostname;
   }

   // TODO To consider multiple traffic definitions in feature
   public static String generateHostname(NetworkEntity networkEntity, NodeEntity node) throws BddException {
      String vNodeName = node.getVmName();
      Map<NetTrafficType, List<ClusterNetConfigInfo>> networkConfig = node.getNodeGroup().getCluster().getNetworkConfigInfo();
      String hostname = "";
      for (Map.Entry<NetTrafficType, List<ClusterNetConfigInfo>> networkConfigEntry : networkConfig.entrySet()) {
         for (ClusterNetConfigInfo netConfigInfo : networkConfigEntry.getValue()) {
            if (netConfigInfo.getIsGenerateHostname() && netConfigInfo.getNetworkName().equals(networkEntity.getName())) {
               hostname = generateHostnameWithTrafficType(networkConfigEntry.getKey(), vNodeName, null);
               break;
            }
         }
      }
      return hostname;
   }

   private static String generateHostnameWithTrafficType(NetTrafficType netTrafficType, String vNodeName, String hostnamePrefix) throws BddException {
      String prefix = getHostnamePrefix();
      if (hostnamePrefix != null) {
         prefix = hostnamePrefix;
      }
      String hdfsSuffix = getHostnameHdfsSuffix();
      String mapredSuffix = getHostnameMapredSuffix();
      String hostname = "";
      String suffix = "";
      switch(netTrafficType) {
      case MGT_NETWORK:
         suffix = vNodeName;
         break;
      case HDFS_NETWORK:
         suffix = vNodeName  + hdfsSuffix;
         break;
      case MAPRED_NETWORK:
         suffix = vNodeName + mapredSuffix;
         break;
      default:
         suffix = vNodeName;
         break;
      }
      hostname = (prefix + suffix).replaceAll("[^a-zA-Z0-9\\-]", "-").replaceAll("([\\-]*[\\-])", "-");

      return hostname.toLowerCase();
   }

}
