package com.vmware.bdd.manager;

import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ValidationUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by qjin on 11/14/14.
 */
public class ShrinkManager {
   private static final Logger logger = Logger.getLogger(ScaleManager.class);

   private IClusterEntityManager clusterEntityMgr;
   private JobManager jobManager;

   public long shrinkNodeGroup(String clusterName, String nodeGroupName, int newInstanceNum) throws Exception {
      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);
      ClusterStatus originalStatus =
            clusterEntityMgr.findByName(clusterName).getStatus();
      logger.info("Before shrink, cluster status is:" + originalStatus);

      List<JobParameters> jobParametersList = buildJobParameters(clusterName, nodeGroupName, newInstanceNum);
      if (jobParametersList.size() == 0) {
         throw ShrinkException.NO_NEED_TO_SHRINK();
      }
      //launch sub job to shrink nodegroup by processing node one by one
      try {
         logger.info("set cluster to maintenace");
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.MAINTENANCE);
         return jobManager.runSubJobForNodes(JobConstants.SHRINK_CLUSTER_JOB_NAME,
               jobParametersList, clusterName, originalStatus,
               ClusterStatus.ERROR);
      } catch (Throwable t) {
         logger.error("Failed to shrink cluster " + clusterName, t);
         clusterEntityMgr.updateClusterStatus(clusterName, originalStatus);
         throw ShrinkException.SHRINK_NODE_GROUP_FAILED(t, clusterName, t.getMessage());
      }
   }

   private List<JobParameters> buildJobParameters(String clusterName, String nodeGroupName, int newInstanceNum) {
      List<NodeEntity> nodes =
            clusterEntityMgr.findAllNodes(clusterName, nodeGroupName);
      List<JobParameters> jobParametersList = new ArrayList<JobParameters>();
      JobParametersBuilder parametersBuilder = new JobParametersBuilder();
      for (NodeEntity nodeEntity : nodes) {
         if (nodeEntity.isObsoleteNode()) {
            logger.info("Ingore node "
                  + nodeEntity.getVmName()
                  + ", for it violate VM name convention "
                  + "or exceed defined group instance number. ");
            continue;
         }
         String nodeName = nodeEntity.getVmName();
         long index = CommonUtil.getVmIndex(nodeName);
         if (index >= newInstanceNum) {
            logger.info("Trying to add " + nodeName + " in " + nodeEntity.getStatus() + " status to VMDeleteList");
            JobParameters nodeParameters =
                  parametersBuilder
                        .addString(JobConstants.SUB_JOB_NODE_NAME, nodeName)
                        .addString(JobConstants.GROUP_NAME_JOB_PARAM, nodeGroupName)
                        .addString(JobConstants.CLUSTER_NAME_JOB_PARAM,clusterName)
                        .toJobParameters();
            jobParametersList.add(nodeParameters);
         } else {
            logger.info(nodeName + " doesn't need to be delete. ");
            continue;
         }
      }
      //sort in decreasing way
      Comparator<JobParameters> shrinkJobComparator = new Comparator<JobParameters>() {
         @Override
         public int compare(JobParameters job1, JobParameters job2) {
            long index1 = CommonUtil.getVmIndex(job1.getString(JobConstants.SUB_JOB_NODE_NAME));
            long index2 = CommonUtil.getVmIndex(job2.getString(JobConstants.SUB_JOB_NODE_NAME));
            if (index1 == index2) {
               return 0;
            } else if (index1 < index2) {
               return 1;
            } else {
               return -1;
            }
         }
      };
      Collections.sort(jobParametersList, shrinkJobComparator);
      return jobParametersList;
   }

   /**
    * @return the clusterEntityMgr
    */
   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   /**
    * @param clusterEntityMgr
    *           the clusterEntityMgr to set
    */
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   /**
    * @return the jobManager
    */
   public JobManager getJobManager() {
      return jobManager;
   }

   /**
    * @param jobManager
    *           the jobManager to set
    */
   public void setJobManager(JobManager jobManager) {
      this.jobManager = jobManager;
   }


}
