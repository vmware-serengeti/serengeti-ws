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
package com.vmware.bdd.service.sp;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.vmware.bdd.utils.VHMConstants;
import org.apache.log4j.Logger;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.impl.vim.option.OptionValueImpl;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vim.vm.ConfigSpec;

/**
 * Store Procedure for set auto elasticity
 */
public class SetAutoElasticitySP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(SetAutoElasticitySP.class);
   private final String clusterName;
   private final VcVirtualMachine vcVm;
   private final String serengetiUUID;
   private final String masterMoId;
   private final String masterUUID;
   private final Boolean enableAutoElasticity;
   private final int minComputeNodeNum;
   private final int maxComputeNodeNum;
   private final String jobTrackerPort;
   private final boolean isComputeOnlyNode;

   public SetAutoElasticitySP(String clusterName, VcVirtualMachine vcVm, String serengetiUUID, String masterMoId, String masterUUID, Boolean enableAutoElasticity,
         int minComputeNodeNum, int maxComputeNodeNum, String jobTrackerPort, boolean isComputeOnlyNode) {
      this.clusterName = clusterName;
      this.vcVm = vcVm;
      this.serengetiUUID = serengetiUUID;
      this.masterMoId = masterMoId;
      this.masterUUID = masterUUID;
      this.enableAutoElasticity = enableAutoElasticity;
      this.minComputeNodeNum = minComputeNodeNum;
      this.maxComputeNodeNum = maxComputeNodeNum;
      this.jobTrackerPort = jobTrackerPort;
      this.isComputeOnlyNode = isComputeOnlyNode;
   }

   @Override
   public Void call() throws Exception {
      if (vcVm == null) {
         logger.info("vm not exist. Ignore the enable auto elasticity request.");
         return null;
      }
      final VcVirtualMachine vm = VcCache.getIgnoreMissing(vcVm.getId());
      if (vm == null) {
         logger.info("vm not exist. Ignore the enable auto elasticity request.");
      }
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {

            List<OptionValue> options = new ArrayList<OptionValue>();
            if (vm.getId().equalsIgnoreCase(masterMoId)) {
               options.add(new OptionValueImpl(VHMConstants.VHM_ENABLE, enableAutoElasticity.toString()));
               options.add(new OptionValueImpl(VHMConstants.VHM_INSTANCERANGE_COMPUTENODE_NUM,
                     (new Integer(minComputeNodeNum)).toString() + ":" + (new Integer(maxComputeNodeNum)).toString()));
               options.add(new OptionValueImpl(VHMConstants.VHM_JOBTRACKER_PORT, jobTrackerPort));
               options.add(new OptionValueImpl(VHMConstants.VHM_CLUSTER_NAME, clusterName));
            }

            options.add(new OptionValueImpl(VHMConstants.VHM_MASTER_MOID, masterMoId.split(":")[2]));
            options.add(new OptionValueImpl(VHMConstants.VHM_MASTER_UUID, masterUUID));
            options.add(new OptionValueImpl(VHMConstants.VHM_SERENGETI_UUID, serengetiUUID));
            options.add(new OptionValueImpl(VHMConstants.VHM_ELASTIC, (new Boolean(isComputeOnlyNode)).toString()));

            OptionValue[] optionValues = options.toArray((OptionValue[]) Array.newInstance(OptionValue.class, options.size()));
            ConfigSpec spec = new ConfigSpecImpl();
            spec.setExtraConfig(optionValues);
            vm.reconfigure(spec);
            logger.info("set autoElasticity, masterMoId=" + masterMoId + ", masterUUID=" + masterUUID + ", isComputeOnlyNode=" + isComputeOnlyNode + ", enable="
                  + enableAutoElasticity + ", jobTrackerPort=" + jobTrackerPort + ", minComputeNodeNum=" + minComputeNodeNum + ", maxComputeNodeNum=" + maxComputeNodeNum);
            return null;
         }
         protected boolean isTaskSession() {
            return true;
         }
      });
      return null;
   }
}
