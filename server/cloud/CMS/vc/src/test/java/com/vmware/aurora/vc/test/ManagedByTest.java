/************************************************************
 * Copyright (c) 2011 VMware, Inc.  All rights reserved.
 ************************************************************/
package com.vmware.aurora.vc.test;

import java.util.List;

import org.testng.annotations.Test;

import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Extension;
import com.vmware.vim.binding.vim.ExtensionManager;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.ext.ManagedByInfo;
import com.vmware.vim.binding.vim.ext.ManagedEntityInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/**
 * Test code to develop VcPerformance class.
 */
public class ManagedByTest extends AbstractVcTest {

   /**
    * Returns all datacenters in the root folder.
    */
   public static List<Datacenter> getDatacenters() throws Exception {
      Folder rootFolder = MoUtil.getRootFolder();
      List<Datacenter> dcList = MoUtil.getChildEntity(rootFolder, Datacenter.class);
      return dcList;
   }

   /**
    * Returns all virtual machines in the data center.
    */
   public static List<VirtualMachine> getVirtualMachines(Datacenter dc) throws Exception {
      Folder vmFolder = MoUtil.getManagedObject(dc.getVmFolder());
      List<VirtualMachine> list = MoUtil.getChildEntity(vmFolder, VirtualMachine.class);
      return list;
   }

   /**
    * Tests VcVirtualMachine managed-by functionality
    * @throws Exception
    */
   @Test
   public void testManagedBy() throws Exception {
      System.out.println("We are: " + VcContext.getService().getExtensionKey());

      //enumAllExtensions();
      //mungeVm("mdb1-469663-1");
      //enumAllManagedBys();
      enumAllManagedVMs();
   }

   public void enumAllManagedBys() throws Exception {
      // iterate datacenters to iterate virtual machines
      for (Datacenter dc : getDatacenters()) {
         System.out.println("DC " + dc);
         for (VirtualMachine rawVm: getVirtualMachines(dc)) {
            // Skip templates?
            //if (vm.getConfig().isTemplate()) {
            //   continue;
            //}

            VcVirtualMachine vm = VcCache.get(rawVm._getRef());
            System.out.println("VM " + vm.getName() + ":");

            System.out.println("  managed by: " + mbToString(vm.getManagedBy()));
         }
      }
   }

   public void enumAllManagedVMs() throws Exception {
      // iterate datacenters to iterate virtual machines
      for (Datacenter dc : getDatacenters()) {
         System.out.println("DC " + dc);
         for (VirtualMachine rawVm: getVirtualMachines(dc)) {
            VcVirtualMachine vm = VcCache.get(rawVm._getRef());
            ManagedByInfo mb = vm.getManagedBy();
            if (mb != null) {
               System.out.println("VM " + vm.getName() + " is managed by " + mbToString(mb));
            }
         }
      }
   }

   public void mungeVm(String mungeeName) throws Exception {
      // iterate datacenters to iterate virtual machines
      for (Datacenter dc : getDatacenters()) {
         System.out.println("DC " + dc);
         for (VirtualMachine rawVm: getVirtualMachines(dc)) {
            if (!rawVm.getName().equals(mungeeName)) {
               continue;
            }

            VcVirtualMachine vm = VcCache.get(rawVm._getRef());
            System.out.println("VM " + vm.getName() + ":");

            System.out.println("  before: managed by: " + mbToString(vm.getManagedBy()));

            vm.setManagedBy(VcContext.getService().getExtensionKey(), "dbvm");

            System.out.println("  after: managed by: " + mbToString(vm.getManagedBy()));
         }
      }
   }

   public void enumAllExtensions() throws Exception {
      ManagedObjectReference emRef = vcService.getServiceInstanceContent().getExtensionManager();
      ExtensionManager em = (ExtensionManager) MoUtil.getManagedObject(emRef);
      Extension[] extensions = em.getExtensionList();
      int extensionCount = 0;
      int auroraCount = 0;

      for (Extension e: extensions) {
         System.out.println("Extension with key " + e.getKey());
         //System.out.println(e.toString());
         System.out.println("  label: " + e.getDescription().getLabel());
         System.out.println("  summary: " + e.getDescription().getSummary());
         System.out.println("  company: " + e.getCompany());
         System.out.println("  type: " + e.getType());
         ManagedEntityInfo[] meis = e.getManagedEntityInfo();
         if (meis != null) {
            System.out.println(String.format("  managing %d entity types:", meis.length));
            for (ManagedEntityInfo mei : meis) {
               System.out.println(String.format("    managed type: %s, %s, %s", mei.getType(), mei.getDescription(), mei.getSmallIconUrl()));
            }
         }

         ++extensionCount;
         if (e.getKey().contains("aurora")) {
            ++auroraCount;
         }
      }

      System.out.println(String.format("Found %d extensions, %d are Aurora", extensionCount, auroraCount));
   }

   private String mbToString(ManagedByInfo manager) {
      if (manager == null) {
         return "nobody";
      }
      return manager.getExtensionKey() + "," + manager.getType();
   }
}
