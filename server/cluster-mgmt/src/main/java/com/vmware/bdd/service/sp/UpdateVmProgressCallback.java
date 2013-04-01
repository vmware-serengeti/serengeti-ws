package com.vmware.bdd.service.sp;

import java.util.concurrent.Callable;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;

public class UpdateVmProgressCallback extends BaseProgressCallback {
   private ClusterEntityManager clusterEntityMgr;
   private String clusterName;
   public UpdateVmProgressCallback(ClusterEntityManager clusterEntityMgr,
         StatusUpdater statusUpdator, String clusterName) {
      super(statusUpdator);
      this.clusterEntityMgr = clusterEntityMgr;
      this.clusterName = clusterName;
   }

   @Override
   public void progressUpdate(Callable<Void> sp, ExecutionResult result, boolean compensate, int total) {
      super.progressUpdate(sp, result, compensate, total);
      clusterEntityMgr.syncUp(clusterName);
   }
}
