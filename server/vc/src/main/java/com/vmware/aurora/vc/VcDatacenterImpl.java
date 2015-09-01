package com.vmware.aurora.vc;

import com.vmware.aurora.util.worker.CmsWorker;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

import java.util.EnumSet;
import java.util.List;

/**
 * Created by xiaoliangl on 7/16/15.
 */
@SuppressWarnings("serial")
class VcDatacenterImpl extends VcObjectImpl implements VcDatacenter {
   private String name;
   private ManagedObjectReference vmFolder;
   private ManagedObjectReference hostFolder;
   private List<ManagedObjectReference> clusters;

   /**
    * Sync request to update clusters.
    */
   protected static class SyncRequest extends VcInventory.SyncRequest {
      SyncRequest(ManagedObjectReference moRef, CmsWorker.WorkQueue queue, boolean forceLoad,
                  EnumSet<VcObjectType> syncSet) {
         super(moRef, queue, forceLoad, syncSet);
      }

      @Override
      protected void syncChildObjects(VcObject obj) {
         if (obj instanceof VcDatacenterImpl) {
            VcDatacenterImpl datacenter = (VcDatacenterImpl)obj;
            for (ManagedObjectReference cluster : datacenter.clusters) {
               add(new VcClusterImpl.SyncRequest(cluster, this));
            }
         }
      }
   }

   public List<ManagedObjectReference> getClusterMoRefs() {
      return clusters;
   }

   private List<ManagedObjectReference> findClusters() throws Exception {
      Folder folder = MoUtil.getManagedObject(hostFolder);
      return MoUtil.getDescendantsMoRef(folder, ClusterComputeResource.class);
   }

   @Override
   protected void update(ManagedObject mo) throws Exception {
      Datacenter dc = (Datacenter)mo;
      this.name = dc.getName();
      this.vmFolder = dc.getVmFolder();
      this.hostFolder = dc.getHostFolder();
      this.clusters = findClusters();
   }

   protected VcDatacenterImpl(Datacenter dc) throws Exception {
      super(dc);
      update(dc);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatacenter#getName()
    */
   @Override
   public String getName() {
      return MoUtil.fromURLString(name);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatacenter#getURLName()
    */
   @Override
   public String getURLName() {
      return name;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatacenter#getVmFolder()
    */
   @Override
   public Folder getVmFolder() throws Exception {
      return MoUtil.getManagedObject(vmFolder);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatacenter#getVmFolderMoRef()
    */
   @Override
   public ManagedObjectReference getVmFolderMoRef() {
      return vmFolder;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatacenter#getClusters()
    */
   @Override
   public List<VcCluster> getVcClusters() {
      // XXX should filter out unqualified cluster: HA & DRS enabled.
      return VcCache.getPartialList(clusters, getMoRef());
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatacenter#getVirtualMachine(java.lang.String)
    */
   @Override
   public VcVirtualMachine getVirtualMachine(String name) throws Exception {
      String urlName = MoUtil.toURLString(name);
      Folder folder = getVmFolder();
      for (VirtualMachine vm : MoUtil.getChildEntity(folder, VirtualMachine.class)) {
         try {
            if (vm.getName().equals(urlName)) {
               return VcCache.get(vm);
            }
         } catch (ManagedObjectNotFound e) {
            // We don't cache VM list, so no need to refresh VcDatacenter.
            logger.info("skip bad vm moref " + vm);
         }
      }
      return null;
   }
}
