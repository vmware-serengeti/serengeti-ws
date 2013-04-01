package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.apitypes.TaskRead.Type;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;

public class JobManager {
   static final Logger logger = Logger.getLogger(JobManager.class);
   JobRepository jobRepository;
   JobLauncher jobLauncher;
   JobExplorer jobExplorer;
   JobOperator jobOperator;
   JobRegistry jobRegistry;
   JobExecutionStatusHolder jobExecutionStatusHolder;

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
         throw BddException.NOT_FOUND("task", Long.toString(jobExecutionId));
      }

      TaskRead jobStatus = new TaskRead();
      jobStatus.setId(jobExecutionId);
      String clusterName =
            jobExecution.getJobInstance().getJobParameters()
                  .getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      jobStatus.setTarget(clusterName);
      jobStatus.setProgress(jobExecutionStatusHolder
            .getCurrentProgress(jobExecutionId));

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

      if (status.equals(Status.FAILED)) {
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
         task.setType(Type.INNER);
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

   public ClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   public void setClusterEntityMgr(ClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }
}
