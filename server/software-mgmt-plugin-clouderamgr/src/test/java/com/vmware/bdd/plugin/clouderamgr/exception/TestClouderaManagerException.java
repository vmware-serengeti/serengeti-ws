package com.vmware.bdd.plugin.clouderamgr.exception;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Testcase to ensure message bundle integrity.
 */
public class TestClouderaManagerException {
   private final static Logger LOGGER = Logger.getLogger(TestClouderaManagerException.class);
   public static final String PRODUCT = "product";
   public static final String VERSION = "1.0";
   public static final String REF_MSG = "details";
   String clusterName = "cluster";
   String appMgr = "cloudera";
   Exception cause = new Exception("TestSWMgmtPluginException");

   @Test
   public void testLoadMessage() {
      ArrayList<ClouderaManagerException> exs = new ArrayList<>();

      exs.add(ClouderaManagerException.ACTIVATE_PARCEL_FAIL(PRODUCT, VERSION, REF_MSG));
      exs.add(ClouderaManagerException.CHECK_CONFIGURED_EXCEPTION(cause, clusterName));
      exs.add(ClouderaManagerException.DEPROVISION_EXCEPTION(cause, clusterName));
      exs.add(ClouderaManagerException.DISTRIBUTE_PARCEL_FAIL(PRODUCT, VERSION, REF_MSG));
      exs.add(ClouderaManagerException.DOWNLOAD_PARCEL_FAIL(PRODUCT, VERSION, REF_MSG));
      exs.add(ClouderaManagerException.FAIL_FETCH_CM_SERVER_HOST_ID());
      exs.add(ClouderaManagerException.GET_SUPPORT_CONFIGS_EXCEPTION(cause));
      exs.add(ClouderaManagerException.INIT_EXCEPTION(cause, clusterName));
      exs.add(ClouderaManagerException.INSTALL_AGENTS_FAIL(cause));

      for(ClouderaManagerException ex : exs) {
         assertException(ex);
      }
   }

   protected static void assertException(ClouderaManagerException ex) {
      Assert.assertNotNull(ex.getErrCode());
      LOGGER.info(ex.getMessage());
      Assert.assertFalse(ex.getMessage().contains("<#") && ex.getMessage().contains("#>"));
   }
}
