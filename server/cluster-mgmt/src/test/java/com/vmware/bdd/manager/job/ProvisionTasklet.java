package com.vmware.bdd.manager.job;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;

public class ProvisionTasklet extends TrackableTasklet {
   private static final Logger logger = Logger.getLogger(ProvisionTasklet.class);

   private void randomlyFail(double p) throws Exception {
      if (RandomUtils.nextDouble() > 1 - p) {
         throw BddException.INTERNAL(new Exception(), "randomly fail");
      }
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      getFromJobExecutionContext(chunkContext, "clusterCreate", ClusterCreate.class);

      Thread.sleep(500);

      logger.info("provision cluster vms: "
            + getJobParameters(chunkContext).getString("cluster.name"));
      Integer processed = getFromJobExecutionContext(chunkContext, "processed.count", Integer.class); 

      for (int i = processed == null ? 0 : processed; i < 100; ++i) {
         logger.info("provision vm #" + i);
         putIntoJobExecutionContext(chunkContext, "processed.count", i); 
         jobExecutionStatusHolder.setCurrentStepProgress(
               getJobExecutionId(chunkContext), i / 100.0);
         Thread.sleep(10);
         randomlyFail(0.02);
      }

      return RepeatStatus.FINISHED;
   }
}