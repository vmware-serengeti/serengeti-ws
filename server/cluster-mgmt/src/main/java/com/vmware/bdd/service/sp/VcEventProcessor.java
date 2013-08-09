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

import java.util.EnumSet;

import org.apache.log4j.Logger;

import com.vmware.aurora.util.CmsWorker;
import com.vmware.aurora.util.CmsWorker.WorkQueue;
import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcUtil;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.IVcEventHandler;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;
import com.vmware.aurora.vc.vcevent.VcEventListener;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vim.event.EventEx;
import com.vmware.vim.binding.vim.event.VmClonedEvent;
import com.vmware.vim.binding.vim.event.VmEvent;
import com.vmware.vim.binding.vim.event.VmPoweredOffEvent;
import com.vmware.vim.binding.vim.event.VmPoweredOnEvent;
import com.vmware.vim.binding.vim.event.VmSuspendedEvent;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

public class VcEventProcessor {
   private static final Logger logger = Logger
         .getLogger(VcEventProcessor.class);
   private static final EnumSet<VcEventType> vmEvents = EnumSet.of(
         VcEventType.VmConfigMissing,
         VcEventType.VmConnected,
         VcEventType.VmCreated,
         VcEventType.VmDasBeingReset,
         VcEventType.VmDasResetFailed, 
         VcEventType.VmDisconnected,
         VcEventType.VmMessage,
         VcEventType.VmMessageError,
         VcEventType.VmMessageWarning,
         VcEventType.VmOrphaned,
         VcEventType.VmPoweredOn,
         VcEventType.VmPoweredOff,
         VcEventType.VmReconfigured,
         VcEventType.VmRelocated,
         VcEventType.VmRemoved,
         VcEventType.VmRenamed,
         VcEventType.VmResourcePoolMoved,
         VcEventType.VmResuming,
         VcEventType.VmSuspended,
         VcEventType.VmAppHealthChanged,
         VcEventType.NotEnoughResourcesToStartVmEvent,
         VcEventType.VmMaxRestartCountReached,
         VcEventType.VmFailoverFailed,
         VcEventType.VmCloned);


   public VcEventProcessor(final ClusterEntityManager clusterEntityMgr) {
      /* High level handler for external vm events. */
      VcEventListener.installExtEventHandler(vmEvents, new IVcEventHandler() {
         @Override
         public boolean eventHandler(VcEventType type, Event e)
               throws Exception {
            // Event can be either VmEvent or EventEx (TODO: Explicitly check for
            // VM specific EventEx class usage? e.g. VcEventType.VmAppHealthChanged?)
            AuAssert.check(e instanceof VmEvent || e instanceof EventEx);
            ManagedObjectReference moRef = e.getVm().getVm();
            String moId = MoUtil.morefToString(moRef);
            logger.debug("received vm event: " + e);
            try {
               switch (type) {
               case VmRemoved: {
                  logger.debug("received vm removed event for vm: " + moId);
                  if (clusterEntityMgr.getNodeByMobId(moId) != null) {
                     clusterEntityMgr.refreshNodeByMobId(moId, null, true);
                  }
                  return false;
               }
               case VmPoweredOn: {
                  VmPoweredOnEvent event = (VmPoweredOnEvent) e;
                  e.getVm();
                  VcVirtualMachine vm =
                        VcCache.getIgnoreMissing(event.getVm().getVm());
                  if (vm == null) {
                     return false;
                  }
                  vm.updateRuntime();
                  if (clusterEntityMgr.getNodeByMobId(moId) != null) {
                     logger.info("received serengeti managed vm powered on event for vm: "
                           + vm.getName());
                     clusterEntityMgr.refreshNodeByMobId(moId,
                           Constants.NODE_ACTION_WAITING_IP, true);
                     NodePowerOnRequest request =
                           new NodePowerOnRequest(clusterEntityMgr, moId);
                     CmsWorker.addRequest(WorkQueue.VC_TASK_NO_DELAY, request);
                  }
                  break;
               }
               case VmPoweredOff: {
                  VmPoweredOffEvent event = (VmPoweredOffEvent) e;
                  VcVirtualMachine vm =
                        VcCache.getIgnoreMissing(event.getVm().getVm());
                  if (vm == null) {
                     return false;
                  }
                  vm.updateRuntime();
                  if (clusterEntityMgr.getNodeByMobId(moId) != null) {
                     logger.info("received serengeti managed vm powered off event for vm: "
                           + vm.getName());
                     clusterEntityMgr.refreshNodeByMobId(moId, null, true);
                  }
                  break;
               }
               case VmSuspended: {
                  VmSuspendedEvent event = (VmSuspendedEvent) e;
                  VcVirtualMachine vm =
                        VcCache.getIgnoreMissing(event.getVm().getVm());
                  if (vm == null) {
                     return false;
                  }
                  vm.updateRuntime();
                  if (clusterEntityMgr.getNodeByMobId(moId) != null) {
                     logger.info("received serengeti managed vm suspended event for vm: "
                           + vm.getName());
                     clusterEntityMgr.refreshNodeByMobId(moId, null, true);
                  }
                  break;
               }
               }
               VcCache.refreshAll(moRef);
               return false;
            } catch (ManagedObjectNotFound exp) {
               VcUtil.processNotFoundException(exp, moId, logger);
               return false;
            }
         }
      });

      /* High level handler for internal vm events. */
      VcEventListener.installEventHandler(vmEvents, new IVcEventHandler() {
         @Override
         public boolean eventHandler(VcEventType type, Event e)
               throws Exception {
            // Event can be either VmEvent or EventEx (TODO: Explicitly check for
            // VM specific EventEx class usage? e.g. VcEventType.VmAppHealthChanged?)
            AuAssert.check(e instanceof VmEvent || e instanceof EventEx);
            ManagedObjectReference moRef = e.getVm().getVm();
            String moId = MoUtil.morefToString(moRef);
            logger.debug("received vm event: " + e);
            try {
               switch (type) {
               case VmRemoved: {
                  logger.debug("received vm removed event for vm: " + moId);
                  if (clusterEntityMgr.getNodeByMobId(moId) != null) {
                     clusterEntityMgr.refreshNodeByMobId(moId, null, true);
                  }
                  return false;
               }
               case VmPoweredOn: {
                  VmPoweredOnEvent event = (VmPoweredOnEvent) e;
                  e.getVm();
                  VcVirtualMachine vm =
                     VcCache.getIgnoreMissing(event.getVm().getVm());
                  if (vm == null) {
                     return false;
                  }
                  vm.updateRuntime();
                  if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null) {
                     logger.info("received internal vm powered on event for vm: "
                           + vm.getName());
                     clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(),
                           Constants.NODE_ACTION_WAITING_IP, true);
                  }
                  break;
               }
               case VmCloned: {
                  VmClonedEvent event = (VmClonedEvent) e;
                  e.getVm();
                  VcVirtualMachine vm =
                     VcCache.getIgnoreMissing(event.getVm().getVm());
                  if (vm == null) {
                     return false;
                  }
                  vm.updateRuntime();
                  if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null) {
                     logger.info("received internal vm cloned event for vm: "
                           + vm.getName());
                     clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(),
                           Constants.NODE_ACTION_RECONFIGURE, true);
                  }
                  break;
               }
               case VmSuspended: {
                  VmSuspendedEvent event = (VmSuspendedEvent) e;
                  VcVirtualMachine vm =
                     VcCache.getIgnoreMissing(event.getVm().getVm());
                  if (vm == null) {
                     return false;
                  }
                  vm.updateRuntime();
                  if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null) {
                     logger.info("received internal vm suspended event for vm: "
                           + vm.getName());
                     clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(),
                           null, true);
                  }
                  break;
               }
               case VmPoweredOff: {
                  VmPoweredOffEvent event = (VmPoweredOffEvent) e;
                  VcVirtualMachine vm =
                     VcCache.getIgnoreMissing(event.getVm().getVm());
                  if (vm == null) {
                     return false;
                  }
                  vm.updateRuntime();
                  if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null) {
                     logger.info("received internal vm powered off event for vm: "
                           + vm.getName());
                     clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(),
                           null, true);
                  }
                  break;
               }
               }
               return false;
            } catch (ManagedObjectNotFound exp) {
               VcUtil.processNotFoundException(exp, moId, logger);
               return false;
            }
         }
      });
   }
}
