package com.vmware.bdd.software.mgmt.plugin.exception;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Created by admin on 8/6/14.
 */
public class TestSWMgmtPluginException {
   private final static Logger LOGGER = Logger.getLogger(TestSWMgmtPluginException.class);
   String clusterName = "cluster";
   String appMgr = "cloudera";
   Exception cause = new Exception("TestSWMgmtPluginException");

   @Test
   public void testLoadMessageFail() {
      SoftwareManagementPluginException ex =
      SoftwareManagementPluginException.APP_MANAGER_COMMON_EXCEPTION("NON_EXIST_CODE", cause, clusterName);

      Assert.assertTrue(ex.getMessage().contains("<#") && ex.getMessage().contains("#>"));
   }

   @Test
   public void testLoadMessage() {
      ArrayList<SoftwareManagementPluginException> exs = new ArrayList<SoftwareManagementPluginException>();

      exs.add(SoftwareManagementPluginException.ADD_CLUSTER_REPORT_FAIL(cause));
      exs.add(SoftwareManagementPluginException.CHECK_SERVICE_FAILED(clusterName, cause));
      exs.add(SoftwareManagementPluginException.CLUSTER_ALREADY_EXIST(clusterName));
      exs.add(SoftwareManagementPluginException.CONFIGURE_SERVICE_FAILED(cause));
      exs.add(SoftwareManagementPluginException.CREATE_CLUSTER_EXCEPTION(cause, appMgr, clusterName));
      exs.add(SoftwareManagementPluginException.CREATE_CLUSTER_FAIL(appMgr, clusterName));
      exs.add(SoftwareManagementPluginException.CLUSTER_ALREADY_EXIST(clusterName));
      exs.add(SoftwareManagementPluginException.DELETE_CLUSTER_FAILED(cause, appMgr, clusterName));
      exs.add(SoftwareManagementPluginException.DELETE_NODES_FAILED(cause, appMgr, new String[]{"node"}));
      exs.add(SoftwareManagementPluginException.INSTALL_COMPONENTS_FAIL(appMgr, clusterName));
      exs.add(SoftwareManagementPluginException.QUERY_CLUSTER_STATUS_FAILED(clusterName, cause));
      exs.add(SoftwareManagementPluginException.RECONFIGURE_CLUSTER_FAILED(cause, appMgr, clusterName));
      exs.add(SoftwareManagementPluginException.RETRIEVE_SUPPORTED_STACKS_FAIL(cause, appMgr));
      exs.add(SoftwareManagementPluginException.SCALE_OUT_CLUSTER_FAILED(cause, appMgr, clusterName));
      exs.add(SoftwareManagementPluginException.START_CLUSTER_FAILED(cause, appMgr, clusterName));
      exs.add(SoftwareManagementPluginException.START_CLUSTER_FAILED_NOT_PROV_BY_BDE(clusterName));
      exs.add(SoftwareManagementPluginException.START_SERVICE_FAILED(cause, appMgr, clusterName));
      exs.add(SoftwareManagementPluginException.STOP_CLUSTER_EXCEPTION(cause, appMgr, clusterName));
      exs.add(SoftwareManagementPluginException.STOP_CLUSTER_FAILED(appMgr, clusterName, null));
      exs.add(SoftwareManagementPluginException.UNKNOWN_CERTIFICATE("cert"));
      exs.add(SoftwareManagementPluginException.UNSURE_CLUSTER_EXIST(appMgr, clusterName));

      for(SoftwareManagementPluginException ex : exs) {
         assertException(ex);
      }
   }

   protected static void assertException(SoftwareManagementPluginException ex) {
      Assert.assertNotNull(ex.getErrCode());
      LOGGER.info(ex.getMessage());
      Assert.assertFalse(ex.getMessage().contains("<#") && ex.getMessage().contains("#>"));
   }
}
