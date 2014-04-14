/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.vmclone.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.google.gson.internal.Pair;
import com.vmware.aurora.composition.CreateVmSP;
import com.vmware.aurora.composition.compensation.CompensateCreateVmSP;
import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.composition.concurrent.Scheduler.ProgressCallback;
import com.vmware.bdd.clone.spec.VmCreateResult;
import com.vmware.bdd.clone.spec.VmCreateSpec;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.vmclone.service.intf.IClusterCloneService;
import com.vmware.vim.binding.vim.fault.VmFaultToleranceOpIssuesList;

/**
 * vm clone service
 *
 * @author tli
 *
 */
@Service
public class SimpleClusterCloneService implements IClusterCloneService {
   private static final Logger logger = Logger
         .getLogger(SimpleClusterCloneService.class);

   @Override
   public List<VmCreateResult<?>> createCopies(VmCreateSpec resource,
         int maxConcurrentCopy, List<VmCreateSpec> consumers,
         ProgressCallback callback) {
      Pair<Callable<Void>, Callable<Void>>[] storeProcedures =
            new Pair[consumers.size()];

      for (int i = 0; i < consumers.size(); i++) {
         VmCreateSpec consumer = consumers.get(i);
         CreateVmSP cloneVmSp =
               new CreateVmSP(consumer.getVmName(), consumer.getSchema(),
                     consumer.getTargetRp(), consumer.getTargetDs(),
                     consumer.getPrePowerOn(), consumer.getPostPowerOn(),
                     consumer.getBootupConfigs(), false,
                     consumer.getTargetFolder(), consumer.getTargetHost());
         CompensateCreateVmSP deleteVmSp = new CompensateCreateVmSP(cloneVmSp);
         storeProcedures[i] =
               new Pair<Callable<Void>, Callable<Void>>(cloneVmSp, deleteVmSp);
      }

      try {
         // execute store procedures to create VMs
         logger.info("ClusterCloneService, start to create vms.");
         Pair<ExecutionResult, ExecutionResult>[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProcedures, storeProcedures.length, callback);
         if (result == null) {
            logger.error("No VM is created.");
            return null;
         }

         int total = 0;
         List<VmCreateResult<?>> cloned = new ArrayList<VmCreateResult<?>>();
         for (int i = 0; i < storeProcedures.length; i++) {
            Pair<ExecutionResult, ExecutionResult> pair = result[i];
            VmCreateResult<VmCreateSpec> createResult = new VmCreateResult<VmCreateSpec>();
            VmCreateSpec node = consumers.get(i);
            createResult.setSpec(node);
            CreateVmSP sp = (CreateVmSP) storeProcedures[i].first;
            if (sp.getVM() != null) {
               node.setVmId(sp.getVM().getId());
            }
            if (pair.first.finished && pair.first.throwable == null
                  && pair.second.finished == false) {
               ++total;
               createResult.setSuccess(true);
            } else if (pair.first.throwable != null) {
               createResult.setSuccess(false);
               processException(createResult, pair.first.throwable);
               logger.error("Failed to create VM " + node.getVmName(),
                     pair.first.throwable);
            }
            cloned.add(createResult);
         }
         logger.info(total + " VMs are successfully created.");
         return cloned;
      } catch (InterruptedException e) {
         logger.error("error in creating VMs", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }

   private void processException(VmCreateResult<VmCreateSpec> node, Throwable throwable) {
      node.setErrMessage(throwable.getMessage());
      node.setErrTimeStamp(CommonUtil.getCurrentTimestamp());
      while (throwable.getCause() != null) {
         throwable = throwable.getCause();
         if (throwable instanceof VmFaultToleranceOpIssuesList) {
            logger.error("Got FT operation error: "
                  + throwable.getLocalizedMessage());
            VmFaultToleranceOpIssuesList ftIssues =
                  (VmFaultToleranceOpIssuesList) throwable;
            Exception[] errors = ftIssues.getErrors();
            if (errors != null) {
               for (Exception e : errors) {
                  logger.error("FT error: " + e.getLocalizedMessage());
               }
            }
            Exception[] warnings = ftIssues.getWarnings();
            if (warnings != null) {
               for (Exception e : warnings) {
                  logger.error("FT warning: " + e.getLocalizedMessage());
               }
            }
         }
      }
   }
}