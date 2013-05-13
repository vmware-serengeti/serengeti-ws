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
package com.vmware.bdd.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NodeGroup.InstanceType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupRacks;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.AuAssert;

/**
 * Node Group Entity: node group info
 * 
 */
@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "node_group_seq", allocationSize = 1)
@Table(name = "node_group")
public class NodeGroupEntity extends EntityBase {

   @Column(name = "name", nullable = false)
   private String name;

   @Column(name = "defined_instance_num", nullable = false)
   private int defineInstanceNum;

   @Column(name = "cpu")
   private int cpuNum;

   // MB
   @Column(name = "memory")
   private int memorySize;

   @Column(name = "swap_ratio")
   private Float swapRatio = 1F;

   @Enumerated(EnumType.STRING)
   @Column(name = "storage_type")
   private DatastoreType storageType;

   // GB
   @Column(name = "storage_size")
   private int storageSize;

   @Enumerated(EnumType.STRING)
   @Column(name = "ioshare_type")
   private Priority ioShares;
   
   @Column(name = "vhm_target_num")
   private Integer vhmTargetNum;

   @ManyToOne
   @JoinColumn(name = "cluster_id")
   private ClusterEntity cluster;

   @OneToMany(mappedBy = "nodeGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   private Set<NodeEntity> nodes;

   /*
    * cluster definition field. VCResourcePool inside this array may not be used
    * by this node group, so we should avoid setting up the ManyToMany mapping.
    * JSON encoded VCResourcePoolEntity name array
    */
   @Column(name = "vc_rp_names")
   @Type(type = "text")
   private String vcRpNames;

   /*
    * cluster definition field. VCDataStores inside this array may not be used
    * by this node group, so we should avoid setting up the ManyToMany mapping.
    * JSON encoded VCDataStoreEntity name array
    */
   @Column(name = "vc_datastore_names")
   @Type(type = "text")
   private String vcDatastoreNames;

   @Column(name = "roles")
   private String roles; // JSON string

   @Column(name = "node_type")
   private InstanceType nodeType;

   @Column(name = "ha_flag")
   private String haFlag;

   @Column(name = "configuration")
   @Type(type = "text")
   private String hadoopConfig;

   @Column(name = "instance_per_host")
   private Integer instancePerHost;

   @OneToMany(mappedBy = "nodeGroup", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
   private Set<NodeGroupAssociation> groupAssociations;

   @Column(name = "group_racks")
   @Type(type = "text")
   private String groupRacks;

   @Column(name = "vm_folder_path")
   private String vmFolderPath;


   public NodeGroupEntity() {
      // default share level
      this.ioShares = Priority.NORMAL;
   }

   public NodeGroupEntity(String name, int defineInstanceNum, int cpuNum,
         int memorySize, Float swapRatio, DatastoreType storageType,
         int storageSize) {
      super();
      this.name = name;
      this.defineInstanceNum = defineInstanceNum;
      this.cpuNum = cpuNum;
      this.memorySize = memorySize;
      this.swapRatio = swapRatio;
      this.storageType = storageType;
      this.storageSize = storageSize;
      // default share level
      this.ioShares = Priority.NORMAL;
   }

   public String getHadoopConfig() {
      return hadoopConfig;
   }

   public void setHadoopConfig(String hadoopConfig) {
      this.hadoopConfig = hadoopConfig;
   }

   public String getHaFlag() {
      return haFlag;
   }

   public void setHaFlag(String haFlag) {
      this.haFlag = haFlag;
   }

   public InstanceType getNodeType() {
      return nodeType;
   }

   public void setNodeType(InstanceType nodeType) {
      this.nodeType = nodeType;
   }

   public NodeGroupEntity(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getDefineInstanceNum() {
      return defineInstanceNum;
   }

   public void setDefineInstanceNum(int defineInstanceNum) {
      this.defineInstanceNum = defineInstanceNum;
   }

   public int getCpuNum() {
      return cpuNum;
   }

   public void setCpuNum(int cpuNum) {
      this.cpuNum = cpuNum;
   }

   public int getMemorySize() {
      return memorySize;
   }

   public void setMemorySize(int memorySize) {
      this.memorySize = memorySize;
   }

   public Float getSwapRatio() {
      return swapRatio;
   }

   public void setSwapRatio(Float swapRatio) {
      this.swapRatio = swapRatio;
   }

   public DatastoreType getStorageType() {
      return storageType;
   }

   public void setStorageType(DatastoreType storageType) {
      this.storageType = storageType;
   }

   public int getStorageSize() {
      return storageSize;
   }

   public void setStorageSize(int storageSize) {
      this.storageSize = storageSize;
   }

   public Priority getIoShares() {
      return ioShares;
   }

   public void setIoShares(Priority ioShares) {
      this.ioShares = ioShares;
   }
   
   public Integer getVhmTargetNum() {
      return vhmTargetNum;
   }
   
   public void setVhmTargetNum(Integer vhmTargetNum) {
      this.vhmTargetNum = vhmTargetNum;
   }

   public Set<NodeEntity> getNodes() {
      return nodes;
   }

   public void setNodes(Set<NodeEntity> nodes) {
      this.nodes = nodes;
   }

   public ClusterEntity getCluster() {
      return cluster;
   }

   public void setCluster(ClusterEntity cluster) {
      this.cluster = cluster;
   }

   public String getVcRpNames() {
      return this.vcRpNames;
   }

   @SuppressWarnings("unchecked")
   public List<String> getVcRpNameList() {
      return (new Gson()).fromJson(vcRpNames,
            (new ArrayList<String>()).getClass());
   }

   public void setVcRpNameList(List<String> vcRpNameList) {
      this.vcRpNames = (new Gson()).toJson(vcRpNameList);
   }

   public String getVcDatastoreNames() {
      return this.vcDatastoreNames;
   }

   @SuppressWarnings("unchecked")
   public List<String> getVcDatastoreNameList() {
      return (new Gson()).fromJson(vcDatastoreNames,
            (new ArrayList<String>()).getClass());
   }

   public void setVcDatastoreNameList(List<String> vcDatastoreNameList) {
      this.vcDatastoreNames = (new Gson()).toJson(vcDatastoreNameList);
   }

   public String getRoles() {
      return roles;
   }

   public void setRoles(String roles) {
      this.roles = roles;
   }

   public Integer getInstancePerHost() {
      return instancePerHost;
   }

   public void setInstancePerHost(Integer instancePerHost) {
      this.instancePerHost = instancePerHost;
   }

   public Set<NodeGroupAssociation> getGroupAssociations() {
      return groupAssociations;
   }

   public void setGroupAssociations(Set<NodeGroupAssociation> groupAssociations) {
      this.groupAssociations = groupAssociations;
   }

   public String getGroupRacks() {
      return groupRacks;
   }

   public void setGroupRacks(String groupRacks) {
      this.groupRacks = groupRacks;
   }

   public String getVmFolderPath() {
      return vmFolderPath;
   }

   public void setVmFolderPath(String vmFolderPath) {
      this.vmFolderPath = vmFolderPath;
   }

   public void setVmFolderPath(ClusterEntity cluster) {
      this.vmFolderPath = cluster.getRootFolder() + "/" + this.name;
   }

   @SuppressWarnings("unchecked")
   public List<String> getRoleNameList() {
      return (new Gson()).fromJson(this.roles,
            (new ArrayList<String>()).getClass());
   }

   public int getRealInstanceNum() {
      return nodes.size();
   }

   public Set<VcResourcePoolEntity> getUsedVcResourcePools() {
      HashSet<VcResourcePoolEntity> rps = new HashSet<VcResourcePoolEntity>();
      for (NodeEntity node : nodes) {
         if (node.getVcRp() != null) {
            rps.add(node.getVcRp());
         }
      }

      return rps;
   }

   public Set<String> getUsedVcDatastores() {
      Set<String> datastores = new HashSet<String>();
      for (NodeEntity node : nodes) {
         List<String> vcDss = node.getDatastoreNameList();
         if (vcDss != null) {
            datastores.addAll(vcDss);
         }
      }

      return datastores;
   }

   // this method should be called inside a transaction
   public NodeGroupRead toNodeGroupRead() {
      NodeGroupRead nodeGroupRead = new NodeGroupRead();
      nodeGroupRead.setName(this.name);
      nodeGroupRead.setCpuNum(this.cpuNum);
      nodeGroupRead.setMemCapacityMB(this.memorySize);
      nodeGroupRead.setSwapRatio(this.swapRatio);
      nodeGroupRead.setInstanceNum(this.getRealInstanceNum());
      nodeGroupRead.setIoShares(this.ioShares);
      nodeGroupRead.setVhmTargetNum(this.vhmTargetNum);

      Gson gson = new Gson();
      @SuppressWarnings("unchecked")
      List<String> groupRoles = gson.fromJson(roles, List.class);
      Collections.sort(groupRoles, new Comparator<String>() {
         @Override
         public int compare(String str1, String str2) {
            if (HadoopRole.fromString(str1).shouldRunAfterHDFS()) {
               return 1;
            } else if (HadoopRole.fromString(str2).shouldRunAfterHDFS()) {
               return -1;
            } else {
               return 0;
            }
         }
      });
      nodeGroupRead.setRoles(groupRoles);

      StorageRead storage = new StorageRead();
      storage.setType(this.storageType.toString());
      storage.setSizeGB(this.storageSize);
      storage.setDsNames(getVcDatastoreNameList());
      nodeGroupRead.setStorage(storage);

      List<NodeRead> nodeList = new ArrayList<NodeRead>();
      for (NodeEntity node : this.nodes) {
         nodeList.add(node.toNodeRead(true));
      }
      nodeGroupRead.setInstances(nodeList);

      List<GroupAssociation> associations = new ArrayList<GroupAssociation>();
      for (NodeGroupAssociation relation : groupAssociations) {
         GroupAssociation association = new GroupAssociation();
         association.setReference(relation.getReferencedGroup());
         association.setType(relation.getAssociationType());
         associations.add(association);
      }

      PlacementPolicy policy = new PlacementPolicy();
      policy.setInstancePerHost(instancePerHost);
      policy.setGroupAssociations(associations);
      policy.setGroupRacks(new Gson().fromJson(groupRacks, GroupRacks.class));

      nodeGroupRead.setPlacementPolicies(policy);

      return nodeGroupRead;
   }

   @Override
   public int hashCode() {
      AuAssert.check(this.name != null,
            "id = " + this.id + " memory = " + this.getMemorySize());
      return this.name.hashCode();
   }

   @Override
   public boolean equals(Object nodeGroup) {
      AuAssert.check(this.name != null,
            "id = " + this.id + " memory = " + this.getMemorySize());
      if (!(nodeGroup instanceof NodeGroupEntity))
         return false;
      NodeGroupEntity group = (NodeGroupEntity) nodeGroup;
      if (this.cluster != null && group.getCluster() != null) {
         return this.cluster.getName().equals(group.getCluster().getName())
               && this.name.equals(group.getName());
      }
      return this.name.equals(group.getName());
   }
}
