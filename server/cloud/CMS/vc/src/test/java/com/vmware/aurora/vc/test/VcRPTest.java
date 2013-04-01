/************************************************************
 * Copyright (c) 2011 VMware, Inc.  All rights reserved.
 ************************************************************/
package com.vmware.aurora.vc.test;

import org.testng.annotations.Test;

import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.vim.binding.impl.vim.ResourceAllocationInfoImpl;
import com.vmware.vim.binding.impl.vim.SharesInfoImpl;
import com.vmware.vim.binding.vim.ResourceAllocationInfo;
import com.vmware.vim.binding.vim.SharesInfo;

/**
 * Test code to enumerate resource pools.
 */
public class VcRPTest extends AbstractVcTest {
   @Test
   public void testLoadInventory() throws Exception {
      VcInventory.loadInventory();
      Thread.sleep(5000);
   }

   @Test
   public void testFindRP() throws Exception {
      System.out.println(VcTestConfig.getTestRP());
   }

   @Test
   public void testGetRPs() throws Exception {
      for (VcCluster cluster : VcInventory.getClusters()) {
         System.out.println(cluster);
         for (VcResourcePool rp : cluster.getQualifiedRPs()) {
            if (rp.getName().contains("rp\\5")) {
               System.out.println("got special char");
               rp.updateConfig("rp%5", null, null);
            } else if (rp.getName().contains("rp%5")) {
               System.out.println("got special char");
               rp.updateConfig("rp\\5", null, null);
            }
            System.out.println(rp);
            VcCluster c = rp.getVcCluster();
            AuAssert.check(c.getId().equals(cluster.getId()));
         }
      }
   }

   @Test
   public void testVcClusterConnectInfo() throws Exception {
      for (VcCluster cluster : VcInventory.getClusters()) {
         System.out.println(cluster);
         for (VcNetwork net : cluster.getSharedNetworks()) {
            System.out.println(net);
         }
         for (VcDatastore ds : cluster.getSharedDatastores()) {
            System.out.println(ds + " supported: " + ds.isSupported());
            VcDatastore ds1 = VcCache.get(ds.getId());
            System.out.println(ds1 + " supported: " + ds.isSupported());
         }
      }
   }

   @Test
   public void testVcObjectId() throws Exception {
      for (VcCluster cluster : VcInventory.getClusters()) {
         String id = cluster.getId();
         VcCluster newCluster = VcCache.get(id);
         System.out.println(cluster + ", id=" + newCluster.getId());
         AuAssert.check(cluster.getId().equals(newCluster.getId()));

         for (VcResourcePool rp : cluster.getQualifiedRPs()) {
            VcResourcePool newRp = VcCache.get(rp.getId());
            System.out.println(newRp + ", id=" + newRp.getId());
            AuAssert.check(newRp.getId().equals(rp.getId()));
            AuAssert.check(newRp.getPath().equals(rp.getPath()));
            AuAssert.check(newRp.getName().equals(rp.getName()));
         }

         for (VcNetwork net : cluster.getSharedNetworks()) {
            VcNetwork newNet = VcCache.get(net.getId());
            System.out.println(newNet + ", id=" + newNet.getId());
            AuAssert.check(net.getId().equals(newNet.getId()));
         }

         for (VcDatastore ds : cluster.getSharedDatastores()) {
            VcDatastore newDs = VcCache.get(ds.getId());
            System.out.println(newDs + ", id=" + newDs.getId());
            AuAssert.check(ds.getId().equals(newDs.getId()));
         }
      }
   }

   @Test
   public void testCreateDeleteRP() throws Exception {
      VcResourcePool parentRP = VcTestConfig.getTestRP();
      VcResourcePool childRP = null;
      VcResourcePool grandChildRP = null;

      Long reservation = Long.valueOf(0);
      Boolean expandable = Boolean.valueOf(false);
      Long limit = Long.valueOf(-1);
      SharesInfo shares = new SharesInfoImpl(100, SharesInfo.Level.custom);
      ResourceAllocationInfo cpu = new ResourceAllocationInfoImpl(
            reservation, expandable, limit, shares, null);
      ResourceAllocationInfo mem = new ResourceAllocationInfoImpl(
            reservation, expandable, limit, shares, null);

      childRP = parentRP.createChild("child_rp", cpu, mem);
      grandChildRP = childRP.createChild("grand_child_rp", cpu, mem);

      System.out.println(parentRP);
      System.out.println(childRP);
      System.out.println(grandChildRP);

      childRP.destroyChildren();
      System.out.println(childRP);

      /* Test waiting for rp.destroy task completion. */
      childRP.destroy();
      parentRP = VcCache.get(parentRP.getMoRef());
      System.out.println(parentRP);
   }

}
