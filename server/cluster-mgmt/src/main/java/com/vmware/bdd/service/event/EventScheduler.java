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

import com.vmware.aurora.global.Configuration;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Author: Xiaoding Bian
 * Date: 1/2/14
 * Time: 1:35 PM
 *
 * This is a generic framework to accelerate VC events handling.
 * First, while the message is received, put it into one sequential queue,
 * and there is one dedicated thread, message expander, processing the messages one by one.
 * The first queue is to make sure the event receiver not blocked by message processing.
 * Secondly, the message expander will put all events to a set of queues and keep the
 * received order, classified by moid.
 * A thread pool(currently with fixed number) is also monitoring these queues. While there
 * is new events coming, it will try to pick up events of a queue to process.
 */
public class EventScheduler {
   private static final Logger logger = Logger.getLogger(EventScheduler.class);
   volatile private boolean running = true;
   private int processorNum;
   private static final int threadPriority = Thread.MAX_PRIORITY;
   private BlockingQueue<IEventWrapper> produceQueue;
   //private static final ThreadLocal<Object> CurItem = new ThreadLocal<Object>();
   private Expander expander;
   private Handler handler;
   // controlled by expander
   private ConcurrentMap<String, BlockingQueue<IEventWrapper>> eventsMapByKey
         = new ConcurrentHashMap<String, BlockingQueue<IEventWrapper>>();
   private KeyedReentrantLock<String> keyedReentrantLock;
   private IEventProcessor eventProcessor;
   private int inspectIntervalInSec;

   public EventScheduler(IEventProcessor eventProcessor) {
      this(eventProcessor, 60);
   }

   public EventScheduler(IEventProcessor eventProcessor, int inspectIntervalInSec) {
      this.eventProcessor = eventProcessor;
      this.produceQueue = new LinkedBlockingQueue<IEventWrapper>();
      this.keyedReentrantLock = new KeyedReentrantLock<String>();
      this.inspectIntervalInSec = inspectIntervalInSec;
      String poolsize = Configuration.getNonEmptyString("serengeti.event_processor.poolsize");
      if (poolsize != null) {
         this.processorNum = Integer.parseInt(poolsize);
      } else {
         this.processorNum = 8;
      }
   }

   public synchronized void start() {
      eventProcessor.produceEvent(produceQueue); // launch producer thread
      expander = new Expander();
      expander.start();
      handler = new Handler();
      handler.start();
   }

   public synchronized void stop() {
      logger.info("stopping Event Scheduler");
      running = false;
      if (handler != null) {
         handler.interrupt();
         handler.doStop();
      }
      if (expander != null) {
         expander.interrupt();
      }
   }

   /**
    * add events from "produceQueue" to "eventsMapByKey"
    */
   private class Expander extends Thread {
      private Map<String, List<IEventWrapper>> toExpandEvents;
      private final int expanderInterval = 300;
      public Expander() {
         toExpandEvents = new HashMap<String, List<IEventWrapper>>();
         this.setDaemon(true);
         this.setName("Event_Expander");
         this.setPriority(threadPriority);
      }

      @Override
      public void run() {
         logger.info(getName() + ": starting");
         List<IEventWrapper> eventList = new ArrayList<IEventWrapper>();
         while(running) {
            try {
               if (produceQueue.size() > 0) {
                  eventList.clear();
                  produceQueue.drainTo(eventList);
                  expand(eventList);
               }

               sleep(expanderInterval);
            } catch (InterruptedException e) {
               if (running) {
                  logger.error(getName() + " caught: " + e);
               }
            } catch (Exception e) {
               logger.error(getName() + " caught: " + e);
            }

         }
         logger.info(getName() + ": exiting");
      }

      private void expand(List<IEventWrapper> eventList) {
         if (!eventList.isEmpty()) {
            logger.info("expanding " + eventList.size() + " new events");
            logger.info("Events to expand: " + eventList.toString());
         }
         for (IEventWrapper event : eventList) {
            String key = event.getPrimaryKey();
            List<IEventWrapper> vmEventList = toExpandEvents.get(key);
            if (vmEventList == null) {
               vmEventList = new LinkedList<IEventWrapper>();
               toExpandEvents.put(key, vmEventList);
            }
            vmEventList.add(event);
         }

         for (String key : toExpandEvents.keySet()) {
            if (!toExpandEvents.get(key).isEmpty()) {
               if (!keyedReentrantLock.tryLock(key)) {
                  // skip this VM immediately if cannot retrieve lock
                  continue;
               }

               BlockingQueue<IEventWrapper> eventQueue = eventsMapByKey.get(key);
               if (eventQueue == null) {
                  eventQueue = new LinkedBlockingQueue<IEventWrapper>();
                  eventsMapByKey.put(key, eventQueue);
               }

               try {
                  Iterator<IEventWrapper> iterator = toExpandEvents.get(key).iterator();
                  while (iterator.hasNext()) {
                     IEventWrapper event = iterator.next();
                     // TODO: now queues of "eventsMapByKey" do not have capacity limitation, no need to consider this case
                     if (!eventQueue.offer(event)) {
                        break;
                     } else {
                        iterator.remove();
                     }
                  }
               } finally {
                  if (keyedReentrantLock.isLocked(key) && keyedReentrantLock.isHeldByCurrentThread(key)) {
                     keyedReentrantLock.unlock(key);
                  }
               }
            }
         }
      }
   }

   /**
    * launch a threadpool to pick up and process events from "eventsMapByKey"
    */
   private class Handler extends Thread {
      private ExecutorService processerPool;
      private BlockingQueue<IEventWrapper> consumedEvents;

      public Handler() {
         // for monitor
         consumedEvents = new LinkedBlockingQueue<IEventWrapper>();
         processerPool = Executors.newFixedThreadPool(processorNum);
         for (int i = 0; i < processorNum; i++) {
            Processor processor = new Processor(i);
            processerPool.execute(processor);
         }

         this.setDaemon(true);
         this.setName("Event_Handler");
         this.setPriority(threadPriority);
      }

      private class Processor implements Runnable {
         private String name;
         public Processor(int index) {
            this.name = "Event_Processor-" + index;
         }

         public String getProcessorName() {
            return name;
         }

         @Override
         public void run() {
            List<IEventWrapper> toProcessList = new ArrayList<IEventWrapper>();
            while(running) {
               try {
                  Thread.sleep((int)(Math.random() * 100));
               } catch (InterruptedException e) {
                  if (running) {
                     logger.error(getName() + " caught: " + e);
                  }
               }

               // TODO: subscribe signal rather than loop
               for (String key : eventsMapByKey.keySet()) {
                  try {
                     if (!keyedReentrantLock.tryLock(key)) {
                        continue;
                     }

                     BlockingQueue<IEventWrapper> eventQueue = eventsMapByKey.get(key);
                     if (eventQueue == null || eventQueue.isEmpty()) {
                        continue;
                     }

                     toProcessList.clear();
                     eventQueue.drainTo(toProcessList);
                     eventProcessor.consumeEvent(toProcessList);
                     for (IEventWrapper event : toProcessList) {
                        // add to consumed events list for monitoring
                        consumedEvents.put(event);
                     }
                  } catch (InterruptedException e) {
                     if (running) {
                        logger.error(getName() + "caught: " + e);
                     }

                  } finally {
                     if (keyedReentrantLock.isLocked(key) && keyedReentrantLock.isHeldByCurrentThread(key)) {
                        keyedReentrantLock.unlock(key);
                     }
                  }
               }
            }
            logger.info(getName() + ": exiting");
         }
      }

      @Override
      public void run() {
         logger.info(getName() + ": starting");

         List<IEventWrapper> consumeList = new LinkedList<IEventWrapper>();
         while(running) {
            try {
               TimeUnit.SECONDS.sleep(inspectIntervalInSec);
               consumeList.clear();
               consumedEvents.drainTo(consumeList);
               if (!consumeList.isEmpty()) {
                  logger.info("processed " + consumeList.size() + " new events");
                  // TODO: log processor thread id/name too, in DEBUG level
                  logger.info("Events processed: " + consumeList.toString());
               }

            } catch (InterruptedException e) {
               if (running) {
                  logger.error(getName() + " caught: " + e);
               }
            } catch (Exception e) {
               logger.error(getName() + " caught: " + e);
            }

         }
         logger.info(getName() + ": exiting");
      }

      public synchronized void doStop() {
         processerPool.shutdown();
      }
   }
}
