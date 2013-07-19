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

import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;

/**
 * Utility Class for the NetworkSchema
 *
 * @author sridharr
 *
 */
public class NetworkSchemaUtil {

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
         AuAssert.check(vN != null);
         VirtualDevice nic = null;
         if (network.nicLabel != null) {
            nic = vcVm.getDeviceByLabel(network.nicLabel);
            if (nic != null) {
               // drop existing network to replace with vmxnet3 nic
               changes.add(VmConfigUtil.removeDeviceSpec(nic));
            }
         }
         // Add new networks
         VirtualDeviceSpec deviceSpec = VmConfigUtil.createNetworkDevice(
               VmConfigUtil.EthernetControllerType.VMXNET3, network.nicLabel, vN);
         changes.add(deviceSpec);
      }

      spec.setDeviceChange(changes.toArray(new VirtualDeviceSpec[changes.size()]));
   }
}
