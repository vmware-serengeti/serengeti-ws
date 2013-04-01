/* Copyright (c) 2013 VMware, Inc.  All rights reserved. */

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