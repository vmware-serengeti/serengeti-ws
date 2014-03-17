package com.vmware.bdd.service;

import com.vmware.bdd.service.job.StatusUpdater;

public interface IClusterUpgradeService {
   /**
    * upgrade all nodes in cluster
    *
    * @param clusterName
    *
    */
   public boolean upgrade(String clusterName, StatusUpdater statusUpdator);

   public boolean upgradeFailed(String clusterName, StatusUpdater statusUpdator);
}
