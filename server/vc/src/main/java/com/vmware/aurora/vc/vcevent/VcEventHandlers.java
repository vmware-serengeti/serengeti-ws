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

/**
 * <code>VcEventHandlers</code> maintains data structures and APIs for event
 * handler registration and firing. A handler could be registered for any event
 * listed in VcEventType. If needed, add another line to VcEventType for any
 * missing VC event or event category.
 *
 * TODO Event handlers are currently fired in the context of VcEventListener
 * thread. We need to move to a thread pool model for handlers. VcEventListener
 * needs to go back to its poll loop; it must not be affected by any of the
 * exceptions coming from rogue handlers.
 *
 * @since   0.7
 * @version 0.7
 * @author Boris Weissman
 */

package com.vmware.aurora.vc.vcevent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsType;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcTask;
import com.vmware.aurora.vc.VcTaskMgr;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.impl.vim.event.EventImpl;
import com.vmware.vim.binding.vim.TaskInfo;
import com.vmware.vim.binding.vim.event.DatastoreRemovedOnHostEvent;
import com.vmware.vim.binding.vim.event.DrsVmPoweredOnEvent;
import com.vmware.vim.binding.vim.event.EnteredMaintenanceModeEvent;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vim.event.EventEx;
import com.vmware.vim.binding.vim.event.ExitMaintenanceModeEvent;
import com.vmware.vim.binding.vim.event.ExtendedEvent;
import com.vmware.vim.binding.vim.event.GeneralUserEvent;
import com.vmware.vim.binding.vim.event.HostAddedEvent;
import com.vmware.vim.binding.vim.event.HostRemovedEvent;
import com.vmware.vim.binding.vim.event.HostShutdownEvent;
import com.vmware.vim.binding.vim.event.NotEnoughResourcesToStartVmEvent;
import com.vmware.vim.binding.vim.event.ResourcePoolCreatedEvent;
import com.vmware.vim.binding.vim.event.ResourcePoolDestroyedEvent;
import com.vmware.vim.binding.vim.event.ResourcePoolMovedEvent;
import com.vmware.vim.binding.vim.event.ResourcePoolReconfiguredEvent;
import com.vmware.vim.binding.vim.event.ResourceViolatedEvent;
import com.vmware.vim.binding.vim.event.VmClonedEvent;
import com.vmware.vim.binding.vim.event.VmConfigMissingEvent;
import com.vmware.vim.binding.vim.event.VmConnectedEvent;
import com.vmware.vim.binding.vim.event.VmCreatedEvent;
import com.vmware.vim.binding.vim.event.VmDasBeingResetEvent;
import com.vmware.vim.binding.vim.event.VmDasBeingResetWithScreenshotEvent;
import com.vmware.vim.binding.vim.event.VmDasResetFailedEvent;
import com.vmware.vim.binding.vim.event.VmDisconnectedEvent;
import com.vmware.vim.binding.vim.event.VmFailoverFailed;
import com.vmware.vim.binding.vim.event.VmMaxRestartCountReached;
import com.vmware.vim.binding.vim.event.VmMessageErrorEvent;
import com.vmware.vim.binding.vim.event.VmMessageEvent;
import com.vmware.vim.binding.vim.event.VmMessageWarningEvent;
import com.vmware.vim.binding.vim.event.VmMigratedEvent;
import com.vmware.vim.binding.vim.event.VmOrphanedEvent;
import com.vmware.vim.binding.vim.event.VmPoweredOffEvent;
import com.vmware.vim.binding.vim.event.VmPoweredOnEvent;
import com.vmware.vim.binding.vim.event.VmReconfiguredEvent;
import com.vmware.vim.binding.vim.event.VmRelocatedEvent;
import com.vmware.vim.binding.vim.event.VmRemovedEvent;
import com.vmware.vim.binding.vim.event.VmRenamedEvent;
import com.vmware.vim.binding.vim.event.VmResourcePoolMovedEvent;
import com.vmware.vim.binding.vim.event.VmResourceReallocatedEvent;
import com.vmware.vim.binding.vim.event.VmResumingEvent;
import com.vmware.vim.binding.vim.event.VmSuspendedEvent;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.types.VmodlType;
import com.vmware.vim.vmomi.core.types.VmodlTypeMap;


public class VcEventHandlers {
   /**
    * A handler interface for VC events and simulated pseudo events.
    */
   public interface IVcEventHandler {
      /**
       * A handler for any of the several classes of events:
       * - VC events
       * - VC event categories: supertypes representing collections of related events
       *   such as VmEvent
       * - Pseudo-events simulated via PC with no direct VC analog such as TaskFinished
       *
       * If both specific and generic event handlers apply to a particular event, both
       * kinds will be fired in some undetermined order. Multiple handlers could be
       * registered for the same event.
       *
       * @param event   firing event
       * @return status true if event was of interest and caused processing
       * @throws Exception
       */
      boolean eventHandler(VcEventType type, Event event) throws Exception;
   }

   /**
    * All registered event types. A type could be a leaf VC type or any Event
    * derived interface or abstract class that designates an event category
    * such as VmEvent or HostEvent. To extend, just add another line.
    *
    * VC defines two kinds of events: static and dynamic (but don't look for
    * these in the docs...) The semantic of static events is declaratively
    * captured by the defining java type. Dynamic events could mean different
    * things at runtime depending on the dynamic value of string attribute
    * "eventTypeId". All dynamic events are instances of the same two static
    * event types: EventEx and ExtendedEvent. Dynamic events are entirely
    * undocumented even relative to static events.
    *
    * Here we normalize dynamic vc events and convert them into a well known
    * static set of VcEventType instances that we know and care about.
    */
   static public enum VcEventType {
      /* Static events */

      // Pseudo events
      TaskFinished (TaskFinishedEvent.class),
      TaskUpdateProgress (TaskUpdateProgressEvent.class),

      // VmEvent subtypes
      VmConfigMissing (VmConfigMissingEvent.class),
      VmConnected (VmConnectedEvent.class),
      VmCreated (VmCreatedEvent.class),
      VmCloned (VmClonedEvent.class),
      VmDasBeingReset (VmDasBeingResetEvent.class),
      VmDasBeingResetWithScreenshot (VmDasBeingResetWithScreenshotEvent.class),
      VmDasResetFailed (VmDasResetFailedEvent.class),
      VmDrsPoweredOn (DrsVmPoweredOnEvent.class),
      VmDisconnected (VmDisconnectedEvent.class),
      VmMessage (VmMessageEvent.class),
      VmMessageError (VmMessageErrorEvent.class),
      VmMessageWarning (VmMessageWarningEvent.class),
      VmMigrated (VmMigratedEvent.class),
      VmOrphaned (VmOrphanedEvent.class),
      VmPoweredOn (VmPoweredOnEvent.class),
      VmPoweredOff (VmPoweredOffEvent.class),
      VmReconfigured (VmReconfiguredEvent.class),
      VmRelocated (VmRelocatedEvent.class),
      VmRemoved (VmRemovedEvent.class),
      VmRenamed (VmRenamedEvent.class),
      VmResourcePoolMoved(VmResourcePoolMovedEvent.class),
      VmResourceReallocated (VmResourceReallocatedEvent.class),
      VmResuming (VmResumingEvent.class),
      VmSuspended (VmSuspendedEvent.class),
      NotEnoughResourcesToStartVmEvent(NotEnoughResourcesToStartVmEvent.class),
      VmMaxRestartCountReached(VmMaxRestartCountReached.class),
      VmFailoverFailed(VmFailoverFailed.class),

      // ResourcePoolEvent subtypes
      ResourcePoolCreated (ResourcePoolCreatedEvent.class),
      ResourcePoolDestroyed (ResourcePoolDestroyedEvent.class),
      ResourcePoolMoved (ResourcePoolMovedEvent.class),
      ResourcePoolReconfigured (ResourcePoolReconfiguredEvent.class),
      ResourceViolated (ResourceViolatedEvent.class),

      // HostEvent subtypes
      DatastoreRemovedOnHost (DatastoreRemovedOnHostEvent.class),
      EnteredMaintenanceMode (EnteredMaintenanceModeEvent.class),
      ExitMaintenanceMode (ExitMaintenanceModeEvent.class),
      HostAdded (HostAddedEvent.class),
      HostRemoved (HostRemovedEvent.class),
      HostShutdown (HostShutdownEvent.class),

      // Used by junit tests
      GeneralUser (GeneralUserEvent.class),

      /* Dynamic events */

      // HA category
      VmAppHealthChanged (EventEx.class,
            "com.vmware.vc.vmam.VmAppHealthMonitoringStateChangedEvent"),
      DasHostFailedEvent (EventEx.class,
            "com.vmware.vc.HA.DasHostFailedEvent"),
      DasHostIsolatedEvent (EventEx.class,
            "com.vmware.vc.HA.DasHostIsolatedEvent"),
      // TODO: Should we capture com.vmware.vc.HA.HostStateChangeEvent too
      // to indicate Host is now ok?

      // VHM events
      VhmError (EventEx.class,
            "com.vmware.vhadoop.vhm.vc.events.error"),
      VhmWarning (EventEx.class,
            "com.vmware.vhadoop.vhm.vc.events.warning"),
      VhmInfo (EventEx.class,
            "com.vmware.vhadoop.vhm.vc.events.info"),
      VhmUser (EventEx.class,
            "com.vmwre.vhadoop.vhm.vc.events.user");


      private static String eventTypeIds[] = null;
      private final Class<? extends Event> eventClass;
      private final String eventTypeId;

      /**
       * Generic constructor used for all static event types. EventTypeId
       * is derived from the class name for all static events.
       * @param clazz java class representing the event
       */
      private VcEventType(Class<? extends Event> clazz) {
         AuAssert.check(isStaticEvent(clazz));
         eventClass = clazz;
         eventTypeId = getStaticEventTypeId(clazz);
      }

      /**
       * This constructor can only be used by dynamically typed events such as
       * EventEx and ExtendedEvent that have programmatically defined type ids.
       * @param clazz java class representing the event
       * @param typeId  eventTypeId string
       */
      private VcEventType(Class<? extends Event> clazz, String typeId) {
         AuAssert.check(isDynamicEvent(clazz));
         eventClass = clazz;
         eventTypeId = typeId;
      }

      public Class<? extends Event> getEventClass() {
         return eventClass;
      }

      private String getEventTypeId() {
         return eventTypeId;
      }

      /**
       * Returns true for classes that represent dynamic vc events.
       * @param clazz   class to check
       * @return true   if dynamic
       */
      private static boolean isDynamicEvent(Class<? extends Event> clazz) {
         return EventEx.class.isAssignableFrom(clazz) ||
                ExtendedEvent.class.isAssignableFrom(clazz);
      }

      private static boolean isStaticEvent(Class<? extends Event> clazz) {
         return !isDynamicEvent(clazz);
      }

      /**
       * Returns a type id for a static event. These default/static type ids
       * look like: "vim.event.VmPoweredOnEvent". Don't use this function for
       * dynamic customizable events such as EventEx.
       * @param clazz   static event class
       * @return static eventTypeId
       */
      public static String getStaticEventTypeId(Class<? extends Event> clazz) {
         AuAssert.check(isStaticEvent(clazz));
         VmodlTypeMap typeMap = VmodlTypeMap.Factory.getTypeMap();
         VmodlType vmodlType = typeMap.getVmodlType(clazz);
         /* Pseudo-events will have null vmodlType - ok to skip. */
         if (vmodlType == null) {
            return null;
         }

         final String prefix = "com.vmware.vim.binding.";
         String fullName = vmodlType.getTypeName().getName();
         AuAssert.check(fullName.startsWith(prefix));
         String eventTypeId = fullName.substring(prefix.length());
         return eventTypeId;
      }

      /**
       * For all events declared above, returns an array of something vc sdk
       * sometimes calls event type ids. These could be used by event filters
       * to filter out unwanted events. Example of a static event type id:
       *    "vim.event.VmPoweredOnEvent".
       * New dynamic events have arbitrary statically unknown and entirely
       * undocumented type ids by design.
       *
       * New APIs like these more than short wsdlNames favored by old APIs...
       * @return array of event type ids.
       */
      public static String[] getEventTypeIds() {
         if (eventTypeIds == null) {
            synchronized (VcEventType.class) {
               if (eventTypeIds == null) {
                  List<String> eventIds = new ArrayList<String>();
                  for (VcEventType eventType : VcEventType.values()) {
                     String eventId = eventType.getEventTypeId();
                     if (eventId != null) {
                        eventIds.add(eventId);
                     }
                  }
                  eventTypeIds = eventIds.toArray(new String[0]);
               }
            }
         }
         return eventTypeIds;
      }

      /**
       * Returns VcEventType instance that describes the passed event.
       * @param vim event
       * @return VcEventType
       */
      public static VcEventType getInstance(Event e) {
         for (VcEventType eventType : VcEventType.values()) {
            if (eventType.isInstance(e)) {
               return eventType;
            }
         }
         return null;
      }

      /**
       * Returns true if a passed event matches this VcEventType. For static events,
       * this is a simple subtyping check for a class associated with VcEventType. For
       * dynamic events, we must also ensure a match of "evetTypeId" attribute.
       * @param event   event to check
       * @return true   if e is an instance of this VcEventType
       */
      private boolean isInstance(Event e) {
         Class<? extends Event> clazz = getEventClass();
         if (clazz.isInstance(e)) {
            if (VcEventType.isDynamicEvent(clazz)) {
               AuAssert.check(getEventTypeId() != null);
               String receivedTypeId;
               if (e instanceof EventEx) {
                  receivedTypeId = ((EventEx) e).getEventTypeId();
               } else {
                  AuAssert.check(e instanceof ExtendedEvent);
                  receivedTypeId = ((ExtendedEvent) e).getEventTypeId();
               }
               return getEventTypeId().equals(receivedTypeId);
            } else {
               return true;
            }
         }
         return false;
      }
   }

   /**
    * This is a pseudo-event that unfortunately does not have a direct
    * VC analog. VC generates an event on task initiation, but not task
    * completion. We simulate it with PC to maintain a uniform event API.
    * This class is defined for API uniformity with real events.
    */
   @SuppressWarnings("serial")
   public class TaskFinishedEvent extends EventImpl {
      private final ManagedObjectReference taskMoRef;
      private final TaskInfo.State state;

      TaskFinishedEvent(ManagedObjectReference taskMoRef, TaskInfo.State state) {
         this.taskMoRef = taskMoRef;
         this.state = state;
         VcTask vcTask = taskMgr.getPendingTask(taskMoRef);
         if (vcTask != null) {
            setChainId(vcTask.getEventChainId());
         }
      }

      public ManagedObjectReference getTaskMoRef() {
         return taskMoRef;
      }

      public TaskInfo.State getTaskState() {
         return state;
      }
   }

   /**
    * Another pseudo-event fired each time we get a notification from VC about
    * a task progress change.
    */
   @SuppressWarnings("serial")
   public class TaskUpdateProgressEvent extends EventImpl {
      private ManagedObjectReference taskMoRef;
      private Integer progress;

      TaskUpdateProgressEvent(ManagedObjectReference taskMoRef, Integer progress) {
         this.taskMoRef = taskMoRef;
         this.progress = progress;
         VcTask vcTask = taskMgr.getPendingTask(taskMoRef);
         if (vcTask != null) {
            setChainId(vcTask.getEventChainId());
         }
      }

      public ManagedObjectReference getTaskMoRef() {
         return taskMoRef;
      }

      public Integer getProgress() {
         return progress;
      }
   }

   private static Logger logger = Logger.getLogger(VcEventHandlers.class);

   private VcTaskMgr taskMgr;
   /* Maps: VcEventType -> event handlers. */
   private HashMap<VcEventType, List<IVcEventHandler>> handlerMap;
   private HashMap<VcEventType, List<IVcEventHandler>> extHandlerMap;
   private static VcEventHandlers instance = new VcEventHandlers();

   protected static VcEventHandlers getInstance() {
      return instance;
   }

   private VcEventHandlers() {
      handlerMap = new HashMap<VcEventType, List<IVcEventHandler>>();
      extHandlerMap = new HashMap<VcEventType, List<IVcEventHandler>>();
      /*
       *  XXX There is a chicken and egg problem described as below:
       *  The taskMgr serves as both the filter for
       *  internal/external events and an even handler.
       *  Thus, at the time of creating taskMgr, we cannot
       *  insert itself as an event handler because VcEventHandlers
       *  hasn't been initialized yet.
       *
       *  As a compromise, we first create taskMgr and
       *  insert it as event handler here.
       */
      taskMgr = VcContext.getGlobalTaskMgr();
      /* Install cms event handler: TaskFinishedEvent -> taskMgr.eventHandler() */
      installEventHandler(VcEventType.TaskFinished,
            taskMgr.getTaskFinishedHandler(), false);
      installEventHandler(VcEventType.TaskUpdateProgress,
            taskMgr.getTaskUpdateProgressHandler(), false);
   }

   /**
    * Installs a specified handler into a passed handler map.
    * @param eventType  event to handle
    * @param handler    handler
    * @param hMap       target handler map (internal or external)
    */
   private void installHandler(VcEventType eventType, IVcEventHandler handler,
         HashMap<VcEventType, List<IVcEventHandler>> hMap) {
      List<IVcEventHandler> handlers = hMap.get(eventType);
      if (handlers == null) {
         handlers = new ArrayList<IVcEventHandler>();
         hMap.put(eventType, handlers);
      }
      handlers.add(handler);
   }

   /**
    * Install an event handler for a specified event type. Multiple handlers may
    * be installed for the same type. The firing order is unspecified. Installed
    * handlers survive VC connection re-initialization.
    *
    * @param eventType  target event kind
    * @param handler    supplied event handler
    * @param external   triggered outside CMS?
    */
   protected synchronized void installEventHandler(VcEventType eventType,
         IVcEventHandler handler, boolean external) {
      AuAssert.check(handler != null);
      String eventKind = external ? "external " : "internal ";
      StringBuilder msg = new StringBuilder("Installed ");
      msg = msg.append(eventKind)
            .append("event handler: ")
            .append(eventType)
            .append(" -> ")
            .append(handler);
      installHandler(eventType, handler, external ? extHandlerMap : handlerMap);
      logger.info(msg);
   }

   /**
    * Removes a handler from a given handler map.
    * @param eventType  target event kind
    * @param handler    handler to remove
    * @param hMap       target handler map
    * @return status    true if handler was removed
    */
   private boolean removeEventHandler(VcEventType eventType, IVcEventHandler handler,
         HashMap<VcEventType, List<IVcEventHandler>> hMap) {
      List<IVcEventHandler> handlers = hMap.get(eventType);
      if (handlers != null) {
         return handlers.remove(handler);
      }
      return false;
   }

   /**
    * Remove a handler for a specified event type. Returns true if the handler was
    * previously installed and was, in fact, removed. False otherwise. If multiple
    * handlers are installed, this removes only a single handler instance.
    *
    * @param eventType  target event kind
    * @param handler    handler to be removed
    * @param boolean    triggered outside CMS?
    * @return true if hanlder removed
    */
   protected synchronized boolean removeEventHandler(VcEventType eventType,
         IVcEventHandler handler, boolean external) {
      return removeEventHandler(eventType, handler,
            external ? extHandlerMap : handlerMap);
   }

   /**
    * Returns true if event matches at least one of the subscribed types listed
    * in VcEvenType, false otherwise.
    * @param e event to check
    * @return
    */
   private boolean isSubscribed(Event e) {
      boolean subscribed = false;
      for (VcEventType eventType : VcEventType.values()) {
         if (eventType.isInstance(e)) {
            subscribed = true;
         }
      }
      return subscribed;
   }

   /**
    * Returns a handler map for the passed Event. If event was triggered
    * as a result of a cms task, returns handlers for internal cms events.
    * Otherwise returns handlers for external events.
    * @param event
    * @return handlerMap
    */
   private HashMap<VcEventType, List<IVcEventHandler>> getHandlerMap(boolean external) {
      return external ? extHandlerMap : handlerMap;
   }

   /**
    * Called by VcEventListener upon detecting a VC event. Fires all
    * general and specific handlers registered for this event. If event is
    * determined to be triggered by a cms task, internal cms handlers are
    * fired. If the event was originated outside cms (hw malfunction, VI admin
    * actions), fire all external event handlers.
    * TODO: move to thread pool model. At that time synchronized will turn
    *       into readLock().lock and handler installation into writeLock.lock().
    * @param  event     firing event
    * @throws Exception
    */
   public synchronized void fire(Event e) throws Exception {
      AuAssert.check(e != null);
      AuAssert.check(isSubscribed(e));   // Should not see unsubscribed events.
      boolean external = taskMgr.eventFireCallback(e);
      HashMap<VcEventType, List<IVcEventHandler>> hMap = getHandlerMap(external);
      for (VcEventType eventType : hMap.keySet()) {
         if (eventType.isInstance(e)) {
            StatsType oldSrc = Profiler.pushInc(
                  external? StatsType.VC_EVENT_EXT : StatsType.VC_EVENT_INT, eventType);
            List<IVcEventHandler> handlers = hMap.get(eventType);
            for (IVcEventHandler h : handlers) {
               h.eventHandler(eventType, e);
            }
            Profiler.pop(oldSrc);
         }
      }
   }
}
