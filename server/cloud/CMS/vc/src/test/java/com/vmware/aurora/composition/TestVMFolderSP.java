/* Copyright (c) 2012 VMware, Inc.  All rights reserved. */

package com.vmware.aurora.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

public class TestVMFolderSP extends AbstractTmTest {
   private List<Folder> folders;
   @Test
   public void testCreateFolder() throws Exception {
      VcDatacenter dc = VcInventory.getDatacenters().get(0);

      List<String> folderNames = Arrays.asList("a", "b", "c");

      CreateVMFolderSP sp1 = new CreateVMFolderSP(dc, null, folderNames);
      sp1.call();
      Assert.assertEquals(sp1.getResult().size(), folderNames.size());
      for (int i = 0; i < folderNames.size(); ++i) {
         Assert.assertEquals(sp1.getResult().get(i).getName(), folderNames.get(i));
      }

      folderNames = new ArrayList<String>(folderNames);
      folderNames.add("d");
      folderNames.add("e");
      CreateVMFolderSP sp2 = new CreateVMFolderSP(dc, null, folderNames);
      sp2.call();
      Assert.assertEquals(sp2.getResult().size(), folderNames.size());
      for (int i = 0; i < folderNames.size(); ++i) {
         Assert.assertEquals(sp2.getResult().get(i).getName(), folderNames.get(i));
      }

      folders = sp2.getResult();
   }

   @Test (dependsOnMethods = {"testCreateFolder"})
   public void destroyVMFolder() throws Exception {
      DeleteVMFolderSP sp = new DeleteVMFolderSP(folders.get(4), false); // "e"
      sp.call();

      sp = new DeleteVMFolderSP(folders.get(2), false); // "c"
      try {
         sp.call();
         Assert.assertTrue(false, "Should not reach here");
      } catch(Exception ex) {
         // Exception should be thrown because we don't delete the folder forcedly.
      }

      sp = new DeleteVMFolderSP(folders.get(2), true); // "c"
      sp.call();

      sp = new DeleteVMFolderSP(Arrays.asList(folders.get(0), folders.get(1)), false, true); // "a", "b"
      try {
         sp.call();
         Assert.assertTrue(false, "Should not reach here");
      } catch(Exception ex) {
         // Exception should be thrown because we don't delete the folder forcedly.
      }

      sp = new DeleteVMFolderSP(Arrays.asList(folders.get(0), folders.get(1)), false, false); // "a", "b"
      sp.call(); // "b" should be removed

      sp = new DeleteVMFolderSP(Arrays.asList(folders.get(0), folders.get(1)), false, false); // "a"
      try {
         sp.call(); // "a" should be removed
      } catch(ManagedObjectNotFound ex) {
         // b cannot be found
      }
   }
}
