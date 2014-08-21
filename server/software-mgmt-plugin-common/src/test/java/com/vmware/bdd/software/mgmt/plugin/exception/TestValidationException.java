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
public class TestValidationException{
   private final static Logger LOGGER = Logger.getLogger(TestValidationException.class);
   private static List<String> failedMsgList;
   private static List<String> warningMsgList;

   @BeforeClass
   public static void beforeClass() {
      failedMsgList = new ArrayList<String>();
      failedMsgList.add("fail");
      warningMsgList = new ArrayList<String>();
      warningMsgList.add("warn");
   }

   @Test
   public void testLoadMsg() {
      ArrayList<ValidationException> exs = new ArrayList<ValidationException>();
      exs.add(ValidationException.VALIDATION_FAIL("item", failedMsgList, warningMsgList));

      for(ValidationException ex : exs) {
         assertException(ex);
      }
   }

   protected static void assertException(ValidationException ex) {
      Assert.assertNotNull(ex.getErrCode());
      LOGGER.info(ex.getMessage());
      Assert.assertFalse(ex.getMessage().contains("<#") && ex.getMessage().contains("#>"));

      Assert.assertEquals(failedMsgList, ex.getFailedMsgList());
      Assert.assertEquals(warningMsgList, ex.getWarningMsgList());
   }

}
