package com.vmware.bdd.manager.task;

import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.BddMessageUtil;
import com.vmware.bdd.utils.ClusterCmdUtil;

public class PrioritizeClusterListener implements TaskListener {
   private static final long serialVersionUID = 1126241595749117009L;
   private static final Logger logger = Logger.getLogger(PrioritizeClusterListener.class);

   private String clusterName;

   public PrioritizeClusterListener(String clusterName) {
      super();
      this.clusterName = clusterName;
   }

   @Override
   public void onSuccess() {
      logger.debug("prioritize cluster " + clusterName
            + " task listener called onSuccess");
   }

   @Override
   public void onFailure() {
      logger.debug("prioritize cluster listener called onFailure");
   }

   @Override
   public void onMessage(Map<String, Object> mMap) {
      logger.debug("prioritize cluster " + clusterName
            + " task listener received message " + mMap);

      BddMessageUtil.validate(mMap, clusterName);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);
      AuAssert.check(cluster != null);

      // parse cluster data from message and store them in db
      String description =
            (new Gson()).toJson(mMap.get(BddMessageUtil.CLUSTER_DATA_FIELD));
      BddMessageUtil.processClusterData(clusterName, description);
   }

   @Override
   public String[] getTaskCommand(String clusterName, String fileName) {
      return ClusterCmdUtil.getQueryClusterCmdArray(clusterName, fileName);
   }
}
