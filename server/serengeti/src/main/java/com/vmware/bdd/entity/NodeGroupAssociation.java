package com.vmware.bdd.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;

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
