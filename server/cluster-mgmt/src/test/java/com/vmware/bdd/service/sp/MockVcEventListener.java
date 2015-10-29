/***************************************************************************
 * Copyright (c) 2013-2015 VMware, Inc. All Rights Reserved.
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
