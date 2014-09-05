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
package com.vmware.bdd.plugin.clouderamgr.exception;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Created by admin on 8/6/14.
 */
public class TestCommandExecFailureException {
   private final static Logger LOGGER = Logger.getLogger(TestCommandExecFailureException.class);
   public static final String PRODUCT = "product";
   public static final String VERSION = "1.0";
   public static final String REF_MSG = "details";
   String clusterName = "cluster";
   String appMgr = "cloudera";
   Exception cause = new Exception("TestSWMgmtPluginException");

   @Test
   public void testLoadMessage() {
      ArrayList<CommandExecFailException> exs = new ArrayList<>();

      exs.add(CommandExecFailException.EXECUTE_COMMAND_FAIL("hostId", REF_MSG));

      for(CommandExecFailException ex : exs) {
         assertException(ex);
      }
   }

   protected static void assertException(CommandExecFailException ex) {
      Assert.assertNotNull(ex.getErrCode());
      LOGGER.info(ex.getMessage());
      Assert.assertFalse(ex.getMessage().contains("<#") && ex.getMessage().contains("#>"));
      Assert.assertEquals("hostId", ex.getRefHostId());
   }

}
