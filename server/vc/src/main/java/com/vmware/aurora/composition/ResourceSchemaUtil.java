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

import javax.xml.bind.JAXBException;

import com.vmware.aurora.exception.CommonException;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.bdd.apitypes.LatencyPriority;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.vim.binding.impl.vim.ResourceAllocationInfoImpl;
import com.vmware.vim.binding.impl.vim.SharesInfoImpl;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.vim.ResourceAllocationInfo;
import com.vmware.vim.binding.vim.SharesInfo;
import com.vmware.vim.binding.vim.SharesInfo.Level;

/**
 * Utility Class for the ResourceSchema
 *
 * @author sridharr
 *
 */
public class ResourceSchemaUtil {

   public static ResourceSchema getSchema(String xmlSchema)
         throws JAXBException {
      return SchemaUtil.getSchema(xmlSchema, ResourceSchema.class);
   }

   public static ResourceSchema getSchema(File file) throws JAXBException {
      return SchemaUtil.getSchema(file, ResourceSchema.class);
   }

   public static void setResourceSchema(ConfigSpecImpl spec,
         ResourceSchema resourceSchema) {
      spec.setNumCPUs(resourceSchema.numCPUs);
      VmConfigUtil.setMemoryAndBalloon(spec, resourceSchema.memSize);

      Long unlimited = Long.valueOf(-1);
      SharesInfo.Level shareLevel;
      switch (resourceSchema.priority) {
      case Low:
         shareLevel = Level.low;
         break;
      case Normal:
         shareLevel = Level.normal;
         break;
      case High:
         shareLevel = Level.high;
         break;
      case Automatic:
         shareLevel = Level.normal; // TODO: Automatic implies normal? or...
         break;
      default:
         throw CommonException.INTERNAL();
      }
      SharesInfo shares = new SharesInfoImpl(100, shareLevel);
      spec.setCpuAllocation(new ResourceAllocationInfoImpl(
            resourceSchema.cpuReservationMHz, false /* not expandable */,
            unlimited,
            shares, null));

      if(resourceSchema != null){
         VmConfigUtil
               .setLatencySensitivity(spec, resourceSchema.latencySensitivity);
      }

      if(resourceSchema.latencySensitivity == LatencyPriority.HIGH) {
         spec.setMemoryAllocation(new ResourceAllocationInfoImpl(
               resourceSchema.memSize, false /* not expandable */, unlimited,
               shares, null));
      }else{
         spec.setMemoryAllocation(new ResourceAllocationInfoImpl(
               resourceSchema.memReservationSize, false /* not expandable */, unlimited,
               shares, null));
      }
   }

   public static void setMemReservationSize(ConfigSpecImpl spec,
         ResourceAllocationInfo rs, long memory) {
      spec.setMemoryAllocation(
            new ResourceAllocationInfoImpl(
                  memory, false, rs.getLimit(), rs.getShares(),
                  null));
   }

   public static void setCpuAllocationSize(ConfigSpecImpl spec,
         ResourceAllocationInfo rs, long cpu) {
      spec.setCpuAllocation(new ResourceAllocationInfoImpl(
            cpu, false, rs.getLimit(), rs.getShares(), null));

   }

}
