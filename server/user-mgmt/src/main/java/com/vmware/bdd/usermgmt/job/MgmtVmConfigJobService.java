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

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.JobManager;
import com.vmware.bdd.service.job.JobConstants;

/**
 * Created By xiaoliangl on 11/28/14.
 */
@Component
public class MgmtVmConfigJobService {
   private final static Logger LOGGER = Logger.getLogger(MgmtVmConfigJobService.class);

   @Autowired
   private JobManager jobManager;

   public long enableLdap(UserMgmtServer usrMgmtServer) {
      Gson gson = new Gson();

      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM, new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM, new JobParameter(ClusterStatus.CONFIGURE_ERROR.name()));
      param.put("UserMgmtServer", new JobParameter(gson.toJson(usrMgmtServer)));

      JobParameters jobParameters = new JobParameters(param);

      try {
         return jobManager.runJob("MgmtVMUserMgmtServerCfgJob", jobParameters);
      } catch (Exception e) {
         throw new BddException(e, "BDD.MgmtVMConfig", "EXCEPTION");
      }
   }

   public long disableLocalAccount() {
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM, new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM, new JobParameter(ClusterStatus.CONFIGURE_ERROR.name()));

      JobParameters jobParameters = new JobParameters(param);

      try {
         return jobManager.runJob("MgmtVMDisableLocalAccountJob", jobParameters);
      } catch (Exception e) {
         throw new BddException(e, "BDD.MgmtVMConfig", "EXCEPTION");
      }
   }
}
