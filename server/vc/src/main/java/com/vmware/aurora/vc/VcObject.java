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

@SuppressWarnings("serial")
abstract class VcObjectImpl implements VcObject {
   static final Logger logger = Logger.getLogger(VcObject.class);
   final protected ManagedObjectReference moRef;
   final private VcObjectType type;

   /**
    * VC update types, each updates a set of VC properties.
    */
   protected enum UpdateType {
      CONFIG,
      RUNTIME
   }

   @Override
   final public String getId() {
      return MoUtil.morefToString(moRef);
   }

   @Override
   final public VcObjectType getVcObjectType() {
      return type;
   }

   @Override
   final public ManagedObjectReference getMoRef() {
      return moRef;
   }

   protected VcObjectImpl(ManagedObject mo) {
      this.moRef = mo._getRef();
      this.type = VcObjectType.fromMo(mo);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T extends ManagedObject> T getManagedObject() {
      return (T)MoUtil.getManagedObject(moRef);
   }

   /**
    * @return the VcObject from ManagedObjectReference
    */
   static protected VcObjectImpl
   loadFromMoRef(ManagedObjectReference moRef)
   throws Exception {
      AuAssert.check(VcContext.isInSession());
      VcObjectImpl obj = null;
      ManagedObject mo = MoUtil.getManagedObject(moRef);
      if (mo instanceof Datacenter) {
         obj = new VcDatacenterImpl((Datacenter)mo);
      } else if (mo instanceof ClusterComputeResource) {
         obj = new VcClusterImpl((ClusterComputeResource)mo);
      } else if (mo instanceof Network) {
         obj = new VcNetworkImpl((Network)mo);
      } else if (mo instanceof Datastore) {
         obj = new VcDatastoreImpl((Datastore)mo);
      } else if (mo instanceof ResourcePool) {
         obj = new VcResourcePoolImpl((ResourcePool)mo);
      } else if (mo instanceof VirtualMachine) {
         obj = new VcVirtualMachineImpl((VirtualMachine)mo);
      } else if (mo instanceof Snapshot) {
         AuAssert.unreachable();
      } else if (mo instanceof HostSystem) {
         obj = new VcHostImpl((HostSystem)mo);
      }
      Profiler.inc(StatsType.VC_LOAD_MO, obj);
      return obj;
   }

   /*
    * @return the VcSnapshot object.
    */
   static protected VcSnapshotImpl
   loadSnapshotFromMoRef(ManagedObjectReference moRef,
                         VcVirtualMachineImpl parent, String name)
   throws Exception {
      AuAssert.check(VcContext.isInSession());
      VcSnapshotImpl obj = null;
      ManagedObject mo = MoUtil.getManagedObject(moRef);
      AuAssert.check(mo instanceof Snapshot);
      obj = new VcSnapshotImpl((Snapshot)mo, parent, name);
      Profiler.inc(StatsType.VC_LOAD_MO, obj);
      return obj;
   }

   /*
    * Update VC object configuration properties.
    */
   abstract protected void update(ManagedObject mo) throws Exception;

   /*
    * Update VC object runtime properties.
    */
   protected void updateRuntime(ManagedObject mo) throws Exception {
   }

   protected boolean isCached() {
      VcObject other = VcCache.lookup(moRef);
      return other == this;
   }

   protected synchronized void updateInternal(EnumSet<UpdateType> updates)
   throws Exception {
      try {
         ManagedObject mo = getManagedObject();
         if (updates.contains(UpdateType.CONFIG)) {
            Profiler.inc(StatsType.VC_UPDATE_CONFIG, this);
            update(mo);
         }
         if (updates.contains(UpdateType.RUNTIME)) {
            Profiler.inc(StatsType.VC_UPDATE_RUNTIME, this);
            updateRuntime(mo);
         }
      } catch (ManagedObjectNotFound mnf) {
         if (mnf.getObj().equals(moRef)) {
            processNotFoundException();
         } else {
            throw mnf;
         }
      }
   }

   protected void processNotFoundException() throws Exception {
      logger.error("VC object " + MoUtil.morefToString(moRef)
            + " is already deleted from VC. Purge from vc cache");
      // in case the event is lost
      VcCache.purge(moRef);
   }

   @Override
   public void update() throws Exception {
      updateInternal(EnumSet.of(UpdateType.CONFIG));
      /* If this VC object in VcCache is not the same as this one,
       * also update the cached one.
       */
      VcObject other = VcCache.lookup(moRef);
      if (other != this && other != null) {
         logger.info("update other " + other);
         other.update();
      }
   }

   @Override
   public void updateRuntime() throws Exception {
      updateInternal(EnumSet.of(UpdateType.RUNTIME));
      /* If this VC object in VcCache is not the same as this one,
       * also update the cached one.
       */
      VcObject other = VcCache.lookup(moRef);
      if (other != this && other != null) {
         logger.info("update other " + other);
         other.updateRuntime();
      }
   }

   /*
    * Check if a property of a VC object is valid.
    * A null property means that the VC object has not been fully created
    * and is not ready for access and we'll throw a MOREF_NOT_READY exception.
    */
   protected <T> T checkReady(final T property) {
      if (property == null) {
         throw VcException.MOREF_NOT_READY(getId());
      }
      return property;
   }

   /**
    * @return true if two objects contain the same ManagedObjectReference, false otherwise.
    */
   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj instanceof VcObject) {
         return moRef.equals(((VcObject)obj).getMoRef());
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return moRef.hashCode();
   }

   /**
    * A proxy of the VcObject that throws an exception on every invocation.
    */
   protected static class VcInvalidProxy implements InvocationHandler, Serializable {
      ManagedObjectReference moRef;
      VcException e;

      protected VcInvalidProxy(ManagedObjectReference moRef, VcException e) {
         this.moRef = moRef;
         this.e = e;
      }

      @Override
      public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
         throw AuroraException.stitchStackTraces(e);
      }
   }

   protected static VcObject getVcInvalidProxy(ManagedObjectReference moRef,
                                               VcException e) {
      Class<? extends VcObject> type = VcObjectType.fromMoRef(moRef).getVcClass();
      return (VcObject)Proxy.newProxyInstance(type.getClassLoader(),
                                              new Class[] { type },
                                              new VcInvalidProxy(moRef, e));
   }

   /**
    * This class is used to serialize all VcObject instances.
    * The proxy saves VC moRef as part of the serialization and
    * looks up the shared copy of the VC object from VcCache
    * when the object is restored.
    *
    * @serial include
    */
   protected static class SerializationProxy implements java.io.Serializable {
      /**
       * The moref of the object.
       */
      protected final ManagedObjectReference moRef;

      protected SerializationProxy(VcObject obj) {
         moRef = obj.getMoRef();
      }

      /**
       * Override to customize object retrieval.
       */
      protected VcObject getCachedObject() throws VcException {
         return VcCache.get(moRef);
      }

      final protected Object readResolve() {
         try {
            return getCachedObject();
         } catch (VcException e) {
            return getVcInvalidProxy(moRef, e);
         }
      }
   }

   /**
    * Override the default serialization object.
    */
   protected Object writeReplace() {
      return new SerializationProxy(this);
   }

}
