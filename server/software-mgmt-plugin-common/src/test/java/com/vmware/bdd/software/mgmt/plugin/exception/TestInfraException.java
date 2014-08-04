package com.vmware.bdd.software.mgmt.plugin.exception;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 8/6/14.
 */
public class TestInfraException {
   private final static Logger LOGGER = Logger.getLogger(TestInfraException.class);
   private static List<String> failedMsgList;

   @BeforeClass
   public static void beforeClass() {
      failedMsgList = new ArrayList<>();
      failedMsgList.add("fail");
   }

   @Test
   public void testLoadMsg() {
      ArrayList<InfrastructureException> exs = new ArrayList<>();
      exs.add(InfrastructureException.FORMAT_DISK_FAIL("cluster", failedMsgList));

      for(InfrastructureException ex : exs) {
         assertException(ex);
      }
   }

   private static void assertException(InfrastructureException ex) {
      Assert.assertNotNull(ex.getErrCode());
      LOGGER.info(ex.getMessage());
      Assert.assertFalse(ex.getMessage().contains("<#") && ex.getMessage().contains("#>"));

      Assert.assertEquals(failedMsgList, ex.getFailedMsgList());
   }

}
