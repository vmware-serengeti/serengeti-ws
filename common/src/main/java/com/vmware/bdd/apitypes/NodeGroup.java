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
package com.vmware.bdd.apitypes;

import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Node Group Commons
 */
public class NodeGroup {

   public enum InstanceType {
      EXTRA_LARGE, LARGE, MEDIUM, SMALL;

      public int getCpuNum() {
         switch (this) {
         case EXTRA_LARGE:
            return 8;
         case LARGE:
            return 4;
         case MEDIUM:
            return 2;
         case SMALL:
            return 1;
         default:
            return 0;
         }
      }

      public int getMemoryMB() {
         switch (this) {
         case EXTRA_LARGE:
            return 30000;
         case LARGE:
            return 15000;
         case MEDIUM:
            return 7500;
         case SMALL:
            return 3748;
         default:
            return 0;
         }
      }
   }

   public static class PlacementPolicy {

      public static class GroupRacks {
         public enum GroupRacksType {
            ROUNDROBIN, SAMERACK
         }
         @Expose
         @SerializedName("type")
         private GroupRacksType type;
         @Expose
         @SerializedName("racks")
         private String[] racks;

         public GroupRacksType getType() {
            return type;
         }

         public void setType(GroupRacksType type) {
            this.type = type;
         }

         public String[] getRacks() {
            return racks;
         }

         public void setRacks(String[] racks) {
            this.racks = racks;
         }

         @Override
         public String toString() {
            return "GroupRacks [racks=" + Arrays.toString(racks) + "; type=" + type + "]";
         }
      }

      public static class GroupAssociation {
         public enum GroupAssociationType {
            WEAK, STRICT
         }

         @Expose
         @SerializedName("reference")
         private String reference;
         @Expose
         @SerializedName("type")
         private GroupAssociationType type;

         @RestRequired
         public String getReference() {
            return reference;
         }

         public void setReference(String reference) {
            this.reference = reference;
         }

         public GroupAssociationType getType() {
            return type;
         }

         public void setType(GroupAssociationType type) {
            this.type = type;
         }

         @Override
         public String toString() {
            return "GroupAssosiation [reference=" + reference + ", type=" + type + "]";
         }
      }

      @Expose
      @SerializedName("group_racks")
      private GroupRacks groupRacks;
      @Expose
      @SerializedName("instance_per_host")
      private Integer instancePerHost;
      @Expose
      @SerializedName("group_associations")
      private List<GroupAssociation> groupAssociations;

      public GroupRacks getGroupRacks() {
         return groupRacks;
      }

      public void setGroupRacks(GroupRacks groupRacks) {
         this.groupRacks = groupRacks;
      }

      public Integer getInstancePerHost() {
         return instancePerHost;
      }

      public void setInstancePerHost(Integer instancePerHost) {
         this.instancePerHost = instancePerHost;
      }

      public List<GroupAssociation> getGroupAssociations() {
         return groupAssociations;
      }

      public void setGroupAssociations(List<GroupAssociation> groupAssociations) {
         this.groupAssociations = groupAssociations;
      }

      @Override
      public String toString() {
         return "PlacePolicy [instancePerHost=" + instancePerHost
               + ", groupAssociations=" + groupAssociations + "]";
      }
   }
}
