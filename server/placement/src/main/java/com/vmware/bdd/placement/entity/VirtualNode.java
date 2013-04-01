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

package com.vmware.bdd.placement.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.utils.AuAssert;

/**
 * a virtual node combines VMs that should be on the same host
 * 
 * @author tli
 * 
 */
public class VirtualNode {

   private VirtualGroup parent;

   private List<BaseNode> nodes;

   private String referToGroup;

   private Boolean strictAssociated;

   public VirtualNode() {
      nodes = new ArrayList<BaseNode>();
   }

   public VirtualNode(VirtualGroup parent) {
      this.parent = parent;
      nodes = new ArrayList<BaseNode>();
   }

   public VirtualGroup getParent() {
      return parent;
   }

   public void setParent(VirtualGroup parent) {
      this.parent = parent;
   }

   public void addNode(BaseNode node) {
      nodes.add(node);
   }

   public List<BaseNode> getBaseNodes() {
      return nodes;
   }

   public void setBaseNodes(List<BaseNode> nodes) {
      this.nodes = nodes;
   }

   // MHz
   public int getCpu() {
      int cpu = 0;
      for (BaseNode node : nodes) {
         cpu += node.getCpu();
      }

      return cpu;
   }

   // MB
   public int getMem() {
      int mem = 0;
      for (BaseNode node : nodes) {
         mem += node.getMem();
      }

      return mem;
   }

   // GB
   public int getStorage() {
      int total = 0;

      for (BaseNode node : this.nodes) {
         total += node.getStorageSize();
      }

      return total;
   }

   public String getReferToGroup() {
      if (referToGroup != null)
         return referToGroup;
      return this.parent.getReferToGroup();
   }

   public void setReferToGroup(String referToGroup) {
      this.referToGroup = referToGroup;
   }

   public Boolean getStrictAssociated() {
      if (strictAssociated != null)
         return strictAssociated;
      return this.parent.isStrictAssociated();
   }

   public void setStrictAssociated(Boolean strictAssociated) {
      this.strictAssociated = strictAssociated;
   }

   /**
    * decides whether a VC Host has enough storage for this virtual node. This
    * implementation checks whether the free space on the input host is great
    * than the total required space (by all base nodes in this virtual node).
    * The answer should be further verified with a disk allocation plan.
    * 
    * @param host
    *           VC Host
    * @return true is there are enough storage on this host, false otherwise
    */
   public boolean hasEnoughStorage(AbstractHost host) {
      // total required storage space in this vNode
      int required = 0;

      Set<String> namePatterns = new HashSet<String>();

      for (BaseNode node : this.nodes) {
         AuAssert.check(node.getDisks() != null);
         for (DiskSpec disk : node.getDisks()) {
            required += disk.getSize();
         }
         namePatterns.addAll(Arrays.asList(node.getDatastoreNamePattern()));
      }

      int sum =
            host.getTotalSpaceInGB(namePatterns.toArray(new String[namePatterns
                  .size()]));

      return (sum >= required);
   }

   /**
    * decides whether a VC Host has enough cpu for this virtual node
    * 
    * @param host
    *           VC Host
    * @return true is there are enough storage on this host, false otherwise
    */
   public boolean hasEnoughCpu(AbstractHost host) {
      return true;
   }

   /**
    * decides whether a VC Host has enough memory for this virtual node
    * 
    * @param host
    *           VC Host
    * @return true is there are enough storage on this host, false otherwise
    */
   public boolean hasEnoughMemory(AbstractHost host) {
      return true;
   }

   public boolean hasInstancePerHostPolicy() {
      AuAssert.check(nodes.size() >= 1);
      return this.parent.hasInstancePerHostPolicy();
   }

   public List<String> getBaseNodeNames() {
      ArrayList<String> nodeNames = new ArrayList<String>();

      for (BaseNode node : this.nodes) {
         nodeNames.add(node.getVmName());
      }

      return nodeNames;
   }

   @Override
   public String toString() {
      return "VirtualNode nodes=\n" + nodes;
   }
}
