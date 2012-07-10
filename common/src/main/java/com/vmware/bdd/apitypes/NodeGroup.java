/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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

   public static class PlacePolicy {
      public static class GroupAssociation {
         public enum GroupAssociationType {
            LOOSE, STRICT
         }

         private String reference;
         private GroupAssociationType type;

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

      private Integer instancePerHost;
      private GroupAssociation groupAssociation;

      public Integer getInstancePerHost() {
         return instancePerHost;
      }

      public void setInstancePerHost(Integer instancePerHost) {
         this.instancePerHost = instancePerHost;
      }

      public GroupAssociation getGroupAssociation() {
         return groupAssociation;
      }

      public void setGroupAssociation(GroupAssociation groupAssociation) {
         this.groupAssociation = groupAssociation;
      }

      @Override
      public String toString() {
         return "PlacePolicy [instancePerHost=" + instancePerHost
               + ", groupAssociation=" + groupAssociation + "]";
      }
   }
}
