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
package com.vmware.bdd.placement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.StorageRead.DiskType;
import com.vmware.bdd.placement.entity.AbstractDatacenter;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractCluster;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractDatastore;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.util.PlacementUtil;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.utils.AuAssert;

public class TestPlacementUtil {
   private static final Logger logger = Logger
         .getLogger(TestPlacementUtil.class);

   public static final String SIMPLE_CLUSTER_SPEC =
         "src/test/resources/clusterSpec.json";
   public static final String DC_SPLIT_CLUSTER_SPEC =
         "src/test/resources/dc-split-clusterSpec.json";
   public static final String DATACENTER_SPEC =
         "src/test/resources/datacenter.json";
   public static final String EXISTED_NODE_SPEC =
         "src/test/resources/existedNodes.json";
   public static final String RESIZE_NODE_SPEC =
         "src/test/resources/resizeNodes.json";
   public static final String RESIZE_DATACENTER_SPEC =
         "src/test/resources/datacenter-4-resize.json";
   public static final String WITH_RACK_CLUSTER_SPEC =
         "src/test/resources/cluster-with-rack.json";
   public static final String WITH_SAME_RACK_CLUSTER_SPEC =
         "src/test/resources/cluster-with-samerack.json";

   public static final int SYSTEM_DISK_SIZE = 4;

   private static String readJson(String fileName) {
      File file = new File(fileName);

      StringBuffer json = new StringBuffer();
      try {
         BufferedReader reader = new BufferedReader(new FileReader(file));
         String line;
         while ((line = reader.readLine()) != null) {
            json.append(line.trim());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return json.toString();
   }

   public static ClusterCreate getSimpleClusterSpec(String specName)
         throws Exception {
      String json = TestPlacementUtil.readJson(specName);
      logger.info(json);

      ObjectMapper mapper = new ObjectMapper();

      try {
         return mapper.readValue(json, ClusterCreate.class);
      } catch (Exception e) {
         logger.error(e.getMessage());
         throw e;
      }
   }

   public static List<BaseNode> getExistedNodes(String fileName)
         throws Exception {
      ClusterCreate cluster = getSimpleClusterSpec(DC_SPLIT_CLUSTER_SPEC);

      String json = readJson(fileName);
      ObjectMapper mapper = new ObjectMapper();
      List<BaseNode> existedNodes;
      try {
         existedNodes = mapper.readValue(json, new TypeReference<List<BaseNode>>(){});
      } catch (Exception e) {
         logger.error(e.getMessage());
         throw e;
      }
      Assert.assertNotNull(existedNodes);
      for (BaseNode node : existedNodes) {
         node.setCluster(cluster);
         String groupName = node.getVmName().split("-")[1];
         node.setNodeGroup(cluster.getNodeGroup(groupName));
      }

      return existedNodes;
   }

   public static AbstractDatacenter getAbstractDatacenter(String fileName)
         throws Exception {
      String json = TestPlacementUtil.readJson(fileName);
      logger.info(json);

      ObjectMapper mapper = new ObjectMapper();

      try {
         AbstractDatacenter dc = mapper.readValue(json, AbstractDatacenter.class);

         // replace the abstract datastore objects in cluster/host with the ones
         // in dc.datastores
         for (AbstractCluster cluster : dc.getClusters()) {
            // replace datastores in cluster level
            List<AbstractDatastore> dsList = new ArrayList<AbstractDatastore>();
            for (AbstractDatastore datastore : cluster.getDatastores()) {
               AbstractDatastore ds =
                     dc.findAbstractDatastore(datastore.getName());
               AuAssert.check(ds != null);
               dsList.add(ds);
            }
            cluster.setDatastores(dsList);

            // replace datastores in host level
            for (AbstractHost host : cluster.getHosts()) {
               List<AbstractDatastore> datastores =
                     new ArrayList<AbstractDatastore>();
               for (AbstractDatastore datastore : host.getDatastores()) {
                  AbstractDatastore ds =
                        dc.findAbstractDatastore(datastore.getName());
                  AuAssert.check(ds != null);
                  datastores.add(ds);
               }
               host.setDatastores(datastores);
               host.setParent(cluster);
            }
         }
         Assert.assertTrue(dc.findAbstractCluster("cluster-ws") != null,
               "cluster-ws is missing, datacenter spec is not correctly resolved");
         return dc;
      } catch (Exception e) {
         logger.error(e.getMessage());
         throw e;
      }
   }

   public static BaseNode getTemplateNode() {
      BaseNode template = new BaseNode("template-node");

      List<DiskSpec> disks = new ArrayList<DiskSpec>();

      DiskSpec systemDisk = new DiskSpec();
      systemDisk.setName("OS.vmdk");
      systemDisk.setSeparable(false);
      systemDisk.setDiskType(DiskType.SYSTEM_DISK);
      systemDisk.setSize(SYSTEM_DISK_SIZE);

      disks.add(systemDisk);
      template.setDisks(disks);
      return template;
   }

   /**
    * validate cluster placement policy, including vc rp, instance_per_host and
    * strict group association
    *
    * @param cluster
    * @param nodes
    * @param partial
    *           is parameter nodes composite only part of the cluster?
    * @return
    */
   public static boolean validate(ClusterCreate cluster, List<BaseNode> nodes,
         boolean partial) {
      Assert.assertNotNull(nodes);
      Assert.assertTrue(nodes.size() > 0);

      Map<String, Map<String, Integer>> hostMapByGroup =
            new HashMap<String, Map<String, Integer>>();
      Map<String, Map<String, String>> rpMapByGroup =
            new HashMap<String, Map<String, String>>();

      for (BaseNode node : nodes) {
         String groupName = node.getGroupName();
         String targetHost = node.getTargetHost();
         String targetRp = node.getTargetRp();
         String targetVcCluster = node.getTargetVcCluster();

         if (!hostMapByGroup.containsKey(groupName)) {
            hostMapByGroup.put(groupName, new HashMap<String, Integer>());
         }
         Map<String, Integer> hostMap = hostMapByGroup.get(groupName);
         if (!hostMap.containsKey(targetHost)) {
            hostMap.put(targetHost, 0);
         }
         hostMap.put(targetHost, hostMap.get(targetHost) + 1);

         // validate rp usage
         if (!rpMapByGroup.containsKey(groupName)) {
            rpMapByGroup.put(groupName, new HashMap<String, String>());
         }
         Map<String, String> rpMap = rpMapByGroup.get(groupName);
         if (!rpMap.containsKey(targetVcCluster)) {
            rpMap.put(targetVcCluster, targetRp);
         } else {
            Assert.assertTrue(rpMap.get(targetVcCluster).equals(targetRp),
                  "node groups on the same vc cluster should use the same vc rp");
         }
      }

      for (NodeGroupCreate nodeGroup : cluster.getNodeGroups()) {
         boolean instancePerHost = false;
         boolean strictAssociated = false;
         String referToGroup = null;

         PlacementPolicy placement = nodeGroup.getPlacementPolicies();
         if (placement != null && placement.getInstancePerHost() != null) {
            instancePerHost = true;
         }

         if (placement != null && placement.getGroupAssociations() != null
               && placement.getGroupAssociations().size() > 0) {
            GroupAssociation association =
                  placement.getGroupAssociations().get(0);
            if (GroupAssociationType.STRICT.equals(association.getType())) {
               strictAssociated = true;
               referToGroup = association.getReference();
            }
         }

         int count = 0;
         Map<String, Integer> usage = hostMapByGroup.get(nodeGroup.getName());
         // this node group does not have any existed nodes
         if (usage == null)
            continue;
         for (String host : usage.keySet()) {
            count += usage.get(host);

            // instance_per_host validation
            if (instancePerHost)
               Assert.assertTrue(
                     placement.getInstancePerHost().equals(usage.get(host)),
                     "should follow instance_per_host policy");

            // strict association policy validation
            if (strictAssociated) {
               Assert.assertTrue(
                     hostMapByGroup.get(referToGroup).containsKey(host)
                           && hostMapByGroup.get(referToGroup).get(host) > 0,
                     "should follow strict group association policy");
            }
         }

         if (!partial)
            Assert.assertTrue(count == nodeGroup.getInstanceNum(),
                  "total instance number should be correct");
      }
      return true;
   }

   @Test
   public void testPlaceParaVirtualScsi() {
      int index = 20;
      int nextIndex = PlacementUtil.getNextValidParaVirtualScsiIndex(index);
      Assert.assertEquals(24, nextIndex);
      for (index = 0; index < 50; ) {
         String scsiAddr = PlacementUtil.getParaVirtualAddress(index);
         System.out.println("scsi address: " + scsiAddr);
         index = PlacementUtil.getNextValidParaVirtualScsiIndex(index);
      }
   }
}