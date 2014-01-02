/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.service.event;

import org.testng.annotations.Test;

/**
 * Author: Xiaoding Bian
 * Date: 1/2/14
 * Time: 11:33 AM
 */
public class TestKeyedReentrantLock {

   private final int workerNum = 5;
   private final int roundOfEachWroker = 20;
   private final int interval = 50;
   private String[] vms = {"vm-0", "vm-1", "vm-2", "vm-3"};

   private class Worker extends Thread {
      private KeyedReentrantLock<String> keyedReentrantLock;
      private boolean blocking;

      public Worker(KeyedReentrantLock<String> keyedReentrantLock, int index, boolean blocking) {
         this.keyedReentrantLock = keyedReentrantLock;
         this.setName("worker-" + index);
         this.blocking = blocking;
      }

      @Override
      public void run() {
         for (int i = 0; i < roundOfEachWroker; i++) {
            String vm = vms[(int) (Math.random() * vms.length)];
            System.out.println(getName() + " try to handle vm: " + vm + ", round=" + i);
            boolean success = false;
            if (blocking) {
               keyedReentrantLock.lock(vm);
            } else {
               success = keyedReentrantLock.tryLock(vm);
            }
            if (blocking || success) {
               System.out.println(getName() + " successfully locked vm: " + vm + ", round=" + i);
               try {
                  Thread.sleep((int) (Math.random() * interval * 2) );
               }
               catch(InterruptedException e) {

               } finally {
                  if (keyedReentrantLock.isLocked(vm)) {
                     System.out.println(getName() + " released vm: " + vm + ", round=" + i);
                     keyedReentrantLock.unlock(vm);
                  } else {
                     System.out.println(getName() + " no need to release vm: " + vm + ", round=" + i);
                  }
               }
            } else {
               System.out.println(getName() + " failed to lock vm: " + vm + ",round=" + i);
               try {
                  Thread.sleep((int) (Math.random() * interval * 2) );
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
         }

      }
   }

   private void testKeyedReentrantLock(boolean blocking) {
      KeyedReentrantLock<String> keyedReentrantLock = new KeyedReentrantLock<String>();
      Thread[] workers = new Thread[workerNum];
      for (int i = 0; i < workerNum; i++) {
         Worker worker = new Worker(keyedReentrantLock, i, blocking);
         workers[i] = new Thread(worker);
         workers[i].start();
      }

      for (int i = 0; i < roundOfEachWroker; i++) {
         System.out.println(keyedReentrantLock.toString());
         try {
            Thread.sleep(interval);
         } catch (InterruptedException e) {
         }
      }

      for (int i = 0; i < workerNum; i++) {
         try {
            workers[i].join();
         } catch (InterruptedException e) {
         }
      }

      System.out.println(keyedReentrantLock.toString());
   }

   @Test(groups = "TestKeyedReentrantLock")
   public void testBlockingLock() {
      System.out.println("starting BLOCKING lock test");
      testKeyedReentrantLock(true);
      System.out.println("blocking lock test done");
   }

   @Test(groups = "TestKeyedReentrantLock")
   public void testNonBlockingLock() {
      System.out.println("starting NON-BLOCKING lock test");
      testKeyedReentrantLock(false);
      System.out.println("non-blocking lock test don");
   }
}
