package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.service.job.JobConstants;

public class TestJobManager {
   static final Logger logger = Logger.getLogger(TestJobManager.class);

   public static JobParameters createClusterCreateParameters(String name) {
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put("cluster.name", new JobParameter(name));
      return new JobParameters(param);
   }

   public static String stepsToString(Collection<StepExecution> ses) {
      StringBuilder sb = new StringBuilder();
      for (StepExecution se : ses) {
         if (sb.length() > 0) {
            sb.append(", ");
         }
         sb.append(se.getStepName()).append(":").append(se.getStatus())
               .append("-").append(se.getExecutionContext());
      }
      return sb.toString();
   }

   @Test
   public void testJobOperations() throws Exception {
      ApplicationContext context =
            new ClassPathXmlApplicationContext("spring/*.xml");

      JobManager jobManager = (JobManager) context.getBean("jobManager");

      long jobExecutionId =
            jobManager.runJob("helloWorldJob",
                  createClusterCreateParameters("cluster-" + new Date()));

      int retry = 0;
      while (retry <= 5) {
         Thread.sleep(50);
         TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
         System.out.println("progress = " + tr.getProgress());
         if (TaskRead.Status.COMPLETED.equals(tr.getStatus())) {
            logger.info("===========COMPLETED============");
            break;
         }
         if (TaskRead.Status.FAILED.equals(tr.getStatus())
               || TaskRead.Status.STOPPED.equals(tr.getStatus())) {
            ++retry;
            logger.info("failed with error message: " + tr.getErrorMessage());
            logger.info("===========RESTART: #" + jobExecutionId
                  + "============");

            jobExecutionId = jobManager.restartJobExecution(jobExecutionId);
         }
      }

   }

   @Test
   public void testJobWithCondition() throws Exception {
      ApplicationContext context =
            new ClassPathXmlApplicationContext("spring/*.xml");

      JobManager jobManager = (JobManager) context.getBean("jobManager");
      JobParametersBuilder parametersBuilder = new JobParametersBuilder();
      JobParameters nodeParameters =
            parametersBuilder
                  .addString(JobConstants.SUB_JOB_NODE_NAME,
                        "node-fail-forever")
                  .addString("date", new Date().toString()).toJobParameters();
      long jobExecutionId =
            jobManager.runJob("simpleJobWithCondition", nodeParameters);
      logger.info("started simple job with condition");
      int retry = 0;
      while (retry <= 5) {
         Thread.sleep(50);
         TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
         System.out.println("progress = " + tr.getProgress());
         if (TaskRead.Status.COMPLETED.equals(tr.getStatus())) {
            logger.info("===========COMPLETED============");
            break;
         }
         if (TaskRead.Status.FAILED.equals(tr.getStatus())
               || TaskRead.Status.STOPPED.equals(tr.getStatus())) {
            ++retry;
            logger.info("failed with error message: " + tr.getErrorMessage());
            logger.info("===========RESTART: #" + jobExecutionId
                  + "============");

            jobExecutionId = jobManager.restartJobExecution(jobExecutionId);
         }
      }

   }

   @Test
   public void testJobWithConditionSuccess() throws Exception {
      ApplicationContext context =
            new ClassPathXmlApplicationContext("spring/*.xml");

      JobManager jobManager = (JobManager) context.getBean("jobManager");
      JobParametersBuilder parametersBuilder = new JobParametersBuilder();
      JobParameters nodeParameters =
            parametersBuilder
                  .addString(JobConstants.SUB_JOB_NODE_NAME, "node1")
                  .addString("date", new Date().toString()).toJobParameters();
      long jobExecutionId =
            jobManager.runJob("simpleJobWithCondition", nodeParameters);
      logger.info("started simple job with condition");
      int retry = 0;
      while (retry <= 5) {
         Thread.sleep(50);
         TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
         System.out.println("progress = " + tr.getProgress());
         if (TaskRead.Status.COMPLETED.equals(tr.getStatus())) {
            logger.info("===========COMPLETED============");
            break;
         }
         if (TaskRead.Status.FAILED.equals(tr.getStatus())
               || TaskRead.Status.STOPPED.equals(tr.getStatus())) {
            ++retry;
            logger.info("failed with error message: " + tr.getErrorMessage());
            logger.info("===========RESTART: #" + jobExecutionId
                  + "============");

            jobExecutionId = jobManager.restartJobExecution(jobExecutionId);
         }
      }

   }

   @Test
   public void testRunSubJobForNodes() throws Exception {
      ApplicationContext context =
            new ClassPathXmlApplicationContext("spring/*.xml");
      JobManager jobManager = (JobManager) context.getBean("jobManager");
      List<JobParameters> jobParametersList = new ArrayList<JobParameters>();
      JobParametersBuilder parametersBuilder = new JobParametersBuilder();
      String clusterName = "test-cluster";
      JobParameters nodeParameters =
            parametersBuilder
                  .addString(JobConstants.SUB_JOB_NODE_NAME, "node1")
                  .addString("cluster.name", clusterName).toJobParameters();
      jobParametersList.add(nodeParameters);
      nodeParameters =
            parametersBuilder
                  .addString(JobConstants.SUB_JOB_NODE_NAME, "node2")
                  .addString("cluster.name", clusterName).toJobParameters();
      jobParametersList.add(nodeParameters);
      nodeParameters =
            parametersBuilder
                  .addString(JobConstants.SUB_JOB_NODE_NAME, "node3")
                  .addString("cluster.name", clusterName).toJobParameters();
      jobParametersList.add(nodeParameters);
      String subJobName = "simpleJob";
      long jobExecutionId =
            jobManager.runSubJobForNodes(subJobName, jobParametersList,
                  clusterName, ClusterRead.ClusterStatus.RUNNING,
                  ClusterRead.ClusterStatus.ERROR);
      while (true) {
         Thread.sleep(50);
         TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
         logger.info("======= main job progress = " + tr.getProgress());
         if (TaskRead.Status.COMPLETED.equals(tr.getStatus())
               || TaskRead.Status.FAILED.equals(tr.getStatus())) {
            logger.info("===========COMPLETED/FAILED============");
            for (TaskRead.NodeStatus success : tr.getSucceedNodes()) {
               logger.info("success node: " + success);
            }
            for (TaskRead.NodeStatus fail : tr.getFailNodes()) {
               logger.info("fail node: " + fail);
            }
            break;
         }
      }
   }

   @Test
   public void testRunSubJobForNodesWithFailure() throws Exception {
      ApplicationContext context =
            new ClassPathXmlApplicationContext("spring/*.xml");
      JobManager jobManager = (JobManager) context.getBean("jobManager");
      List<JobParameters> jobParametersList = new ArrayList<JobParameters>();
      JobParametersBuilder parametersBuilder = new JobParametersBuilder();
      String clusterName = "test-cluster";
      JobParameters nodeParameters =
            parametersBuilder
                  .addString(JobConstants.SUB_JOB_NODE_NAME, "node1")
                  .addString("cluster.name", clusterName).toJobParameters();
      jobParametersList.add(nodeParameters);
      nodeParameters =
            parametersBuilder
                  .addString(JobConstants.SUB_JOB_NODE_NAME,
                        "node-fail-forever")
                  .addString("cluster.name", clusterName).toJobParameters();
      jobParametersList.add(nodeParameters);
      nodeParameters =
            parametersBuilder
                  .addString(JobConstants.SUB_JOB_NODE_NAME, "node3")
                  .addString("cluster.name", clusterName).toJobParameters();
      jobParametersList.add(nodeParameters);
      long jobExecutionId =
            jobManager.runSubJobForNodes("simpleJob", jobParametersList,
                  clusterName, ClusterRead.ClusterStatus.RUNNING,
                  ClusterRead.ClusterStatus.ERROR);
      while (true) {
         Thread.sleep(50);
         TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
         logger.info("======= main job progress = " + tr.getProgress());
         if (TaskRead.Status.COMPLETED.equals(tr.getStatus())
               || TaskRead.Status.FAILED.equals(tr.getStatus())) {
            logger.info("===========COMPLETED/FAILED============");
            for (TaskRead.NodeStatus success : tr.getSucceedNodes()) {
               logger.info("success node: " + success);
            }
            for (TaskRead.NodeStatus fail : tr.getFailNodes()) {
               logger.info("fail node: " + fail);
            }
            break;
         }
      }
   }

   public static void main(String[] args) throws Exception {
      new TestJobManager().testJobOperations();
   }
}
