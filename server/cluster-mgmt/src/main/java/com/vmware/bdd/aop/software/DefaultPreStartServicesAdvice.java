/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.software.mgmt.exception.SoftwareManagementException;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.intf.PreStartServices;
import com.vmware.bdd.utils.CommonUtil;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Priority;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.software.mgmt.plugin.aop.PreConfiguration;
import com.vmware.bdd.software.mgmt.plugin.exception.InfrastructureException;
import com.vmware.bdd.utils.VcVmUtil;

@Aspect
public class DefaultPreStartServicesAdvice implements PreStartServices {
   private static final Logger logger = Logger.getLogger(DefaultPreStartServicesAdvice.class);
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

      String[] paramNames = signature.getParameterNames();
      Object[] args = pjp.getArgs();
      String clusterName = null;
      for (int i = 0; i < paramNames.length; i++) {
         if (paramNames[i].equals(nameParam)) {
            clusterName = (String)args[i];
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
      preStartServices(clusterName);
      return pjp.proceed();
   }

   @Override
   public void preStartServices(String clusterName) throws SoftwareManagementPluginException {
      preStartServices(clusterName, Collections.EMPTY_LIST, false);
   }

   @Override
   public void preStartServices(String clusterName, List<String> addedNodeNameList, boolean force) throws SoftwareManagementPluginException {
      logger.info("Pre configuration for cluster " + clusterName);
      synchronized(this) {
         if (clusterEntityMgr == null) {
            ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:/../spring/*-context.xml");
            clusterEntityMgr = (IClusterEntityManager) context.getBean("clusterEntityManager");
         }
      }

      waitVmBootup(clusterName, force);

      updateNodes(clusterName, addedNodeNameList, force);
   }

   private void waitVmBootup(String clusterName, boolean force) {
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(clusterName);
      Callable<Void>[] callables = new Callable[nodes.size()];
      int i = 0;
      for (NodeEntity node : nodes) {
         if (node.getMoId() == null || node.getMoId().isEmpty()) {
            continue;
         }
         node = clusterEntityMgr.getNodeWithNicsByMobId(node.getMoId());
         if (force && !node.nicsReady()) {
            continue;
         }
         WaitVMStatusTask task = new WaitVMStatusTask(node.getMoId());
         callables[i] = task;
         i++;
      }
      try {
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler.executeStoredProcedures(Priority.INTERACTIVE, callables,
                     callback);
         if (result.length == 0) {
            logger.error("Waiting for nodes bootup task is not executed.");
            throw BddException.INTERNAL(null, "Waiting for nodes bootup task is not executed.");
         }
         List<String> errorMsgList = new ArrayList<String>();
         for (i = 0; i < callables.length; i++) {
            if (result[i].throwable != null) {
               errorMsgList.add(result[i].throwable.getMessage());
               logger.error(result[i].throwable.getMessage());
            }
         }
         if (!errorMsgList.isEmpty()) {
            if (force) {
               logger.warn(JobConstants.FORCE_CLUSTER_OPERATION_IGNORE_EXCEPTION);
            } else {
               throw InfrastructureException.WAIT_VM_STATUS_FAIL(clusterName, errorMsgList);
            }
         }
      }  catch (InterruptedException e) {
         String errorMessage = "error when waiting for nodes bootup";
         logger.error(errorMessage);
         if (force) {
            logger.warn(JobConstants.FORCE_CLUSTER_OPERATION_IGNORE_EXCEPTION);
         } else {
            throw BddException.INTERNAL(e, e.getMessage());
         }
      }
   }

   @Transactional
   private Void updateNodes(final String clusterName, final List<String> addedNodeNameList, final boolean force) {

      return VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {

            List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(clusterName);
            for (NodeEntity node : nodes) {
               if (node.getMoId() == null || node.getMoId().isEmpty()) {
                  continue;
               }

               node = clusterEntityMgr.getNodeWithNicsByMobId(node.getMoId());

               if (force && !node.nicsReady()) {
                  logger.warn(String.format("try to remove node %s for nicsNotReady", node.getVmName()));
                  //remove the node from resizing nodes list
                  addedNodeNameList.remove(node.getVmName());
                  continue;
               }

               VcVirtualMachine vm = VcCache.getIgnoreMissing(node.getMoId());
               String hostname = VcVmUtil.getMgtHostName(vm, node.getPrimaryMgtIpV4());
               if (hostname == null || hostname.isEmpty()) {
                  String errMsg = "Failed to get FQDN from vm " + vm.getName();
                  logger.error(errMsg);
                  if (force) {
                     logger.warn(JobConstants.FORCE_CLUSTER_OPERATION_IGNORE_EXCEPTION);

                     logger.warn(String.format("try to remove node %s for NoFQDN", node.getVmName()));
                     //remove the node from resizing nodes list
                     addedNodeNameList.remove(node.getVmName());
                     continue;
                  } else {
                     throw SoftwareManagementException.FAILED_TO_GET_FQDN(vm.getName());
                  }
               }
               if (!hostname.equals(node.getGuestHostName())) {
                  node.setGuestHostName(hostname);
                  clusterEntityMgr.update(node);
                  logger.info("Update management NIC FQDN of node " + node.getVmName() + " to " + node.getGuestHostName());
               }
            }

            return null;
         }
      });

   }
}
