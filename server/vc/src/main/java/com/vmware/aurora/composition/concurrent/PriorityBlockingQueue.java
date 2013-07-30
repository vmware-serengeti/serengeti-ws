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

package com.vmware.aurora.composition.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.vmware.aurora.util.AuAssert;

/**
 * An unbounded {@linkplain BlockingQueue blocking queue} that maintains a separate
 * queue for each priority and supplies blocking retrieval operations.
 * While this queue is logically unbounded, attempted additions may fail due to
 * resource exhaustion (causing <tt>OutOfMemoryError</tt>).
 * This class does not permit <tt>null</tt> elements.
 *
 * The element removal methods, {@link #poll() poll}, {@link #poll(long, TimeUnit) poll(timeout)},
 * and {@link #take() take} will check the {@link PriorityThreadPoolExecutor} which priority
 * has less active tasks than allowed maximal concurrent tasks, then first element of the queue
 * for that priority will be returned if that queue is not empty,
 * if it's empty, other queues will be checked, high priority is preferred.
 *
 * @author Xin Li (xinli)
 *
 */

class PriorityBlockingQueue<E extends PriorityFutureTask<?>> extends AbstractQueue<E> implements BlockingQueue<E> {

   private final ReentrantLock lock = new ReentrantLock(false);
   private final Condition notEmpty = lock.newCondition();

   private final Queue<E>[] queues;
   private PriorityThreadPoolExecutor executor;

   /**
    * Creates a <tt>PriorityBlockingQueue</tt> with given number of all possible priorities.
    * @param queueCount Number of all possible priorities.
    */
   @SuppressWarnings("unchecked")
   PriorityBlockingQueue(int queueCount) {
      queues = new Queue[queueCount];
      for (int i = 0; i < queueCount; ++i) {
         queues[i] = new LinkedList<E>();
      }
   }

   void setPriorityThreadPoolExecutor(final PriorityThreadPoolExecutor executor) {
      this.executor = executor;
   }

   @Override
   public E poll() {
      lock.lock();
      try {
         int idx = executor.getNextPriorityToProcess();
         E e = queues[idx].poll();
         if (e == null) {
            idx = 0;
            for(Queue<E> q : queues) {
               e = q.poll();
               if (e != null) {
                  break;
               }
               ++idx;
            }
         }
         if (e != null) {
            executor.beforeExecute(idx);
         }
         return e;
     } finally {
         lock.unlock();
     }
   }

   @Override
   public E peek() {
      lock.lock();
      try {
         int idx = executor.getNextPriorityToProcess();
         E e = queues[idx].peek();
         if (e == null) {
            for (Queue<E> q : queues) {
               e = q.peek();
               if (e != null) {
                  break;
               }
            }
         }
         return e;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean offer(E e) {
      int idx = e.getPriority().ordinal();
      lock.lock();
      try {
         boolean ret = queues[idx].offer(e);
         notEmpty.signal();
         return ret;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public void put(E e) throws InterruptedException {
      offer(e); // no need to block
   }

   @Override
   public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
      return offer(e); // no need to block
   }

   @Override
   public E take() throws InterruptedException {
      lock.lockInterruptibly();
      try {
         try {
            while (size() == 0) {
               notEmpty.await();
            }
         } catch(InterruptedException e) {
            notEmpty.signal(); // propagate to non-interrupted thread
            throw e;
         }

         E e = poll();
         AuAssert.check(e != null);
         return e;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E poll(long timeout, TimeUnit unit) throws InterruptedException {
      long nanos = unit.toNanos(timeout);
      lock.lockInterruptibly();
      try {
         for (;;) {
            E x = poll();
            if (x != null)
               return x;
            if (nanos <= 0)
               return null;
            try {
               nanos = notEmpty.awaitNanos(nanos);
            } catch (InterruptedException ex) {
               notEmpty.signal(); // propagate to non-interrupted thread
               throw ex;
            }
         }
      } finally {
         lock.unlock();
      }
   }

   @Override
   public int remainingCapacity() {
      return Integer.MAX_VALUE;
   }

   @Override
   public int drainTo(Collection<? super E> c) {
      if (c == null)
         throw new NullPointerException();
      if (c == this)
         throw new IllegalArgumentException();

      lock.lock();
      try {
         int n = 0;
         E e;
         for (Queue<E> q : queues) {
            while ( (e = q.poll()) != null) {
               c.add(e);
               ++n;
            }
         }
         return n;
     } finally {
         lock.unlock();
     }
  }

   @Override
   public int drainTo(Collection<? super E> c, int maxElements) {
      if (c == null)
         throw new NullPointerException();
     if (c == this)
         throw new IllegalArgumentException();
     if (maxElements <= 0)
         return 0;
     lock.lock();
     try {
         int n = 0;
         E e;
         for (Queue<E> q : queues) {
            while (n < maxElements && (e = q.poll()) != null) {
               c.add(e);
               ++n;
            }
         }
         return n;
     } finally {
         lock.unlock();
     }
   }

   /**
    * This method is not used by <tt>PriorityThreadPoolExecutor</tt>
    * @throws UnsupportedOperationException
    */
   @Override
   public Iterator<E> iterator() {
      throw new UnsupportedOperationException();
   }

   @Override
   public int size() {
      int size = 0;
      for (Queue<E> q : queues) {
         size += q.size();
      }
      return size;
   }
}
