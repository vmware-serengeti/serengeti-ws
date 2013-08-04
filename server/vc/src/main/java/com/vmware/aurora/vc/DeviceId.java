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

import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.vim.binding.vim.vm.device.VirtualController;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualSCSIController;

/**
 * A unique identifier for a virtual controller or disk device
 * in a VM configuration.
 *
 * If a unitNum is null, a DeviceId object identifies a virtual controller.
 * Otherwise, it points to an end-point drive.
 *
 */
public class DeviceId {
   public String controllerType;
   public Integer busNum;
   public Integer unitNum;

   public DeviceId(String typeName, Integer busNum, Integer unitNum) {
      controllerType = typeName;
      this.busNum = busNum;
      this.unitNum = unitNum;
   }

   public DeviceId(String deviceId) {
      String parts[] = deviceId.split(":");
      AuAssert.check(parts.length == 2 || parts.length == 3);
      controllerType = parts[0];
      getTypeClass(); // Validate the controller type
      busNum = new Integer(parts[1]);
      unitNum = parts.length > 2 ? new Integer(parts[2]) : null;
   }

   public DeviceId(VirtualDevice controller, VirtualDevice device)
   {
      controllerType = null;
      for (VmConfigUtil.ScsiControllerType type: VmConfigUtil.ScsiControllerType.values()) {
         if (type.infClass.isInstance(controller)) {
            controllerType = type.infClass.getSimpleName();
            break;
         }
      }
      if (controllerType == null) {
         throw VcException.UNSUPPORTED_CONTROLLER_TYPE(controllerType);
      }
      busNum = ((VirtualController)controller).getBusNumber();
      unitNum = device.getUnitNumber();
   }

   public Class<? extends VirtualSCSIController> getTypeClass() {
      for (VmConfigUtil.ScsiControllerType type: VmConfigUtil.ScsiControllerType.values()) {
         if (type.infClass.getSimpleName().equals(controllerType)) {
            return type.infClass;
         }
      }
      throw VcException.UNSUPPORTED_CONTROLLER_TYPE(controllerType);
   }

   static public boolean isSupportedController(VirtualDevice controller) {
      for (VmConfigUtil.ScsiControllerType type: VmConfigUtil.ScsiControllerType.values()) {
         if (type.infClass.isInstance(controller)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public String toString() {
      // format string can handle null -- it will print "null"
      if (unitNum != null) {
         return String.format("%s:%d:%d", controllerType, busNum, unitNum);
      } else {
         return String.format("%s:%d", controllerType, busNum);
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof DeviceId) {
         DeviceId id = (DeviceId)obj;

         return (controllerType == id.controllerType || controllerType != null && controllerType.equals(id.controllerType))
             && (busNum == id.busNum || busNum != null && busNum.equals(id.busNum))
             && (unitNum == id.unitNum || unitNum != null && unitNum.equals(id.unitNum));
      }
      return false;
   }

   @Override
   public int hashCode() {
       return toString().hashCode();
   }
}
