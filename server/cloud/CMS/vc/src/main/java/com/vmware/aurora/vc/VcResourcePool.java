/* ***************************************************************************
 * Copyright (c) 2011-2012 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/

package com.vmware.aurora.vc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcTask.TaskType;
import com.vmware.aurora.vc.VcTaskMgr.IVcPseudoTaskBody;
import com.vmware.aurora.vc.VcTaskMgr.IVcTaskBody;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.impl.vim.ResourceConfigSpecImpl;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.HttpNfcLease;
import com.vmware.vim.binding.vim.ImportSpec;
import com.vmware.vim.binding.vim.ManagedEntity;
import com.vmware.vim.binding.vim.ResourceAllocationInfo;
import com.vmware.vim.binding.vim.ResourceConfigSpec;
import com.vmware.vim.binding.vim.ResourcePool;
import com.vmware.vim.binding.vim.ResourcePool.ResourceUsage;
import com.vmware.vim.binding.vim.VirtualApp;
import com.vmware.vim.binding.vim.event.ResourcePoolCreatedEvent;
import com.vmware.vim.binding.vim.event.ResourcePoolEvent;
import com.vmware.vim.binding.vim.fault.DuplicateName;
import com.vmware.vim.binding.vim.vApp.ProductInfo;
import com.vmware.vim.binding.vim.vApp.VAppConfigSpec;
import com.vmware.vim.binding.vim.vm.ConfigSpec;
import com.vmware.vim.binding.vim.vm.FileInfo;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

public interface VcResourcePool extends VcObject {

   /**
    * @return the full path name
    */
   abstract String getPath();

   abstract boolean isVApp();

   abstract String getVersion();

   abstract String getFullVersion();

   /**
    * Returns the closest ancestor vApp of the current RP or null if
    * this RP is not under any vApps.
    * @return ancestor vApp RP.
    */
   abstract VcResourcePool getAncestorVAppRp();

   abstract boolean isHADRSCompatible();

   abstract boolean isLeaf();

   abstract boolean isRootRP();

   /**
    * @return reasons for the VC resource pool to be incompatible for resource bundle
    */
   abstract List<String> getIncompatReasonsForRBQual();

   /**
    * @return true if the VC resource pool can be used in a resource bundle.
    */
   abstract boolean isQualifiedResourceBundleRP();

   /**
    * @return the VC cluster that contains this resource pool
    */
   abstract VcCluster getVcCluster();

   /**
    * @return the parent VC resource pool, null if the current is root.
    */
   abstract VcResourcePool getParent();

   /**
    * Create a child resource pool
    * @param name child resource pool name
    * @param cpuAllocation
    * @param memAllocation
    * @return the new resource pool
    * @throws Exception
    */
   abstract VcResourcePool createChild(final String name,
         final ResourceAllocationInfo cpuAllocation,
         final ResourceAllocationInfo memAllocation) throws Exception;

   /**
    * Create a child resource pool
    * @param name child resource pool name
    * @param cpuAllocation
    * @param memAllocation
    * @param force true to suppress DuplicateName exception, and remove existing child RP
    *              false to throw DuplicateName exception.
    * @return the new resource pool
    * @throws Exception
    */
   abstract VcResourcePool createChild(final String name,
         final ResourceAllocationInfo cpuAllocation,
         final ResourceAllocationInfo memAllocation,
         final boolean force) throws Exception;
   /**
    * @return child resource pools
    */
   abstract List<VcResourcePool> getChildren();

   /**
    * @return VMs under the resource pool
    */
   abstract List<VcVirtualMachine> getChildVMs();

   /**
    * Import a VM template (step 1).
    * @param spec VM template import spec
    * @return lease used for uploading OVF files
    * @throws Exception
    * XXX do we really need to copy files from CMS to datastores?
    * TODO: not tested
    */
   abstract HttpNfcLease importVApp(ImportSpec spec) throws Exception;

   /**
    * Create a VM under the resource pool
    * @param config VM configuration
    * @param callback (optional) call-back function for the task
    * @return task for the VM creation operation
    * @throws Exception
    * TODO: not tested
    */
   abstract VcTask createVm(final ConfigSpec config,
         final IVcTaskCallback callback) throws Exception;

   abstract VcVirtualMachine createVm(ConfigSpec config) throws Exception;

   /**
    * Use this method when the VM needs to be created on a specific datastore
    * and when it is not already captured in the input <code>config</code>.
    * @throws Exception
    */
   abstract VcVirtualMachine createVm(ConfigSpec config, VcDatastore ds)
         throws Exception;

   /**
    * Finds a VM by its name and deletes it.
    *
    * @throws Exception
    */
   abstract void destroyVm(String vmName) throws Exception;

   /**
    * Remove all child resource pools recursively.
    * @throws Exception
    */
   abstract void destroyChildren() throws Exception;

   /**
    * Remove this resource pool.
    * @throws Exception
    */
   abstract void destroy() throws Exception;

   /**
    * Reconfigure resource settings.
    * @name optional, to rename resource pool
    * @cpuAllocation optional, change CPU allocation
    * @memAllocation optional, change memory allocation
    * @throws Exception
    */
   abstract void updateConfig(final String name,
         final ResourceAllocationInfo cpuAllocation,
         final ResourceAllocationInfo memAllocation) throws Exception;

   /**
    * Reconfigure version and fullVersion for vApp
    * @version
    * @fullVersion
    * @throws Exception
    *
    */
   abstract void updateVAppConfig(VAppConfigSpec spec) throws Exception;

   abstract ResourceAllocationInfo getCpuAllocationInfo();

   abstract ResourceAllocationInfo getMemAllocationInfo();

   abstract ResourceUsage getCpuUsageInfo();

   abstract ResourceUsage getMemUsageInfo();

   abstract String getName();

}

@SuppressWarnings("serial")
class VcResourcePoolImpl extends VcObjectImpl implements VcResourcePool {
   private String path;  // full path of the resource pool from the cluster
   private String name;  // name of the resource pool

   private ManagedObjectReference owner; // the cluster that contains the resource pool
   private List<ManagedObjectReference> childVMs;
   private ManagedObjectReference parent;
   private boolean isVApp;
   private String version;
   private String fullVersion;
   private ResourceAllocationInfo cpuAlloc;
   private ResourceAllocationInfo memAlloc;

   // Usage fields
   private ResourceUsage cpuUsage;
   private ResourceUsage memUsage;

   /**
    * Sync request to send update requests for a subtree of RPs.
    */
   protected static class SyncRequest extends VcInventory.SyncRequest {
      SyncRequest(ManagedObjectReference moRef, VcInventory.SyncRequest parent) {
         super(moRef, parent);
      }

      @Override
      protected void syncChildObjects(VcObject obj) {
         if (obj instanceof VcResourcePoolImpl) {
            VcResourcePoolImpl rp = (VcResourcePoolImpl)obj;
            for (ManagedObjectReference child : rp.getChildRps()) {
               add(new SyncRequest(child, this));
            }
         }
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getPath()
    */
   @Override
   public String getPath() { return path; }

   @Override
   protected void update(ManagedObject mo) throws Exception {
      ResourcePool rp = (ResourcePool)mo;
      childVMs = Arrays.asList(rp.getVm());
      owner = rp.getOwner();
      parent = rp.getParent();
      name = rp.getName();
      isVApp = (rp instanceof VirtualApp);
      if (isVApp) {
         VirtualApp vapp = (VirtualApp)mo;
         ProductInfo[] pInfo = vapp.getVAppConfig().getProduct();
         version = pInfo[0].getVersion();
         fullVersion = pInfo[0].getFullVersion();
      }
      ResourceConfigSpec config = rp.getConfig();
      cpuAlloc = config.getCpuAllocation();
      memAlloc = config.getMemoryAllocation();

      // XXX: The cpuUsgae and memUsgae should really belong
      // to the runtime info. We putting them into configuration category is
      // to temporarily fix bug 865341 in Borealis. Should be refactorred later.
      ResourcePool.RuntimeInfo runtime = rp.getRuntime();
      cpuUsage = runtime.getCpu();
      memUsage = runtime.getMemory();

      for (ManagedObjectReference vmRef : childVMs) {
         VcCache.putVmRpPair(vmRef, getMoRef());
      }
      updatePath();
   }

   @Override
   protected void updateRuntime(ManagedObject mo) throws Exception {
      ResourcePool rp = (ResourcePool)mo;
      ResourcePool.RuntimeInfo runtime = rp.getRuntime();
      cpuUsage = runtime.getCpu();
      memUsage = runtime.getMemory();
   }

   protected VcResourcePoolImpl(ResourcePool rp) throws Exception {
      super(rp);
      update(rp);
      updateRuntime(rp);
   }

   /**
    * Slow way to load an RP, because we need to construct the RP paths
    * from the VC RP tree and we cannot use VC cache to lookup parents.
    * @param rp
    * @return path to RP
    * @throws Exception
    */
   private static String calculatePathSlow(ResourcePool rp) throws Exception {
      LinkedList<ResourcePool> rpStack  = new LinkedList<ResourcePool>();
      while (rp != null) {
         ManagedEntity parent = MoUtil.getManagedObject(rp.getParent());
         AuAssert.check(parent != null);
         if (!(parent instanceof ResourcePool)) {
            // skip the root RP's name in the path
            break;
         }
         rpStack.addFirst(rp);
         rp = (ResourcePool)parent;
      }
      // Path is encoded as: [cluster]/path/to/rpName
      StringBuffer pathBuf = new StringBuffer();
      ClusterComputeResource cluster = MoUtil.getManagedObject(rp.getOwner());
      pathBuf.append('[').append(MoUtil.fromURLString(cluster.getName())).append(']');
      for (ResourcePool r : rpStack) {
         pathBuf.append("/").append(MoUtil.fromURLString(r.getName()));
      }
      return pathBuf.toString();
   }

   /*
    * Calculate and update the path in the resource tree.
    *
    * Note: Cannot use VcCache.get() because this function is called by a
    *       CmsWorker thread.
    */
   private void updatePath() throws Exception {
      if (isRootRP()) {
         VcCluster cluster = VcCache.lookup(owner);
         if (cluster != null) {
            path = '[' + cluster.getName() + ']';
            return;
         }
      } else {
         VcResourcePool parentRP = VcCache.lookup(parent);
         if (parentRP != null) {
            path = parentRP.getPath() + "/" + getName();
            return;
         }
      }
      path = calculatePathSlow((ResourcePool)getManagedObject());
      return;
   }

   @Override
   public String toString() {
      Long cpuLimit = cpuAlloc.getLimit();
      Long cpuReservation = cpuAlloc.getReservation();
      Long memLimit = memAlloc.getLimit();
      Long memReservation = memAlloc.getReservation();
      return String.format("RP[%s](cpu:R=%d,L=%d,mem:R=%d,L=%d,#vm=%d)", path,
            cpuReservation, cpuLimit, memReservation, memLimit, childVMs.size());
   }

   private List<ManagedObjectReference> getChildRps() {
      ResourcePool rp = null;
      if (VcContext.isInSession()) {
         rp = getManagedObject();
      } else {
         rp = VcContext.inVcSessionDo(new VcSession<ResourcePool>() {
            @Override
            protected ResourcePool body() throws Exception {
               return getManagedObject();
            }
         });
      }
      return Arrays.asList(rp.getResourcePool());
   }

   @Override
   public boolean isLeaf() {
      return getChildRps().isEmpty();
   }

   @Override
   public boolean isVApp() {
      return isVApp;
   }

   @Override
   public String getVersion() {
      return version;
   }

   @Override
   public String getFullVersion() {
      return fullVersion;
   }

   @Override
   public VcResourcePool getAncestorVAppRp() {
      VcResourcePool parentRp = this;
      while ((parentRp = parentRp.getParent()) != null) {
         if (parentRp.isVApp()) {
            return parentRp;
         }
      }
      return null;
   }

   /**
    * Helper class to define a walker routine for traversing a ResourcePool tree.
    */
   abstract static class RPWalker<T> {
      /*
       * The list that holds objects to be returned.
       */
      public List<T> results;
      public RPWalker() {
         results = new ArrayList<T>();
      }
      /**
       * @return true if needs to traverse child nodes
       */
      abstract public boolean processRP(VcResourcePool rp) ;
   }

   /**
    * Walk the resource pool in pre-order.
    *
    * @param <T> Class of data created by walker
    * @param rp Root resource pool
    * @param walker Tree traversing code
    * @return the collection of data created by walker
    * @throws Exception
    */
   private static <T> List<T>
   walkResourcePools(VcResourcePool rp, RPWalker<T> walker)
   {
      if (walker.processRP(rp)) {
         for (VcResourcePool child : rp.getChildren()) {
            walkResourcePools(child, walker);
         }
      }
      return walker.results;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#isHADRSCompatible()
    */
   @Override
   public boolean isHADRSCompatible() {
      return getVcCluster().getHADRSIncompatReasonsForAlert().isEmpty();
   }

   @Override
   public List<String> getIncompatReasonsForRBQual() {
      List<String> reasons = VcUtil.getCPUMemAllocIncompatReasons(cpuAlloc, memAlloc);
      reasons.addAll(getVcCluster().getHADRSIncompatReasonsForRBQual());
      return reasons;
   }

   @Override
   public boolean isQualifiedResourceBundleRP() {
      return getIncompatReasonsForRBQual().isEmpty();
   }

   private List<VcResourcePool>
   getQualifiedRPsWork(final boolean forBundle) {
      return walkResourcePools(this, new RPWalker<VcResourcePool>() {
         public boolean processRP(VcResourcePool rp) {
            if (rp.isVApp()) {
               // If RP is a vapp, skip the subtree.
               logger.debug("Skip VirtualApp RP " + rp.getName());
               return false;
            } if (!rp.isLeaf()) {
               // Not a leaf not, visit the child RPs.
               return true;
            } else {
               if (!forBundle || rp.isQualifiedResourceBundleRP()) {
                  results.add(rp);
               }
               return false;
            }
         }
      });
   }

   /**
    * Get RPs in the subtree qualified for resource bundle.
    */
   protected List<VcResourcePool> getQualifiedRPs() {
      return getQualifiedRPsWork(true);
   }

   /**
    * Get all leaf RPs in the subtree.
    */
   protected List<VcResourcePool> getAllRPs() {
      return getQualifiedRPsWork(false);
   }

   /**
    * Search for a resource pool that matches the path.
    * The path should be in the form: "[clusterName]/rp1/rp2".
    * @param path
    * @return the matching resource pool, null if not found
    * @throws Exception
    */
   protected VcResourcePool searchRP(final String path) {
      List<VcResourcePool> result =
         walkResourcePools(this, new RPWalker<VcResourcePool>() {
            public boolean processRP(VcResourcePool rp) {
               if (rp.getPath().equals(path)) {
                  results.add(rp);
                  return false;
               }
               if (path.startsWith(rp.getPath())) {
                  return true;
               }
               return false;
            }
         });
      if (result.isEmpty()) {
         return null;
      }
      return result.get(0);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getVcCluster()
    */
   @Override
   public VcCluster getVcCluster() {
      return VcCache.get(owner);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getParent()
    */
   @Override
   public VcResourcePool getParent() {
      AuAssert.check(VcContext.isInSession());
      if (isRootRP()) {
         return null;
      }
      return VcCache.get(parent);
   }

   public VcResourcePool createChild(final String name,
         final ResourceAllocationInfo cpuAllocation,
         final ResourceAllocationInfo memAllocation) throws Exception {
      return createChild(name, cpuAllocation, memAllocation, false);
   }
   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#createChild(java.lang.String, com.vmware.vim.binding.vim.ResourceAllocationInfo, com.vmware.vim.binding.vim.ResourceAllocationInfo)
    */
   @Override
   public VcResourcePool createChild(final String name,
         final ResourceAllocationInfo cpuAllocation,
         final ResourceAllocationInfo memAllocation,
         final boolean force) throws Exception
   {
      final ResourceConfigSpec spec = new ResourceConfigSpecImpl(
               null, null, null, cpuAllocation, memAllocation);
      ManagedObjectReference ref =
         VcContext.getTaskMgr().execPseudoTask("ResourcePool.createResourcePool",
            VcEventType.ResourcePoolCreated, getMoRef(),
            new IVcPseudoTaskBody() {
         @Override
         public final ManagedObjectReference body() throws Exception {
            final ResourcePool rp = getManagedObject();
            ManagedObjectReference child = null;
            //at least execute once
            while (true) {
               try {
                  child = rp.createResourcePool(name, spec);
                  break;
               } catch (DuplicateName ex) {
                  if (force) {
                     //suppress this exception, and remove duplicated RP, then retry.
                     update();
                     for (VcResourcePool vcChildRP : getChildren()) {
                        String childName = vcChildRP.getName();
                        if (childName.equals(name)) {
                           vcChildRP.destroy();
                           break;
                        }
                     }
                  } else {
                     throw ex;
                  }
               }
            }
            return child;
         }
      });

      try {
         return VcCache.get(ref);
      } catch (Exception e) {
         // XXX fix exception here
         logger.error(this + ":race detected in creating RP " + name, e);
         throw new Exception("bad moref");
      }
   }

   /**
    * Returns "target moRef" for an RP event. For createResourcePool operation,
    * we use parent moRef as the key (child moRef not available at start) and the
    * target for the corresponding event is RP parent. In all other cases, target
    * moRef is RP itself.
    * @param event
    * @return target moRef
    */
   protected static ManagedObjectReference getEventTargetMoRef(ResourcePoolEvent event) {
      if (event instanceof ResourcePoolCreatedEvent) {
         ResourcePoolCreatedEvent rpCreatedEvent = (ResourcePoolCreatedEvent) event;
         if (rpCreatedEvent != null) {
            return rpCreatedEvent.getParent().getResourcePool();
         }
      } else if (event.getResourcePool() != null) {
         return event.getResourcePool().getResourcePool();
      }
      return null;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getChildren()
    *
    * This function is used to explore RPs in the wild while they
    * are being concurrently created/deleted to find candidates for
    * RBs. Skip over stale/deleted RPs.
    */
   @Override
   public List<VcResourcePool> getChildren() {
      return VcCache.getPartialList(getChildRps(), getMoRef());
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getChildVMs()
    */
   @Override
   public List<VcVirtualMachine> getChildVMs() {
      return VcCache.getPartialList(childVMs, getMoRef());
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#importVApp(com.vmware.vim.binding.vim.ImportSpec)
    */
   @Override
   public HttpNfcLease importVApp(ImportSpec spec) throws Exception {
      AuAssert.check(VcContext.isInTaskSession());
      ResourcePool rp = getManagedObject();
      VcDatacenter dc = getVcCluster().getDatacenter();
      return MoUtil.getManagedObject(rp.importVApp(spec, dc.getVmFolderMoRef(), null));
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#createVm(com.vmware.vim.binding.vim.vm.ConfigSpec, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask createVm(final ConfigSpec config, final IVcTaskCallback callback)
   throws Exception {
      final VcDatacenter dc = getVcCluster().getDatacenter();
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            final Folder vmFolder = dc.getVmFolder();
            return new VcTask(TaskType.CreateVm,
                  vmFolder.createVm(config, moRef, null), callback);
         }
      });
      logger.debug("create_vm task " + "VM " + config.getName() +
            " under " + this + " created");
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#createVm(com.vmware.vim.binding.vim.vm.ConfigSpec)
    */
   @Override
   public VcVirtualMachine createVm(ConfigSpec config) throws Exception {
      VcTask task = createVm(config, VcCache.getRefreshVcTaskCB(this));
      VcVirtualMachine vm = (VcVirtualMachine) task.waitForCompletion();
      return vm;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#createVm(com.vmware.vim.binding.vim.vm.ConfigSpec, com.vmware.aurora.vc.VcDatastore)
    */
   @Override
   public VcVirtualMachine createVm(ConfigSpec config, VcDatastore ds)
         throws Exception {
      String vmPathName = String.format("[%s]", ds.getURLName());
      FileInfo info = new com.vmware.vim.binding.impl.vim.vm.FileInfoImpl();
      info.setVmPathName(vmPathName);
      AuAssert.check(config.getFiles() == null);
      config.setFiles(info);
      return createVm(config);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#destroyVm(java.lang.String)
    */
   @Override
   public void destroyVm(String vmName) throws Exception {
      for (ManagedObjectReference vm : childVMs) {
         VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vm);
         if (vcVm != null) {
            String vcVmName = vcVm.getName();
            if (vcVmName != null && vmName.equals(vcVmName)) {
               vcVm.destroy();
               return;
            }
         }
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#destroyChildren()
    */
   @Override
   public void destroyChildren() throws Exception {
      AuAssert.check(VcContext.isInTaskSession());
      ResourcePool rp = getManagedObject();
      rp.destroyChildren();
      for (ManagedObjectReference rpRef : getChildRps()) {
         VcCache.purge(rpRef);
      }
      update();
   }

   /**
    * Remove this resource pool.
    * @param callback (optional) task callback
    * @throws Exception
    */
   private VcTask destroy(final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            final ResourcePool rp = getManagedObject();
            return new VcTask(TaskType.DestroyRp, rp.destroy(), callback);
         }
      });

      logger.debug("destroy_rp task RP " + this + " created");
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#destroy()
    */
   @Override
   public void destroy() throws Exception {
      final ManagedObjectReference oldParent = parent;
      try {
         VcTask task = destroy(new IVcTaskCallback() {
            @Override
            public void completeCB(VcTask task) {
               VcCache.purge(getMoRef());
               VcCache.refresh(oldParent);
            }
            @Override
            public void syncCB() {
               VcCache.sync(oldParent);
            }});
         task.waitForCompletion();
      } catch (ManagedObjectNotFound e) {
         logger.info("cannot destroy " + this + ", not found.");
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#updateConfig(java.lang.String, com.vmware.vim.binding.vim.ResourceAllocationInfo, com.vmware.vim.binding.vim.ResourceAllocationInfo)
    */
   @Override
   public void updateConfig(final String name,
         final ResourceAllocationInfo cpuAllocation,
         final ResourceAllocationInfo memAllocation) throws Exception {
      VcContext.getTaskMgr().execPseudoTask("ResourcePool.updateConfig",
            VcEventType.ResourcePoolReconfigured, getMoRef(),
            new IVcPseudoTaskBody() {
         @Override
         public ManagedObjectReference body() throws Exception {
            final ResourcePool rp = getManagedObject();
            ResourceConfigSpec spec = null;
            if (cpuAllocation != null || memAllocation != null) {
               spec = new ResourceConfigSpecImpl(
                     null, null, null, cpuAllocation, memAllocation);
               }
            rp.updateConfig(MoUtil.toURLString(name), spec);
            update();
            VcCache.refresh(parent);  // to refresh its parent's cpu/mem usage
            return getMoRef();
         }
      });
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#updateConfig(java.lang.String, com.vmware.vim.binding.vim.ResourceAllocationInfo, com.vmware.vim.binding.vim.ResourceAllocationInfo)
    */
   @Override
   public void updateVAppConfig(final VAppConfigSpec spec) throws Exception {
      // We're not using pseudotask but invoking one time synchronously as this is a short call
      // and updateVAppConfig also does not generate a specific event. This is also a one-time
      // call if the version does not match the CMS version, which will happen after vum update.
      final VirtualApp vapp = (VirtualApp)getManagedObject();
      vapp.updateVAppConfig(spec);
      update();
   }


   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getCpuAllocationInfo()
    */
   @Override
   public ResourceAllocationInfo getCpuAllocationInfo() {
      return cpuAlloc;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getMemAllocationInfo()
    */
   @Override
   public ResourceAllocationInfo getMemAllocationInfo() {
      return memAlloc;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getCpuUsageInfo()
    */
   @Override
   public ResourceUsage getCpuUsageInfo() {
      return cpuUsage;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getMemUsageInfo()
    */
   @Override
   public ResourceUsage getMemUsageInfo() {
      return memUsage;
   }

   @Override
   public boolean isRootRP() {
      return !(MoUtil.isOfType(parent, ResourcePool.class) ||
               MoUtil.isOfType(parent, VirtualApp.class));
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcResourcePool#getName()
    */
   @Override
   public String getName() {
      return isRootRP() ? getPath() :
             MoUtil.fromURLString(name);
   }
}
