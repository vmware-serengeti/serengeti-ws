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
package com.vmware.bdd.apitypes;

import java.util.EnumSet;

public enum ClusterStatus {
   RUNNING,
   PROVISIONING,
   PROVISION_ERROR,
   UPGRADING,
   UPDATING,
   DELETING,
   STOPPED,
   EXPANDING,
   ERROR,
   STOPPING,
   STARTING,
   CONFIGURING,
   CONFIGURE_ERROR,
   NA,
   VHM_RUNNING,
   VMRECONFIGURING,
   MAINTENANCE,
   UPGRADE_ERROR,
   SERVICE_STOPPED,
   SERVICE_WARNING;

   private static EnumSet<ClusterStatus> stableStatus = EnumSet.of(
         RUNNING, STOPPED,
         CONFIGURE_ERROR, ERROR,
         PROVISION_ERROR, UPGRADE_ERROR,
         SERVICE_STOPPED, SERVICE_WARNING);

   private static EnumSet<ClusterStatus> activeServiceStatus = EnumSet.of(
         RUNNING, SERVICE_WARNING);

   private static EnumSet<ClusterStatus> syncServiceStatus = EnumSet.of(
         RUNNING, SERVICE_WARNING,
         SERVICE_STOPPED, VHM_RUNNING);

   public boolean isStableStatus() {
      return stableStatus.contains(this);
   }

   public boolean isActiveServiceStatus() {
      return activeServiceStatus.contains(this);
   }

   public boolean isSyncServiceStatus() {
      return syncServiceStatus.contains(this);
   }
}
