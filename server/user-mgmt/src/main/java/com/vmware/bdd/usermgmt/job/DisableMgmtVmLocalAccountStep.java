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

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.service.ISoftwareSyncUpService;
import com.vmware.bdd.service.job.DefaultStatusUpdater;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.service.job.software.ISoftwareManagementTask;

/**
 * Created By xiaoliangl on 11/28/14.
 */
public class DisableMgmtVmLocalAccountStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(DisableMgmtVmLocalAccountStep.class);
   private ISoftwareSyncUpService serviceSyncup;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
                                   JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      StatusUpdater statusUpdater =
            new DefaultStatusUpdater(jobExecutionStatusHolder,
                  getJobExecutionId(chunkContext));

      ISoftwareManagementTask task = null;

      task = null/*createThriftTask(chunkContext, "mgmtvm", statusUpdater)*/;


      if (task != null) {
         Map<String, Object> ret = task.call();

         if (!(Boolean) ret.get("succeed")) {
            String errorMessage = (String) ret.get("errorMessage");
            putIntoJobExecutionContext(chunkContext,
                  JobConstants.CURRENT_ERROR_MESSAGE, errorMessage);
            putIntoJobExecutionContext(chunkContext,
                  JobConstants.SOFTWARE_MANAGEMENT_STEP_FAILE, true);
            throw TaskException.EXECUTION_FAILED(errorMessage);
         }
      }

      return RepeatStatus.FINISHED;
   }

}
