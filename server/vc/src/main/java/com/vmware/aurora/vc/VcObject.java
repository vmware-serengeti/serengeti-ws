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

package com.vmware.aurora.vc;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;

import org.apache.log4j.Logger;

import com.vmware.aurora.exception.AuroraException;
import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsType;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.Network;
import com.vmware.vim.binding.vim.ResourcePool;
import com.vmware.vim.binding.vim.Task;
import com.vmware.vim.binding.vim.VirtualApp;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.dvs.DistributedVirtualPortgroup;
import com.vmware.vim.binding.vim.vm.Snapshot;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

public interface VcObject extends Serializable, VcCache.IVcCacheObject {
   /**
    * Enumeration of all VcObject types.
    */
   public enum VcObjectType {
      VC_DATACENTER(Datacenter.class, VcDatacenter.class),
      VC_CLUSTER(ClusterComputeResource.class, VcCluster.class),
      VC_DATASTORE(Datastore.class, VcDatastore.class),
      VC_DVPORTGROUP(DistributedVirtualPortgroup.class, VcNetwork.class),
      VC_NETWORK(Network.class, VcNetwork.class),
      VC_VAPP(VirtualApp.class, VcResourcePool.class),
      VC_RP(ResourcePool.class, VcResourcePool.class),
      VC_VM(VirtualMachine.class, VcVirtualMachine.class),
      VC_SNAPSHOT(Snapshot.class, VcSnapshot.class),
      VC_TASK(Task.class, VcTask.class),
      VC_HOST(HostSystem.class, VcHost.class);

      Class<?> vimClass;
      Class<? extends VcObject> vcClass;

      VcObjectType(Class<?> vimClass, Class<? extends VcObject> vcClass) {
         this.vimClass = vimClass;
         this.vcClass = vcClass;
      }

      protected static VcObjectType fromMo(ManagedObject mo) {
         for (VcObjectType type : VcObjectType.values()) {
            if (type.vimClass.isInstance(mo)) {
               return type;
            }
         }
         throw VcException.INVALID_MOREF(MoUtil.morefToString(mo._getRef()));
      }

      protected static VcObjectType fromMoRef(ManagedObjectReference moRef) {
         for (VcObjectType type : VcObjectType.values()) {
            /* XXX The following requires exact match of class name.
             *     Thus, we need to enumerate all vim classes we support.
             */
            if (MoUtil.isOfType(moRef, type.getVimClass())) {
               return type;
            }
         }
         throw VcException.INVALID_MOREF(MoUtil.morefToString(moRef));
      }

      protected Class<?> getVimClass() {
         return vimClass;
      }

      protected Class<? extends VcObject> getVcClass() {
         return vcClass;
      }
   }

   public ManagedObjectReference getMoRef();

   /**
    * Gets a unique identity of a VC object converted from
    * its managed object reference, which persists in VC server.
    *
    * @return a unique string for the VC object.
    */
   String getId();

   /**
    * @return enum type of the VC object.
    */
   VcObjectType getVcObjectType();

   /**
    * Get the managed object. Cache if not already set.
    * @return the ManagedObject for the VC object.
    * @throws Exception
    */
   <T extends ManagedObject> T getManagedObject() throws Exception;

   /**
    * Update VC object config values from VC.
    * @throws Exception
    */
   public void update() throws Exception;

   /**
    * Update VC object runtime values from VC.
    * @throws Exception
    */
   public void updateRuntime() throws Exception;
   
}

