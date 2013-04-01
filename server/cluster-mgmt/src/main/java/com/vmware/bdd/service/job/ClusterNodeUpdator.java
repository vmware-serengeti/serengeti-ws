package com.vmware.bdd.service.job;

import java.util.List;

import com.vmware.aurora.util.CmsWorker.PeriodicRequest;
import com.vmware.aurora.util.CmsWorker.WorkQueue;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.ClusterEntityManager;

public class ClusterNodeUpdator extends PeriodicRequest {
   
   private ClusterEntityManager entityMgr;
   
   public ClusterNodeUpdator(ClusterEntityManager entityMgr) {
      super(WorkQueue.VC_TASK_FIVE_MIN_DELAY);
      this.entityMgr = entityMgr;
   }

   protected boolean executeOnce() {
      List<ClusterEntity> clusters = entityMgr.findAllClusters();
      for (ClusterEntity cluster : clusters) {
         if (cluster.inStableStatus()) {
            syncUp(cluster.getName());
         }
      }
      return true;
   }

   public void syncUp(String clusterName) {
      entityMgr.syncUp(clusterName);
   }
}
