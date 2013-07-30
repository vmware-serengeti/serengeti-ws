/* ***************************************************************************
 * Copyright (c) 2011-2012 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/

package com.vmware.aurora.vc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcTask.TaskType;
import com.vmware.aurora.vc.VcTaskMgr.IVcTaskBody;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.ComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.cluster.ConfigInfoEx;
import com.vmware.vim.binding.vim.cluster.ConfigSpecEx;
import com.vmware.vim.binding.vim.cluster.DasVmConfigInfo;
import com.vmware.vim.binding.vim.host.ConnectInfo.DatastoreInfo;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public interface VcCluster extends VcObject {

   abstract String getName();

   /**
    * @return the datacenter of this cluster
    * @throws Exception
    */
   abstract VcDatacenter getDatacenter();

   /**
    * @return the ESX hosts in this cluster
    * @throws Exception
    */
   abstract List<VcHost> getHosts() throws Exception;

   /**
    * @return a list of networks shared by all hosts in the cluster
    */
   abstract List<VcNetwork> getSharedNetworks();

   /**
    * Return a list of all networks (shared by all hosts and not).
    *
    * @return all networks
    * @throws Exception
    */
   abstract List<VcNetwork> getAllNetworks();

   /**
    * Get the given named network
    *
    * @param name
    *           name of the network
    * @return the network, null if not found
    */
   abstract VcNetwork getNetwork(String name);

   /**
    * Get a network connected by all hosts.
    *
    * @param name
    *           name of the network
    * @return the network, null if not found
    * @throws Exception
    */
   abstract VcNetwork getSharedNetwork(String name);

   /**
    * @return a list of datastores shared by all hosts in the cluster
    */
   abstract List<VcDatastore> getSharedDatastores();

   /**
    * Return a list of all datastores (shared by all hosts and not).
    *
    * @return all datastores
    * @throws Exception
    */
   abstract List<VcDatastore> getAllDatastores();

   /**
    * Get the given named datastore
    *
    * @param name
    *           name of the datastore
    * @return the datastore, null if not found
    */
   abstract VcDatastore getDatastore(String name);

   /**
    * @return the root RP of this cluster
    * @throws Exception
    */
   abstract VcResourcePool getRootRP();

   /**
    * Find the RP corresponding to the path.
    *
    * @param path
    * @return the resource pool
    * @throws Exception
    */
   abstract VcResourcePool searchRP(String path) throws Exception;

   /**
    * @return all resource pools qualified for resource bundle
    * @throws Exception
    */
   abstract List<VcResourcePool> getQualifiedRPs() throws Exception;

   /**
    * @return all leaf resource pools in this cluster
    * @throws Exception
    */
   abstract List<VcResourcePool> getAllRPs() throws Exception;

   /**
    * @return Whether High Availability is enabled for this cluster
    * @throws Exception
    **/
   abstract VcClusterConfig getConfig();

   abstract List<String> getHADRSIncompatReasonsForAlert();

   abstract List<String> getHADRSIncompatReasonsForRBQual();

   abstract DasVmConfigInfo[] getVmConfigInfo();

   /**
    * Reconfigure cluster settings
    *
    * @param spec
    *           specification of change
    * @param callback
    *           (optional) call-back function for the task
    * @return a task object for the reconfiguration operation
    * @throws Exception
    */
   abstract VcTask reconfigure(final ConfigSpecEx spec,
         final IVcTaskCallback callback) throws Exception;

   /**
    * Reconfigure cluster settings. Blocks until change is complete, returns
    * only success/failure.
    *
    * @param spec
    *           specification of the change
    * @return true on success, false on failure
    * @throws Exception
    */
   abstract boolean reconfigure(final ConfigSpecEx spec) throws Exception;
}

@SuppressWarnings("serial")
class VcClusterImpl extends VcObjectImpl implements VcCluster {
   private ComputeResource.Summary summary;
   private String name;

   // either a folder or datacenter
   private ManagedObjectReference parent;
   private ManagedObjectReference[] host;
   private ManagedObjectReference[] network;
   private ManagedObjectReference[] datastore;
   private ManagedObjectReference resourcePool;
   private List<ManagedObjectReference> sharedNetwork;
   private List<ManagedObjectReference> sharedDatastore;
   private VcClusterConfig config;
   private List<String> haDrsIncompatReasonsForRBQual;
   private List<String> haDrsIncompatReasonsForAlert;

   // XXX : TODO - move this field to VcClusterConfig
   private transient DasVmConfigInfo[] vmConfigInfo = null;

   /**
    * Sync request to update networks, datastores and/or RPs.
    */
   protected static class SyncRequest extends VcInventory.SyncRequest {
      SyncRequest(ManagedObjectReference moRef,
            VcDatacenterImpl.SyncRequest parent) {
         super(moRef, parent);
      }

      @Override
      protected void syncChildObjects(VcObject obj) {
         if (obj instanceof VcClusterImpl) {
            VcClusterImpl cluster = (VcClusterImpl) obj;
            if (syncSet.contains(VcObjectType.VC_NETWORK)) {
               for (ManagedObjectReference net : cluster.network) {
                  VcCache.loadAsync(net);
               }
            }
            if (syncSet.contains(VcObjectType.VC_DATASTORE)) {
               for (ManagedObjectReference ds : cluster.datastore) {
                  VcCache.loadAsync(ds);
               }
            }
            if (syncSet.contains(VcObjectType.VC_RP)) {
               add(new VcResourcePoolImpl.SyncRequest(cluster.resourcePool,
                     this));
            }
            if (syncSet.contains(VcObjectType.VC_HOST)) {
               for (ManagedObjectReference h : cluster.host) {
                  VcCache.loadAsync(h);
               }
            }
         }
      }
   }

   protected VcClusterImpl(ClusterComputeResource cluster) throws Exception {
      super(cluster);
      update(cluster);
   }

   private void updateRP(ManagedObjectReference rp) {
      if (!rp.equals(resourcePool)) {
         if (resourcePool != null) {
            VcCache.loadAsync(resourcePool);
         }
         resourcePool = rp;
         VcCache.loadAsync(rp);
      }
   }

   @Override
   protected void update(ManagedObject mo) throws Exception {
      AuAssert.check(this.moRef.equals(mo._getRef()));
      ClusterComputeResource cluster = (ClusterComputeResource) mo;
      name = cluster.getName();
      summary = checkReady(cluster.getSummary());
      parent = checkReady(cluster.getParent());
      host = checkReady(cluster.getHost());
      network = checkReady(cluster.getNetwork());
      datastore = checkReady(cluster.getDatastore());
      sharedNetwork = getSharedNetworkInt();
      sharedDatastore = getSharedDatastoreInt();
      ConfigInfoEx configEx =
            (ConfigInfoEx) checkReady(cluster.getConfigurationEx());
      config = VcClusterConfig.create(configEx);
      // Update the transient dasVmConfigInfo
      vmConfigInfo = configEx.getDasVmConfig();
      haDrsIncompatReasonsForRBQual =
            VcClusterConfig.skipHADRSCheck() ? new ArrayList<String>() : config
                  .getHADRSIncompatReasons(false); // Lenient check for a minimal set of config
      haDrsIncompatReasonsForAlert =
            VcClusterConfig.skipHADRSCheck() ? new ArrayList<String>() : config
                  .getHADRSIncompatReasons(true); // Strict check, alert on each config problem
      updateRP(checkReady(cluster.getResourcePool()));
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#getName()
    */
   @Override
   public String getName() {
      return MoUtil.fromURLString(name);
   }

   @Override
   public String toString() {
      return String.format("CLUSTER[%s](cpu=%d,mem=%d,#host=%d)", name,
            summary.getTotalCpu(), summary.getTotalMemory(), host.length);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#getDatacenter()
    */
   @Override
   public VcDatacenter getDatacenter() {
      return VcCache.get(MoUtil.getAncestorMoRef(parent, Datacenter.class));
   }

   /*
    * (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#getHosts()
    */
   @Override
   public List<VcHost> getHosts() throws Exception {
      List<VcHost> results = new ArrayList<VcHost>();
      for (ManagedObjectReference ref : host) {
         VcHost h = VcCache.get(ref);
         results.add(h);
      }
      return results;
   }

   private List<ManagedObjectReference> getSharedNetworkInt() throws Exception {
      List<HostSystem> hostList = MoUtil.getManagedObjects(host);
      List<ManagedObjectReference> results =
            new ArrayList<ManagedObjectReference>();
      ManagedObjectReference[] candidateList = null;
      HashMap<ManagedObjectReference, Integer> map =
            new HashMap<ManagedObjectReference, Integer>();
      if (hostList.size() == 0) {
         return results;
      }

      for (HostSystem h : hostList) {
         ManagedObjectReference[] netMorefs = h.getNetwork();
         if (candidateList == null) {
            candidateList = netMorefs;
         }
         for (ManagedObjectReference mo : netMorefs) {
            Integer count = map.get(mo);
            if (count != null) {
               map.put(mo, count + 1);
            } else {
               map.put(mo, Integer.valueOf(1));
            }
         }
      }

      // collect those in the map with as many counts as the number of hosts
      for (ManagedObjectReference mo : candidateList) {
         if (map.get(mo).equals(hostList.size())) {
            results.add(mo);
         }
      }
      return results;
   }

   @Override
   public List<VcNetwork> getSharedNetworks() {
      List<VcNetwork> results = new ArrayList<VcNetwork>();
      for (ManagedObjectReference ref : sharedNetwork) {
         VcNetwork net = VcCache.get(ref);
         if (!net.isUplink()) {
            results.add(net);
         }
      }
      return results;
   }

   @Override
   public List<VcNetwork> getAllNetworks() {
      List<VcNetwork> results = new ArrayList<VcNetwork>();
      for (ManagedObjectReference ref : network) {
         VcNetwork net = VcCache.get(ref);
         if (!net.isUplink()) {
            results.add(net);
         }
      }
      return results;
   }

   @Override
   public VcNetwork getNetwork(String name) {
      String urlName = MoUtil.toURLString(name);
      for (ManagedObjectReference ref : network) {
         VcNetwork net = VcCache.get(ref);
         if (urlName.equalsIgnoreCase(net.getName())) {
            if (net.isUplink()) {
               return null;
            }
            return net;
         }
      }
      return null;
   }

   @Override
   public VcNetwork getSharedNetwork(String name) {
      String urlName = MoUtil.toURLString(name);
      for (ManagedObjectReference ref : sharedNetwork) {
         VcNetwork net = VcCache.get(ref);
         if (urlName.equalsIgnoreCase(net.getName())) {
            if (net.isUplink()) {
               return null;
            }
            return net;
         }
      }
      return null;
   }

   private List<ManagedObjectReference> getSharedDatastoreInt()
         throws Exception {
      AuAssert.check(VcContext.isInSession());
      List<HostSystem> hostList = MoUtil.getManagedObjects(host);
      List<ManagedObjectReference> results =
            new ArrayList<ManagedObjectReference>();
      DatastoreInfo[] candidateList = null;
      HashMap<String, Integer> map = new HashMap<String, Integer>();
      if (hostList.size() == 0) {
         return results;
      }
      for (HostSystem h : hostList) {
         DatastoreInfo[] info = h.queryConnectionInfo().getDatastore();
         if (candidateList == null) {
            candidateList = info;
         }
         for (DatastoreInfo n : info) {
            String name = n.getSummary().getName();
            Integer count = map.get(name);
            if (count != null) {
               map.put(name, count + 1);
            } else {
               map.put(name, Integer.valueOf(1));
            }
         }
      }

      // collect those in the map with as many counts as the number of hosts
      for (DatastoreInfo n : candidateList) {
         if (map.get(n.getSummary().getName()).equals(hostList.size())) {
            results.add(n.getSummary().getDatastore());
         }
      }
      return results;
   }

   @Override
   public List<VcDatastore> getSharedDatastores() {
      return VcCache.<VcDatastore> getPartialList(sharedDatastore, getMoRef());
   }

   @Override
   public List<VcDatastore> getAllDatastores() {
      return VcCache.<VcDatastore> getPartialList(Arrays.asList(datastore),
            getMoRef());
   }


   @Override
   public VcDatastore getDatastore(String name) {
      String urlName = MoUtil.toURLString(name);
      for (ManagedObjectReference ref : datastore) {
         VcDatastore ds = VcCache.get(ref);
         if (urlName.equalsIgnoreCase(ds.getName())) {
            if (!ds.isAccessible()) {
               return null;
            }
            return ds;
         }
      }
      return null;
   }


   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#getRootRP()
    */
   @Override
   public VcResourcePool getRootRP() {
      return VcCache.get(resourcePool);
   }

   private VcResourcePoolImpl getRootRPImpl() {
      return VcCache.get(resourcePool);
   }

   @Override
   public VcResourcePool searchRP(final String path) throws Exception {
      try {
         return getRootRPImpl().searchRP(path);
      } catch (VcException e) {
         if (!e.isINVALID_MOREF() && !e.isMOREF_NOTREADY()) {
            throw e;
         } else {
            return null;
         }
      }
   }

   @Override
   public List<VcResourcePool> getQualifiedRPs() throws Exception {
      try {
         return getRootRPImpl().getQualifiedRPs();
      } catch (VcException e) {
         if (!e.isINVALID_MOREF() && !e.isMOREF_NOTREADY()) {
            throw e;
         } else {
            return new ArrayList<VcResourcePool>();
         }
      }
   }

   @Override
   public List<VcResourcePool> getAllRPs() throws Exception {
      try {
         return getRootRPImpl().getAllRPs();
      } catch (VcException e) {
         if (!e.isINVALID_MOREF() && !e.isMOREF_NOTREADY()) {
            throw e;
         } else {
            return new ArrayList<VcResourcePool>();
         }
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#getConfig()
    */
   @Override
   public VcClusterConfig getConfig() {
      return config;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#getHADRSIncompatReasonsForAlert()
    */
   @Override
   public List<String> getHADRSIncompatReasonsForAlert() {
      return haDrsIncompatReasonsForAlert;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#getHADRSIncompatReasonsForRBQual()
    */
   @Override
   public List<String> getHADRSIncompatReasonsForRBQual() {
      return haDrsIncompatReasonsForRBQual;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#getVmConfigInfo()
    */
   @Override
   public DasVmConfigInfo[] getVmConfigInfo() {
      return vmConfigInfo;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#reconfigure(com.vmware.vim.binding.vim.cluster.ConfigSpec, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask reconfigure(final ConfigSpecEx spec,
         final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            final ClusterComputeResource cluster = getManagedObject();
            return new VcTask(TaskType.ReconfigCluster, cluster.reconfigureEx(
                  spec, true), callback);
         }
      });
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcCluster#reconfigure(com.vmware.vim.binding.vim.cluster.ConfigSpec)
    */
   @Override
   public boolean reconfigure(final ConfigSpecEx spec) throws Exception {
      VcTask task = reconfigure(spec, VcCache.getRefreshVcTaskCB(this));
      task.waitForCompletion();
      return task.taskCompleted();
   }
}
