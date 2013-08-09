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

/**
 * <code>VcCleaner</code> is a thread that asynchronously cleans up vc
 * connections when they go bad or on shutdown. It needs to be in a separate
 * thread from the requester because logging out from larger vc sessions
 * (with server side state: property collectors, filters, etc.) might take
 * dozens of seconds. It is also responsible for tearing down all physical
 * resources associated with bad vc session: thread pools, etc.
 *
 * @since   0.7
 * @version 0.7
 * @author Boris Weissman
 */

package com.vmware.aurora.vc.vcservice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.vmware.aurora.util.AuAssert;
import com.vmware.vim.binding.vim.SessionManager;
import com.vmware.vim.binding.vim.fault.NotAuthenticated;
import com.vmware.vim.vmomi.client.Client;
import com.vmware.vim.vmomi.client.http.HttpConfiguration;

public class VcCleaner extends Thread {
   /**
    * Base class for VcCleaner operations.
    */
   private abstract class Op {
      protected String name;              // Operation name.

      protected String getName() {
         return name;
      }
   }

   /**
    * A vc logout request.
    */
   private class LogoutOp extends Op {
      private Client         vmomiClient;
      private SessionManager sessionManager;
      private ExecutorService executor;
      private HttpConfiguration httpConfig;
      private Thread         submitterThread;
      private String         serviceName;
      private long           submitNanos;

      private LogoutOp(Thread submitterThread, String serviceName,
            Client vmomiClient, SessionManager sessionManager,
            ExecutorService executor, HttpConfiguration httpConfig) {
         this.submitterThread = submitterThread;
         this.serviceName = serviceName;
         this.vmomiClient = vmomiClient;
         this.sessionManager = sessionManager;
         this.executor = executor;
         this.httpConfig = httpConfig;
         this.submitNanos = System.nanoTime();
         this.name = "logout";
      }

      /**
       * Logout from vc and tear down all resources associated with the
       * client: thread pool, etc. The client resources are freed even if
       * we fail to execute a proper wdsl logout on vc.
       * @throws Exception
       */
      private void logout() throws Exception {
         Exception logoutException = null;
         try {
            if (sessionManager != null) {
               sessionManager.logout();
            }
         } catch (Exception e) {
            if (e instanceof NotAuthenticated) {
               // If session has expired, this is expected.
               logger.info("Expected: got NotAuthenticated when logging out "
                           + serviceName);
            } else {
               logoutException = e;
            }
         }
         if (vmomiClient != null) {
            vmomiClient.shutdown();
         }
         if (httpConfig != null) {
            httpConfig.shutdown();
         }
         if (executor != null) {
            int count = 0;
            executor.shutdown();
            while (!executor.isTerminated()) {
               executor.awaitTermination(1, TimeUnit.MINUTES);
               if (!executor.isTerminated()) {
                  ++count;
                  logger.warn("started terminating " + serviceName +
                        " for executor " + executor + " for " + count + " minutes");
               }
            }
         }

         if (logoutException != null) {
            throw logoutException;
         }
      }
   }

   /**
    * Causes VcCleaner thread shutdown after all already pending requests
    * complete.
    */
   private class TerminateOp extends Op {
      private TerminateOp() {
         this.name = "terminate";
      }
   }

   private static Logger logger = Logger.getLogger(VcCleaner.class);
   private final static long shutdownTimeoutMillis = 60000; // Wait 1min.
   //private final static int maxQueuedOps = 4;               // 3 services + terminate
   private int logoutCount;                                 // Total processed.
   private LinkedBlockingQueue<Op> queue;                   // Request queue.

   /**
    * Creates VcCleaner and starts its daemon thread.
    */
   public VcCleaner() {
      //queue = new LinkedBlockingQueue<Op>(maxQueuedOps);
      /*
       * XXX
       * The assumption that there are at most 4 logout tasks is incorrect when VC is busy.
       * Then the runtime exception with "Queue full" would arise and
       * there should be leakage including thread pool, http connection.
       * Use unlimited queue as a workaround, and this should be fixed in Dawn.
       */
      queue = new LinkedBlockingQueue<Op>();
      setName("VcCleaner");
      setDaemon(true);
      logoutCount = 0;
      start();
   }

   /**
    * A request to logout a specified vc session. The logout will happen
    * asynchronously and has a best effort semantic - vc might not be
    * accessible or available. The caller must relinquish all use of the
    * submitted session because on completion, all http threads and ongoing
    * I/O will be terminated.
    * @param serviceName        our service associated with the vc session
    * @param vmomiClient        vmomi Client to shut down
    * @param sessionManager     vc session manager
    * @param executor           thread pool for http processing to shut down
    * @param httpConfig         to shut down
    */
   public void logout(String serviceName, Client vmomiClient,
         SessionManager sessionManager, ExecutorService executor,
         HttpConfiguration httpConfig) {
      LogoutOp op = new LogoutOp(Thread.currentThread(), serviceName,
               vmomiClient, sessionManager, executor, httpConfig);
      queue.add(op);
   }

   /**
    * VcCleaner thread: keeps waiting for and executing requested vc operations
    * until shutdown.
    */
   public void run() {
      while (true) {
         Op op = null;
         LogoutOp logoutOp = null;
         StringBuilder buf;
         try {
            long waitNanos, startNanos, finishNanos;
            op = queue.take();  // Blocks until next request.
            if (op instanceof TerminateOp) {
               break;
            } else {
               AuAssert.check(op instanceof LogoutOp);
            }
            logoutOp = (LogoutOp)op;
            startNanos = System.nanoTime();
            waitNanos = startNanos - logoutOp.submitNanos;
            logoutOp.logout();
            finishNanos = System.nanoTime();
            logoutCount++;
            buf = new StringBuilder("VC logout on behalf of {")
               .append(logoutOp.submitterThread.getName()).append(":")
               .append(logoutOp.serviceName).append("}")
               .append(" delay: ").append(TimeUnit.NANOSECONDS.toMillis(waitNanos)).append("ms")
               .append(" logout: ").append(
                     TimeUnit.NANOSECONDS.toMillis(finishNanos - startNanos)).append("ms");
            logger.info(buf);
         } catch (Exception e) {
            buf = new StringBuilder("Failed VC ");
            buf = buf.append( (op == null) ? "" : op.getName());
            if (logoutOp != null) {
               buf = buf.append(" on behalf of {");
               buf = buf.append(logoutOp.submitterThread.getName()).append(":")
                  .append(logoutOp.serviceName).append("}");
            }
            logger.error(buf, e);
         }
      }
   }

   /**
    * Safely shuts down VcCleaner thread. VcCleaner will complete all logout
    * requests submitted so far before shutting down. The caller of this
    * function is blocked until shutdown completes. Because we don't want to
    * interrupt vc logouts already in progress, we can't interrupt VcCleaner.
    * Instead, just submit a fake logout/termination request.
    * @throws InterruptedException
    */
   public void shutDown() {
      AuAssert.check(Thread.currentThread() != this);
      TerminateOp op = new TerminateOp();
      queue.add(op);
      try {
         /* Give up after timeout. VcCleaner is a daemon. */
         join(shutdownTimeoutMillis);
      } catch (InterruptedException e) {
      }
      if (isAlive()) {
         logger.warn("VcCleaner failed to shut down, continue.");
      } else {
         logger.info("VcCleaner shutdown; total vc logouts: " + logoutCount);
      }
   }
}
