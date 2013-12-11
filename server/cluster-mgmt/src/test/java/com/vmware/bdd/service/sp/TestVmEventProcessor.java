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

import junit.framework.Assert;
import mockit.Mockit;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;
import com.vmware.bdd.manager.ConcurrentWriteLockedClusterEntityManager;
import com.vmware.bdd.service.MockVcCache;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vim.event.VmClonedEvent;
import com.vmware.vim.binding.vim.event.VmEvent;
import com.vmware.vim.binding.vim.event.VmEventArgument;
import com.vmware.vim.binding.vim.event.VmPoweredOffEvent;
import com.vmware.vim.binding.vim.event.VmPoweredOnEvent;
import com.vmware.vim.binding.vim.event.VmRemovedEvent;
import com.vmware.vim.binding.vim.event.VmSuspendedEvent;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public class TestVmEventProcessor {
   private static VmEventProcessor processor;
   private static ConcurrentWriteLockedClusterEntityManager entityMgr;

   @AfterClass
   public static void tearDown() throws Exception {
      Mockit.tearDownMocks();
   }
   
   @BeforeClass
   public static void setUp() throws Exception {
      Mockit.setUpMock(MockVcCache.class);
      MockVcCache.setGetFlag(true);
      Mockit.setUpMock(MockVcContext.class);
      Mockit.setUpMock(MockVcEventListener.class);
      entityMgr = new MockConcurrentClusterEntityManager();
      processor = new VmEventProcessor(entityMgr);
      processor.installEventHandler();
      processor.start();
   }

   private <T extends VmEvent> T getVmEvent(final String id, Class<T> eventClass) {
      T e = Mockito.mock(eventClass);
      VmEventArgument ea = Mockito.mock(VmEventArgument.class);
      Mockito.when(e.getVm()).thenReturn(ea);

      Mockito.when(ea.getVm()).thenAnswer(new Answer<ManagedObjectReference>() {
         public ManagedObjectReference answer(InvocationOnMock invocation) throws Throwable {
            ManagedObjectReference moRef = Mockito.mock(ManagedObjectReference.class);
            Mockito.when(moRef.getServerGuid()).thenReturn(null);
            Mockito.when(moRef.getType()).thenReturn("VirtualMachine");
            Mockito.when(moRef.getValue()).thenReturn(id);

            System.out.println("processed event " + id);
            return (ManagedObjectReference) moRef;
         }
      });

      return e;
   }

   @Test
   public void testConcurrency() throws Exception {
      sendEvents(100, 20);
   }

   @Test
   public void testInterrupt() throws Exception {
      processor.interrupt();
      Thread.sleep(100);
      Assert.assertTrue("the executing thread is not stopped.", processor.isTerminate());

      processor = new VmEventProcessor(entityMgr);
      processor.installEventHandler();
      processor.start();

      sendEvents(500, 0);
      Thread.sleep(20);
      processor.interrupt();
      Thread.sleep(100);
      Assert.assertTrue("the executing thread is not stopped.", processor.isTerminate());
   }

   private void sendEvents(int number, int sleepMS) throws Exception {
      for (int i = 0; i < number; i ++) {
         Event e = getVmEvent(Integer.toString(i), VmRemovedEvent.class);
         MockVcEventListener.getExtHandler().eventHandler(VcEventType.VmRemoved, e);
         e = getVmEvent(Integer.toString(i), VmPoweredOnEvent.class);
         MockVcEventListener.getIntHandler().eventHandler(VcEventType.VmPoweredOn, e);
         e = getVmEvent(Integer.toString(i), VmClonedEvent.class);
         MockVcEventListener.getIntHandler().eventHandler(VcEventType.VmCloned, e);
         e = getVmEvent(Integer.toString(i), VmSuspendedEvent.class);
         MockVcEventListener.getExtHandler().eventHandler(VcEventType.VmSuspended, e);
         e = getVmEvent(Integer.toString(i), VmPoweredOffEvent.class);
         MockVcEventListener.getIntHandler().eventHandler(VcEventType.VmPoweredOff, e);
         if (sleepMS != 0) {
            Thread.sleep(sleepMS);
         }
      }
      System.out.println("Finished " + number + " events sent.");
   }

}
