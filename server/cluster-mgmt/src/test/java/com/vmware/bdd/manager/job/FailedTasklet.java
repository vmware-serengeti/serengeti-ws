package com.vmware.bdd.manager.job;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;

public class FailedTasklet extends TrackableTasklet {
   private static final Logger logger = Logger.getLogger(FailedTasklet.class);

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      logger.info("failed task!!!!!!");
      return RepeatStatus.FINISHED;
   }
}