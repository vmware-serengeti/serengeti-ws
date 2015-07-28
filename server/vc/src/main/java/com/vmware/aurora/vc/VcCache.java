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
package com.vmware.aurora.vc;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsType;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.util.CmsWorker;
import com.vmware.aurora.util.CmsWorker.WorkQueue;
import com.vmware.aurora.util.CmsWorker.WorkerThread;
import com.vmware.aurora.vc.VcObjectImpl.UpdateType;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/**
 * {@link VcCache} maintains an in-memory cache for vc objects.
 * Each VC object is indexed by its unique moref in the cache.
 *
 * When a thread requires a reference to a VC object and misses the cache,
 * it creates a unique {@link VcObjectRequest} for the VC object and
 * queue it in {@link CmsWorker.WorkQueue.VC_QUERY_NO_DELAY}, which will
 * be processed in order by a CmsWorker thread. In processing the request,
 * a copy of the VC object is created by fetching data from VC.
 * The requesting threads wait on the request until the VC object has been
 * created and inserted into the cache.
 *
 * A VC object in the cache may have a long life and can be accessed concurrently
 * by multiple requesting threads. Some properties of the object are fetched
 * and calculated at construction time. Other properties are set to null initially
 * and are fetched dynamically on demand. The goal is to have all of VC operations
 * performed by the VC daemon, thus no VC context sessions will be needed by
 * requesting threads.
 *
 * Cached objects can be purged to guarantee no stale values of the VC object
 * can be fetched from the cache.
 *
 * Cached objects can be refreshed to update values from VC. Similar to getting
 * a missing VC Object, a request would be created and the update would be fulfilled
 * by a CmsWorker thread.
 *
 */
public class VcCache {
   private static Logger logger = Logger.getLogger(VcCache.class);
   private static VcCache instance = new VcCache();

   /**
    * Objects mapped in VC cache.
    */
   public interface IVcCacheObject {
      /**
       * @return the unique MoRef identifier
       */
      ManagedObjectReference getMoRef();
   }

   // VcObject cache
   private ConcurrentMap<ManagedObjectReference, IVcCacheObject> objCache;
   /**
    *  Reverse map from VM to RP
    *
    *  VC's VM event doesn't contain a reference to related RP, thus
    *  it's not possible to find which RP creates or deletes a VM. We keep
    *  a (VM->RP) reverse map to lookup an RP from VM moRef.
    */
   private ConcurrentMap<ManagedObjectReference, ManagedObjectReference> vmRpMap;

   public static VcCache getInstance() {
      return instance;
   }

   private VcCache() {
      objCache = new ConcurrentHashMap<ManagedObjectReference, IVcCacheObject>();
      vmRpMap = new ConcurrentHashMap<ManagedObjectReference, ManagedObjectReference>();
   }

   /**
    * Reinitialize VcCache.
    * Should only be called by JUnit tests.
    */
   public synchronized void restart() {
      // Start over.
      objCache = new ConcurrentHashMap<ManagedObjectReference, IVcCacheObject>();
      vmRpMap = new ConcurrentHashMap<ManagedObjectReference, ManagedObjectReference>();
   }

   public final static void
   putVmRpPair(ManagedObjectReference vmRef, ManagedObjectReference rpRef) {
      getInstance().vmRpMap.put(vmRef, rpRef);
   }

   public final static ManagedObjectReference
   removeVmRpPair(ManagedObjectReference vmRef) {
      return getInstance().vmRpMap.remove(vmRef);
   }

   private VcObject
   lookupVcObject(ManagedObjectReference moRef) {
      IVcCacheObject obj = objCache.get(moRef);
      if (obj instanceof VcObject) {
         return (VcObject)obj;
      }
      return null;
   }

   /**
    * Get a cached VcObject. If not cached, request a fresh copy.
    * @param moRef
    * @return the VcObject
    */
   private VcObject
   getObject(ManagedObjectReference moRef) {
      // Should not call get object from the VcObject cache worker thread itself.
      AuAssert.check(!WorkerThread.VC_QUERY_THREAD.isCurrentThread());
      VcObject obj = lookupVcObject(moRef);
      if (obj != null) {
         Profiler.inc(StatsType.VC_GET_HIT, obj);
         return obj;
      }
      Profiler.inc(StatsType.VC_GET_MISS, moRef);
      return requestObject(moRef, true, null, true);
   }

   /**
    * Get a cached VcObject with updates from VC.
    * If the object doesn't exist in the cache, creates a fresh VcObject.
    * @param moRef
    * @parem updates update requests
    * @return the VcObject
    */
   protected VcObject
   getUpdate(ManagedObjectReference moRef, EnumSet<UpdateType> updates) {
      // Should not call get object from the VcObject cache worker thread itself.
      AuAssert.check(!WorkerThread.VC_QUERY_THREAD.isCurrentThread());
      return requestObject(moRef, true, updates, true);
   }

   private VcObject
   getUpdateAsync(ManagedObjectReference moRef) {
      return requestObject(moRef, false, EnumSet.of(UpdateType.CONFIG), true);
   }

   /**
    * Sync for completing existing request on an VcObject.
    * If the object or a request on the object doesn't exist in VcCache,
    * the function returns immediately.
    * If the request generates an exception, it is ignored and return.
    * @param moRef
    * @return cached VcObject if a result is available, null otherwise
    */
   private VcObject
   syncRequest(ManagedObjectReference moRef) {
      try {
         return requestObject(moRef, true, null, false);
      } catch (VcException e) {
         if (!e.isINVALID_MOREF()) {
            logger.info("got exception " + e + " while sync the object on " + moRef);
         }
         return null;
      }
   }

   /**
    * Asynchronously update a VcObject if cached.
    * @param moRef
    * @return the cached VcObject if it exists
    */
   private VcObject
   refreshAsync(ManagedObjectReference moRef, EnumSet<UpdateType> updates) {
      return requestObject(moRef, false, updates, false);
   }

   /**
    * Send request to load or update a VcObject with synchronization.
    *
    * @param moRef
    * @param waitForRequest true if the caller blocks until the request finishes.
    * @param forcedUpdates the set of forced updates on the object
    * @param forceLoad true if force fetching the object when missing the cache.
    * @return the VcObject if available, null is possible if {@code waitForRequest}
    *         is set to false.
    */
   private VcObject
   requestObject(ManagedObjectReference moRef, boolean waitForRequest,
                 final EnumSet<UpdateType> forcedUpdates,
                 boolean forceLoad) {
      VcObjectRequest req = null;
      boolean isNewRequest = false;
      // Use objCache lock to atomically insert/lookup an entry.
      synchronized (objCache) {
         IVcCacheObject obj = objCache.get(moRef);
         // request already posted
         if (obj instanceof VcObjectRequest) {
            req = (VcObjectRequest)obj;
            AuAssert.check(req.getMoRef().equals(moRef));
            if (forcedUpdates != null) {
               // Try to renew an existing request with new updates request.
               VcObjectImpl renewResult = req.addUpdates(forcedUpdates);
               if (renewResult != null) {
                  // The existing request is done.
                  obj = renewResult;
               }
            }
         }
         if (obj == null) {
            if (forceLoad) {
               req = new VcObjectRequest(objCache, moRef);
               isNewRequest = true;
            } else {
               return null;
            }
         } else if (obj instanceof VcObject) {
            if (forcedUpdates != null) {
               req = new VcObjectRequest(objCache, (VcObjectImpl)obj, forcedUpdates);
               isNewRequest = true;
            } else {
               return (VcObject)obj;
            }
         }
      }
      if (isNewRequest) {
         // post request without holding objCache lock as we may block here
         CmsWorker.addRequest(WorkQueue.VC_QUERY_NO_DELAY, req);
      }

      if (waitForRequest && req != null) {
         // block to get result
         return req.getResult();
      } else {
         return null;
      }
   }

   public static void put(ManagedObjectReference moRef, VcObject vcObject) {
      getInstance().objCache.put(moRef, vcObject);
   }

   /**
    * Remove an old VcObject mapped by moRef in the cache.
    * @param moRef
    * @return old VcObject, null if none existed
    */
   protected VcObject remove(ManagedObjectReference moRef) {
      while (true) {
         IVcCacheObject obj;
         // Use objCache lock to atomically remove an entry.
         synchronized(objCache) {
            obj = objCache.get(moRef);
            if (obj instanceof VcObject) {
               objCache.remove(moRef);
               return (VcObject)obj;
            }
         }
         if (obj instanceof VcObjectRequest) {
            /* If renew request fails, it would return the new VC object
             * and we would try again.
             */
            if (((VcObjectRequest)obj).renew() == null) {
               return null;
            }
         } else {
            AuAssert.check(obj == null);
            return null;
         }
      }
   }

   /**
    * Gets VC object from VC cache. If not present in the cache, wait for
    * VcCacheThread to fetch value from VC and create the object.
    * @param <T>
    * @param moRef
    * @return
    */
   @SuppressWarnings("unchecked")
   public static <T extends VcObject> T get(ManagedObjectReference moRef) {
      return (T)instance.getObject(moRef);
   }

   @SuppressWarnings("unchecked")
   public static <T extends VcObject> T get(ManagedObject mo) {
      return (T)instance.getObject(mo._getRef());
   }

   /**
    * Get a list of VC Objects from cache. Throws an exception if any of
    * the passed morefs is missing.
    * @param <T>
    * @param moRefs
    * @return
    */
   public static <T extends VcObject> List<T> getList(Iterable<ManagedObjectReference> moRefs) {
      return getPartialList(moRefs, null);
   }

   /**
    * Get a list of vc objects from cache. If supplied refreshMoRef parameter
    * is not null, proceeds to retrieve a partial list in the face of missing
    * objects and initiates the update of refreshMoRef which might contain stale
    * pointers.
    * @param <T>
    * @param moRefs to get
    * @param refreshMoRef to update on any missing objects
    * @return
    */
   public static <T extends VcObject> List<T> getPartialList(
         Iterable<ManagedObjectReference> moRefs, ManagedObjectReference refreshMoRef) {
      List<T> list = new ArrayList<T>();
      boolean refresh = false;
      for (ManagedObjectReference moRef : moRefs) {
         try {
            T vcObj = VcCache.<T>get(moRef);
            if (vcObj != null) {
               list.add(vcObj);
            }
         } catch (VcException e) {
            if (e.isINVALID_MOREF()) {
               /* Ok to skip missing vc objects. */
               refresh = true;
            } else if (e.isMOREF_NOTREADY()) {
               logger.info("skipping child " + moRef + ": not ready");
            } else {
               throw e;
            }
         }
      }
      if (refresh && refreshMoRef != null) {
         VcCache.refresh(refreshMoRef);
      }
      return list;
   }

   /**
    * Looks up a VC object identified by moRef.
    * @param <T>
    * @param moRef
    * @return the object or null if not cached
    */
   @SuppressWarnings("unchecked")
   public static <T extends VcObject> T lookup(ManagedObjectReference moRef) {
      return (T)instance.lookupVcObject(moRef);
   }

   /**
    * Loads a new copy of the VC object, with value fetched
    * from VC by a worker thread.
    * @param moRef
    * @return VC object
    */
   @SuppressWarnings("unchecked")
   static public <T extends VcObject> T load(ManagedObjectReference moRef) {
      return (T)instance.getUpdate(moRef, EnumSet.of(UpdateType.CONFIG));
   }

   /**
    * Synchronously update the runtime info of an object.
    * @param <T>
    * @param moRef
    * @return the object
    */
   @SuppressWarnings("unchecked")
   static public <T extends VcObject> T loadRuntime(ManagedObjectReference moRef) {
      return (T)instance.getUpdate(moRef, EnumSet.of(UpdateType.RUNTIME));
   }

   /**
    * Same as {@code load()} except that the caller don't block on the request.
    * @param <T>
    * @param moRef
    * @return
    */
   @SuppressWarnings("unchecked")
   static protected <T extends VcObject> T loadAsync(ManagedObjectReference moRef) {
      return (T)instance.getUpdateAsync(moRef);
   }

   /**
    * Sync for existing request on an VcObject.
    * Same as {@code syncRequest()}.
    */
   static protected VcObject sync(ManagedObjectReference moRef) {
      return instance.syncRequest(moRef);
   }

   static public VcObject purge(ManagedObjectReference moRef) {
      return instance.remove(moRef);
   }

   /**
    * Asynchronously refresh the values of an object if it is cached.
    * @param moRef
    * @param updates
    */
   static private void
   refresh(ManagedObjectReference moRef, EnumSet<UpdateType> updates) {
      Profiler.inc(StatsType.VC_REFRESH);
      instance.refreshAsync(moRef, updates);
   }

   static public void refresh(ManagedObjectReference moRef) {
      refresh(moRef, EnumSet.of(UpdateType.CONFIG));
   }

   static public void refreshRuntime(ManagedObjectReference moRef) {
      refresh(moRef, EnumSet.of(UpdateType.RUNTIME));
   }

   static public void refreshAll(ManagedObjectReference moRef) {
      refresh(moRef, EnumSet.allOf(UpdateType.class));
   }

   /*
    * Get a VC task callback to refresh a VcObject asynchronously
    * and wait for refresh in the VC task caller.
    */
   static private IVcTaskCallback getRefreshVcTaskCB(final VcObject obj,
         final EnumSet<UpdateType> updates) {
      final ManagedObjectReference moref = obj.getMoRef();
      return new IVcTaskCallback() {
         @Override
         public final void completeCB(VcTask task) {
            refresh(moref, updates);
         }

         @Override
         public final void syncCB() {
            sync(moref);
            /*
             *  XXX We should be able to assert that obj == sync(moref),
             *      as CMS always references the same copy of the VcObject.
             *      Currently we're not enabling this as we haven't
             *      converted all sagas to use objects.
             */
         }
      };
   }

   /**
    * Get a callback to refresh a VcObject asynchronously
    * and wait for refresh in the VC task caller.
    * @param obj
    * @return the callback
    */
   static protected IVcTaskCallback getRefreshVcTaskCB(final VcObject obj) {
      return getRefreshVcTaskCB(obj, EnumSet.of(UpdateType.CONFIG));
   }

   static protected IVcTaskCallback getRefreshRuntimeVcTaskCB(final VcObject obj) {
      return getRefreshVcTaskCB(obj, EnumSet.of(UpdateType.RUNTIME));
   }

   static protected IVcTaskCallback getRefreshAllVcTaskCB(final VcObject obj) {
      return getRefreshVcTaskCB(obj, EnumSet.allOf(UpdateType.class));
   }

   static private ManagedObjectReference getMoRef(String id) {
      ManagedObjectReference moRef = MoUtil.stringToMoref(id);
      if (moRef == null) {
         throw VcException.INVALID_MOREF(id);
      }
      return moRef;
   }

   /**
    * Gets a VcObject identified by id.
    * @param unique id of the VcObject
    * @return the VcObject if id is valid
    * @throws VcException.INVALID_MOREF if object not found
    */
   static public <T extends VcObject> T get(String id) {
      return VcCache.<T>get(getMoRef(id));
   }

   /**
    * Similar to get(), but return null if the object is missing.
    */
   static public <T extends VcObject> T getIgnoreMissing(String id) {
      try {
         return VcCache.<T>get(id);
      } catch (VcException e) {
         if (e.isINVALID_MOREF()) {
            return null;
         }
         throw e;
      }
   }

   static public <T extends VcObject> T getIgnoreMissing(ManagedObjectReference moRef) {
      try {
         return VcCache.<T>get(moRef);
      } catch (VcException e) {
         if (e.isINVALID_MOREF()) {
            return null;
         }
         throw e;
      }
   }

   /**
    * Similar to (), but return null if the object is missing.
    */
   static public <T extends VcObject> T loadIgnoreMissing(String id) {
      try {
         return VcCache.<T>load(getMoRef(id));
      } catch (VcException e) {
         if (e.isINVALID_MOREF()) {
            return null;
         }
         throw e;
      }
   }

   /**
    * Force reload values of a VcObject.
    * @param id
    * @return
    * @throws Exception
    */
   static public VcObject load(String id) {
      return load(getMoRef(id));
   }

   /**
    * Synchronously load the runtime info of a VcObject.
    * @param <T>
    * @param id
    * @return
    */
   static public <T extends VcObject> T loadRuntime(String id) {
      return VcCache.<T>loadRuntime(getMoRef(id));
   }
}
