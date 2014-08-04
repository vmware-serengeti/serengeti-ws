package com.vmware.bdd.plugin.ambari.exception;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Testcase to ensure message bundle integrity.
 */
public class TestAmException {
   private final static Logger LOGGER = Logger.getLogger(TestAmException.class);
   String clusterName = "cluster";
   String appMgr = "cloudera";
   Exception cause = new Exception("TestSWMgmtPluginException");

   @Test
   public void testLoadMessage() {
      ArrayList<AmException> exs = new ArrayList<>();

      exs.add(AmException.BLUEPRINT_ALREADY_EXIST(clusterName));
      exs.add(AmException.BOOTSTRAP_FAILED(new Object[]{"node"}));
      exs.add(AmException.BOOTSTRAP_FAILED_EXCEPTION(cause, clusterName));
      exs.add(AmException.CLUSTER_NOT_PROVISIONED(clusterName));
      exs.add(AmException.CREATE_BLUEPRINT_FAILED(cause, clusterName));
      exs.add(AmException.PROVISION_WITH_BLUEPRINT_FAILED(cause, clusterName));
      exs.add(AmException.UNSURE_BLUEPRINT_EXIST(clusterName));

      for(AmException ex : exs) {
         assertException(ex);
      }
   }

   protected static void assertException(AmException ex) {
      Assert.assertNotNull(ex.getErrCode());
      LOGGER.info(ex.getMessage());
      Assert.assertFalse(ex.getMessage().contains("<#") && ex.getMessage().contains("#>"));
   }
}
