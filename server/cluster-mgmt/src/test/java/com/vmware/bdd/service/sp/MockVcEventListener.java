package com.vmware.bdd.service.sp;

import java.util.EnumSet;

import mockit.Mock;
import mockit.MockClass;

import com.vmware.aurora.vc.vcevent.VcEventListener;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.IVcEventHandler;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;

@MockClass(realClass = VcEventListener.class)
public class MockVcEventListener {
   private static IVcEventHandler extHandler;
   private static IVcEventHandler intHandler;

   @Mock
   public static void installExtEventHandler(EnumSet<VcEventType> eventTypes,
         IVcEventHandler handler) {
      extHandler = handler;
   }

   @Mock
   public static void installEventHandler(EnumSet<VcEventType> eventTypes,
         IVcEventHandler handler) {
      intHandler = handler;
   }

   public static IVcEventHandler getExtHandler() {
      return extHandler;
   }

   public static IVcEventHandler getIntHandler() {
      return intHandler;
   }
}
