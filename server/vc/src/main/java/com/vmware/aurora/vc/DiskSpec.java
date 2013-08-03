/* ***************************************************************************
 * Copyright (c) 2012 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/
package com.vmware.aurora.vc;

import java.io.Serializable;

import com.vmware.aurora.global.DiskSize;

/**
 * <code>DiskSpec</code> defines the specification of a single VMDK disk in DBVM.
 */
@SuppressWarnings("serial")
public class DiskSpec implements Serializable {
   public enum AllocationType {
      THICK,
      ZEROEDTHICK,
      THIN
   }

   private DiskType diskType;
   // External device address, in the format of <DeviceType>:<Bus_Id>:<Unit_Num>
   // For example: VirtualLsiLogicController:0:0
   private String extAddr;
   // Internal file system address, for example: /opt/aurora/data, D:, E:
   private String intAddr;
   // Default storage alpha for this disk
   private double alpha;
   private DiskSize initialSize;
   private boolean externalAddressParsed;
   private String controllerType;
   private int busId;
   private int unitId;

   public DiskSpec(DiskType diskType, String externalAddr, String internalAddr) {
      this(diskType, externalAddr, internalAddr, diskType.getDefaultAlpha());
   }

   public DiskSpec(DiskType diskType, String externalAddr, String internalAddr, double alpha) {
      this.diskType = diskType;
      this.extAddr = externalAddr;
      this.intAddr = internalAddr;
      this.alpha = alpha;
   }

   public DiskType getDiskType() {
      return diskType;
   }

   public String getExternalAddress() {
      return extAddr;
   }

   public DeviceId getDeviceId() {
      return new DeviceId(extAddr);
   }

   public String getInternalAddress() {
      return intAddr;
   }

   public double getAlpha() {
      return alpha;
   }

   private void parseExternalAddress() {
      if (!externalAddressParsed) {
         String[] parsedAddress = extAddr.split(":");
         controllerType = parsedAddress[0];
         busId = Integer.parseInt(parsedAddress[1]);
         unitId = Integer.parseInt(parsedAddress[2]);
         externalAddressParsed = true;
      }
   }

   public int getBusId() {
      parseExternalAddress();
      return busId;
   }

   public int getUnitId() {
      parseExternalAddress();
      return unitId;
   }

   public String getControllerType() {
      parseExternalAddress();
      return controllerType;
   }

   public void setInitialSize(DiskSize initialSize) {
      this.initialSize = initialSize;
   }

   public DiskSize getInitialSize() {
      return initialSize;
   }
}
