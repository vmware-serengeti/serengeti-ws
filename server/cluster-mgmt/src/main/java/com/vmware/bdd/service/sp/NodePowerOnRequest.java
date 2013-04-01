package com.vmware.bdd.service.sp;

import org.apache.log4j.Logger;

import com.vmware.aurora.util.CmsWorker.SimpleRequest;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.utils.Constants;

public class NodePowerOnRequest extends SimpleRequest {
   private static final Logger logger = Logger.getLogger(NodePowerOnRequest.class);
   private ClusterEntityManager entityMgr;
   private String vmId;

   public NodePowerOnRequest(ClusterEntityManager entityMgr, String vmId) {
      this.entityMgr = entityMgr;
      this.vmId = vmId;
   }

   @Override
   protected boolean execute() {
      logger.info("Start to waiting for VM " + vmId +
      		" post power on status");
      QueryIpAddress query =
         new QueryIpAddress(Constants.VM_POWER_ON_WAITING_SEC);
      query.setVmId(vmId);
      try {
         query.call();
      } catch (Exception e) {
         logger.error("Failed to query ip address of vm: " + vmId);
      }
      entityMgr.refreshNodeByMobId(vmId, false);
      return true;
   }
}
