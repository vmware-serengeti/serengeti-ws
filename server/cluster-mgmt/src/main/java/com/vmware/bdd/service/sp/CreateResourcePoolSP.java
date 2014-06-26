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

package com.vmware.bdd.service.sp;

import java.util.List;
import java.util.concurrent.Callable;

import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.vim.binding.impl.vim.ResourceAllocationInfoImpl;
import com.vmware.vim.binding.impl.vim.SharesInfoImpl;
import com.vmware.vim.binding.vim.ResourceAllocationInfo;
import com.vmware.vim.binding.vim.SharesInfo;

/**
 * Stored procedure to create resource pool in a VC data centre. This stored
 * procedure can create resource pool, under the parent resource pool (or vc cluster).
 */

public class CreateResourcePoolSP implements Callable<Void> {

   private VcResourcePool parentVcResourcePool;
   private String childVcResourcePoolName;
   private NodeGroupCreate nodeGroup;
   private SoftwareManager softManager;

   public CreateResourcePoolSP(VcResourcePool parentVcResourcePool,
         final String childVcResourcePoolName) {
      this(parentVcResourcePool, childVcResourcePoolName, null, null);
   }

   public CreateResourcePoolSP(VcResourcePool parentVcResourcePool,
         final String childVcResourcePoolName, NodeGroupCreate nodeGroup,
         SoftwareManager softManager) {
      this.parentVcResourcePool = parentVcResourcePool;
      this.childVcResourcePoolName = childVcResourcePoolName;
      this.nodeGroup = nodeGroup;
      this.softManager = softManager;
   }

   @Override
   public Void call() throws Exception {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected boolean isTaskSession() {
            return true;
         }

         @Override
         protected Void body() throws Exception {
            createResourcePool(parentVcResourcePool, childVcResourcePoolName);
            return null;
         }
      });
      return null;
   }

   private VcResourcePool createResourcePool(VcResourcePool rp,
         String childRpName) throws Exception {
      VcResourcePool childVcResourcePool =
            getChildVcResourcePool(rp, childRpName);
      if (childVcResourcePool == null) {
         Long reservation = Long.valueOf(0);
         Boolean expandable = Boolean.valueOf(true);
         Long limit = Long.valueOf(-1);
         SharesInfo shares = new SharesInfoImpl();
         if (nodeGroup != null && softManager.isComputeOnlyRoles(nodeGroup.getRoles())) {
            shares.setLevel(SharesInfo.Level.low);
         } else {
            shares.setLevel(SharesInfo.Level.normal);
         }
         ResourceAllocationInfo cpu =
               new ResourceAllocationInfoImpl(reservation, expandable, limit,
                     shares, null);
         ResourceAllocationInfo mem =
               new ResourceAllocationInfoImpl(reservation, expandable, limit,
                     shares, null);
         childVcResourcePool = rp.createChild(childRpName, cpu, mem);
      }
      return childVcResourcePool;
   }

   private VcResourcePool getChildVcResourcePool(VcResourcePool rp,
         final String childVcResourcePoolName) {
      List<VcResourcePool> childVcResourcePools = rp.getChildren();
      for (VcResourcePool childVcResourcePool : childVcResourcePools) {
         if (childVcResourcePoolName.equals(childVcResourcePool.getName())) {
            return childVcResourcePool;
         }
      }
      return null;
   }

}
