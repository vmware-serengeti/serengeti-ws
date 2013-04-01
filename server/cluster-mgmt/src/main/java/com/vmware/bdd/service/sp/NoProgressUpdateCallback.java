package com.vmware.bdd.service.sp;

import java.util.concurrent.Callable;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler.ProgressCallback;

public class NoProgressUpdateCallback implements ProgressCallback {

   @Override
   public void progressUpdate(Callable<Void> sp, ExecutionResult result,
         boolean compensate, int total) {
   }
}
