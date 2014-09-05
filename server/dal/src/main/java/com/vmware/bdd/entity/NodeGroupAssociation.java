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
package com.vmware.bdd.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.vmware.bdd.apitypes.PlacementPolicy.GroupAssociation.GroupAssociationType;


@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "node_group_association_seq", allocationSize = 1)
@Table(name = "node_group_association")
public class NodeGroupAssociation extends EntityBase {

   // put null or empty string here if there are no association
   @Column(name = "referenced_group", nullable = false)
   private String referencedGroup;

   // STRICT or WEAK
   @Enumerated(EnumType.STRING)
   @Column(name = "association_type", nullable = false)
   private GroupAssociationType associationType;

   @ManyToOne
   @JoinColumn(name = "node_group_id")
   private NodeGroupEntity nodeGroup;

   public NodeGroupAssociation() {
   }

   public String getReferencedGroup() {
      return referencedGroup;
   }

   public void setReferencedGroup(String referencedGroup) {
      this.referencedGroup = referencedGroup;
   }

   public GroupAssociationType getAssociationType() {
      return associationType;
   }

   public void setAssociationType(GroupAssociationType associationType) {
      this.associationType = associationType;
   }

   public NodeGroupEntity getNodeGroup() {
      return nodeGroup;
   }

   public void setNodeGroup(NodeGroupEntity nodeGroup) {
      this.nodeGroup = nodeGroup;
   }

   @Override
   public String toString() {
      return "GroupAssosiation [referencedGroup=" + referencedGroup + ", type="
            + associationType + "]";
   }
}
