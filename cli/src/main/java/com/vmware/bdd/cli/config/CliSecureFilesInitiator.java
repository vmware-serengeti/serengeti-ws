/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.cli.config;

import com.vmware.bdd.cli.commands.CliException;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * Created by xiaoliangl on 9/23/15.
 */
@Component
public class CliSecureFilesInitiator {
   private static Logger LOGGER = Logger.getLogger(CliSecureFilesInitiator.class);


   @PostConstruct
   protected void initAndSecureCliFiles() throws CliException {
      initAndSecureACLs(Constants.CLI_HISTORY_FILE);
      LOGGER.info("finish initAndSecureACLs: " + Constants.CLI_HISTORY_FILE);
      initAndSecureACLs(Constants.PROPERTY_FILE);
      LOGGER.info("finish initAndSecureACLs: " + Constants.PROPERTY_FILE);
   }

   public void initAndSecureACLs(String fileName) throws CliException {
      File cliHistory = new File(fileName);
      //create if not exists
      if (cliHistory.exists()) {
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(fileName + " already exists");
         }
      } else {
         try {
            cliHistory.createNewFile();
         } catch (IOException e) {
            LOGGER.error("failed to create file: " + fileName, e);
            throw new CliException("failed to init " + fileName);
         }
      }

      //secure the ACLs
      try {
         CommonUtil.setOwnerOnlyReadWrite(fileName);
      } catch (IOException e) {
         LOGGER.error("failed to set secure ACLs to: " + fileName, e);
         throw new CliException("failed to set secure ACLs to: " + fileName);
      }
   }
}
