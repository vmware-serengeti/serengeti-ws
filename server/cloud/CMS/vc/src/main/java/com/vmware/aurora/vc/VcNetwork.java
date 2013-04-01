/* ***************************************************************************
 * Copyright (c) 2011 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/

package com.vmware.aurora.vc;

import com.vmware.aurora.util.AuAssert;
import com.vmware.vim.binding.impl.vim.dvs.PortConnectionImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualEthernetCardImpl;
import com.vmware.vim.binding.vim.DistributedVirtualSwitch;
import com.vmware.vim.binding.vim.Network;
import com.vmware.vim.binding.vim.dvs.DistributedVirtualPortgroup;
import com.vmware.vim.binding.vim.dvs.PortConnection;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public interface VcNetwork extends VcObject {

   abstract String getName();

   abstract boolean isUplink();

   /**
    * @return VC network backing object for virtual NIC devices
    */
   abstract VirtualDevice.BackingInfo getBackingInfo();

   /**
    * Gets the name of the ip pool attached to the network.
    * @return The name of the attached ip pool. Null if no ip pool is attached.
    */
   abstract String getIpPoolName();

}

@SuppressWarnings("serial")
class VcNetworkImpl extends VcObjectImpl implements VcNetwork {
   private Network.Summary summary;
   // The following fields are not null iff this is a DV portgroup.
   private String portgroupKey = null;
   private String switchUuid = null;
   private boolean isUplink = false;

   @Override
   protected void update(ManagedObject mo) throws Exception {
      Network net = (Network)mo;
      summary = net.getSummary();
      portgroupKey = null;
      switchUuid = null;
      isUplink = false;
      if (net instanceof DistributedVirtualPortgroup) {
         DistributedVirtualPortgroup pg = (DistributedVirtualPortgroup)net;
         DistributedVirtualSwitch dvs =
            MoUtil.getManagedObject(pg.getConfig().getDistributedVirtualSwitch());
         for (ManagedObjectReference ref : dvs.getConfig().getUplinkPortgroup()) {
            if (ref.equals(moRef)) {
               isUplink = true;
               break;
            }
         }
         portgroupKey = pg.getKey();
         switchUuid = dvs.getUuid();
      }
   }

   protected VcNetworkImpl(Network net) throws Exception {
      super(net);
      update(net);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcNetwork#getName()
    */
   @Override
   public String getName() {
      return MoUtil.fromURLString(summary.getName());
   }

   private boolean isDvPortGroup() {
      return portgroupKey != null;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcNetwork#isUplink()
    */
   @Override
   public boolean isUplink() {
      return isUplink;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcNetwork#getBackingInfo()
    */
   @Override
   public VirtualDevice.BackingInfo getBackingInfo() {
      AuAssert.check(!isUplink());
      if (isDvPortGroup()) {
         PortConnection conn = new PortConnectionImpl();
         conn.setPortgroupKey(portgroupKey);
         conn.setSwitchUuid(switchUuid);
         return new VirtualEthernetCardImpl.DistributedVirtualPortBackingInfoImpl(conn);
      } else {
         Boolean autoConnect = true;
         return new VirtualEthernetCardImpl.NetworkBackingInfoImpl(
            getName(), autoConnect, null, null);
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcNetwork#getIpPoolName()
    */
   @Override
   public String getIpPoolName() {
      return summary.getIpPoolName();
   }

   public String toString() {
      if (isDvPortGroup()) {
         return String.format("NETPG[%s,id=%s,up=%b]",
               summary.getName(), portgroupKey, isUplink);
      } else {
         return String.format("NET[%s]", summary.getName());
      }
   }
}