/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.aurora.vc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.callbacks.VcTaskCallable;
import com.vmware.aurora.vc.callbacks.VcTaskCallableSequence;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/**
 * Created By xiaoliangl on 6/11/15.
 */
public class AsyncVcTaskMgr {
   private final static Logger LOGGER = Logger.getLogger(AsyncVcTaskMgr.class);

   private ExecutorService executorService = Executors.newCachedThreadPool();

   private final static AsyncVcTaskMgr instance = new AsyncVcTaskMgr();

   private ConcurrentMap<ManagedObjectReference, VcTaskCallable> pendingTaskMap = new ConcurrentHashMap<>();

   public static AsyncVcTaskMgr getInstance() {
      return instance;
   }

   public void submit(final VcTaskCallable vcTaskCallable) throws Exception {
      if (LOGGER.isInfoEnabled()) {
         LOGGER.info("submit VcTaskCallable");
      }

      Runnable futureTask = new Runnable() {
         @Override
         public void run() {
            VcContext.inVcSessionDo(new VcSession<VcTask>() {
               @Override
               protected boolean isTaskSession() {
                  return true;
               }

               @Override
               protected VcTask body() throws Exception {
                  VcTask vcTask = VcContext.getTaskMgr().execute(new VcTaskMgr.IVcTaskBody() {
                     @Override
                     public VcTask body() throws Exception {
                        return vcTaskCallable.call();
                     }
                  });

                  //@TODO when server is busy, this maybe slow than even notify.
                  pendingTaskMap.put(vcTask.getMoRef(), vcTaskCallable);

                  return vcTask;
               }
            });
         }

      };

      executorService.submit(futureTask);
   }


   public void shutdown() {
      executorService.shutdown();
   }

   public void submitSequence(VcTaskCallableSequence callableBatch) throws Exception {
      if (LOGGER.isInfoEnabled()) {
         LOGGER.info("submit VcTaskCallableBatch");
      }

      submit(callableBatch.getTaskCallableQueue().poll());
   }

   public void taskFinished(final VcTask task) {
      final VcTaskCallable vcTaskCallable = pendingTaskMap.get(task.getMoRef());

      Runnable futureTask = new Runnable() {
         @Override
         public void run() {
            //tempo solution for get result
            try {
               if (LOGGER.isInfoEnabled()) {
                  LOGGER.info("execute waitForCompletion() for" + task);
               }

               VcContext.inVcSessionDo(new VcSession<Void>() {
                  @Override
                  protected Void body() throws Exception {
                     vcTaskCallable.setResult(task.waitForCompletion());
                     return null;
                  }
               });


               if (LOGGER.isInfoEnabled()) {
                  LOGGER.info("VcTaskCallable finish runnable is done.");
               }

               submit(vcTaskCallable.getTaskParent().getTaskCallableQueue().poll());
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      };

      if (LOGGER.isInfoEnabled()) {
         LOGGER.info("submit VcTaskCallable finish runnable.");
      }

      executorService.submit(futureTask);
   }
}
