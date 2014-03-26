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
package com.vmware.bdd.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import mockit.Mockit;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.aurora.composition.DiskSchema.Disk;
import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.composition.VmSchema;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.StorageRead.DiskType;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.service.MockTmScheduler;
import com.vmware.bdd.service.MockTmScheduler.VmOperation;
import com.vmware.bdd.service.MockVcCache;
import com.vmware.bdd.service.MockVcResourceUtils;
import com.vmware.bdd.service.sp.StartVmSP;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

public class TestVcVmUtil {
   @BeforeMethod()
   public void setMockup() {
      Mockit.setUpMock(MockVcResourceUtils.class);
      Mockit.setUpMock(MockTmScheduler.class);
      Mockit.setUpMock(MockVcCache.class);
   }

   @AfterMethod
   public void cleanFlag() {
      MockVcResourceUtils.cleanFlag();
      MockVcCache.setGetFlag(false);
      MockTmScheduler.cleanFlag();
      Mockit.tearDownMocks();
   }

   private ClusterCreate getClusterSpec() {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("test");
      NodeGroupCreate[] nodeGroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      group.setVmFolderPath("root/test/master");
      group.setName("master");
      group.setCpuNum(2);
      nodeGroups[0] = group;
      spec.setNodeGroups(nodeGroups);
      List<NetworkAdd> networkAdds = new ArrayList<NetworkAdd>();
      NetworkAdd network = new NetworkAdd();
      network.setDhcp(true);
      network.setPortGroup("test-portgroup");
      networkAdds.add(network);
      spec.setNetworkings(networkAdds);
      return spec;
   }

   @Test
   public void testGetVmSchema() {
      ClusterCreate spec = getClusterSpec();
      List<DiskSpec> diskSet = getDiskSpec();
      VmSchema schema =
            VcVmUtil.getVmSchema(spec, "master", diskSet, "vm-101", "snap-102");
      Assert.assertTrue(schema.resourceSchema.numCPUs == 2,
            "Excepted cpu number is 2, but got "
                  + schema.resourceSchema.numCPUs);
      List<Disk> disks = schema.diskSchema.getDisks();
      Assert.assertTrue(disks != null && disks.size() == 1,
            "Excepted one disk, but got " + schema.diskSchema.getDisks());
      Assert.assertTrue(disks.get(0).name.equals("OS"),
            "Excepted name is OS, but got " + schema.diskSchema.getName());
   }

   @Test
   public void testGetTargetRp() {
      NodeEntity node = new NodeEntity();
      VcResourcePoolEntity rpEntity = new VcResourcePoolEntity();
      rpEntity.setVcCluster("test-cluster");
      rpEntity.setVcResourcePool("test-rp");
      node.setVcRp(rpEntity);
      VcVmUtil.getTargetRp("test-cluster", "test-group", node);
   }

   @Test
   public void testGetTargetRpException() {
      NodeEntity node = new NodeEntity();
      VcResourcePoolEntity rpEntity = new VcResourcePoolEntity();
      rpEntity.setVcCluster("test-cluster");
      rpEntity.setVcResourcePool("test-rp");
      node.setVcRp(rpEntity);
      MockVcResourceUtils.setFlag(false);
      try {
         VcVmUtil.getTargetRp("test-cluster", "test-group", node);
         Assert.assertTrue(false,
               "Should get exception for cannot get resource pool.");
      } catch (ClusteringServiceException e) {
         // expected result
      }
   }

   @Test
   public void testGetTargetRpDisabledDRS() {
      NodeEntity node = new NodeEntity();
      VcResourcePoolEntity rpEntity = new VcResourcePoolEntity();
      rpEntity.setVcCluster("test-cluster");
      rpEntity.setVcResourcePool("test-rp");
      node.setVcRp(rpEntity);
      MockVcResourceUtils.setDisableDRS(true);
      VcResourcePool rp =
            VcVmUtil.getTargetRp("test-cluster", "test-group", node);
      Assert.assertTrue(rp.getName() != null, "Should get root rp, but got "
            + rp.getName());
      Assert.assertTrue(rp.getName().equals("root"),
            "Should get root rp, but got " + rp.getName());
   }

   @Test
   public void testRunSPOnSingleVMNoVM() {
      NodeEntity node = new NodeEntity();
      Callable<Void> callable = getCallable();
      boolean result = VcVmUtil.runSPOnSingleVM(node, callable);
      Assert.assertTrue(result == false, "Should get false, but got " + result);

      node.setMoId("vm-001");
      result = VcVmUtil.runSPOnSingleVM(node, callable);
      Assert.assertTrue(result == false, "Should get false, but got " + result);

      MockVcCache.setGetFlag(true);
      MockTmScheduler.setResultIsNull(true);
      result = VcVmUtil.runSPOnSingleVM(node, callable);
      Assert.assertTrue(result == false, "Should get false, but got " + result);
   }

   private Callable<Void> getCallable() {
      Callable<Void> callable =
            new StartVmSP(Mockito.mock(VcVirtualMachine.class), null,
                  Mockito.mock(IPrePostPowerOn.class),
                  Mockito.mock(VcHost.class));
      return callable;
   }

   @Test
   public void testRunSPOnSingleVMException() {
      NodeEntity node = new NodeEntity();
      node.setMoId("vm-001");

      MockVcCache.setGetFlag(true);
      MockTmScheduler.setResultIsNull(false);
      MockTmScheduler.setFlag(VmOperation.START_VM, false);
      Callable<Void> callable = getCallable();
      try {
         VcVmUtil.runSPOnSingleVM(node, callable);
         Assert.assertTrue(false, "Should get exception, but not.");
      } catch (BddException e) {

      }
   }

   @Test
   public void testRunSPOnSingleVMPositive() {
      NodeEntity node = new NodeEntity();
      node.setMoId("vm-001");
      MockVcCache.setGetFlag(true);
      MockTmScheduler.setResultIsNull(false);
      MockTmScheduler.setFlag(VmOperation.START_VM, true);
      Callable<Void> callable = getCallable();
      boolean result = VcVmUtil.runSPOnSingleVM(node, callable);
      Assert.assertTrue(result, "Should get true, but got " + result);
   }

   private List<DiskSpec> getDiskSpec() {
      DiskSpec disk = new DiskSpec();
      disk.setName("OS");
      disk.setDiskType(DiskType.SYSTEM_DISK);
      disk.setSize(20);
      disk.setAllocType("THIN");
      disk.setVmdkPath("[ds]");
      disk.setDiskMode(DiskMode.nonpersistent.name());
      List<DiskSpec> diskSet = new ArrayList<DiskSpec>();
      diskSet.add(disk);
      return diskSet;
   }
}
