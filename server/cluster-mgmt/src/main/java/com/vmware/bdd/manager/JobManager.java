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
package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.job.JobParametersExtractor;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.apitypes.TaskRead.Type;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.SimpleStepExecutionListener;
import com.vmware.bdd.service.job.SubJobStatus;
import com.vmware.bdd.service.job.SubJobStep;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.utils.JobUtils;

public class JobManager {
   static final Logger logger = Logger.getLogger(JobManager.class);

   private JobRepository jobRepository;
   private JobLauncher jobLauncher;
   private JobExplorer jobExplorer;
   private JobOperator jobOperator;
   private JobRegistry jobRegistry;
   private JobExecutionStatusHolder jobExecutionStatusHolder;
   private JobExecutionStatusHolder mainJobExecutionStatusHolder;
   private JobParametersExtractor jobParametersExtractor;
   private JobExecutionListener mainJobExecutionListener;

   @Autowired
   private ClusterEntityManager clusterEntityMgr;

   /**
    * Run a new job
    * 
    * @param jobName
    *           job name
    * @param param
    *           job parameters
    * @return jobExecution id
    * @throws Exception
    */
   public long runJob(String jobName, JobParameters param) throws Exception {
      // TODO handle errors
      Job job = jobRegistry.getJob(jobName);
      return jobLauncher.run(job, param).getId();
   }

   /**
    * Run Spring Batch job with sub job. The number of sub jobs is size of
    * "jobParamtersList". Every element in the "jobParametersList" will be the
    * job parameters for one sub job.
    * 
    * @param jobName
    *           sub job name
    * @param jobParametersList
    *           List of job parameters for the sub job
    * @param clusterName
    *           cluster name
    * @param sucessStatus
    *           the status to be set on the cluster if job success
    * @param failStatus
    *           the status to be set on the cluster if job fail
    * @return job execution id
    * @throws Exception
    */
   public long runSubJobForNodes(String jobName,
         List<JobParameters> jobParametersList, String clusterName,
         ClusterRead.ClusterStatus successStatus,
         ClusterRead.ClusterStatus failStatus) throws Exception {
      return createAndLaunchJobWithSubJob(clusterName, jobName,
            jobParametersList, successStatus, failStatus);
   }

   /**
    * Run Spring Batch job with sub job.
    * 
    * @param jobName
    *           the Spring Batch job name
    * @param param
    *           job parameters
    * @param subJobName
    *           sub job name
    * @param sucessStatus
    *           the status to be set on the cluster if job success
    * @param failStatus
    *           the status to be set on the cluster if job fail
    * @return job exection id
    * @throws Exception
    */
   public long runJobWithSubJob(String jobName, JobParameters param,
         String subJobName, ClusterRead.ClusterStatus successStatus,
         ClusterRead.ClusterStatus failStatus) throws Exception {
      logger.debug("::runJobWithSubJob: " + jobName + ", subJobName: "
            + subJobName);
      long result = Long.MIN_VALUE;
      JobParameter clusterNameParameter =
            param.getParameters().get(JobConstants.CLUSTER_NAME_JOB_PARAM);
      String clusterName = (String) clusterNameParameter.getValue();
      Job preparingJob = jobRegistry.getJob(jobName);
      JobExecution preparingJobExecution = jobLauncher.run(preparingJob, param);
      int subJobNumber = 0;
      waitJobExecution(preparingJobExecution.getId(), Long.MAX_VALUE);
      if (preparingJobExecution.getStatus() == BatchStatus.COMPLETED) {
         subJobNumber =
               preparingJobExecution.getExecutionContext().getInt(
                     (JobConstants.SUB_JOB_NUMBER));
         if (subJobNumber > 0) {
            logger.debug("sub job number: " + subJobNumber);
            List<JobParameters> subJobParametersList =
                  new ArrayList<JobParameters>();
            for (int i = 0; i < subJobNumber; i++) {
               JobParameters subJobParameters =
                     (JobParameters) preparingJobExecution
                           .getExecutionContext()
                           .get(JobConstants.SUB_JOB_PARAMETERS_KEY_PREFIX + i);
               subJobParametersList.add(subJobParameters);
            }
            result =
                  createAndLaunchJobWithSubJob(clusterName, subJobName,
                        subJobParametersList, successStatus, failStatus);
         }
      }
      if (result == Long.MIN_VALUE) {
         logger.warn("Failure in preparing sub jobs");
         throw TaskException.EXECUTION_FAILED("failed to prepare sub jobs.");
      }
      return result;
   }

   /**
    * Create new Spring Batch job to execute sub jobs. The sub job name is
    * "subJobName", the number of sub jobs is size of "subJobParameters". One
    * element in the "subJobParameters" will be the JobParameters of sub job.
    * 
    * @param clusterName
    *           cluster name
    * @param subJobName
    *           sub job name
    * @param subJobParameters
    *           sub job parameter
    * @param sucessStatus
    *           the status to be set on the cluster if job success
    * @param failStatus
    *           the status to be set on the cluster if job fail
    * @return job execution id
    * @throws Exception
    */
   private synchronized long createAndLaunchJobWithSubJob(String clusterName,
         String subJobName, List<JobParameters> subJobParameters,
         ClusterRead.ClusterStatus successStatus,
         ClusterRead.ClusterStatus failStatus) throws Exception {
      SimpleJob mainJob =
            new SimpleJob("composed-job-" + clusterName + "-" + subJobName
                  + "-" + System.nanoTime());
      //SimpleJob mainJob = new SimpleJob("composed-job-" + clusterName + "-" + subJobName);
      StepExecutionListener[] jobStepListeners = createJobStepListener();
      Map<String, JobParameter> mainJobParams =
            new TreeMap<String, JobParameter>();
      mainJobParams.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(
            new Date()));
      mainJobParams.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      mainJobParams.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(successStatus.name()));
      mainJobParams.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(failStatus.name()));
      //enable sub job indicator to for job progress query
      mainJobParams.put(JobConstants.SUB_JOB_ENABLED, new JobParameter(1l));
      Job subJob = jobRegistry.getJob(subJobName);
      for (int stepNumber = 0, j = subJobParameters.size(); stepNumber < j; stepNumber++) {
         SubJobStep subJobStep = new SubJobStep();
         subJobStep.setName(subJobName + "-subJobStep-" + stepNumber);
         subJobStep.setJob(subJob);
         subJobStep.setJobParametersExtractor(jobParametersExtractor);
         subJobStep.setJobExecutionStatusHolder(jobExecutionStatusHolder);
         subJobStep
               .setMainJobExecutionStatusHolder(mainJobExecutionStatusHolder);
         subJobStep.setJobLauncher(jobLauncher);
         subJobStep.setJobRepository(jobRepository);
         subJobStep.setStepExecutionListeners(jobStepListeners);
         subJobStep.afterPropertiesSet();
         mainJob.addStep(subJobStep);
         logger.debug("added sub job step: " + subJobStep.getName());
         int subJobParametersNumber =
               subJobParameters.get(stepNumber).getParameters().keySet().size();
         mainJobParams.put(JobConstants.SUB_JOB_PARAMETERS_NUMBER + stepNumber,
               new JobParameter((long) subJobParametersNumber));
         int count = 0;
         for (String key : subJobParameters.get(stepNumber).getParameters()
               .keySet()) {
            int index = count++;
            mainJobParams.put(
                  JobUtils.getSubJobParameterPrefixKey(stepNumber, index),
                  new JobParameter(key));
            mainJobParams.put(
                  JobUtils.getSubJobParameterPrefixValue(stepNumber, index),
                  subJobParameters.get(stepNumber).getParameters().get(key));
         }
      }
      mainJob
            .setJobExecutionListeners(new JobExecutionListener[] { mainJobExecutionListener });
      mainJob.setJobRepository(jobRepository);
      mainJob.afterPropertiesSet();
      JobFactory jobFactory = new ReferenceJobFactory(mainJob);
      jobRegistry.register(jobFactory);
      logger.info("registered job: " + mainJob.getName());
      JobParameters mainJobParameters = new JobParameters(mainJobParams);
      JobExecution mainJobExecution =
            jobLauncher.run(mainJob, mainJobParameters);
      logger.info("launched main job: " + mainJob.getName());
      return mainJobExecution.getId();
   }

   private StepExecutionListener[] createJobStepListener() {
      SimpleStepExecutionListener jobStepListener =
            new SimpleStepExecutionListener();
      jobStepListener.setJobRegistry(jobRegistry);
      jobStepListener.setJobExecutionStatusHolder(mainJobExecutionStatusHolder);
      return new SimpleStepExecutionListener[] { jobStepListener };
   }

   /**
    * Try to stop a jobExecution
    * 
    * @param jobExecutionId
    *           jobExecution Id
    * @return true if the message was successfully sent (does not guarantee that
    *         the job has stopped)
    * @throws Exception
    */
   public boolean stopJobExecution(long jobExecutionId) throws Exception {
      // TODO handle errors
      return jobOperator.stop(jobExecutionId);
   }

   /**
    * Restart a jobExecution
    * 
    * @param jobExecutionId
    *           old jobExecution id
    * @return new jobExecution id
    * @throws Exception
    */
   public long restartJobExecution(long jobExecutionId) throws Exception {
      // TODO handle errors
      return jobOperator.restart(jobExecutionId);
   }

   /**
    * Get job execution status
    * 
    * @param jobExecutionId
    * @return job status
    * @throws NoSuchJobException
    */
   public TaskRead getJobExecutionStatus(long jobExecutionId) {
      JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
      if (jobExecution == null) {
         throw BddException.NOT_FOUND("Task", Long.toString(jobExecutionId));
      }

      TaskRead jobStatus = new TaskRead();
      jobStatus.setId(jobExecutionId);

      //identify VHM jobs
      String jobName = jobExecution.getJobInstance().getJobName();
      if (jobName.equals(JobConstants.SET_MANUAL_ELASTICITY_JOB_NAME)) {
         jobStatus.setType(Type.VHM);
      } else if (jobName.equals(JobConstants.DELETE_CLUSTER_JOB_NAME)) {
         jobStatus.setType(Type.DELETE);
      }

      JobParameters jobParameters =
            jobExecution.getJobInstance().getJobParameters();
      String clusterName =
            jobParameters.getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      jobStatus.setTarget(clusterName);
      long subJobEnabled = jobParameters.getLong(JobConstants.SUB_JOB_ENABLED);
      if (subJobEnabled != 1) {
         jobStatus.setProgress(jobExecutionStatusHolder
               .getCurrentProgress(jobExecutionId));
      } else {
         jobStatus.setProgress(mainJobExecutionStatusHolder
               .getCurrentProgress(jobExecutionId));
      }
      Status status = null;
      switch (jobExecution.getStatus()) {
      case ABANDONED:
         status = Status.ABANDONED;
         break;
      case COMPLETED:
         status = Status.COMPLETED;
         break;
      case FAILED:
         status = Status.FAILED;
         break;
      case STARTED:
         status = Status.STARTED;
         break;
      case STARTING:
         status = Status.STARTING;
         break;
      case STOPPED:
         status = Status.STOPPED;
         break;
      case STOPPING:
         status = Status.STOPPING;
         break;
      case UNKNOWN:
      default:
         status = Status.UNKNOWN;
      }
      jobStatus.setStatus(status);
      if (subJobEnabled == 1) {
         List<SubJobStatus> succeedNodes =
               (ArrayList<SubJobStatus>) jobExecution.getExecutionContext()
                     .get(JobConstants.SUB_JOB_NODES_SUCCEED);
         List<SubJobStatus> failNodes =
               (ArrayList<SubJobStatus>) jobExecution.getExecutionContext()
                     .get(JobConstants.SUB_JOB_NODES_FAIL);
         if (succeedNodes != null) {
            jobStatus.setSucceedNodes(convert(succeedNodes));
         }
         if (failNodes != null) {
            jobStatus.setFailNodes(convert(failNodes));
         }
      }
      if (status.equals(Status.FAILED) && subJobEnabled != 1) {
         String workDir =
               TrackableTasklet.getFromJobExecutionContext(
                     jobExecution.getExecutionContext(),
                     JobConstants.CURRENT_COMMAND_WORK_DIR, String.class);
         String errorMessage =
               TrackableTasklet.getFromJobExecutionContext(
                     jobExecution.getExecutionContext(),
                     JobConstants.CURRENT_ERROR_MESSAGE, String.class);
         jobStatus.setErrorMessage(errorMessage);
         jobStatus.setWorkDir(workDir);
         logger.error("mark task as failed: " + errorMessage);
      }

      return jobStatus;
   }

   private List<TaskRead.NodeStatus> convert(List<SubJobStatus> subJobStatus) {
      List<TaskRead.NodeStatus> result = new ArrayList<TaskRead.NodeStatus>();
      for (SubJobStatus status : subJobStatus) {
         TaskRead.NodeStatus nodeStatus =
               new TaskRead.NodeStatus(status.getNodeName(),
                     status.isSucceed(), status.getErrorMessage());
         result.add(nodeStatus);
      }
      return result;
   }

   /**
    * the latest_task_id attribute of a cluster entity records the latest job id
    * the cluster executes
    * 
    * @return
    */
   public List<TaskRead> getLatestTaskForExistedClusters() {
      List<Long> taskIds = clusterEntityMgr.getLatestTaskIds();
      List<TaskRead> taskReads = new ArrayList<TaskRead>(taskIds.size());

      for (Long id : taskIds) {
         if (id == null)
            continue;
         TaskRead task = getJobExecutionStatus(id);
         if (task.getType() == null) {
            task.setType(Type.INNER);
         }
         if (task.getStatus() == TaskRead.Status.COMPLETED) {
            task.setProgress(1.0);
         }
         taskReads.add(task);
      }

      return taskReads;
   }

   /**
    * Wait for job execution to finish.
    * 
    * @param jobExecutionId
    * @param timeoutSec
    * @return job result status
    * @throws TimeoutException
    */
   public TaskRead waitJobExecution(long jobExecutionId, long timeoutMs)
         throws TimeoutException {
      long start = System.currentTimeMillis();
      while (true) {
         TaskRead tr = getJobExecutionStatus(jobExecutionId);
         Status status = tr.getStatus();
         if (Status.ABANDONED.equals(status) || Status.COMPLETED.equals(status)
               || Status.FAILED.equals(status) || Status.STOPPED.equals(status)) {
            return tr;
         }

         long now = System.currentTimeMillis();
         if (now - start >= timeoutMs) {
            throw new TimeoutException("wait for job finish timeout");
         }

         try {
            Thread.sleep(3000);
         } catch (InterruptedException e) {
         }
      }
   }

   public JobRepository getJobRepository() {
      return jobRepository;
   }

   public void setJobRepository(JobRepository jobRepository) {
      this.jobRepository = jobRepository;
   }

   public JobLauncher getJobLauncher() {
      return jobLauncher;
   }

   public void setJobLauncher(JobLauncher jobLauncher) {
      this.jobLauncher = jobLauncher;
   }

   public JobExplorer getJobExplorer() {
      return jobExplorer;
   }

   public void setJobExplorer(JobExplorer jobExplorer) {
      this.jobExplorer = jobExplorer;
   }

   public JobOperator getJobOperator() {
      return jobOperator;
   }

   /**
    * @return the jobParametersExtractor
    */
   public JobParametersExtractor getJobParametersExtractor() {
      return jobParametersExtractor;
   }

   /**
    * @param jobParametersExtractor
    *           the jobParametersExtractor to set
    */
   public void setJobParametersExtractor(
         JobParametersExtractor jobParametersExtractor) {
      this.jobParametersExtractor = jobParametersExtractor;
   }

   public void setJobOperator(JobOperator jobOperator) {
      this.jobOperator = jobOperator;
   }

   public JobRegistry getJobRegistry() {
      return jobRegistry;
   }

   public void setJobRegistry(JobRegistry jobRegistry) {
      this.jobRegistry = jobRegistry;
   }

   public JobExecutionStatusHolder getJobExecutionStatusHolder() {
      return jobExecutionStatusHolder;
   }

   public void setJobExecutionStatusHolder(
         JobExecutionStatusHolder jobExecutionStatusHolder) {
      this.jobExecutionStatusHolder = jobExecutionStatusHolder;
   }

   /**
    * @return the mainJobExecutionStatusHolder
    */
   public JobExecutionStatusHolder getMainJobExecutionStatusHolder() {
      return mainJobExecutionStatusHolder;
   }

   /**
    * @param mainJobExecutionStatusHolder
    *           the mainJobExecutionStatusHolder to set
    */
   public void setMainJobExecutionStatusHolder(
         JobExecutionStatusHolder subJobExecutionStatusHolder) {
      this.mainJobExecutionStatusHolder = subJobExecutionStatusHolder;
   }

   public ClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   public void setClusterEntityMgr(ClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   /**
    * @return the mainJobExecutionListener
    */
   public JobExecutionListener getMainJobExecutionListener() {
      return mainJobExecutionListener;
   }

   /**
    * @param mainJobExecutionListener
    *           the mainJobExecutionListener to set
    */
   public void setMainJobExecutionListener(
         JobExecutionListener mainJobExecutionListener) {
      this.mainJobExecutionListener = mainJobExecutionListener;
   }
}
