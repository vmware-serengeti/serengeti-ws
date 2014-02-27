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

package com.vmware.aurora.composition;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDeviceSpecImpl;
import com.vmware.vim.binding.vim.vm.ConfigSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualEthernetCard;

/**
 * Utility Class for the NetworkSchema
 *
 * @author sridharr
 *
 */
public class NetworkSchemaUtil {
   private static final Logger logger = Logger
         .getLogger(NetworkSchemaUtil.class);

   public static NetworkSchema getSchema(String xmlSchema) throws JAXBException {
      return SchemaUtil.getSchema(xmlSchema, NetworkSchema.class);
   }

   public static NetworkSchema getSchema(File file) throws JAXBException {
      return SchemaUtil.getSchema(file, NetworkSchema.class);
   }

   public static void setNetworkSchema(ConfigSpecImpl spec, VcCluster cluster,
         NetworkSchema networkSchema, VcVirtualMachine vcVm) throws Exception {
      List<VirtualDeviceSpec> changes = new ArrayList<VirtualDeviceSpec>();

      for (NetworkSchema.Network network : networkSchema.networks) {
         VcNetwork vN = cluster.getNetwork(network.vcNetwork);
         if (vN == null) {
            logger.error("Network " + network.vcNetwork + " is not defined on cluster " + cluster.getName());
            throw new Exception("Network " + network.vcNetwork + " is not defined on cluster " + cluster.getName());
         }

         if (network.nicLabel != null
               && vcVm.getDeviceByLabel(network.nicLabel) != null) {
            // Edit existing networks
            changes.add(vcVm.reconfigNetworkSpec(network.nicLabel, vN));
         } else {
            // Add new networks
            VirtualDeviceSpec deviceSpec =
                  VmConfigUtil.createNetworkDevice(
                        VmConfigUtil.EthernetControllerType.VMXNET3,
                        network.nicLabel, vN);
            changes.add(deviceSpec);
         }

      }

      spec.setDeviceChange(changes.toArray(new VirtualDeviceSpec[changes.size()]));
   }

   public static void copyMacAddresses(ConfigSpec configSpec,
         VcVirtualMachine parentVm, VcVirtualMachine childVm,
         NetworkSchema networkSchema) {
      List<VirtualDeviceSpec> changes = new ArrayList<VirtualDeviceSpec>();

      for (NetworkSchema.Network network : networkSchema.networks) {
         VirtualDevice nic = null;
         String macAddr = null;
         if (network.nicLabel != null) {
            nic = parentVm.getDeviceByLabel(network.nicLabel);
            if (nic == null) {
               continue;
            }
            macAddr = ((VirtualEthernetCard) nic).getMacAddress();
            if (macAddr == null) {
               continue;
            }
            logger.info("get parent vm's mac address " + macAddr);

            // edit mac address
            nic = childVm.getDeviceByLabel(network.nicLabel);

            if (nic == null) {
               logger.info("child vm does not have nic " + network.nicLabel);
               continue;
            }

            // set mac address
            ((VirtualEthernetCard) nic).setAddressType("Manual");
            ((VirtualEthernetCard) nic).setMacAddress(macAddr);

            VirtualDeviceSpec deviceSpec = new VirtualDeviceSpecImpl();
            deviceSpec.setOperation(VirtualDeviceSpec.Operation.edit);
            deviceSpec.setDevice(nic);

            changes.add(deviceSpec);
         }
      }

      configSpec.setDeviceChange(changes.toArray(new VirtualDeviceSpec[changes
            .size()]));
   }
}