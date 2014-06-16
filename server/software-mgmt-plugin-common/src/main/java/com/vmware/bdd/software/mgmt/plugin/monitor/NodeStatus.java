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
package com.vmware.bdd.software.mgmt.plugin.monitor;

public enum NodeStatus {
   /*
    * Note: The statusChanged field rely on status sequence defined in this type
    */
   NOT_EXIST ( "Not Exist" ),
   DISCONNECTED ( "Unavailable" ),
   POWERED_OFF ( "Powered Off" ),
   POWERED_ON ( "Powered On" ),
   VM_READY ( "VM Ready" ),
   BOOTSTRAP_FAILED ( "Bootstrap Failed" ),
   SERVICE_READY ( "Service Ready"),
   UNKNOWN ( "Unknown Status" ); // illegal status

   private String description;
   private NodeStatus(String description) {
      this.description = description;
   }
   public String toString() {
      return description;
   }
   public static NodeStatus fromString(String description) {
      for (NodeStatus status:NodeStatus.values()) {
         if (status.description.equals(description)) {
            return status;
         }
      }
      return UNKNOWN;
   }
}
