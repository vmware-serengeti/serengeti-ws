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

import java.util.Random;
import java.util.concurrent.Callable;

import org.testng.annotations.Test;

import com.google.gson.internal.Pair;
import com.vmware.aurora.util.AuAssert;

public class TestScheduler {
   private static final int numberOfStoredProcedures = 1000;

   static class DummyStoredProcedure implements Callable<Void> {
      private int sleepTime; // in seconds
      private boolean successful;

      DummyStoredProcedure(int sleepTime, boolean successful) {
         this.sleepTime = sleepTime;
         this.successful = successful;
      }

      @Override
      public Void call() throws Exception {
         try {
            Thread.sleep(sleepTime * 1000L);
         } catch (Exception ex) {
            // ignore
         }

         if(!successful) {
            throw new Exception("Stored procedure failed");
         }
         return null;
      }
   }

   static class ProgressUpdate implements Scheduler.ProgressCallback {
      @Override
      public void progressUpdate(Callable<Void> sp, ExecutionResult result,
            boolean compensate, int total) {
         System.out.println("Finish");
      }
   }

   @Test
   public void testBestEffort() throws InterruptedException {
      // In this test, all stored procedures are executed in a best-effort way,
      // no compensation stored procedures are executed.
      Scheduler.init(20, 20);
      Random random = new Random();
      DummyStoredProcedure[] sps = new DummyStoredProcedure[numberOfStoredProcedures];
      int totalFailure = 0;

      for (int i = 0; i < sps.length; ++i) {
         if (random.nextBoolean()) {
            sps[i] = new DummyStoredProcedure(2, true);
         } else {
            sps[i] = new DummyStoredProcedure(2, false);
            ++totalFailure;
         }
      }

      ExecutionResult[] result = Scheduler.executeStoredProcedures(Priority.BACKGROUND, sps, new ProgressUpdate());
      AuAssert.check(Util.getNumberOfSuccessfulExecution(result) == (sps.length - totalFailure));

      Scheduler.shutdown(true);
   }

   @Test
   public void testCompensation() throws InterruptedException {
      // In this test, dummy compensation stored procedures will be
      // executed if the stored procedures fail and the total number of
      // failed stored procedures exceeds the allowed number.
      Scheduler.init(20, 20);
      Random random = new Random();
      int totalFailure = 0;
      @SuppressWarnings("unchecked")
      Pair<Callable<Void>, Callable<Void>>[] sps = new Pair[numberOfStoredProcedures];

      for (int i = 0; i < sps.length; ++i) {
         Callable<Void> sp = null;
         if (random.nextBoolean()) {
            sp = new DummyStoredProcedure(2, true);
         } else {
            sp = new DummyStoredProcedure(2, false);
            ++totalFailure;
         }

         sps[i] = new Pair<Callable<Void>, Callable<Void>>(sp, sp);
      }

      Pair<ExecutionResult, ExecutionResult>[] result = Scheduler.executeStoredProcedures(Priority.INTERACTIVE, sps, 0, new ProgressUpdate());
      AuAssert.check(Util.getNumberOfSuccessfulExecution(result) == 0);

      result = Scheduler.executeStoredProcedures(Priority.INTERACTIVE, sps, 1, new ProgressUpdate());
      AuAssert.check(Util.getNumberOfSuccessfulExecution(result) == 0);

      result = Scheduler.executeStoredProcedures(Priority.BACKGROUND, sps, totalFailure / 2, null);
      AuAssert.check(Util.getNumberOfSuccessfulExecution(result) == 0);

      result = Scheduler.executeStoredProcedures(Priority.INTERACTIVE, sps, totalFailure - 1, new ProgressUpdate());
      AuAssert.check(Util.getNumberOfSuccessfulExecution(result) == 0);

      result = Scheduler.executeStoredProcedures(Priority.BACKGROUND, sps, totalFailure, new ProgressUpdate());
      AuAssert.check(Util.getNumberOfSuccessfulExecution(result) == (sps.length - totalFailure));

      result = Scheduler.executeStoredProcedures(Priority.INTERACTIVE, sps, totalFailure + 1, null);
      AuAssert.check(Util.getNumberOfSuccessfulExecution(result) == (sps.length - totalFailure));

      result = Scheduler.executeStoredProcedures(Priority.BACKGROUND, sps, sps.length, new ProgressUpdate());
      AuAssert.check(Util.getNumberOfSuccessfulExecution(result) == (sps.length - totalFailure));

      Scheduler.shutdown(true);
   }
}
