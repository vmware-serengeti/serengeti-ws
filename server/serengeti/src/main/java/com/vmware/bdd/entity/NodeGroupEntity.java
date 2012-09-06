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
package com.vmware.bdd.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.hibernate.criterion.Restrictions;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NodeGroup.InstanceType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.exception.BddException;
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

   @Enumerated(EnumType.STRING)
   @Column(name = "storage_type")
   private DatastoreType storageType;

   // GB
   @Column(name = "storage_size")
   private int storageSize;

   @ManyToOne
   @JoinColumn(name = "cluster_id")
   private ClusterEntity cluster;

   @OneToMany(mappedBy = "nodeGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   private Set<HadoopNodeEntity> hadoopNodes;

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

   public NodeGroupEntity() {

   }

   public NodeGroupEntity(String name, int defineInstanceNum, int cpuNum,
         int memorySize, DatastoreType storageType, int storageSize) {
      super();
      this.name = name;
      this.defineInstanceNum = defineInstanceNum;
      this.cpuNum = cpuNum;
      this.memorySize = memorySize;
      this.storageType = storageType;
      this.storageSize = storageSize;
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

   public Set<HadoopNodeEntity> getHadoopNodes() {
      return hadoopNodes;
   }

   public void setHadoopNodes(Set<HadoopNodeEntity> hadoopNodes) {
      this.hadoopNodes = hadoopNodes;
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

   @SuppressWarnings("unchecked")
   public List<String> getRoleNameList() {
      return (new Gson()).fromJson(this.roles,
            (new ArrayList<String>()).getClass());
   }

   public int getRealInstanceNum() {
      return hadoopNodes.size();
   }

   public Set<VcResourcePoolEntity> getUsedVcResourcePools() {
      HashSet<VcResourcePoolEntity> rps = new HashSet<VcResourcePoolEntity>();
      for (HadoopNodeEntity node : hadoopNodes) {
         rps.add(node.getVcRp());
      }

      return rps;
   }

   public Set<String> getUsedVcDatastores() {
      HashSet<String> datastores = new HashSet<String>();
      for (HadoopNodeEntity node : hadoopNodes) {
         datastores.addAll(node.getDatastoreNameList());
      }

      return datastores;
   }

   public NodeGroupRead toNodeGroupRead() {
      NodeGroupRead nodeGroupRead = new NodeGroupRead();
      nodeGroupRead.setName(this.name);
      nodeGroupRead.setCpuNum(this.cpuNum);
      nodeGroupRead.setMemCapacityMB(this.memorySize);
      nodeGroupRead.setInstanceNum(this.getRealInstanceNum());
      nodeGroupRead.setRoles(this.getRoleNameList());

      StorageRead storage = new StorageRead();
      storage.setType(this.storageType.toString());
      storage.setSizeGB(this.storageSize);
      storage.setDsNames(getVcDatastoreNameList());
      nodeGroupRead.setStorage(storage);

      List<NodeRead> nodeList = new ArrayList<NodeRead>();
      for (HadoopNodeEntity node : this.hadoopNodes) {
         nodeList.add(node.toNodeRead());
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

   public static NodeGroupEntity findNodeGroupEntityById(Long groupId) {
      return DAL.findById(NodeGroupEntity.class, groupId);
   }

   public static NodeGroupEntity findNodeGroupEntityByName(
         ClusterEntity cluster, String groupName) {
      Map<String, Object> conditions = new HashMap<String, Object>();
      conditions.put("cluster", cluster);
      conditions.put("name", groupName);
      return DAL.findUniqueByCriteria(NodeGroupEntity.class,
            Restrictions.allEq(conditions));
   }

   public static List<NodeGroupEntity> findByClusters(
         Collection<ClusterEntity> clusters) {
      if (!clusters.isEmpty()) {
         return DAL.findByCriteria(NodeGroupEntity.class,
               Restrictions.in("cluster", clusters));
      } else {
         return new ArrayList<NodeGroupEntity>(0);
      }
   }

   public void validateHostNumber(int instanceNum) {
      Set<NodeGroupAssociation> associations = getGroupAssociations();
      if (associations != null && !associations.isEmpty()) {
         AuAssert.check(associations.size() == 1,
               "only support 1 group association now");
         NodeGroupAssociation association = associations.iterator().next();
         if (association.getAssociationType() == GroupAssociationType.STRICT) {
            NodeGroupEntity refGroup = NodeGroupEntity.findNodeGroupEntityByName(
                  getCluster(), association.getReferencedGroup());
            AuAssert.check(refGroup != null, "shold not happens");

            int hostNum = 1;
            int refHostNum = refGroup.getDefineInstanceNum();
            if (getInstancePerHost() != null) {
               hostNum = instanceNum / getInstancePerHost();
            }
            if (refGroup.getInstancePerHost() != null) {
               refHostNum = refGroup.getDefineInstanceNum() / refGroup.getInstancePerHost();
            }

            if (hostNum > refHostNum) {
               throw BddException.INVALID_PARAMETER(
                     "instance number",
                     new StringBuilder(100)
                     .append(instanceNum)
                     .append(": required host number is larger " +
                           "than the referenced node group").toString());
            }
         }
      }
   }
}
