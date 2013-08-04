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
package com.vmware.aurora.vc.vcservice;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.vim.binding.vim.fault.NotAuthenticated;
import com.vmware.vim.vmomi.core.exception.InternalException;
import com.vmware.vim.vmomi.core.exception.UnmarshallException;

/**
 * A long running tasks in VC generally return a task object to monitor
 * progress and return immediately. Synchronous task executions are
 * meant to be short, but some may not be.
 *
 * This handler is for VC long running tasks that do not return a task object.
 * If the task runs longer than HTTP session timeout, an HTTP timeout exception
 * would be thrown. As a result, such exception shouldn't be treated as
 * VC connection failure.
 *
 * Besides, as the call would block the HTTP request thread, we shouldn't
 * let such calls share the same thread/thread pool as other requests.
 * Thus, this thread is specially designed to handle this case.
 */
public class VcLongCallHandler extends Thread {
   private static Logger logger = Logger.getLogger(VcLongCallHandler.class);
   static final int LONGCALL_RETRIES = 3;

   /**
    * A long running VC call.
    *
    * @param <T> type of result (Void if no result)
    */
   public static abstract class VcLongCall<T> {
      private int retries = LONGCALL_RETRIES;
      private boolean done = false;
      private T result = null;
      private Throwable exception = null;

      public VcLongCall() { }

      /**
       * Implementation of the VC call executed by the VcLongCallHandler thread.
       * @return
       * @throws Exception
       */
      abstract protected T callVc() throws Exception;

      /**
       * Undo the VC call. Override if requires actual VC call.
       * @throws Exception
       */
      protected void callUndoVc() throws Exception {

      }

      /*
       * Block and wait for VC call to finish.
       */
      private synchronized T waitForResult() {
         while (!done) {
            try {
               wait();
            } catch (InterruptedException e) {
               logger.info("ignore interruption waiting for long call");
            }
         }
         if (exception != null) {
            throw VcException.GENERAL_ERROR(exception);
         }
         return result;
      }

      /*
       * Execute VC call and notify the caller thread.
       */
      private synchronized void exec() {
         try {
            if (--retries > 0) {
               if (exception != null) {
                  callUndoVc();
               }
               // execute the VC call
               result = callVc();
               // clear the exception if the call succeeds
               exception = null;
            }
         } catch (Throwable e) {
            // remember the last exception
            exception = e;
            if (e instanceof RejectedExecutionException ||
                e instanceof IllegalStateException ||
                e instanceof NotAuthenticated ||
                e instanceof InternalException ||
                e instanceof UnmarshallException) {
               /*
                * Throw exception on possible VC connection errors.
                * The outer loop will retry the current call.
                */
               throw (RuntimeException)e;
            } else {
               logger.info("skipping regular long call exception " + e);
            }
         }
         /* We are done with this call. Here are the cases:
          * 1. The call is executed successfully. (exception == null)
          * 2. The call fails with non VC connection exception.
          * 3. The call fails with a VC connection exception after a few attempts.
          */
         done = true;
         notifyAll();
      }
   }

   private Semaphore initSema;                  // Signals when init completes.
   private LinkedBlockingQueue<VcLongCall<?>> calls;       // FIFO queue of calls

   /* Written by other threads */
   private volatile boolean stopRequested;      // Stop requested?

   protected VcLongCallHandler() {
      stopRequested = false;
      initSema = new Semaphore(0);
      calls = new LinkedBlockingQueue<VcLongCall<?>>();
      setName("VcLongCallHandler");
   }

   /**
    * VcLongCallHandler creator thread can wait here until the listener is
    * fully initialized.
    * @throws InterruptedException
    */
   public void waitUntilStarted() throws InterruptedException {
      AuAssert.check(Thread.currentThread() != this);
      initSema.acquire();
   }

   /**
    * Request Call Handler shutdown and wait until it completes.
    * @throws InterruptedException
    */
   public void shutDown() {
      if (Thread.currentThread() == this) {
         return;
      }
      /* Wait until the Long Call Hander breaks out of the loops. */
      stopRequested = true;
      interrupt();

      while (true) {
         try {
            join();
            logger.info("VcLongCallHandler joined");
            break;
         } catch (InterruptedException e) {
            logger.debug("Interrupted while waiting for VcLongCallHandler shutdown");
         }
      }
   }

   /*
    * Loop for getting calls and process them.
    */
   private void processCallsLoop() {
      VcLongCall<?> call = null;
      while (!stopRequested) {
         try {
               // Only take the next call if the previous call is done.
               if (call == null || call.done) {
                  call = calls.take();
               }
               call.exec();
         } catch (InterruptedException e) {
            logger.debug("long call handler interrupted");
         } catch (Throwable e) {
            logger.error("VcLongCallHandler got runtime exception", e);
            /*
             * VC session went bad. Logout to force automatic relogin that will
             * trigger the establishment of the new VC session. On stop, logout
             * happens along with other cleanup.
             */
            VcService vcService = VcContext.getService();
            if (vcService == null) {
               /*
                * Tomcat likes to clean out ThreadLocal on shutdown. We might land here
                * if something went wrong with the thread executing WebServiceContextListener
                * callbacks before it had a chance to cleanly shut down VcEventListener.
                * Terminate the thread - Bug 733665.
                */
               logger.info("thread local variable for vcService already cleared");
               break;
            } else {
               logger.info("logout vcService " + vcService.getServiceName());
               try {
                  vcService.logout();
               } catch (Throwable t) {
                  logger.error("VcLongCallHandler met unexpected exception", t);
               }
            }
         }
      }
   }

   /**
    * Activate VC Long Call Handler thread.
    */
   public void run() {
      VcContext.startLongCallSession();
      try {
         initSema.release();    // Wake up the creator.
         processCallsLoop();
      } finally {
         VcContext.endSession();
      }
   }

   /**
    * Run a VC long call and wait for result.
    * @param <T> type of the result
    * @param call
    * @return the result
    */
   public <T> T execute(VcLongCall<T> call) {
      calls.add(call);
      return call.waitForResult();
   }
}
