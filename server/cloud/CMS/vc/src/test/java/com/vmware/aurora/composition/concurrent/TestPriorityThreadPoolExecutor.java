/* Copyright (c) 2012 VMware, Inc.  All rights reserved. */

package com.vmware.aurora.composition.concurrent;

import java.util.concurrent.Callable;

import org.testng.annotations.Test;

import com.vmware.aurora.util.AuAssert;

public class TestPriorityThreadPoolExecutor {

   private void sleep(int time) {
      if (time <= 0 ) {
         return;
      }
      try {
         Thread.sleep(time * 1000L);
      } catch (InterruptedException e) {
         // Eat the exception
      }
   }

   class TestCallable implements Callable<Void> {
      private int idx;
      private StringBuffer buffer;
      private int preSleepTime;  // in seconds
      private int postSleepTime; // in seconds

      TestCallable(int idx, StringBuffer buffer, int preSleepTime, int postSleepTime) {
         this.idx = idx;
         this.buffer = buffer;
         this.preSleepTime = preSleepTime;
         this.postSleepTime = postSleepTime;
      }

      @Override
      public Void call() throws Exception {
         sleep(preSleepTime);
         buffer.append(Integer.toString(idx));
         sleep(postSleepTime);
         return null;
      }

      public void setIdx(int idx) {
         this.idx = idx;
      }
   }

   @Test
   public void testNoWaiting() {
      // In this test, all tasks are executed immediately after being submitted,
      // no wait in queue.
      PriorityThreadPoolExecutor executor = new PriorityThreadPoolExecutor(2, 2);
      StringBuffer buffer = new StringBuffer();

      TestCallable call1 = new TestCallable(1, buffer, 3, 3);
      executor.submit(Priority.INTERACTIVE, call1);

      TestCallable call2 = new TestCallable(2, buffer, 8, 2);
      executor.submit(Priority.BACKGROUND, call2);

      sleep(1);
      AuAssert.check(executor.executor.getActiveCount() == 2);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 0);
      AuAssert.check(buffer.toString().equals(""));

      sleep(3);
      AuAssert.check(executor.executor.getActiveCount() == 2);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 0);
      AuAssert.check(buffer.toString().equals("1"));

      sleep(3);
      // call1 completes
      AuAssert.check(executor.executor.getActiveCount() == 1);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 1);
      AuAssert.check(buffer.toString().equals("1"));

      sleep(2);
      AuAssert.check(executor.executor.getActiveCount() == 1);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 1);
      AuAssert.check(buffer.toString().equals("12"));

      sleep(3);
      // call2 completes
      AuAssert.check(executor.executor.getActiveCount() == 0);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 2);
      AuAssert.check(buffer.toString().equals("12"));

      call1.setIdx(3);
      call2.setIdx(4);

      executor.submit(Priority.BACKGROUND, call1);
      executor.submit(Priority.INTERACTIVE, call2);

      sleep(1);
      AuAssert.check(executor.executor.getActiveCount() == 2);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 2);
      AuAssert.check(buffer.toString().equals("12"));

      sleep(3);
      AuAssert.check(executor.executor.getActiveCount() == 2);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 2);
      AuAssert.check(buffer.toString().equals("123"));

      sleep(3);
      AuAssert.check(executor.executor.getActiveCount() == 1);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 3);
      AuAssert.check(buffer.toString().equals("123"));

      sleep(2);
      AuAssert.check(executor.executor.getActiveCount() == 1);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 3);
      AuAssert.check(buffer.toString().equals("1234"));

      sleep(3);
      AuAssert.check(executor.executor.getActiveCount() == 0);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 4);
      AuAssert.check(buffer.toString().equals("1234"));

      executor.shutdown();
   }

   @Test
   public void testWaitingInOneQueue() {
      // In this test, some tasks are queued in queue for one priority.
      for (Priority priority : Priority.values()) {
         PriorityThreadPoolExecutor executor = new PriorityThreadPoolExecutor(2, 2);
         StringBuffer buffer = new StringBuffer();

         TestCallable call1 = new TestCallable(1, buffer, 2, 2);
         TestCallable call2 = new TestCallable(2, buffer, 4, 2);
         TestCallable call3 = new TestCallable(3, buffer, 8, 2);
         TestCallable call4 = new TestCallable(4, buffer, 9, 2);
         TestCallable call5 = new TestCallable(5, buffer, 2, 3);
         TestCallable call6 = new TestCallable(6, buffer, 4, 3);

         executor.submit(priority, call1);
         executor.submit(priority, call2);
         executor.submit(priority, call3);
         executor.submit(priority, call4);
         executor.submit(priority, call5); // will wait in queue for a while
         executor.submit(priority, call6); // will wait in queue for a while

         sleep(1);
         AuAssert.check(executor.executor.getActiveCount() == 4);
         AuAssert.check(executor.executor.getCompletedTaskCount() == 0);
         AuAssert.check(buffer.toString().equals(""));

         sleep(2);
         AuAssert.check(executor.executor.getActiveCount() == 4);
         AuAssert.check(executor.executor.getCompletedTaskCount() == 0);
         AuAssert.check(buffer.toString().equals("1"));

         sleep(2);
         // call1 completes
         AuAssert.check(executor.executor.getActiveCount() == 4);
         AuAssert.check(executor.executor.getCompletedTaskCount() == 1);
         AuAssert.check(buffer.toString().equals("12"));

         sleep(2);
         AuAssert.check(executor.executor.getActiveCount() == 4);
         AuAssert.check(executor.executor.getCompletedTaskCount() == 2);
         AuAssert.check(buffer.toString().equals("125"));

         sleep(10);
         AuAssert.check(executor.executor.getActiveCount() == 0);
         AuAssert.check(executor.executor.getCompletedTaskCount() == 6);
         AuAssert.check(buffer.toString().equals("125346"));

         executor.shutdown();
      }
   }

   @Test
   public void testWaitingInTwoQueues() {
      // In this test, queues for all priorities are used.
      PriorityThreadPoolExecutor executor = new PriorityThreadPoolExecutor(2, 2);
      StringBuffer buffer = new StringBuffer();

      TestCallable call1 = new TestCallable(1, buffer, 2, 1);
      TestCallable call2 = new TestCallable(2, buffer, 6, 4);
      TestCallable call3 = new TestCallable(3, buffer, 10, 2);
      TestCallable call4 = new TestCallable(4, buffer, 12, 1);

      TestCallable call5 = new TestCallable(5, buffer, 1, 2);
      TestCallable call6 = new TestCallable(6, buffer, 2, 2);

      executor.submit(Priority.BACKGROUND,  call1);
      executor.submit(Priority.BACKGROUND,  call2);
      executor.submit(Priority.INTERACTIVE, call3);
      executor.submit(Priority.INTERACTIVE, call4);

      executor.submit(Priority.BACKGROUND,  call5); // will wait in queue for a while
      executor.submit(Priority.INTERACTIVE, call6); // will wait in queue for a while

      sleep(1);
      AuAssert.check(executor.executor.getActiveCount() == 4);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 0);
      AuAssert.check(buffer.toString().equals(""));

      sleep(2);
      AuAssert.check(executor.executor.getActiveCount() == 4);
      AuAssert.check(buffer.toString().equals("1"));

      sleep(2);
      AuAssert.check(executor.executor.getActiveCount() == 4);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 1);
      AuAssert.check(buffer.toString().equals("15"));

      sleep(2);
      AuAssert.check(executor.executor.getActiveCount() == 4);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 2);
      AuAssert.check(buffer.toString().equals("152"));

      sleep(2);
      AuAssert.check(executor.executor.getActiveCount() == 4);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 2);
      AuAssert.check(buffer.toString().equals("1526"));

      sleep(2);
      AuAssert.check(executor.executor.getActiveCount() == 2);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 4);
      AuAssert.check(buffer.toString().equals("15263"));

      sleep(3);
      AuAssert.check(executor.executor.getActiveCount() == 0);
      AuAssert.check(executor.executor.getCompletedTaskCount() == 6);
      AuAssert.check(buffer.toString().equals("152634"));

      executor.shutdown();
   }
}
