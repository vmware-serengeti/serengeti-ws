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

package com.vmware.bdd.placement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.utils.Constants;
import org.apache.log4j.Logger;

import com.google.gson.internal.Pair;
import com.vmware.aurora.composition.NetworkSchema;
import com.vmware.aurora.composition.NetworkSchema.Network;
import com.vmware.aurora.composition.ResourceSchema;
import com.vmware.aurora.interfaces.model.IDatabaseConfig.Priority;
import com.vmware.aurora.vc.DiskSpec.AllocationType;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.DiskSplitPolicy;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupRacks;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupRacks.GroupRacksType;
import com.vmware.bdd.apitypes.StorageRead.DiskScsiControllerType;
import com.vmware.bdd.apitypes.StorageRead.DiskType;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractCluster;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractDatastore;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.entity.VirtualGroup;
import com.vmware.bdd.placement.entity.VirtualNode;
import com.vmware.bdd.placement.exception.PlacementException;
import com.vmware.bdd.placement.interfaces.IPlacementPlanner;
import com.vmware.bdd.placement.util.PlacementUtil;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.spectypes.VcCluster;
import com.vmware.bdd.utils.AuAssert;

public class PlacementPlanner implements IPlacementPlanner {
   static final Logger logger = Logger.getLogger(PlacementPlanner.class);

   boolean init = false;

   ClusterCreate cluster = null;

   BaseNode templateNode;

   // count the number of base nodes each host has, categorized by node group
   Map<String, Map<String, Integer>> hostMapByGroup;

   // count the number of base nodes each host has, categorized by cluster
   Map<String, Integer> hostMapByCluster;

   // count the number of references each vc rp has
   Map<Pair<String, String>, Integer> rpUsage;

   /*
    *  record the cluster-rp assignment for each node group. The assumption is that
    *  all nodes belong to the same node group should be placed under the same
    *  resource pool if they select Host from the same VC Cluster
    */
   Map<String, Map<String, String>> rpMapByGroup;

   // count the number of base nodes each Rack has, categorized by node group
   Map<String, Map<String, Integer>> rackUsageByGroup;

   Map<String, String> hostToRackMap;

   public void init(ClusterCreate cluster, BaseNode template,
         List<BaseNode> existedNodes, Map<String, String> hostToRackMap) {
      this.cluster = cluster;
      this.templateNode = template;

      hostMapByGroup = new HashMap<String, Map<String, Integer>>();
      hostMapByCluster = new HashMap<String, Integer>();
      rpUsage = new HashMap<Pair<String, String>, Integer>();
      rpMapByGroup = new HashMap<String, Map<String, String>>();
      rackUsageByGroup = new HashMap<String, Map<String, Integer>>();
      this.hostToRackMap = hostToRackMap;

      for (NodeGroupCreate nodeGroup : cluster.getNodeGroups()) {
         if (nodeGroup.getPlacementPolicies() != null
               && nodeGroup.getPlacementPolicies().getGroupRacks() != null) {
            if (!rackUsageByGroup.containsKey(nodeGroup.getName())) {
               rackUsageByGroup.put(nodeGroup.getName(),
                     new HashMap<String, Integer>());
            }
            // the validation in ClusteringService assures this assertion
            AuAssert.check(hostToRackMap != null && hostToRackMap.size() != 0);
            for (String rack : hostToRackMap.values()) {
               rackUsageByGroup.get(nodeGroup.getName()).put(rack, 0);
            }
         }
      }

      // populate node to host map
      if (existedNodes != null && existedNodes.size() > 0) {
         for (BaseNode node : existedNodes) {
            String groupName = node.getGroupName();
            String targetHost = node.getTargetHost();
            String rp = node.getTargetRp();
            String vcCluster = node.getTargetVcCluster();
            String rack = node.getTargetRack();

            // populate host by group map
            if (!hostMapByGroup.containsKey(groupName)) {
               hostMapByGroup.put(groupName, new HashMap<String, Integer>());
            }
            Map<String, Integer> hostMap = hostMapByGroup.get(groupName);
            if (!hostMap.containsKey(targetHost)) {
               hostMap.put(targetHost, 0);
            }
            hostMap.put(targetHost, hostMap.get(targetHost) + 1);

            // populate host by cluster map
            if (!hostMapByCluster.containsKey(targetHost)) {
               hostMapByCluster.put(targetHost, 0);
            }
            hostMapByCluster.put(targetHost,
                  hostMapByCluster.get(targetHost) + 1);

            // populate RP by vc cluster map
            Pair<String, String> rpClusterPair =
                  new Pair<String, String>(vcCluster, rp);
            if (!rpUsage.containsKey(rpClusterPair)) {
               rpUsage.put(rpClusterPair, 0);
            }
            rpUsage.put(rpClusterPair, rpUsage.get(rpClusterPair) + 1);

            // populate Cluster-RP assignment for each node group
            if (!rpMapByGroup.containsKey(groupName)) {
               rpMapByGroup.put(groupName, new HashMap<String, String>());
            }
            // assume all nodes from the same node group have been put into the same cluster-rp
            rpMapByGroup.get(groupName).put(vcCluster, rp);

            // populate Rack by node group map
            if (!rackUsageByGroup.containsKey(groupName)) {
               rackUsageByGroup.put(groupName, new HashMap<String, Integer>());
            }
            Map<String, Integer> rackMap = rackUsageByGroup.get(groupName);
            if (!rackMap.containsKey(rack)) {
               rackMap.put(rack, 0);
            }
            rackMap.put(rack, rackMap.get(rack) + 1);
         }
      }

      this.init = true;
   }

   @Override
   public BaseNode getBaseNode(ClusterCreate cluster,
         NodeGroupCreate nodeGroup, int index) {
      String vmName =
            PlacementUtil.getVmName(cluster.getName(), nodeGroup.getName(),
                  index);

      BaseNode node = new BaseNode(vmName, nodeGroup, cluster);

      // initialize disks
      List<DiskSpec> disks = new ArrayList<DiskSpec>();

      DiskSpec systemDisk = new DiskSpec(templateNode.getDisks().get(0));

      // use swap disk by default
      boolean disableSwap = Configuration.getBoolean(Constants.SERENGETI_DISABLE_SWAPDISK, false);
      if (!disableSwap) {
      /*
       * TRICK: here we count the size of vswp file into the system disk size, as the
       * vswp file will be put together with system disk.
       */
         Integer memCapa = nodeGroup.getMemCapacityMB();
         memCapa = (memCapa == null) ? 0 : memCapa;
         systemDisk.setSize(systemDisk.getSize() + (memCapa + 1023) / 1024);
      }

      systemDisk.setDiskType(DiskType.SYSTEM_DISK);
      systemDisk.setSeparable(false);
      disks.add(systemDisk);
      AllocationType diskAllocType = null;
      if (nodeGroup.getStorage().getAllocType() != null) {
         diskAllocType =
               AllocationType.valueOf(nodeGroup.getStorage().getAllocType());
      } else {
         // THICK as by default
         diskAllocType = AllocationType.THICK;
      }
      // swap disk
      int swapDisk =
            (((int) Math.ceil(nodeGroup.getMemCapacityMB()
                  * nodeGroup.getSwapRatio()) + 1023) / 1024);
      disks.add(new DiskSpec(DiskType.SWAP_DISK.getDiskName(), swapDisk, node
            .getVmName(), false, DiskType.SWAP_DISK,
            DiskScsiControllerType.LSI_CONTROLLER, null, diskAllocType
                  .toString(), null, null, null));

      // data disks
      if (!DatastoreType.TEMPFS.name().equalsIgnoreCase(
            nodeGroup.getStorage().getType())) {
         // no need to add data disk for storage type tempfs
         disks.add(new DiskSpec(DiskType.DATA_DISK.getDiskName(), nodeGroup
               .getStorage().getSizeGB(), node.getVmName(), true,
               DiskType.DATA_DISK, nodeGroup.getStorage().getControllerType(),
               nodeGroup.getStorage().getSplitPolicy(), diskAllocType
                     .toString(), null, null, null));
      }
      node.setDisks(disks);

      // target vm folder
      node.setVmFolder(nodeGroup.getVmFolderPath());

      // target network, hard coded as the only one NIC
      NetworkSchema netSchema = new NetworkSchema();

      ArrayList<Network> networks = new ArrayList<Network>();
      netSchema.networks = networks;

      // TODO: enhance this logic to support nodegroup level networks
      for (NetworkAdd networkAdd : cluster.getNetworkings()) {
         Network network = new Network();
         network.vcNetwork = networkAdd.getPortGroup();
         networks.add(network);
      }

      node.getVmSchema().networkSchema = netSchema;

      // resource schema
      ResourceSchema resourceSchema = new ResourceSchema();
      resourceSchema.numCPUs = node.getCpu();
      // we don't reserve cpu resource
      resourceSchema.cpuReservationMHz = 0;
      resourceSchema.memSize = node.getMem();
      resourceSchema.memReservationSize = 0;
      resourceSchema.name = "Resource Schema";
      resourceSchema.priority = Priority.Normal;

      node.getVmSchema().resourceSchema = resourceSchema;

      return node;
   }

   private List<VirtualNode> getVirtualNodes(VirtualGroup vGroup,
         List<BaseNode> existedNodes) {
      /*
       *  are there multiple node groups inside the vGroup?
       *  if it's true, there must be one node group be the master that others are all
       *  strictly referring to it. They all had the instance_per_host constraints meanwhile.
       */
      boolean mixed = vGroup.getNodeGroups().size() > 1;
      Integer vNodeNum = 0;
      // ensure all node groups have the same host number
      if (mixed) {
         NodeGroupCreate primary = vGroup.getPrimaryGroup();
         vNodeNum = primary.getInstanceNum() / primary.instancePerHost();
         AuAssert.check(vNodeNum != null && vNodeNum > 0);

         // slave groups cannot have required host numbers large than the primary group
         for (NodeGroupCreate nodeGroup : vGroup.getNodeGroups()) {
            AuAssert.check(nodeGroup.calculateHostNum() <= vNodeNum);
         }
      } else {
         vNodeNum = vGroup.getNodeGroups().get(0).getInstanceNum();
         if (vGroup.hasInstancePerHostPolicy()) {
            vNodeNum =
                  vNodeNum / vGroup.getNodeGroups().get(0).instancePerHost();
         }
      }

      List<VirtualNode> vNodes = new ArrayList<VirtualNode>();

      for (int i = 0; i < vNodeNum; i++) {
         VirtualNode vNode = new VirtualNode(vGroup);
         int count = 0;
         boolean primaryExisted = false;
         String primaryGroup = null;
         for (NodeGroupCreate nodeGroup : vGroup.getNodeGroups()) {
            /*
             * by default, if there are no instance_per_host policy, one virtual node contains
             * one base node
             */
            int baseNodeNum = 1;
            if (nodeGroup.instancePerHost() != null) {
               // multiple base nodes inside a virtual node
               baseNodeNum = nodeGroup.instancePerHost();
            }

            /*
             * imagine a dc-split case, where a virtual group contains a data group (4 nodes), and a
             * compute group (3 nodes). Apparently, the last virtual node should contain only one data
             * node.
             */
            if (i * baseNodeNum >= nodeGroup.getInstanceNum()) {
               baseNodeNum = 0;
            }

            for (int j = 0; j < baseNodeNum; j++) {
               BaseNode newGuy =
                     getBaseNode(cluster, nodeGroup, i * baseNodeNum + j);
               boolean existed = false;

               if (existedNodes != null && existedNodes.size() > 0) {
                  // existedNodes -> Map<NodeName>
                  for (BaseNode node : existedNodes) {
                     if (newGuy.getVmName().equalsIgnoreCase(node.getVmName())) {
                        existed = true;
                        // handle the special data node existence case here
                        if (mixed
                              && newGuy.getNodeGroup().getReferredGroup() == null) {
                           primaryExisted = true;
                           primaryGroup = newGuy.getNodeGroup().getName();
                        }
                        break;
                     }
                  }
               }
               if (!existed) {
                  vNode.addNode(newGuy);
                  count++;
               }
            }
         }
         if (count == 0)
            continue;
         logger.info("put " + count + " base nodes into one virtual node");
         if (primaryExisted) {
            vNode.setReferToGroup(primaryGroup);
            vNode.setStrictAssociated(true);
         }
         vNodes.add(vNode);
      }

      return vNodes;
   }

   @Override
   public List<VirtualGroup> getVirtualGroups(List<BaseNode> existedNodes) {
      AuAssert.check(init);

      Map<String, VirtualGroup> groups = new HashMap<String, VirtualGroup>();

      for (NodeGroupCreate nodeGroup : cluster.getNodeGroups()) {
         if (groups.containsKey(nodeGroup.getName())) {
            continue;
         }

         boolean single = true;
         if (nodeGroup.getReferredGroup() != null
               && nodeGroup.isStrictReferred()
               && nodeGroup.instancePerHost() != null) {
            NodeGroupCreate referredGroup =
                  cluster.getNodeGroup(nodeGroup.getReferredGroup());
            AuAssert.check(referredGroup != null);
            if (referredGroup.instancePerHost() != null) {
               /*
                *  only put strict associated groups which both has instance_per_host
                *  constraints into one virtual group
                */
               single = false;
               if (groups.containsKey(referredGroup.getName())) {
                  groups.get(referredGroup.getName()).addNodeGroup(nodeGroup);
               } else {
                  VirtualGroup vGroup = new VirtualGroup(this.cluster);
                  vGroup.addNodeGroup(nodeGroup);
                  vGroup.addNodeGroup(referredGroup);
                  groups.put(referredGroup.getName(), vGroup);
               }
            }
         }
         if (single) {
            VirtualGroup vGroup = new VirtualGroup(this.cluster);
            vGroup.addNodeGroup(nodeGroup);
            groups.put(nodeGroup.getName(), vGroup);
         }
      }

      /*
       * mark the groups that are referenced by others. they should be placed first
       * Note that the group association policy has the assumption that:
       * 1. only one level of reference, i.e., if A is referred by B,
       *    B cannot be referred by others
       * 2. not allow the reference to multiple groups, i.e., if A refers to B,
       *    A cannot refer to any others
       */
      for (NodeGroupCreate nodeGroup : cluster.getNodeGroups()) {
         if (nodeGroup.getReferredGroup() != null) {
            AuAssert.check(groups.containsKey(nodeGroup.getReferredGroup()));
            groups.get(nodeGroup.getReferredGroup()).setReferred(true);
            if (groups.containsKey(nodeGroup.getName())) {
               groups.get(nodeGroup.getName()).setReferToGroup(
                     nodeGroup.getReferredGroup());
               if (nodeGroup.isStrictReferred()) {
                  groups.get(nodeGroup.getName()).setStrictAssociated(true);
               }
            }
         }
      }

      List<VirtualGroup> vGroups = new ArrayList<VirtualGroup>(groups.values());

      // process the instance_per_host policy
      for (VirtualGroup vGroup : vGroups) {
         vGroup.setvNodes(getVirtualNodes(vGroup, existedNodes));
      }

      return vGroups;
   }

   private List<AbstractHost> instancePerHostFilter(VirtualNode vNode,
         List<AbstractHost> candidates) {
      for (BaseNode node : vNode.getBaseNodes()) {
         if (!hostMapByGroup.containsKey(node.getGroupName())) {
            Map<String, Integer> map = new HashMap<String, Integer>();
            hostMapByGroup.put(node.getGroupName(), map);
            continue;
         } else {
            Map<String, Integer> map = hostMapByGroup.get(node.getGroupName());
            List<AbstractHost> removed = new ArrayList<AbstractHost>();
            for (AbstractHost host : candidates) {
               if (map.containsKey(host.getName())) {
                  removed.add(host);
               }
            }
            candidates.removeAll(removed);
         }
      }

      return candidates;
   }

   private List<AbstractHost> groupAssociationFilter(VirtualNode node,
         List<AbstractHost> candidates) {
      AuAssert.check(node.getReferToGroup() != null);
      AuAssert.check(hostMapByGroup.containsKey(node.getReferToGroup()));

      List<AbstractHost> associatedCandidates = new ArrayList<AbstractHost>();
      Map<String, Integer> hostMap = hostMapByGroup.get(node.getReferToGroup());

      for (AbstractHost host : candidates) {
         if (hostMap.containsKey(host.getName())) {
            associatedCandidates.add(host);
         }
      }

      return associatedCandidates;
   }

   private AbstractHost getLeastUsed(List<AbstractHost> candidates) {
      int min = Integer.MAX_VALUE;
      AbstractHost candidate = null;

      for (AbstractHost host : candidates) {
         if (!hostMapByCluster.containsKey(host.getName())) {
            return host;
         } else if (hostMapByCluster.get(host.getName()) < min) {
            min = hostMapByCluster.get(host.getName());
            candidate = host;
         }
      }

      return candidate;
   }

   private void assignHost(VirtualNode vNode, AbstractHost host) {
      // update host map by node group
      for (BaseNode node : vNode.getBaseNodes()) {
         if (!hostMapByGroup.containsKey(node.getGroupName())) {
            Map<String, Integer> map = new HashMap<String, Integer>();
            map.put(host.getName(), 1);
            hostMapByGroup.put(node.getGroupName(), map);
         } else {
            Map<String, Integer> map = hostMapByGroup.get(node.getGroupName());
            int oldValue =
                  map.containsKey(host.getName()) ? map.get(host.getName()) : 0;
            map.put(host.getName(), oldValue + 1);
         }
      }

      // update host map by cluster
      int oldValue =
            hostMapByCluster.containsKey(host.getName()) ? hostMapByCluster
                  .get(host.getName()) : 0;

      hostMapByCluster.put(host.getName(), oldValue
            + vNode.getBaseNodes().size());

      // update rack usage
      if (vNode.getParent().getGroupRacks() != null && hostToRackMap != null
            && hostToRackMap.containsKey(host.getName())) {
         String rack = hostToRackMap.get(host.getName());
         Map<String, Integer> usage =
               rackUsageByGroup.get(vNode.getParent().getPrimaryGroup()
                     .getName());
         usage.put(rack, usage.get(rack) + vNode.getBaseNodes().size());
      }
   }

   private List<DiskSpec> placeUnSeparableDisks(List<DiskSpec> disks,
         List<AbstractDatastore> datastores) {
      List<DiskSpec> result = new ArrayList<DiskSpec>();

      Collections.sort(disks, Collections.reverseOrder());
      // balance the datastore usage among multiple calls
      Collections.shuffle(datastores);

      for (DiskSpec disk : disks) {
         int i = 0;
         for (; i < datastores.size(); i++) {
            AbstractDatastore ds = datastores.get(i);
            if (disk.getSize() <= ds.getFreeSpace()) {
               disk.setTargetDs(ds.getName());
               ds.allocate(disk.getSize());
               result.add(disk);
               Collections.rotate(datastores, 1);
               break;
            }
         }
         // cannot find a datastore to hold this disk
         if (i >= datastores.size()) {
            return null;
         }
      }
      return result;
   }

   private List<DiskSpec> evenSpliter(DiskSpec separable,
         List<AbstractDatastore> originDatastores) {
      int minDiskSize = 2;
      int maxNumDatastores = (separable.getSize() + minDiskSize - 1) / minDiskSize;
      Collections.sort(originDatastores);
      List<AbstractDatastore> datastores = new ArrayList<AbstractDatastore>();
      int numDatastores = 0;
      for (AbstractDatastore datastore : originDatastores) {
         if (datastore.getFreeSpace() < minDiskSize) continue;
         datastores.add(datastore);
         numDatastores++;
         if (numDatastores == maxNumDatastores) break;
      }

      int length = datastores.size() + 1;
      int[] free = new int[length];
      int[] partSum = new int[length];

      int iter = 0;
      for (int i = 0; i < length; i++) {
         if (i == 0) {
            free[0] = 0;
            partSum[0] = 0;
         } else {
            free[i] = datastores.get(i - 1).getFreeSpace();
            partSum[i] = iter + (free[i] - free[i - 1]) * (length - i);
            iter = partSum[i];
         }
      }

      if (partSum[length - 1] < separable.getSize()) {
         logger.error("Even Spliter: not sufficient storage space to place disk "
               + separable.toString());
         return null;
      }

      int index = Arrays.binarySearch(partSum, separable.getSize());

      if (index < 0)
         index = -1 * (index + 1);
      // index now is the insertion point of separable.getSize() in the array partSum
      index--;

      int remain =
            (index == 0) ? separable.getSize()
                  : (separable.getSize() - partSum[index]);

      int ave = (remain + length - index - 2) / (length - index - 1);

      int[] allocation = new int[length - 1];
      for (int i = 0; i < length - 1; i++) {
         if (i < index) {
            allocation[i] = free[i + 1];
         } else if (remain > 0) {
            if (remain >= ave) {
               allocation[i] = free[index] + ave;
            } else {
               allocation[i] = free[index] + remain;
            }
            remain -= ave;
         } else {
            allocation[i] = free[index];
         }
      }

      index = 0;
      List<DiskSpec> disks = new ArrayList<DiskSpec>();
      for (int i = 0; i < length - 1; i++) {
         if (allocation[i] != 0) {
            DiskSpec subDisk = new DiskSpec(separable);
            subDisk.setSize(allocation[i]);
            subDisk.setSeparable(false);
            subDisk.setTargetDs(datastores.get(i).getName());
            // new name with index as suffix, e.g., DATA1.vmdk
            subDisk.setName(separable.getName().split("\\.")[0] + index
                  + ".vmdk");

            disks.add(subDisk);
            datastores.get(i).allocate(allocation[i]);
            index++;
         }
      }

      return disks;
   }

   private List<DiskSpec> aggregateSpliter(DiskSpec separable,
         List<AbstractDatastore> datastores) {
      Collections.sort(datastores, Collections.reverseOrder());

      int i = 0;
      int index = 0;
      int remain = separable.getSize();
      List<DiskSpec> disks = new ArrayList<DiskSpec>();

      for (; i < datastores.size(); i++) {
         AbstractDatastore ds = datastores.get(i);
         if (ds.getFreeSpace() == 0)
            continue;

         int size = remain;
         if (remain > ds.getFreeSpace()) {
            size = ds.getFreeSpace();
         }
         remain -= size;

         DiskSpec subDisk = new DiskSpec(separable);
         subDisk.setSize(size);
         subDisk.setSeparable(false);
         subDisk.setTargetDs(ds.getName());
         // new name with index as suffix, e.g., DATA1.vmdk
         subDisk.setName(separable.getName().split("\\.")[0] + index + ".vmdk");

         disks.add(subDisk);
         ds.allocate(size);
         index++;
         if (remain == 0)
            break;
      }
      // not enough space to place this disk
      if (i >= datastores.size()) {
         logger.error("Aggregate Spliter: not sufficient storage space to place disk "
               + separable.toString());
         return null;
      }
      return disks;
   }

   private List<DiskSpec> placeSeparableDisks(List<DiskSpec> separable,
         List<AbstractDatastore> datastores) {
      List<DiskSpec> result = new ArrayList<DiskSpec>();

      Collections.sort(separable, Collections.reverseOrder());
      for (DiskSpec disk : separable) {
         List<DiskSpec> subDisks;
         if (disk.getSplitPolicy() != null
               && DiskSplitPolicy.EVEN_SPLIT.equals(disk.getSplitPolicy())) {
            subDisks = evenSpliter(disk, datastores);
         } else {
            // aggregate split by default
            subDisks = aggregateSpliter(disk, datastores);
         }

         if (subDisks != null)
            result.addAll(subDisks);
         else
            return null;
      }
      return result;
   }

   private int getDiskSize(List<DiskSpec> disks) {
      int size = 0;
      for (DiskSpec disk : disks) {
         size += disk.getSize();
      }
      return size;
   }

   private int getDsFree(List<AbstractDatastore> datastores) {
      int size = 0;
      for (AbstractDatastore ds : datastores) {
         size += ds.getFreeSpace();
      }
      return size;
   }

   // try to place disk onto a host, inject the disk placement plans into BaseNode.disks field
   private boolean placeDisk(VirtualNode vNode, AbstractHost host) {
      AbstractHost clonedHost = AbstractHost.clone(host);

      Map<BaseNode, List<DiskSpec>> result =
            new HashMap<BaseNode, List<DiskSpec>>();

      for (BaseNode node : vNode.getBaseNodes()) {
         List<DiskSpec> disks;

         List<AbstractDatastore> imagestores =
               clonedHost.getDatastores(node.getImagestoreNamePattern());

         List<AbstractDatastore> diskstores =
               clonedHost.getDatastores(node.getDiskstoreNamePattern());

         // system and swap disk
         List<DiskSpec> systemDisks = new ArrayList<DiskSpec>();
         // un-separable disks
         List<DiskSpec> unseparable = new ArrayList<DiskSpec>();
         // separable disks
         List<DiskSpec> separable = new ArrayList<DiskSpec>();

         // process bi_sector split policy disks
         List<DiskSpec> removed = new ArrayList<DiskSpec>();
         for (DiskSpec disk : node.getDisks()) {
            if (disk.getSplitPolicy() != null
                  && DiskSplitPolicy.BI_SECTOR.equals(disk.getSplitPolicy())) {
               int half = disk.getSize() / 2;
               unseparable.add(new DiskSpec(disk.getName().split("\\.")[0]
                     + "0.vmdk", half, node.getVmName(), false, disk
                     .getDiskType(), disk.getController(), null, disk
                     .getAllocType(), null, null, null));
               unseparable.add(new DiskSpec(disk.getName().split("\\.")[0]
                     + "1.vmdk", disk.getSize() - half, node.getVmName(),
                     false, disk.getDiskType(), disk.getController(), null,
                     disk.getAllocType(), null, null, null));
               removed.add(disk);
            }
         }
         // removed bi_sector split disk, they are already split in unseparable disk list
         node.getDisks().removeAll(removed);

         for (DiskSpec disk : node.getDisks()) {
            if (DiskType.DATA_DISK == disk.getDiskType()) {
               if (disk.isSeparable()) {
                  separable.add(disk);
               } else {
                  unseparable.add(disk);
               }
            } else {
               systemDisks.add(disk);
            }
         }

         // place system disks first
         disks = placeUnSeparableDisks(systemDisks, imagestores);
         logger.info("Placing system disks to imagestores: " + new Gson().toJson(imagestores));
         if (disks == null) {
            logger.info("Can not place " + getDiskSize(systemDisks)
                  + " GB system disk on datastore with " + getDsFree(imagestores) + " GB free space.");
            return false;
         }

         // place un-separable disks
         List<DiskSpec> subDisks = null;
         if (unseparable != null && unseparable.size() != 0) {
            subDisks = placeUnSeparableDisks(unseparable, diskstores);
            if (subDisks == null) {
               logger.info("Can not place " + getDiskSize(unseparable)
                     + " GB unseparable disk on datastore with " + getDsFree(diskstores) + " GB free space.");
               return false;
            } else {
               disks.addAll(subDisks);
            }
         }

         // place separable disks
         if (separable != null && separable.size() != 0) {
            subDisks = placeSeparableDisks(separable, diskstores);
            if (subDisks == null) {
               logger.info("Can not place " + getDiskSize(separable)
                     + " GB separable disk on datastore with " + getDsFree(diskstores) + " GB free space.");
               return false;
            } else {
               disks.addAll(subDisks);
            }
         }

         result.put(node, disks);
      }

      // till here, we have successfully placed all base nodes on this host
      for (BaseNode node : vNode.getBaseNodes()) {
         AuAssert.check(result.get(node) != null);
         node.setDisks(result.get(node));
      }

      return true;
   }

   /*
    * order candidate racks in ascending order, sorted by their usages
    */
   private List<String> getRacksInOrder(String groupName,
         List<String> candidateRacks) {
      List<String> result = new LinkedList<String>();

      List<Map.Entry<String, Integer>> sortedList =
            new LinkedList<Map.Entry<String, Integer>>(rackUsageByGroup.get(
                  groupName).entrySet());

      Collections.sort(sortedList,
            new Comparator<Map.Entry<String, Integer>>() {
               public int compare(Map.Entry<String, Integer> e1,
                     Map.Entry<String, Integer> e2) {
                  return e1.getValue().compareTo(e2.getValue());
               }
            });

      for (Map.Entry<String, Integer> e : sortedList) {
         if (candidateRacks != null && candidateRacks.size() > 0) {
            if (candidateRacks.contains(e.getKey()))
               result.add(e.getKey());
         } else if (e != null) {
            result.add(e.getKey());
         }
      }

      return result;
   }

   private List<AbstractHost> rackFilter(List<AbstractHost> hosts,
         String rackName) {
      AuAssert.check(this.hostToRackMap != null
            && this.hostToRackMap.size() != 0);

      List<AbstractHost> result = new ArrayList<AbstractHost>();
      for (AbstractHost host : hosts) {
         if (this.hostToRackMap.containsKey(host.getName())
               && this.hostToRackMap.get(host.getName()).equals(rackName)) {
            result.add(host);
         }
      }

      return result;
   }

   private AbstractHost assignHost(VirtualNode vNode,
         List<AbstractHost> candidates, boolean isAssociatedCandidates) {
      if (candidates == null || candidates.size() == 0)
         return null;

      List<String> candidateRacks = new LinkedList<String>();

      // process rack policy
      boolean rrRackPolicy = false;
      GroupRacks rackPolicy = vNode.getParent().getGroupRacks();
      if (rackPolicy != null
            && GroupRacksType.ROUNDROBIN.equals(rackPolicy.getType())) {
         if (rackPolicy.getRacks() != null && rackPolicy.getRacks().length > 0) {
            candidateRacks = Arrays.asList(rackPolicy.getRacks());
         }
         /*
          * candidate racks are the joint set of the ones that are specified in group's
          * rack policy and the ones defined in hostToRack topology file
          */
         candidateRacks =
               getRacksInOrder(vNode.getParent().getPrimaryGroup().getName(),
                     candidateRacks);

         if (candidateRacks.size() == 0)
            throw PlacementException.INVALID_RACK_INFO(this.cluster.getName(),
                  vNode.getParent().getPrimaryGroup().getName());
         rrRackPolicy = true;
         logger.info("vNode " + vNode.getBaseNodeNames()
               + " has RoundRobin Rack policy");
         logger.info("Candidate racks are " + candidateRacks);
      }

      AbstractHost candidate = null;
      boolean found = false;
      int rackIndex = 0;
      while (candidates.size() > 0) {
         List<AbstractHost> subset = candidates;
         if (rrRackPolicy) {
            while (rackIndex < candidateRacks.size()) {
               subset = rackFilter(candidates, candidateRacks.get(rackIndex));
               if (subset.size() > 0) {
                  break;
               }
               rackIndex++;
            }
            if (rackIndex == candidateRacks.size()) {
               logger.warn("tried with all candidate racks, there are no host are available");
               if (isAssociatedCandidates)
                  return null;
               else
                  throw PlacementException.OUT_OF_RACK(candidateRacks, vNode.getBaseNodeNames());
            }
            logger.info("try hosts on Rack " + candidateRacks.get(rackIndex));
         }

         // least used hosts, RR policy
         candidate = getLeastUsed(subset);

         logger.info("found a candidate host " + candidate
               + ", try to place disk onto it");

         // generate the disk placement plan for a candidate host
         if (placeDisk(vNode, candidate)) {
            // assign host
            logger.info("candidate host " + candidate + " is selected");
            assignHost(vNode, candidate);
            found = true;
            break;
         }

         logger.info("drop candidate host " + candidate.getName()
               + " as it failed to come out a  disk placement plan");
         candidates.remove(candidate);
      }

      if (!found)
         return null;

      return candidate;
   }

   @Override
   public AbstractHost selectHost(VirtualNode vNode,
         List<AbstractHost> candidates) {
      AuAssert.check(init && candidates != null);

      if (candidates.size() == 0)
         return null;

      // filter out candidates that violate instance_per_host constraint
      if (vNode.hasInstancePerHostPolicy()) {
         candidates = instancePerHostFilter(vNode, candidates);
         if (candidates.size() == 0) {
            logger.info("all candidates failed to pass instance_per_host filer");
            throw PlacementException.INSTANCE_PER_HOST_VIOLATION(vNode.getBaseNodeNames());
         }
         logger.info("candidates " + candidates
               + " passed instance_per_host filer");
      }

      // group association filter
      List<AbstractHost> associatedCandidates;
      if (vNode.getReferToGroup() != null) {
         associatedCandidates = groupAssociationFilter(vNode, candidates);
         logger.info("candidates " + associatedCandidates
               + " passed strict group association filter");

         AbstractHost candidate = assignHost(vNode, associatedCandidates, true);
         if (candidate != null) {
            logger.info("found candiate host " + candidate
                  + " satisfying the group association policy");
            return candidate;
         }

         // cannot find a host that satisfy strict group association policy
         if (candidate == null && vNode.getStrictAssociated()) {
            logger.info("cannot find a candidate host to satisfy the strict association policy");
            return null;
         }

         // continue to play weak association policy, select hosts that
         // do not have referred group placed
         candidates.removeAll(associatedCandidates);
         if (candidates.size() == 0)
            return null;
         logger.info("candidates " + candidates
               + " passed weak association constraint");
      }

      return assignHost(vNode, candidates, false);
   }

   private String getLeastUsed(String vcClusterName, List<String> rps) {
      int min = Integer.MAX_VALUE;
      String candidate = null;
      for (String rpName : rps) {
         Pair<String, String> rpPair =
               new Pair<String, String>(vcClusterName, rpName);
         if (!rpUsage.containsKey(rpPair)) {
            // this vc_cluster-rp pair is never been used, return it
            rpUsage.put(rpPair, 0);
            return rpName;
         } else {
            if (rpUsage.get(rpPair) < min) {
               min = rpUsage.get(rpPair);
               candidate = rpName;
            }
         }
      }
      AuAssert.check(candidate != null);
      return candidate;
   }

   @Override
   public Pair<String, String> selectVcRp(BaseNode node, AbstractHost host) {
      AuAssert.check(init);

      String nodeGroupName = node.getNodeGroup().getName();
      AbstractCluster abstractCluster = host.getParent();

      if (rpMapByGroup.containsKey(nodeGroupName)
            && rpMapByGroup.get(nodeGroupName).containsKey(
                  abstractCluster.getName())) {
         /*
          * some nodes from this node group has picked a rp under this vc cluster, let's
          * use the same one
          */
         return new Pair<String, String>(abstractCluster.getName(),
               rpMapByGroup.get(nodeGroupName).get(abstractCluster.getName()));
      }

      List<VcCluster> availableVcClusters =
            node.getNodeGroup().getVcClusters(this.cluster);

      VcCluster targetVcCluster = null;

      for (VcCluster vcCluster : availableVcClusters) {
         if (vcCluster.getName().equals(abstractCluster.getName())) {
            targetVcCluster = vcCluster;
            break;
         }
      }

      AuAssert.check(targetVcCluster != null);

      String targetRp =
            getLeastUsed(targetVcCluster.getName(), targetVcCluster.getVcRps());

      Pair<String, String> rpPair =
            new Pair<String, String>(targetVcCluster.getName(), targetRp);
      // update rp usage map
      rpUsage.put(rpPair, rpUsage.get(rpPair) + 1);

      // update cluster->rp by group map
      if (!rpMapByGroup.containsKey(nodeGroupName)) {
         rpMapByGroup.put(nodeGroupName, new HashMap<String, String>());
      }
      Map<String, String> clusterRpMap = rpMapByGroup.get(nodeGroupName);
      clusterRpMap.put(targetVcCluster.getName(), targetRp);

      return rpPair;
   }

   @Override
   public List<BaseNode> getBadNodes(ClusterCreate cluster,
         List<BaseNode> existedNodes) {
      if (existedNodes == null || existedNodes.size() == 0) {
         return null;
      }

      Set<BaseNode> badNodes = new HashSet<BaseNode>();

      Map<String, Map<String, List<BaseNode>>> map =
            new HashMap<String, Map<String, List<BaseNode>>>();

      for (BaseNode node : existedNodes) {
         if (PlacementUtil.getIndex(node) >= node.getNodeGroup()
               .getInstanceNum()) {
            // remove nodes that have index great than instance number
            badNodes.add(node);
            continue;
         }

         String groupName = node.getGroupName();
         if (!map.containsKey(groupName)) {
            map.put(groupName, new HashMap<String, List<BaseNode>>());
         }

         Map<String, List<BaseNode>> hostMap = map.get(groupName);

         if (!hostMap.containsKey(node.getTargetHost())) {
            hostMap.put(node.getTargetHost(), new ArrayList<BaseNode>());
         }

         List<BaseNode> nodes = hostMap.get(node.getTargetHost());
         nodes.add(node);
         hostMap.put(node.getTargetHost(), nodes);
      }


      for (NodeGroupCreate nodeGroup : cluster.getNodeGroups()) {
         if (map.containsKey(nodeGroup.getName())) {
            // check instance_per_host policy
            if (nodeGroup.instancePerHost() != null) {
               for (String host : map.get(nodeGroup.getName()).keySet()) {
                  int numOnHost = map.get(nodeGroup.getName()).get(host).size();
                  if (nodeGroup.instancePerHost() != numOnHost) {
                     // violate instance_per_host policy, mark all nodes on this host as bad
                     badNodes.addAll(map.get(nodeGroup.getName()).get(host));
                  }
               }
            }

            // check group association policy
            if (nodeGroup.getReferredGroup() != null
                  && nodeGroup.isStrictReferred()) {
               for (String host : map.get(nodeGroup.getName()).keySet()) {
                  String referredGroup = nodeGroup.getReferredGroup();
                  if (!map.containsKey(referredGroup)
                        || !map.get(referredGroup).containsKey(host)) {
                     // the target host does not have any nodes from the referred group
                     badNodes.addAll(map.get(nodeGroup.getName()).get(host));
                  }
               }
            }

         }
      }

      return new ArrayList<BaseNode>(badNodes);
   }

   @Override
   public List<String> getTargetRacks(String groupName) {
      if (!this.rackUsageByGroup.containsKey(groupName)
            || this.rackUsageByGroup.get(groupName).isEmpty()) {
         return null;
      }

      return new ArrayList<String>(this.rackUsageByGroup.get(groupName)
            .keySet());
   }
}
