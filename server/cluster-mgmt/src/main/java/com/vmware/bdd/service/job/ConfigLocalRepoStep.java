/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.job;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.ShellCommandExecutor;

public class ConfigLocalRepoStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(ConfigLocalRepoStep.class);
   private ClusterManager clusterManager;
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;
   private SoftwareManagerCollector softwareMgrs;

   public IExclusiveLockedClusterEntityManager getLockClusterEntityMgr() {
      return lockClusterEntityMgr;
   }

   @Autowired
   public void setLockClusterEntityMgr(
         IExclusiveLockedClusterEntityManager lockClusterEntityMgr) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
   }

   public SoftwareManagerCollector getSoftwareMgrs() {
      return softwareMgrs;
   }

   @Autowired
   public void setSoftwareMgrs(SoftwareManagerCollector softwareMgrs) {
      this.softwareMgrs = softwareMgrs;
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      // This step is only for app manager like ClouderaMgr and Ambari
      String clusterName = getJobParameters(chunkContext).getString(
                    JobConstants.CLUSTER_NAME_JOB_PARAM);
      
      SoftwareManager softwareMgr =
            softwareMgrs.getSoftwareManagerByClusterName(clusterName);

      String appMgrName = softwareMgr.getName();
      if ( Constants.IRONFAN.equals( appMgrName ) ) {
    	  // we do not config any local repo for Ironfan
    	  return RepeatStatus.FINISHED;
      }
          
      ClusterCreate clusterConfig = clusterManager.getClusterConfigMgr().getClusterConfig(clusterName);
      String localRepoURL = clusterConfig.getLocalRepoURL();
      logger.info("Use the following URL as the local yum server:" + localRepoURL);
      
      if ( !CommonUtil.isBlank(localRepoURL) ) {
    	  // 1, Create a local repo file for ClouderaMgr/Ambari.
          logger.info("Create a local repo file for ClouderaMgr/Ambari.");
          
          // we use the same repo id with the ClouderaManager and Ambari for now, but use
          // higher priority for repo searching
          String appMgrRepoID = Constants.NODE_APPMANAGER_YUM_CLOUDERA_MANAGER_REPO_ID;
          if ( appMgrName.equals(Constants.AMBARI_PLUGIN_TYPE) ) {
        	  appMgrRepoID = Constants.NODE_APPMANAGER_YUM_AMBARI_REPO_ID;
          }
          
          // write the repo file contents
          long timestamp = new Date().getTime();
          String tmpFilePathName = "/tmp/localrepo_" + timestamp;
          FileOutputStream fos = new FileOutputStream(tmpFilePathName);
          BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(fos));
          fw.write("[" + appMgrRepoID + "]\n");
          fw.write("name = local app manager yum server\n");
          fw.write("baseurl = " + localRepoURL + "\n");
          fw.write("gpgcheck = 0\n");
          fw.write("enabled = 1\n");
          fw.write("priority = 1\n");
          fw.close();

          // 2, Remote scp the repo file to each node to set the local repo
          String sshUser = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);
          List<NodeEntity> nodes = lockClusterEntityMgr.getClusterEntityMgr().findAllNodes(clusterName);
          for (NodeEntity node : nodes) {
              String nodeIp = node.getPrimaryMgtIpV4();
              
              // add the write permission on the directory /etc/yum.repos.d/
              String changeRepoDirPermCommand = "ssh -tt " + sshUser + "@" + nodeIp + " 'sudo chmod 777 " + 
            		  Constants.NODE_APPMANAGER_YUM_REPO_DIR + "'";
              
              logger.info("The changing repo dir permission command is: " + changeRepoDirPermCommand);
              ShellCommandExecutor.execCmd(changeRepoDirPermCommand, null, null,
                      0, Constants.NODE_ACTION_CHANGE_REPO_DIR_PERMISSION);

              try {
			  	  // create a backup directory /etc/yum.repos.d/backup on each node, and move all the CentOS*.repo here
				  String makeBackupDirCommand = "ssh -tt " + sshUser + "@" + nodeIp + " 'sudo mkdir " + 
						  Constants.NODE_APPMANAGER_YUM_REPO_DIR + "/backup'";
				  String moveCentOSReposCommand = "ssh -tt " + sshUser + "@" + nodeIp + " 'sudo mv " + 
						  Constants.NODE_APPMANAGER_YUM_REPO_DIR + "/CentOS*.repo " + 
						  Constants.NODE_APPMANAGER_YUM_REPO_DIR + "/backup'";
				
				  logger.info("move all the CentOS*.repo to /etc/yum.repos.d/backup directory on each node");
				  ShellCommandExecutor.execCmd(makeBackupDirCommand, null, null, 0, Constants.NODE_ACTION_MAKE_BACKUP_DIR);
				  ShellCommandExecutor.execCmd(moveCentOSReposCommand, null, null, 0, Constants.NODE_ACTION_MOVE_CENTOS_REPO);
			  } catch (Exception e) {
				  // for resume job, these steps might have been executed already, so we do not block the whole task step
				  logger.info(e.getMessage());
			  }

              // copy the local appmgr repo file to remote directory /etc/yum.repos.d/
              String scpLocalRepoFileCommand = "scp " + tmpFilePathName + " " + 
            		  sshUser + "@" + nodeIp + ":" + Constants.NODE_APPMANAGER_YUM_LOCAL_REPO_FILE;
            
              logger.info("The remote scp command to set local repo is: " + scpLocalRepoFileCommand);
              ShellCommandExecutor.execCmd(scpLocalRepoFileCommand, null, null,
            		  0, Constants.NODE_ACTION_SCP_LOCALREPO_FILE);
          }

      }

      return RepeatStatus.FINISHED;
   }

   public ClusterManager getClusterManager() {
      return clusterManager;
   }

   public void setClusterManager(ClusterManager clusterManager) {
      this.clusterManager = clusterManager;
   }
   
}

