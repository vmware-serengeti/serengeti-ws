/* Copyright (c) 2012 VMware, Inc.  All rights reserved. */

package com.vmware.aurora.composition;

import org.testng.annotations.BeforeClass;

import com.google.gson.internal.Pair;
import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.CmsWorker;

import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.vcevent.VcEventRouter;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcService;

abstract public class AbstractTmTest {

   protected static String vmName;
   protected static VcDatacenter dc;
   protected static VcResourcePool rp;
   protected static VcDatastore ds;
   protected static TestUtil util;

   protected static VcService vcService;

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      // XXX hack to approve bootstrap instance id, should be moved out of Configuration
      Configuration.approveBootstrapInstanceId(Configuration.BootstrapUsage.ALLOWED);
      Configuration.approveBootstrapInstanceId(Configuration.BootstrapUsage.FINALIZED);

      VcContext.initVcContext();
      new VcEventRouter();

      CmsWorker.addPeriodic(new VcInventory.SyncInventoryRequest());
      VcInventory.loadInventory();
      try {
         Thread.sleep(1000);
      } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      vmName = "PlatformTestVM";
      Pair<VcDatacenter, VcResourcePool> pair = VcTestConfig.getTestRPAndDC();
      dc = pair.first;
      rp = pair.second;
      ds = VcTestConfig.getTestDS();
      util = new TestUtil();
   }
}
