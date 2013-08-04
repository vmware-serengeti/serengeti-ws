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

import java.util.ArrayList;
import java.util.List;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.AuAssert;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.Datastore.HostMount;
import com.vmware.vim.binding.vim.Datastore.Summary.MaintenanceModeState;
import com.vmware.vim.binding.vim.StoragePod;
import com.vmware.vim.binding.vim.host.VmfsDatastoreInfo;
import com.vmware.vim.binding.vim.host.VmfsVolume;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public interface VcDatastore extends VcObject {

   abstract String getName();

   /**
    * @return name already in URL format as returned from VLSI
    */
   abstract String getURLName();

   /**
    * @return the datacenter of this datastore
    */
   abstract VcDatacenter getDatacenter();

   abstract ManagedObjectReference getDatacenterMoRef();

   abstract String getStorageType();

   abstract long getCapacity();

   abstract long getFreeSpace();

   abstract String getUrl();

   abstract boolean isAccessible();

   abstract boolean isInNormalMode();

   abstract boolean isVmfs();

   abstract String getVmfsVolumeVersion();

   abstract boolean isSupported();

   abstract boolean isSameDatastore(String datastoreId);

   abstract boolean isInStoragePod();

   abstract public boolean isSupportedVmfsVersion();

   abstract public boolean isLocal();

   abstract List<VcHost> getHosts();
}

@SuppressWarnings("serial")
class VcDatastoreImpl extends VcObjectImpl implements VcDatastore {
   private Datastore.Summary summary;
   private Datastore.Info info;
   private ManagedObjectReference datacenter;
   private boolean inStoragePod;
   private Datastore.HostMount[] hosts;

   @Override
   protected void update(ManagedObject mo) {
      Datastore ds = (Datastore) mo;
      summary = ds.getSummary();
      info = ds.getInfo();
      datacenter = MoUtil.getAncestorMoRef(ds.getParent(), Datacenter.class);
      inStoragePod = MoUtil.isOfType(ds.getParent(), StoragePod.class);
      hosts = ds.getHost();
   }

   protected VcDatastoreImpl(Datastore ds) throws Exception {
      super(ds);
      update(ds);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#getName()
    */
   @Override
   public String getName() {
      return MoUtil.fromURLString(summary.getName());
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#getURLName()
    */
   @Override
   public String getURLName() {
      return summary.getName();
   }

   @Override
   public String toString() {
      return String.format("DS[%s]", getName());
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#getDatacenter()
    */
   @Override
   public VcDatacenter getDatacenter() {
      return VcCache.get(datacenter);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#getDatacenterMoRef()
    */
   @Override
   public ManagedObjectReference getDatacenterMoRef() {
      return datacenter;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#getStorageType()
    */
   @Override
   public String getStorageType() {
      return summary.getType();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#getCapacity()
    */
   @Override
   public long getCapacity() {
      return summary.getCapacity();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#getFreeSpace()
    */
   @Override
   public long getFreeSpace() {
      return summary.getFreeSpace();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#getUrl()
    */
   @Override
   public String getUrl() {
      return summary.getUrl();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#isAccessible()
    */
   @Override
   public boolean isAccessible() {
      return summary.isAccessible();
   }

   @Override
   public boolean isInNormalMode() {
      String strMode = summary.getMaintenanceMode();
      if (strMode != null) {
         MaintenanceModeState mode = MaintenanceModeState.valueOf(strMode);
         if (mode == MaintenanceModeState.normal) {
            return true;
         }
      }
      return false;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#isVmfs()
    */
   @Override
   public boolean isVmfs() {
      return "VMFS".equals(getStorageType());
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#getVmfsVolumeVersion()
    */
   @Override
   public String getVmfsVolumeVersion() {
      AuAssert.check(isVmfs());
      VmfsDatastoreInfo vmfsInfo = (VmfsDatastoreInfo) info;
      VmfsVolume vmfsVolume = vmfsInfo.getVmfs();
      return vmfsVolume.getVersion();
   }

   private int getVmfsVolumeMajorVersion() {
      AuAssert.check(isVmfs());
      VmfsDatastoreInfo vmfsInfo = (VmfsDatastoreInfo) info;
      return vmfsInfo.getVmfs().getMajorVersion();
   }

   public boolean isSupportedVmfsVersion() {
      AuAssert.check(isVmfs());
      int majorVersion = getVmfsVolumeMajorVersion();
      return majorVersion > 3
            || (majorVersion == 3 && Configuration.getBoolean(
                  "vc.support_vmfs3", false));
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcDatastore#isSupported()
    */
   @Override
   public boolean isSupported() {
      return VcUtil.getIncompatReasonsForDatastore(this).isEmpty();
   }

   @Override
   public boolean isSameDatastore(String datastoreId) {
      return getId().equals(datastoreId);
   }

   @Override
   public boolean isInStoragePod() {
      return inStoragePod;
   }

   @Override
   public boolean isLocal() {
      return (hosts.length == 1);
   }

   @Override
   public List<VcHost> getHosts() {
      List<VcHost> hosts = new ArrayList<VcHost>();
      for (HostMount host : this.hosts) {
         VcHost h = VcCache.get(host.getKey());
         hosts.add(h);
      }
      return hosts;
   }
}
