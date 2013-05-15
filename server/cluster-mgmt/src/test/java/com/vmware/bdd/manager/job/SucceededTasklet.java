package com.vmware.bdd.manager.job;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;

public class SucceededTasklet extends TrackableTasklet {
   private static final Logger logger = Logger.getLogger(SucceededTasklet.class);

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      logger.info("succeeded task!");
      return RepeatStatus.FINISHED;
   }
}