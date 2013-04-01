/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.aurora.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.vmware.aurora.exception.AuroraException;

@SuppressWarnings("serial")
public class AuAssert extends AuroraException {
   private static final Logger logger = Logger.getLogger(AuAssert.class);

   // Default public constructor. This should only be used by AMF client.
   public AuAssert() {}

   private AuAssert(String errorId) {
      super(null, "AUASSERT", errorId);
   }

   private static AuAssert FAILURE() {
      return new AuAssert("FAILURE");
   }

   private static void log(AuAssert exc, long[] bugIds, String msg, Level level) {
      StringBuffer banner = new StringBuffer("Assertion failure");
      if (bugIds != null) {
         banner.append("(");
         for (int i = 0; i < bugIds.length; i++) {
            banner.append("PR ").append(bugIds[i]);
            if (i < bugIds.length - 1) {
               banner.append(",");
            }
         }
         banner.append(")");
      }
      if (msg != null) {
         banner.append(": ").append(msg);
      } else {
         banner.append(".");
      }
      logger.log(level, banner, exc);
   }

   private static void check(boolean cond, long[] bugIds, String message) {
      if (!cond) {
         AuAssert exc = FAILURE();
         // Log this assertion, even though the top-level will likely log it too.
         // We have several threads that don't log exceptions at their top level,
         // which is worse than logging an assertion failure twice.
         log(exc, bugIds, message, Level.ERROR);
         throw exc;
      }
   }

   private static void warn(boolean cond, long[] bugIds, String message) {
      if (!cond) {
         AuAssert exc = FAILURE();
         log(exc, bugIds, message, Level.WARN);
      }
   }

   public static void check(boolean cond, String message) {
      check(cond, null, message);
   }

   public static void check(boolean cond) {
      check(cond, null, null);
   }

   public static void checkBugs(boolean cond, long ... bugIds) {
      check(cond, bugIds, null);
   }

   public static void warnBugs(boolean cond, long ...bugIds) {
      warn(cond, bugIds, null);
   }

   /**
    * Assert that the code path should never be reached.
    */
   public static void unreachable() {
      check(false, "should not be reached");
   }

   /**
    * Assert that the code has not been implemented.
    */
   public static void notImplemented() {
      check(false, "has not been implemented");
   }
}
