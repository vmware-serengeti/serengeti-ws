package com.vmware.bdd.service;

import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.service.job.StatusUpdater;

public interface IClusterUpgradeService {
   /**
    * upgrade single node or nodes in cluster
    *
    * @param clusterName
    *
    */
   public boolean upgradeNode(NodeEntity node);

   public boolean upgrade(String clusterName, StatusUpdater statusUpdator);

   public boolean upgradeFailed(String clusterName, StatusUpdater statusUpdator);

}
