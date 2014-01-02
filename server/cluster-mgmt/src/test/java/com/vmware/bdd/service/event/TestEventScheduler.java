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

import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Author: Xiaoding Bian
 * Date: 1/2/14
 * Time: 5:55 PM
 */
public class TestEventScheduler {

   private static final int seqNum = 500;
   private static final int vmNum = 5;
   private static final int inspecIntervalInSec = 1;

   public static class SimpleEventManager implements IEventProcessor {
      private EventScheduler eventScheduler;
      private BlockingQueue<IEventWrapper> consumeSeq;
      public SimpleEventManager() {
         this.eventScheduler = new EventScheduler(this, inspecIntervalInSec);
         this.consumeSeq = new LinkedBlockingDeque<IEventWrapper>();
      }

      public void start() {
         this.eventScheduler.start();
      }

      public void stop() {
         this.eventScheduler.stop();
      }

      @Override
      public void produceEvent(BlockingQueue<IEventWrapper> produceQueue) {
         Producer producer = new Producer(produceQueue);
         producer.start();
      }

      private class Producer extends Thread{
         private BlockingQueue<IEventWrapper> produceQueue;
         public Producer(BlockingQueue<IEventWrapper> produceQueue) {
            this.produceQueue = produceQueue;
         }

         @Override
         public void run() {
            for (int i = 0; i < seqNum; i++) {
               SimpleEventWrapper event = new SimpleEventWrapper(i, "vm-" + (int) (Math.random() * vmNum));
               try {
                  produceQueue.put(event);
                  Thread.sleep((int) (Math.random() * 20));
               } catch (InterruptedException e) {
               }
            }
         }
      }

      @Override
      public void consumeEvent(List<IEventWrapper> toProcessEvents) {
         for (IEventWrapper event : toProcessEvents) {
            try {
               Thread.sleep((int) (Math.random() * 50));
               consumeSeq.put(event);
            } catch (InterruptedException e) {
            }
         }
      }

      public static class SimpleEventWrapper implements IEventWrapper {
         private int seq;
         private String name;

         public SimpleEventWrapper(int seq, String name) {
            this.seq = seq;
            this.name = name;
         }

         public int getSeq() {
            return seq;
         }

         @Override
         public String getPrimaryKey() {
            return name;
         }

         @Override
         public String toString() {
            return (new Gson()).toJson(this);
         }
      }

      public boolean validateConsumeSeq() {

         List<IEventWrapper> consumeList = new LinkedList<IEventWrapper>();
         consumeSeq.drainTo(consumeList);
         Map<String, Integer> lastSeq = new HashMap<String, Integer>();
         for (IEventWrapper event : consumeList) {
            SimpleEventWrapper simpleEventWrapper = (SimpleEventWrapper) event;
            Integer lastId = lastSeq.get(simpleEventWrapper.getPrimaryKey());
            if (lastId == null) {
               lastSeq.put(simpleEventWrapper.getPrimaryKey(), simpleEventWrapper.getSeq());
               continue;
            }
            if (lastId > simpleEventWrapper.getSeq()) {
               return false;
            }
            lastSeq.put(event.getPrimaryKey(), ((SimpleEventWrapper) event).getSeq());
         }
         for (String name : lastSeq.keySet()) {
            System.out.println("key=" + name + ", lastseq=" + lastSeq.get(name));
         }
         return true;
      }
   }

   @Test
   public void testPositive1() throws InterruptedException {
      SimpleEventManager simpleEventManager = new SimpleEventManager();
      simpleEventManager.start();
      Thread.sleep(10000);
      simpleEventManager.stop();
      Assert.assertTrue(simpleEventManager.validateConsumeSeq());
   }
}
