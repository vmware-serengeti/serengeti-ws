/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.utils;

import org.apache.log4j.Logger;

public class AuAssert extends RuntimeException {
   private static final long serialVersionUID = 1L;
   private static final Logger logger = Logger.getLogger(AuAssert.class);

   private AuAssert(String errorId) {
      super(errorId, null);
   }

   private static AuAssert FAILURE() {
      return new AuAssert("FAILURE");
   }

   private static void log(AuAssert exc, String msg) {
      StringBuffer banner = new StringBuffer("Assertion failure");
      if (msg != null) {
         banner.append(": ").append(msg);
      } else {
         banner.append(".");
      }
      logger.fatal(banner);
   }

   public static void check(boolean cond, String message) {
      if (!cond && ConfigInfo.isDebugEnabled()) {
         AuAssert exc = FAILURE();
         log(exc, message);
         throw exc;
      }
   }

   public static void check(boolean cond) {
      check(cond, null);
   }

   /**
    * Assert that the code path should never be reached.
    */
   public static void unreachable() {
      check(false, "should not be reached");
   }
}

