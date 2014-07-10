package com.vmware.bdd.service.job.software;

import org.apache.log4j.Logger;

import com.vmware.bdd.software.mgmt.plugin.aop.PreConfiguration;

public class MockPreConfigUser {
   private static final Logger logger = Logger.getLogger(MockPreConfigUser.class);

   @PreConfiguration(clusterNameParam="clusterName", maxWaitingTimeParam="waitingSeconds")
   public boolean testAop(String clusterName, int waitingSeconds) {
      logger.info("In testAop");
      return true;
   }
}
