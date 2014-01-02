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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author: Xiaoding Bian
 * Date: 1/2/14
 * Time: 11:22 AM
 */

public class KeyedReentrantLock<K> {

   private ConcurrentMap<K, ReentrantLock> lockMap;
   private boolean fairSchedule;

   public KeyedReentrantLock() {
      this(false);
   }

   public KeyedReentrantLock(boolean fairSchedule) {
      lockMap = new ConcurrentHashMap<K, ReentrantLock>();
      this.fairSchedule = fairSchedule;
   }

   /**
    * blocking if lock is held by another thread
    * @param key
    */
   public void lock(K key) {
      getLockInstance(key).lock();
   }

   /**
    * unblocking lock request
    * @param key
    * @return
    */
   public boolean tryLock(K key) {
      return getLockInstance(key).tryLock();
   }

   /**
    * unlock TODO: throw exception if does not contain this key
    * @param key
    */
   public void unlock(K key) {
      getLockInstance(key).unlock();
   }

   /**
    * check if locked
    * @param key
    * @return
    */
   public boolean isLocked(K key) {
      return getLockInstance(key).isLocked();
   }

   public boolean isHeldByCurrentThread(K key) {
      return getLockInstance(key).isHeldByCurrentThread();
   }

   private ReentrantLock getLockInstance(K key) {
      if (!lockMap.containsKey(key)) {
         ReentrantLock lock = new ReentrantLock(fairSchedule);
         lockMap.putIfAbsent(key, lock);
      }
      return lockMap.get(key);
   }

   /**
    * inspect this instance, not an atomic info
    * @return
    */
   @Override
   public String toString() {
      StringBuffer stringBuffer = new StringBuffer();
      for (K key : lockMap.keySet()) {
         stringBuffer.append("  key: " + key.toString() + ", locked: " + getLockInstance(key).isLocked()
               + ", isHeldedByCurrentThread: " + getLockInstance(key).isHeldByCurrentThread()
               + ", queueLength: " + getLockInstance(key).getQueueLength() + "\n");
      }
      return stringBuffer.toString();
   }

   //TODO: whenever a lock is initialized, it will never been destroyed, add an cleanup method to free memory
}
