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

package com.vmware.aurora.vc.vcservice;

public enum VcConnectionStatusChangeEvent {
   VC_CONNECTED("All vCenter services operational"),                                           // All VC services operational.
   VC_VERSION_HOSTD("vCenter connection failure: connecting to ESX host"),
   VC_VERSION_INVALID_PRODUCT("vCenter connection failure: connecting to invalid product"),
   VC_VERSION_UNSUPPORTED("vCenter connection failure: unsupported version"),
   VC_SESSION_CREATED("Established vCenter session"),                                          // VC session established.
   VC_SESSION_CREATION_FAILURE("Cannot establish vCenter session"),                            // Failed to create session.
   VC_SESSION_DISCONNECTED("Lost vCenter session");                                            // VC session lost.

   private String description;

   private VcConnectionStatusChangeEvent(String description) {
      this.description = description;
   }

   @Override
   public String toString() {
      return description;
   }

   public interface VcConnectionStatusChangeCallback {
      /**
       * Callback when VC connection status is changed.
       * @param event VC connection status change event
       * @param serviceName Service involved, maybe null.
       */
      public void call(VcConnectionStatusChangeEvent event, String serviceName);
   }
}