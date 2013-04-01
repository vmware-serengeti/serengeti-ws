/* ***************************************************************************
 * Copyright (c) 2011-2012 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/

package com.vmware.aurora.vc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vmware.aurora.util.AuAssert;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public interface VcHost extends VcObject {

   abstract String getName();

   /**
    * @return the cluster of this host
    * @throws Exception
    */
   abstract VcCluster getCluster();

   /**
    * @return the VMs on this host
    * @throws Exception
    */
   abstract List<VcVirtualMachine> getVMs() throws Exception;

   /**
    * @return a list of networks on this host
    */
   abstract List<VcNetwork> getNetworks();

   /**
    * Return a list of all datastores (shared by all hosts and not).
    * @return all datastores
    * @throws Exception
    */
   abstract List<VcDatastore> getDatastores();

}

@SuppressWarnings("serial")
class VcHostImpl extends VcObjectImpl implements VcHost {
   private String name;

   // either a folder or datacenter
   private ManagedObjectReference parent;
   private ManagedObjectReference[] network;
   private ManagedObjectReference[] datastore;

   protected VcHostImpl(HostSystem host) throws Exception {
      super(host);
      update(host);
   }

   @Override
   protected void update(ManagedObject mo) throws Exception {
      AuAssert.check(this.moRef.equals(mo._getRef()));
      HostSystem host = (HostSystem)mo;
      name = host.getName();
      parent = checkReady(host.getParent());
      network = checkReady(host.getNetwork());
      datastore = checkReady(host.getDatastore());
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
      return String.format("HOST[%s]()",
            name);
   }


   @Override
   public List<VcNetwork> getNetworks() {
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
   public List<VcDatastore> getDatastores() {
      return VcCache.<VcDatastore>getPartialList(Arrays.asList(datastore), getMoRef());
   }




   @Override
   public VcCluster getCluster() {
      return VcCache.get(
            MoUtil.getAncestorMoRef(parent, ClusterComputeResource.class));
   }



   @Override
   public List<VcVirtualMachine> getVMs() throws Exception {
      // TODO Auto-generated method stub
      return null;
   }
}
