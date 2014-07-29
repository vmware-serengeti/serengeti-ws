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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.vmware.bdd.software.mgmt.plugin.intf.PreStartServices;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Priority;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.util.AuAssert;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.software.mgmt.plugin.aop.PreConfiguration;
import com.vmware.bdd.software.mgmt.plugin.exception.InfrastructureException;

@Aspect
public class PreConfigurationAdvice implements PreStartServices {
   private static final Logger logger = Logger.getLogger(PreConfigurationAdvice.class);
   private IClusterEntityManager clusterEntityMgr;

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   @Around("@annotation(com.vmware.bdd.software.mgmt.plugin.aop.PreConfiguration)")
   public Object preClusterConfiguration(ProceedingJoinPoint pjp) throws Throwable {
      MethodSignature signature = (MethodSignature) pjp.getSignature();
      Method method = signature.getMethod();
      PreConfiguration beforeConfig = AnnotationUtils.findAnnotation(method, PreConfiguration.class);
      String nameParam = beforeConfig.clusterNameParam();
      String waitingTimeParam = beforeConfig.maxWaitingTimeParam();

      String[] paramNames = signature.getParameterNames();
      Object[] args = pjp.getArgs();
      String clusterName = null;
      int maxWaitingSeconds = 120;
      for (int i = 0; i < paramNames.length; i++) {
         if (paramNames[i].equals(nameParam)) {
            clusterName = (String)args[i];
         }
         if (paramNames[i].equals(waitingTimeParam)) {
            maxWaitingSeconds = (Integer)args[i];
         }
      }
      if (clusterName == null) {
         logger.error("Cluster name is not specified in method");
         throw BddException.INTERNAL(null, "Wrong annotation usage. Cluster name must be specified in method.");
      }
      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }
      preStartServices(clusterName, maxWaitingSeconds);
      return pjp.proceed();
   }

   @Override
   public void preStartServices(String clusterName, int maxWaitingSeconds)
   throws InfrastructureException {
      logger.info("Pre configuration for cluster " + clusterName);
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
         List<String> errorMsgList = new ArrayList<String>();
         for (i = 0; i < callables.length; i++) {
            if (result[i].throwable != null) {
               errorMsgList.add(result[i].throwable.getMessage());
            }
         }
         if (!errorMsgList.isEmpty()) {
            throw InfrastructureException.DISK_FORTMAT_FAILED(clusterName, errorMsgList);
         }
      }  catch (InterruptedException e) {
         logger.error("error in waiting disk format", e);
         throw BddException.INTERNAL(e, e.getMessage());
      }
   }
}
