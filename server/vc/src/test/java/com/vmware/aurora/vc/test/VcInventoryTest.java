/************************************************************
 * Copyright (c) 2011 VMware, Inc.  All rights reserved.
 ************************************************************/
package com.vmware.aurora.vc.test;

import java.util.List;

import org.testng.annotations.Test;

import com.vmware.aurora.vc.MoUtil;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.ComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.Network;
import com.vmware.vim.binding.vim.VirtualMachine;

/**
 * Test code to enumerate most of the resource inventories.
 */
public class VcInventoryTest extends AbstractVcTest {
   
   /**
    * Returns all datacenters in the root folder.
    */
   public static List<Datacenter> getDatacenters() throws Exception {
      Folder rootFolder = MoUtil.getRootFolder();
      List<Datacenter> dcList = MoUtil.getChildEntity(rootFolder, Datacenter.class);
      return dcList;
   }

   /**
    * Returns all networks in the data center.
    */
   public static List<Network> getNetworks(Datacenter dc) throws Exception {
      return MoUtil.getManagedObjects(dc.getNetwork());
   }

   /**
    * Returns all datastores in the data center.
    */
   public static List<Datastore> getDatastores(Datacenter dc) throws Exception {
      return MoUtil.getManagedObjects(dc.getDatastore());
   }
   
   /**
    * Returns all datacenters in the data center.
    */
   public static List<VirtualMachine> getVirtualMachines(Datacenter dc) throws Exception {
      Folder vmFolder = MoUtil.getManagedObject(dc.getVmFolder());
      List<VirtualMachine> list = MoUtil.getChildEntity(vmFolder, VirtualMachine.class);
      return list;
   }
   
   /**
    * Returns all compute resources in the data center.
    */
   public static List<ClusterComputeResource> getClusters(Datacenter dc) throws Exception {
      Folder hostFolder = MoUtil.getManagedObject(dc.getHostFolder());
      List<ClusterComputeResource> list = 
         MoUtil.getChildEntity(hostFolder, ClusterComputeResource.class);
      return list;
   }

   /**
    * Returns all compute resources in the data center.
    */
   public static List<ComputeResource> getComputeResources(Datacenter dc) throws Exception {
      Folder hostFolder = MoUtil.getManagedObject(dc.getHostFolder());
      List<ComputeResource> list = 
         MoUtil.getChildEntity(hostFolder, ComputeResource.class);
      return list;
   }
   
   /** 
    * Tests get all Datacenters
    * @throws Exception
    */ 
   @Test
   public void testGetDatacenters() throws Exception {
      System.out.println("Service url is: " + vcService.getServiceUrl());
      for (Datacenter dc : getDatacenters()) {
         System.out.println(dc);
         for (VirtualMachine vm: getVirtualMachines(dc)) {
            System.out.println("VM" + vm.getName());
         }
         for (Network net: getNetworks(dc)) {
            System.out.println("NETWORK:" + net.getName());
         }
         for (Datastore ds: getDatastores(dc)) {
            System.out.println("DATASTORE: " + ds.getName());
         }
         for (ClusterComputeResource res : getClusters(dc)) {
            System.out.println("CLUSTER: " + res.getName());
         }
         for (ComputeResource res : getComputeResources(dc)) {
            System.out.println("CR: " + res.getName());
         }
      }
   }
}
