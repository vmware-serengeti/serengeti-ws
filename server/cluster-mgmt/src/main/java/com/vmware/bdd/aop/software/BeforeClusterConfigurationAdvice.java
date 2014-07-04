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
package com.vmware.bdd.aop.software;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Priority;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.software.mgmt.exception.SoftwareManagementException;

@Aspect
public class BeforeClusterConfigurationAdvice {
   private static final Logger logger = Logger.getLogger(BeforeClusterConfigurationAdvice.class);
   private IClusterEntityManager clusterEntityMgr;

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   @Before("args(clusterName)")
   public void preClusterConfiguration(String clusterName, int maxWaitingSeconds)
   throws {
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(clusterName);
      Callable<Void>[] callables = new Callable[nodes.size()];
      int i = 0;
      for (NodeEntity node : nodes) {
         WaitVMStatusTask task =
               new WaitVMStatusTask(node.getMoId(), maxWaitingSeconds);
         callables[i] = task;
         i++;
      }
      try {
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
            Scheduler.executeStoredProcedures(Priority.INTERACTIVE, callables,
                  callback);
         if (result == null) {
            logger.error("No disk format waiting task is executed.");
            throw BddException.INTERNAL(null, "No disk format waiting task is executed.");
         }
         for (i = 0; i < callables.length; i++) {
            if (result[i].throwable != null) {
               if (result[i].throwable instanceof SoftwareManagementException) {
                  ((SoftwareManagementException)result[i].throwable)
               }
            }

         }
      }  catch (InterruptedException e) {
         logger.error("error in waiting disk format", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }
}
