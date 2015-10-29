/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.ResourcePoolRead;

/**
 * Work as a message queue
 * 
 */

@NamedQueries({
   @NamedQuery(
      name = "rp.findUsedRpsByNodeGroup",
      query = "select distinct rp from VcResourcePoolEntity rp"
            + " where rp.id in (select node.vcRp.id from NodeEntity node where node.nodeGroup.id=:nodeGroupId)"
   )
})

@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "vc_resource_pool_seq", allocationSize = 1)
@Table(name = "vc_resource_pool")
public class VcResourcePoolEntity extends EntityBase {

   @Column(name = "name", unique = true, nullable = false)
   private String name;

   @Column(name = "vc_cluster", nullable = false)
   private String vcCluster;

   @Column(name = "vc_rp", nullable = false)
   private String vcResourcePool;

   @OneToMany(mappedBy = "vcRp", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   private Set<NodeEntity> hadoopNodes;

   public VcResourcePoolEntity() {

   }

   public VcResourcePoolEntity(String name) {
      super();
      this.name = name;
   }

   public VcResourcePoolEntity(String name, String vcCluster,
         String vcResourcePool) {
      super();
      this.name = name;
      this.vcCluster = vcCluster;
      this.vcResourcePool = vcResourcePool;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getVcCluster() {
      return vcCluster;
   }

   public void setVcCluster(String vcCluster) {
      this.vcCluster = vcCluster;
   }

   public String getVcResourcePool() {
      return vcResourcePool;
   }

   public void setVcResourcePool(String vcResourcePool) {
      this.vcResourcePool = vcResourcePool;
   }

   public Set<NodeEntity> getHadoopNodes() {
      return hadoopNodes;
   }

   public void setHadoopNodes(Set<NodeEntity> hadoopNodes) {
      this.hadoopNodes = hadoopNodes;
   }

   public ResourcePoolRead toRest() {
      ResourcePoolRead read = new ResourcePoolRead();
      read.setRpName(this.getName());
      read.setRpVsphereName(this.getVcResourcePool());
      read.setVcCluster(this.getVcCluster());
      Set<NodeEntity> nodes = this.getHadoopNodes();
      if (nodes == null || nodes.isEmpty()) {
         return read;
      }

      List<NodeRead> nodeReads = new ArrayList<NodeRead>();
      for (NodeEntity node : nodes) {
         NodeRead nodeRead = node.toNodeRead(false);
         nodeReads.add(nodeRead);
      }
      Collections.sort(nodeReads, new Comparator<NodeRead>() {
         @Override
         public int compare(NodeRead arg0, NodeRead arg1) {
            return arg0.getName().compareTo(arg1.getName());
         }
      });
      read.setNodes(nodeReads.toArray(new NodeRead[nodeReads.size()]));
      return read;
   }
}
