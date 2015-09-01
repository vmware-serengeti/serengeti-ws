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

import com.vmware.aurora.exception.AuroraException;
import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsType;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.util.worker.Request;
import com.vmware.aurora.vc.VcCache.IVcCacheObject;
import com.vmware.aurora.vc.VcObjectImpl.UpdateType;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentMap;

/**
 * Request for a VC object.
 *
 * A request is added to VC cache in place of the VcObject itself.
 * All requesting threads would block on the request until the request
 * has been processed and the request object being replaced by the
 * resulting VcObject.
 */
public class VcObjectRequest extends Request implements IVcCacheObject {
   private ConcurrentMap<ManagedObjectReference, IVcCacheObject> map;

   private ManagedObjectReference moRef;
   private VcObjectImpl updateObj = null;
   private EnumSet<UpdateType> updates = EnumSet.noneOf(UpdateType.class);

   // set exception if failed to get the result
   private AuroraException exception = null;
   // result of the request
   private VcObjectImpl result = null;
   // set the flag to request fetch data from VC again
   private boolean renewRequested = false;

   /**
    * Request to create a new VcObject.
    * @param map
    * @param moRef
    */
   protected VcObjectRequest(ConcurrentMap<ManagedObjectReference, IVcCacheObject> map,
                             ManagedObjectReference moRef) {
      super(Profiler.getStatsEntry(StatsType.VC_LOAD_REQ, moRef));
      this.map = map;
      this.moRef = moRef;
      AuAssert.check(Thread.holdsLock(map));
      map.put(moRef, this);
   }

   /**
    * Request to update an existing VcObject.
    * @param map
    * @param obj
    */
   protected VcObjectRequest(ConcurrentMap<ManagedObjectReference, IVcCacheObject> map,
                             VcObjectImpl obj, EnumSet<UpdateType> updates) {
      super(Profiler.getStatsEntry(StatsType.VC_UPDATE_REQ, obj));
      this.map = map;
      this.moRef = obj.getMoRef();
      this.updateObj = obj;
      this.updates = updates;
      AuAssert.check(Thread.holdsLock(map));
      map.put(moRef, this);
   }

   @Override
   public ManagedObjectReference getMoRef() {
      return moRef;
   }

   /*
    * Block for result, or exception on error.
    */
   protected synchronized VcObjectImpl getResult() {
      while (true) {
         if (exception != null) {
            throw AuroraException.stitchStackTraces(exception);
         }
         if (result != null) {
            return result;
         }
         try {
            wait();
         } catch (InterruptedException e) {
            throw AuroraException.INTERNAL(e);
         }
      }
   }

   /*
    * Set exception object and unblock all waiters.
    */
   private synchronized void setException(AuroraException e) {
      AuAssert.check(exception == null && result == null);
      // remove the objCache entry
      map.remove(moRef);
      exception = e;
      notifyAll();
   }

   private synchronized void clearRenewRequest() {
      renewRequested = false;
   }

   /*
    * Set result object and unblock all waiters.
    */
   private synchronized boolean setResult(VcObjectImpl obj) {
      AuAssert.check(exception == null && result == null);
      AuAssert.check(obj.getMoRef().equals(moRef));
      if (renewRequested) {
         return false;
      }
      // replace the objCache entry with real object
      map.put(moRef, obj);
      result = obj;
      notifyAll();
      return true;
   }

   /*
    * Execute this request.
    * Note: should only be called by VcCacheThread
    */
   @Override
   protected boolean execute() {
      try {
         /*
          * Repeat until we successfully set the result.
          */
         while (true) {
            clearRenewRequest();
            VcObjectImpl obj;
            try {
               obj = VcContext.inVcSessionDo(new VcSession<VcObjectImpl>() {
                  public VcObjectImpl body() throws Exception {
                     if (updateObj != null) {
                        updateObj.updateInternal(updates);
                        return updateObj;
                     } else {
                        return VcObjectImpl.loadFromMoRef(moRef);
                     }
                  }
               });
            } catch (AuroraException e) {
               if (e.getCause() instanceof ManagedObjectNotFound) {
                  // Set INVALID_MOREF if the managed object doesn't exist.
                  setException(VcException.INVALID_MOREF(MoUtil.morefToString(moRef)));
               } else {
                  setException(e);
               }
               return true;
            }
            if (setResult(obj)) {
               return true;
            }
         }
      } catch (Throwable e) {
         setException(VcException.GENERAL_ERROR(e));
      }
      return true;
   }

   /*
    * Abort this request by sending exception to all requesters.
    * Note: should only be called by VcCacheThread
    */
   protected void abort() {
      setException(AuroraException.INTERNAL());
   }

   /**
    * Renew this request so that we'll fetch new data.
    * @return null on success and the object if the request was too late.
    */
   protected synchronized VcObjectImpl renew() {
      if (result != null) {
         /* The renew request came in too late.
          * Return the existing object and let the caller handle it.
          */
         return result;
      }
      if (exception != null) {
         // no need to do anything
         return null;
      }
      renewRequested = true;
      return null;
   }

   /**
    * Add update requirements and renew request if it's already finished.
    * @param updates
    * @return
    */
   protected synchronized VcObjectImpl addUpdates(EnumSet<UpdateType> updates) {
      this.updates.addAll(updates);
      return renew();
   }

   @Override
   protected void cleanup() {
      AuAssert.unreachable();
   }
}
