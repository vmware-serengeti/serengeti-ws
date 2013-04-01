package com.vmware.bdd.manager;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.TaskRead;

public class TestJobManager {
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
         sb.append(se.getStepName()).append(":").append(se.getStatus()).append("-")
               .append(se.getExecutionContext());
      }
      return sb.toString();
   }

   @Test
   public void testJobOperations() throws Exception {
      ApplicationContext context = new ClassPathXmlApplicationContext("spring/test-context.xml");

      JobManager jobManager = (JobManager) context.getBean("jobManager");

      long jobExecutionId = jobManager.runJob("helloWorldJob",
            createClusterCreateParameters("cluster-" + new Date()));

      int retry = 0;
      while (retry <= 5) {
         Thread.sleep(50);
         TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
         System.out.println("progress = " + tr.getProgress());
         if (TaskRead.Status.COMPLETED.equals(tr.getStatus())) {
            System.out.println("===========COMPLETED============");
            break;
         }
         if (TaskRead.Status.FAILED.equals(tr.getStatus())
               || TaskRead.Status.STOPPED.equals(tr.getStatus())) {
            ++retry;
            System.out.println("failed with error message: " + tr.getErrorMessage());
            System.out
                  .println("===========RESTART: #" + jobExecutionId + "============");

            jobExecutionId = jobManager.restartJobExecution(jobExecutionId);
         }
      }

   }
   public static void main(String[] args) throws Exception {
      new TestJobManager().testJobOperations();
   }
}
