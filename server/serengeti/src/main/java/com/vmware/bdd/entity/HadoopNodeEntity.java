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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.utils.AuAssert;

/**
 * Hadoop Node Entity class: describes hadoop node info
 * 
 */
@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "hadoop_node_seq", allocationSize = 1)
@Table(name = "hadoop_node")
public class HadoopNodeEntity extends EntityBase {

   @Column(name = "vm_name", unique = true, nullable = false)
   private String vmName;

   @Column(name = "host_name")
   private String hostName;

   // vm status, poweredOn/poweredOff
   @Column(name = "status")
   private String status;

   @Column(name = "action")
   private String action;

   @Column(name = "ip_address")
   private String ipAddress;

   @ManyToOne
   @JoinColumn(name = "node_group_id")
   private NodeGroupEntity nodeGroup;

   @ManyToOne
   @JoinColumn(name = "vc_rp_id")
   private VcResourcePoolEntity vcRp;

   // JSON encoded datastore name array
   @Column(name = "vc_datastores")
   @Type(type = "text")
   private String datastores;

   public HadoopNodeEntity() {

   }

   public HadoopNodeEntity(String vmName, String hostName, String status,
         String ipAddress) {
      super();
      this.vmName = vmName;
      this.hostName = hostName;
      this.status = status;
      this.ipAddress = ipAddress;
   }

   public String getVmName() {
      return vmName;
   }

   public void setVmName(String vmName) {
      this.vmName = vmName;
   }

   public String getHostName() {
      return hostName;
   }

   public void setHostName(String hostName) {
      this.hostName = hostName;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getAction() {
      return action;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public NodeGroupEntity getNodeGroup() {
      return nodeGroup;
   }

   public void setNodeGroup(NodeGroupEntity nodeGroup) {
      this.nodeGroup = nodeGroup;
   }

   public VcResourcePoolEntity getVcRp() {
      return vcRp;
   }

   public void setVcRp(VcResourcePoolEntity vcRp) {
      this.vcRp = vcRp;
   }

   @SuppressWarnings("unchecked")
   public List<String> getDatastoreNameList() {
      return (new Gson()).fromJson(this.datastores,
            (new ArrayList<String>()).getClass());
   }

   public void setDatastoreNameList(List<String> datastoreNames) {
      this.datastores = (new Gson()).toJson(datastoreNames);
   }

   public void copy(HadoopNodeEntity newNode) {
      this.hostName = newNode.getHostName();
      this.ipAddress = newNode.getIpAddress();
      this.status = newNode.getStatus();
      this.action = newNode.getAction();
      if(newNode.getVcRp() != null) {
         this.vcRp = newNode.getVcRp();
      }
      if(newNode.getDatastoreNameList() != null) {
         this.datastores = (new Gson()).toJson(newNode.getDatastoreNameList());
      }
   }

   public NodeRead toNodeRead() {
      NodeRead node = new NodeRead();
      node.setHostName(this.hostName);
      node.setIp(this.ipAddress);
      node.setName(this.vmName);
      node.setStatus(this.status);
      node.setAction(this.action);
      List<String> roleNames = nodeGroup.getRoleNameList();
      node.setRoles(roleNames);
      return node;
   }

   @Override
   public int hashCode() {
      if(this.vmName != null) {
         return this.vmName.hashCode();
      }
      if(this.id != null) {
         return this.id.intValue();
      }
      return 0;
   }

   @Override
   public boolean equals(Object node) {
      if (!(node instanceof HadoopNodeEntity))
         return false;
      if(this.vmName != null && ((HadoopNodeEntity) node).getVmName() != null) {
         return ((HadoopNodeEntity) node).getVmName().equals(this.vmName);
      }
      return false;
   }

   public static List<HadoopNodeEntity> findByNodeGroups(Collection<NodeGroupEntity> groups) {
      if (!groups.isEmpty()) {
         return DAL.findByCriteria(HadoopNodeEntity.class,
               Restrictions.in("nodeGroup", groups));
      } else {
         return new ArrayList<HadoopNodeEntity>(0);
      }
   }

   public static HadoopNodeEntity findByName(NodeGroupEntity group, String name) {
      AuAssert.check(group != null);
      return DAL.findUniqueByCriteria(
            HadoopNodeEntity.class,
            Restrictions.and(Restrictions.eq("nodeGroup", group),
                  Restrictions.eq("vmName", name)));
   }
}
