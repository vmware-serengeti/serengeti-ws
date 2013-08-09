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
package com.vmware.aurora.vc.test;

import org.testng.annotations.Test;

import com.vmware.aurora.exception.AuroraException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vim.fault.NotAuthenticated;
import com.vmware.vim.vmomi.core.exception.InternalException;

/**
 * Test code to enumerate resource pools.
 */
public class VcSessionTest extends AbstractVcTest {

   @SuppressWarnings("serial")
   static private class MyException extends Exception {
      public int count;
      public MyException(int count, String msg) {
         super(msg);
         this.count = count;
      }
   }

   /**
    * Run VC queries and generate all sorts of exceptions.
    */
   static public void doVcSession(final int numExceptions,
                                  final int numBadLogins,
                                  final int numInternals,
                                  final int numVcFailures) throws Exception {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         int internals = 0;
         int badLogins = 0;
         int failures = 0;
         int exceptions = 0;
         public Void body() throws Exception {
            for (VcCluster cluster : VcInventory.getClusters()) {
               System.out.println(cluster);
               for (VcResourcePool rp : cluster.getQualifiedRPs()) {
                  System.out.println(rp);
                  VcCluster c = rp.getVcCluster();
                  AuAssert.check(c.getId().equals(cluster.getId()));
               }
               if (exceptions < numExceptions) {
                  exceptions++;
                  throw new MyException(exceptions, "bad code");
               }
               if (badLogins < numBadLogins) {
                  badLogins++;
                  throw new NotAuthenticated(null, "bad session");
               }
               if (internals < numInternals) {
                  internals++;
                  throw new InternalException("recoverable exception");
               }
               if (failures < numVcFailures) {
                  failures++;
                  VcContext.getService().dropConnection();
                  System.out.println("Forced connection drop. On reconnect, VcCleaner logout failures ok.");
               }
            }
            return null;
         }
      });
   }


   @Test
   public void testVcSession() throws Exception {
      VcContext.endSession();
      AuAssert.check(!VcContext.isInSession());
      doVcSession(0, 0, 0, 0);
      AuAssert.check(!VcContext.isInSession());
   }

   @Test
   public void testVcSessionInterrupted() throws Exception {
      VcContext.endSession();
      try {
         // Throw 3 exceptions.
         doVcSession(3, 0, 0, 0);
      } catch (AuroraException e) {
         Throwable cause = e.getCause();
         if (cause instanceof MyException) {
            MyException e1 = (MyException)cause;
            System.out.println("caught expected exception: " + e1);
            if (e1.count != 1) {
               System.out.println("Unexpected number of retries");
               throw e1;
            }
         }
      }
      System.out.println("done test 1");
      // Do 2 bad login errors.
      doVcSession(0, 2, 0, 0);
      System.out.println("done test 2");
      // Do 2 internal exceptions.
      doVcSession(0, 0, 2, 0);
      System.out.println("done test 3");
      // Do 2 VC connection drops.
      doVcSession(0, 0, 0, 2);
      System.out.println("done test 4");
      // Do 2 internals and 2 connection drops.
      doVcSession(0, 0, 2, 2);
      System.out.println("done test 5");
      System.out.println("all done");
   }

}
