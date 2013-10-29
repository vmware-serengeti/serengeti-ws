package com.vmware.bdd.service.sp;

import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcUtil;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcevent.VcEventListener;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.IVcEventHandler;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
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

public class VmEventProcessor extends Thread {
   private static class EventWrapper {
      private VcEventType type;
      private Event event;
      private boolean external;

      public EventWrapper(VcEventType type, Event event, boolean external) {
         this.type = type;
         this.event = event;
         this.external = external;
      }
   }

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
         VcEventType.VmCloned,
         VcEventType.VhmError,
         VcEventType.VhmWarning,
         VcEventType.VhmInfo,
         VcEventType.VhmUser);

   private static final Logger logger = Logger
         .getLogger(VmEventProcessor.class);
   private BlockingQueue<EventWrapper> queue =
         new LinkedBlockingQueue<EventWrapper>();
   private boolean isTerminate = false;
   private ClusterEntityManager clusterEntityMgr;

   public VmEventProcessor(ClusterEntityManager clusterEntityMgr) {
      super();
      this.clusterEntityMgr = clusterEntityMgr;
   }

   public void installEventHandler() {
      VcEventListener.installExtEventHandler(vmEvents, new IVcEventHandler() {
         @Override
         public boolean eventHandler(VcEventType type, Event e)
               throws Exception {
            logger.debug("Received external VM event " + e);
            add(type, e, true);
            return false;
         }
      });
      VcEventListener.installEventHandler(vmEvents, new IVcEventHandler() {
         @Override
         public boolean eventHandler(VcEventType type, Event e)
               throws Exception {
            logger.debug("Received internal VM event " + e);
            add(type, e, false);
            return false;
         }
      });
   }

   public void add(VcEventType type, Event event, boolean external) {
      EventWrapper wrapper = new EventWrapper(type, event, external);
      queue.add(wrapper);
   }

   public void run() {
      while (!isTerminate) {
         Event event = null;
         try {
            final EventWrapper wrapper = queue.poll(1, TimeUnit.SECONDS);
            if (wrapper != null) {
               event = wrapper.event;
               VcContext.inVcSessionDo(new VcSession<Void>() {
                  @Override
                  protected boolean isTaskSession() {
                     return true;
                  }

                  @Override
                  protected Void body() throws Exception {
                     processEvent(wrapper.type, wrapper.event, wrapper.external);
                     return null;
                  }
               });
            }
         } catch (InterruptedException e) {
            logger.warn("Thread interrupt exception received, exit.");
            isTerminate = true;
         } catch (Exception e) {
            if (event != null) {
               logger.error("Failed to process event: " + event, e);
            } else {
               logger.error("Failed to process VM event.", e);
            }
         }
      }
   }

   public boolean isTerminate() {
      return isTerminate;
   }

   public void setTerminate(boolean isTerminate) {
      this.isTerminate = isTerminate;
   }

   public void processEvent(VcEventType type, Event e, boolean external)
         throws Exception {
      // Event can be either VmEvent or EventEx (TODO: Explicitly check for
      // VM specific EventEx class usage? e.g. VcEventType.VmAppHealthChanged?)
      AuAssert.check(e instanceof VmEvent || e instanceof EventEx);
      ManagedObjectReference moRef = e.getVm().getVm();
      String moId = MoUtil.morefToString(moRef);
      logger.debug("processed vm event: " + e);
      if (external && clusterEntityMgr.getNodeByMobId(moId) == null) {
         logger.debug("Event received for VM not managed by Serengeti");
         return;
      }

      try {
         switch (type) {
         case VmRemoved: {
            logger.debug("received vm removed event for vm: " + moId);
            if (clusterEntityMgr.getNodeByMobId(moId) != null) {
               clusterEntityMgr.refreshNodeByMobId(moId, null, true);
            }
            break;
         }
         case VmPoweredOn: {
            VmPoweredOnEvent event = (VmPoweredOnEvent) e;
            e.getVm();
            VcVirtualMachine vm =
                  VcCache.getIgnoreMissing(event.getVm().getVm());
            if (vm == null) {
               break;
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
               break;
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
               break;
            }
            vm.updateRuntime();
            if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null) {
               logger.info("received internal vm suspended event for vm: "
                     + vm.getName());
               clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(), null,
                     true);
            }
            break;
         }
         case VmPoweredOff: {
            VmPoweredOffEvent event = (VmPoweredOffEvent) e;
            VcVirtualMachine vm =
                  VcCache.getIgnoreMissing(event.getVm().getVm());
            if (vm == null) {
               break;
            }
            vm.updateRuntime();
            if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null) {
               logger.info("received internal vm powered off event for vm: "
                     + vm.getName());
               clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(), null,
                     true);
            }
            break;
         }
         case VhmError:
         case VhmWarning:
         case VhmInfo:
         case VhmUser: {
            EventEx event = (EventEx) e;
            VcVirtualMachine vm =
                  VcCache.getIgnoreMissing(event.getVm().getVm());
            if (vm == null) {
               break;
            }
            vm.updateRuntime();
            if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null) {
               logger.info("received internal vhm event " + event.getDynamicType()
                     + "for vm " + vm.getName() + ": "
                     + event.getMessage());
               clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(),
                     event.getMessage(), true);
            }
            break;
         }
         }
      } catch (ManagedObjectNotFound exp) {
         VcUtil.processNotFoundException(exp, moId, logger);
      }
   }

   public void shutdown() {
      this.interrupt();
   }
}
