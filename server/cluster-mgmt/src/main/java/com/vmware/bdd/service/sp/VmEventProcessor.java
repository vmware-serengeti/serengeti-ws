package com.vmware.bdd.service.sp;

import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vim.event.EventEx;
import com.vmware.vim.binding.vim.event.VmEvent;
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
         VcEventType.VmDasBeingResetWithScreenshot,
         VcEventType.VmDrsPoweredOn,
         VcEventType.VmMigrated,
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

   private boolean processExternalEvent(VcEventType type, Event e, String moId)
   throws Exception {
      if (clusterEntityMgr.getNodeByMobId(moId) != null) {
         return true;
      }
      if (type != VcEventType.VmRemoved) {
         VcVirtualMachine vm = VcCache.getIgnoreMissing(e.getVm().getVm());
         if (vm == null) {
            return false;
         }
         logger.debug("Event received for VM not managed by Serengeti");
         if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null && 
               VcResourceUtils.insidedRootFolder(vm)) {
            logger.info("VM " + vm.getName() + 
                  " is Serengeti created VM, add it into meta-db");
            return true;
         }
      }
      return false;
   }

   public void processEvent(VcEventType type, Event e, boolean external)
         throws Exception {
      // Event can be either VmEvent or EventEx (TODO: Explicitly check for
      // VM specific EventEx class usage? e.g. VcEventType.VmAppHealthChanged?)
      AuAssert.check(e instanceof VmEvent || e instanceof EventEx);
      ManagedObjectReference moRef = e.getVm().getVm();
      String moId = MoUtil.morefToString(moRef);
      logger.debug("processed vm event: " + e);
      if (external) {
         if (processExternalEvent(type, e, moId)) {
            processEvent(type, e, moId, true);
         }
      } else {
         processEvent(type, e, moId, false);
      }
   }

   private void processEvent(VcEventType type, Event e, String moId, boolean external)
   throws Exception {
      try {
         switch (type) {
         case VmRemoved: {
            logger.debug("received vm removed event for vm: " + moId);
            if (clusterEntityMgr.getNodeByMobId(moId) != null) {
               clusterEntityMgr.refreshNodeByMobId(moId, null, true);
            }
            break;
         }
         case VmDisconnected: {
            refreshNodeWithAction(e, null, true,
                  null, "Disconnected");
            break;
         }
         case VmPoweredOn: {
            refreshNodeWithAction(e, moId, true,
                  Constants.NODE_ACTION_WAITING_IP, "Powered On");
            if (external) {
               NodePowerOnRequest request =
                  new NodePowerOnRequest(clusterEntityMgr, moId);
               CmsWorker.addRequest(WorkQueue.VC_TASK_NO_DELAY, request);
            }
            break;
         }
         case VmCloned: {
            refreshNodeWithAction(e, moId, true,
                  Constants.NODE_ACTION_RECONFIGURE, "Cloned");
            break;
         }
         case VmSuspended: {
            refreshNodeWithAction(e, moId, true, null, "Suspended");
            break;
         }
         case VmPoweredOff: {
            refreshNodeWithAction(e, moId, true, null, "Powered Off");
            break;
         }
         case VhmError:
         case VhmWarning:
         case VhmInfo:
         case VhmUser: {
            EventEx event = (EventEx) e;
            refreshNodeWithAction(e, moId, true, null, "Powered Off");
            VcVirtualMachine vm =
               VcCache.getIgnoreMissing(event.getVm().getVm());
            if (vm == null) {
               break;
            }
            if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null) {
               logger.info("received vhm event " + e.getDynamicType()
                     + "for vm " + vm.getName() + ": "
                     + event.getMessage());
               clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(),
                     event.getMessage(), true);
            }
            break;
         }
         default: {
            refreshNodeWithAction(e, moId, false, null, type.name());
            break;
         }
         }
      } catch (ManagedObjectNotFound exp) {
         VcUtil.processNotFoundException(exp, moId, logger);
      }
   }

   private void refreshNodeWithAction(Event e, String moId, boolean setAction,
         String action, String eventName) throws Exception {
      VcVirtualMachine vm =
         VcCache.getIgnoreMissing(e.getVm().getVm());
      if (vm == null) {
         return;
      }
      if (clusterEntityMgr.getNodeByVmName(vm.getName()) != null) {
         logger.info("received vm " + eventName
               + " event for vm: " + vm.getName());
         if (setAction) {
            clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(), action, true);
         } else {
            clusterEntityMgr.refreshNodeByVmName(moId, vm.getName(), true);
         }
      }
      return;
   }

   public void shutdown() {
      this.interrupt();
   }
}
