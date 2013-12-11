package com.vmware.bdd.aop.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockFactory {

   private static ConcurrentHashMap<String, ReentrantReadWriteLock> clusterLocks =
         new ConcurrentHashMap<String, ReentrantReadWriteLock>();

   public static ReentrantReadWriteLock getClusterLock(String clusterName) {
      if (clusterLocks.get(clusterName) == null) {
         clusterLocks.putIfAbsent(clusterName, new ReentrantReadWriteLock());
      }
      return clusterLocks.get(clusterName);
   }
}
