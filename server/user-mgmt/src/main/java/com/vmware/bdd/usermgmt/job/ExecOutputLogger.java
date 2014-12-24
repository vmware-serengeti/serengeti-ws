/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.usermgmt.job;

import org.apache.commons.exec.LogOutputStream;
import org.apache.log4j.Logger;

/**
 * Created By xiaoliangl on 12/22/14.
 */
public class ExecOutputLogger extends LogOutputStream {
   private Logger logger;

   public ExecOutputLogger(Logger logger, boolean isError) {
      super(isError ? -1 : 1);
      this.logger = logger;
   }

   @Override
   protected void processLine(String line, int logLevel) {
      //err output stream
      if(logLevel < 0) {
         logger.error(line);
      }
      //normal output stream
      else {
         if(logger.isInfoEnabled()) {
            logger.info(line);
         }
      }
   }
}
