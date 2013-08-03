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
package com.vmware.aurora.stats;

/**
 * List of STATS types.
 *
 * Each type corresponds to an event, operation or request
 * in the system.
 */
public enum StatsType {
   ROOT,

   VC_LOAD_MO,
   VC_UPDATE_CONFIG,
   VC_UPDATE_RUNTIME,
   VC_REFRESH,
   VC_GET_HIT,
   VC_GET_MISS,
   VC_TASK_EXEC,
   VC_TASK_WAIT,
   VC_EVENT_EXT,
   VC_EVENT_INT,

   CMSWORKER_REQ,
   CMSWORKER_PERIOD,

   VCOBJ_ASYNC_TASK,

   VC_LOAD_REQ,
   VC_UPDATE_REQ,
   VCSYNC_REQ,
   VCSYNC_SCAN_PERIOD,
   VCSYNC_INVENTORY,
   VCSYNC_INVENTORY_PERIOD,
   VCSYNC_BASEVM_PERIOD,
   VCSYNC_RUNTIME,
   VCSYNC_RUNTIME_PERIOD,

   VCDSYNC_ORG,
   VCDSYNC_SCAN_PERIOD,

   SAGA,
   SAGA_DONE,
   SAGA_ABORTED,
}
