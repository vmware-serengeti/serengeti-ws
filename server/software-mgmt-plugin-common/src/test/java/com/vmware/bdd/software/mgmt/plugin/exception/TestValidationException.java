/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
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
