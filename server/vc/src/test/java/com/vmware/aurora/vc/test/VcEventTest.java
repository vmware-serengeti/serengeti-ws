/************************************************************
 * Copyright (c) 2011-2012 VMware, Inc.  All rights reserved.
 ************************************************************/
package com.vmware.aurora.vc.test;

import java.util.List;

import org.testng.annotations.Test;

import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.IVcEventHandler;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;
import com.vmware.aurora.vc.vcevent.VcEventListener;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.event.Event;

/**
 * Test code to:
 *    - set up Event Listener
 *    - post a fake event to a VC cluster
 *    - make sure Event Listener gets it from VC
 */
public class VcEventTest extends AbstractVcTest implements IVcEventHandler {
   private int receivedEventCount;

   @Test
   public void testEventListener() throws Exception {
      List<VcCluster> clusters = VcInventory.getClusters();
      VcCluster cluster;         // Will pick the first cluster.
      VcEventListener eventListener = VcContext.getEventListener();

      receivedEventCount = 0;
      /* GeneralUser is a "fake" event not triggered by any cms vc task. */
      VcEventListener.installExtEventHandler(VcEventType.GeneralUser, this);

      if (clusters != null && (cluster = clusters.get(0)) != null) {
         int eventCount = 1;     // Take it easy - can't delete individual events from VC.
         AuAssert.check(eventListener != null);

         for (int i = 0; i < eventCount; i++ ) {
            System.out.println("Posting GeneralUserEvent to cluster: " + cluster.getName());
            eventListener.postGeneralUserEvent(cluster, "FakeGeneralUserEvent", "" +
                  "CMS -> VC GeneralUserEvent", "Administrator");
         }
         System.out.println("Waiting for GeneralUserEvent");
         while (getReceivedEventCount() < eventCount) {
            Thread.sleep(100);
         }
         System.out.println("Event count: " + eventListener.getTotalEventCount());
         VcEventListener.removeExtEventHandler(VcEventType.GeneralUser, this);
      }
   }

   private synchronized int getReceivedEventCount() {
      return receivedEventCount;
   }

   public synchronized boolean eventHandler(VcEventType type, Event event)
   throws Exception {
      receivedEventCount++;
      System.out.println("External event received: " + event.toString());
      return true;
   }
}