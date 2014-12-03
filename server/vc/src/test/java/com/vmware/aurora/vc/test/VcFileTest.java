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
package com.vmware.aurora.vc.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.vmware.aurora.global.DiskSize;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcFileManager;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcSnapshot;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.vim.vm.ConfigSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

/**
 * Test code to enumerate resource pools.
 */
public class VcFileTest extends AbstractVcTest {

   VcResourcePool myRP = null;
   VcCluster myCluster = null;
   List<VcDatastore> datastores;
   VcDatastore d1, d2;
   List<VcNetwork> networks;
   VcNetwork net1, net2;

   private void init() throws Exception {
      VcInventory.loadInventory();
      Thread.sleep(2000);
      myRP = VcTestConfig.getTestRP();
      myCluster = myRP.getVcCluster();
      if (myRP == null || myCluster == null) {
         throw new Exception("cannot find qualified RP");
      }
      datastores = myCluster.getSharedDatastores();
      if (datastores.size() < 1) {
         throw new Exception("cannot find data stores");
      }
      d1 = d2 = datastores.get(0);
      for (VcDatastore ds : datastores) {
         if (ds.getName().equals(VcTestConfig.testDsName)) {
            d1 = d2 = ds;
            break;
         }
      }

      networks = myCluster.getSharedNetworks();
      if (networks.size() < 1) {
         throw new Exception("cannot find data stores");
      }
      for (VcNetwork network : networks) {
         System.out.println(network);
      }
      net1 = networks.get(0);
      net2 = networks.get(networks.size() - 1);
   }

   private void testFileOp(String dsFilePath) throws Exception {
      String file = dsFilePath.substring(dsFilePath.indexOf(']') + 1).trim();
      System.out.println("testFileOp " + file);
      System.out.println("d1=" + d1 + ",d2=" + d2);
      String testDir = VcTestConfig.testFilePath;
      VcFileManager.copyFile(d1, file,
            d2, testDir + "/VcFileTest2");
      System.out.println("file copied");
      VcFileManager.moveFile(d2, testDir + "/VcFileTest2",
            d2, testDir + "/VcFileTest3");
      System.out.println("file moved");
      System.out.println(VcFileManager.searchFile(d2, testDir + "/VcFileTest3"));
      VcFileManager.deleteFile(d2, testDir + "/VcFileTest3");
      System.out.println(VcFileManager.searchFile(d2, testDir + "/VcFileTest3"));
   }

//   @Test
//   public void uploadVmTemplate() throws Exception {
//      init();
//      System.out.println("upload VM template to " + myRP + d1 + net1 + net2);
//      VcVirtualMachine vm = null, vm1 = null;
//      String vmName = "junit-VcFileTest-VM-" + VcTestConfig.testPostfix;
//      String vm1Name = "junit-VcFileTest-VM%1-" + VcTestConfig.testPostfix;
//      VcDatacenter dc = d1.getDatacenter();
//      try {
//         /*
//          * Always clean up VMs from previous runs.
//          */
//         if ((vm = dc.getVirtualMachine(vmName)) != null) {
//            vm.destroy();
//         }
//         if ((vm1 = dc.getVirtualMachine(vm1Name)) != null) {
//            vm1.destroy();
//         }
//         /*
//          * Import a VM from OVF.
//          */
//         vm = VcFileManager.importVm(vmName, myRP, d1, net1, VcTestConfig.ovfPath);
//         System.out.println(vm);
//         vm = dc.getVirtualMachine(vmName);
//         System.out.println(vm);
//         vm.dumpDevices();
//         System.out.println(VcFileManager.getDsPath(vm, ""));
//
//         /* test device identification */
//         DeviceId diskId = new DeviceId("VirtualLsiLogicController:0:0");
//         VirtualDevice scsi1 = vm.getVirtualController(diskId);
//         VirtualDevice disk1 = vm.getVirtualDevice(diskId);
//         DeviceId genDiskId = new DeviceId(scsi1, disk1);
//         if (!diskId.equals(genDiskId)) {
//            throw new Exception("unmatched disk id " + diskId + " " + genDiskId);
//         }
//
//         VirtualDevice.BackingInfo disk1Bk = vm.getVirtualDevice(diskId).getBacking();
//         System.out.println("Remove a disk");
//         vm.detachVirtualDisk(diskId, false);
//         vm.dumpDevices();
//
//         System.out.println("Attach a new disk");
//         vm.attachVirtualDisk(new DeviceId("VirtualLsiLogicController", 0, 2),
//               VmConfigUtil.createVmdkBackingInfo(vm, "data.vmdk", DiskMode.persistent),
//               true, DiskSize.sizeFromGB(8));
//         vm.dumpDevices();
//
//         /* test setting extra config */
//         Map<String, String> map = new HashMap<String, String>();
//         map.put("vmid", "junit-test-vm");
//         vm.setDbvmConfig(map);
//         map = vm.getDbvmConfig();
//         System.out.println(map);
//
//         if(VmConfigUtil.isDetachDiskEnabled()) {
//            System.out.println("Attach a disk");
//            vm.attachVirtualDisk(diskId, disk1Bk, false, null);
//            vm.dumpDevices();
//         }
//
//         // create a snapshot so that we can do linked clone
//         vm.createSnapshot("snap1", "snap1");
//         // test finding snapshots
//         vm.createSnapshot("snap2", "snap2");
//         VcSnapshot snap1 = vm.getSnapshotByName("snap1");
//         AuAssert.check(snap1 != null);
//         VcSnapshot snap2 = vm.getSnapshotByName("snap2");
//         AuAssert.check(snap2 != null);
//         System.out.println("snap1: " + snap1 + " snap2: " + snap2);
//
//         System.out.println("Mark the VM as template");
//         vm.markAsTemplate();
//         System.out.println(dc.getVirtualMachine(vmName).getInfo());
//
//         /*
//          * Test file operations using data.vmdk
//          */
//         System.out.println("data.vmdk uuid=" +
//               VcFileManager.queryVirtualDiskUuid(
//                     VcFileManager.getDsPath(vm, "data.vmdk"), dc));
//         testFileOp(VcFileManager.getDsPath(vm, "data.vmdk"));
//
//         /*
//          * Clone the template VM.
//          */
//         System.out.println("clone the template VM to a VM");
//         ConfigSpec spec = new ConfigSpecImpl();
//         vm1 = vm.cloneTemplate(vm1Name, myRP, d1, spec);
//         System.out.println(vm1.getInfo());
//         vm1.dumpDevices();
//         map = vm1.getDbvmConfig();
//         System.out.println(map);
//
//         System.out.println("Attach a new disk");
//         DeviceId archiveDiskId = new DeviceId("VirtualLsiLogicController:0:3");
//         DeviceId tempDiskId = new DeviceId("VirtualLsiLogicController:0:4");
//         vm1.attachVirtualDisk(archiveDiskId,
//               VmConfigUtil.createVmdkBackingInfo(vm1, d1, "archive.vmdk", DiskMode.persistent, true),
//               true, DiskSize.sizeFromGB(2));
//         // test copying and attaching a new disk
//         System.out.println("Copy and attach a new disk");
//         vm1.copyAttachVirtualDisk(tempDiskId, vm1, archiveDiskId,
//                                   "temp.vmdk", DiskMode.persistent);
//
//         List<VirtualDeviceSpec> changes = new ArrayList<VirtualDeviceSpec>();
//         changes.add(vm1.reconfigNetworkSpec("Network adapter 1", net1));
//         changes.add(vm1.reconfigNetworkSpec("Network adapter 2", net2));
//         vm1.reconfigure(VmConfigUtil.createConfigSpec(changes));
//
//         /*
//          * Start the VM and reconfigure online.
//          */
//         vm1.powerOn();
//         Thread.sleep(1000);
//         // extend the disk online
//         vm1.extendVirtualDisk(tempDiskId, DiskSize.sizeFromGB(3));
//         vm1.dumpDevices();
//         vm1.powerOff();
//
//         /*
//          * Clean up.
//          */
//         vm.destroy();
//         vm1.destroy();
//      } catch (Exception e) {
//         System.out.println(e);
//         if (vm != null) {
//            vm.destroy();
//         }
//         if (vm1 != null) {
//            vm1.destroy();
//         }
//         throw e;
//      }
//   }

}
