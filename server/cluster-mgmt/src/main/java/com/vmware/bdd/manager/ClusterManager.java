/***************************************************************************

 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package com.vmware.bdd.manager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.apitypes.LimitInstruction;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.exception.ClusterHealServiceException;
import com.vmware.bdd.exception.ClusterManagerException;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.IClusterHealService;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.IExecutionService;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.specpolicy.ClusterSpecFactory;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.VcCluster;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.JobUtils;
import com.vmware.bdd.utils.ValidationUtils;

public class ClusterManager {
   static final Logger logger = Logger.getLogger(ClusterManager.class);
   private ClusterConfigManager clusterConfigMgr;

   private INetworkService networkManager;

   private JobManager jobManager;

   private DistroManager distroManager;

   private IResourceService resMgr;

   private IClusterEntityManager clusterEntityMgr;

   private RackInfoManager rackInfoMgr;

   private IClusteringService clusteringService;

   private IClusterHealService clusterHealService;

   private IExecutionService executionService;

   public JobManager getJobManager() {
      return jobManager;
   }

   public void setJobManager(JobManager jobManager) {
      this.jobManager = jobManager;
   }

   public ClusterConfigManager getClusterConfigMgr() {
      return clusterConfigMgr;
   }

   public void setClusterConfigMgr(ClusterConfigManager clusterConfigMgr) {
      this.clusterConfigMgr = clusterConfigMgr;
   }

   public INetworkService getNetworkManager() {
      return networkManager;
   }

   public void setNetworkManager(INetworkService networkManager) {
      this.networkManager = networkManager;
   }

   public IResourceService getResMgr() {
      return resMgr;
   }

   @Autowired
   public void setResMgr(IResourceService resMgr) {
      this.resMgr = resMgr;
   }

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   public RackInfoManager getRackInfoMgr() {
      return rackInfoMgr;
   }

   @Autowired
   public void setRackInfoMgr(RackInfoManager rackInfoMgr) {
      this.rackInfoMgr = rackInfoMgr;
   }

   public DistroManager getDistroManager() {
      return distroManager;
   }

   @Autowired
   public void setDistroManager(DistroManager distroManager) {
      this.distroManager = distroManager;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   @Autowired
   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   public IClusterHealService getClusterHealService() {
      return clusterHealService;
   }

   @Autowired
   public void setClusterHealService(IClusterHealService clusterHealService) {
      this.clusterHealService = clusterHealService;
   }

   public IExecutionService getExecutionService() {
      return executionService;
   }

   @Autowired
   public void setExecutionService(IExecutionService executionService) {
      this.executionService = executionService;
   }

   public Map<String, Object> getClusterConfigManifest(String clusterName,
         List<String> targets, boolean needAllocIp) {
      ClusterCreate clusterConfig =
            clusterConfigMgr.getClusterConfig(clusterName, needAllocIp);
      Map<String, String> cloudProvider = resMgr.getCloudProviderAttributes();
      ClusterRead read = clusterEntityMgr.toClusterRead(clusterName, true);
      Map<String, Object> attrs = new HashMap<String, Object>();
      attrs.put("cloud_provider", cloudProvider);
      attrs.put("cluster_definition", clusterConfig);
      if (read != null) {
         attrs.put("cluster_data", read);
      }
      if (targets != null && !targets.isEmpty()) {
         attrs.put("targets", targets);
      }
      return attrs;
   }

   private void writeJsonFile(Map<String, Object> clusterConfig, File file) {
      Gson gson =
            new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                  .setPrettyPrinting().create();
      String jsonStr = gson.toJson(clusterConfig);

      AuAssert.check(jsonStr != null);
      logger.info("writing cluster manifest in json " + jsonStr + " to file "
            + file);
      BufferedWriter out = null;
      try {
         out =
               new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                     file), "UTF-8"));
         out.write(jsonStr);
      } catch (IOException ex) {
         logger.error(ex.getMessage()
               + "\n failed to write cluster manifest to file " + file);
         throw BddException.INTERNAL(ex, "Failed to write cluster manifest.");
      } finally {
         if (out != null) {
            try {
               out.close();
            } catch (IOException e) {
               logger.error("falied to close writer" + out, e);
            }
         }
      }
   }

   public File writeClusterSpecFile(String targetName, File workDir,
         boolean needAllocIp) {
      String clusterName = targetName.split("-")[0];
      String fileName = clusterName + ".json";
      List<String> targets = new ArrayList<String>(1);
      targets.add(targetName);
      Map<String, Object> clusterConfig =
            getClusterConfigManifest(clusterName, targets, needAllocIp);

      File file = new File(workDir, fileName);
      writeJsonFile(clusterConfig, file);

      return file;
   }

   private boolean checkAndResetNodePowerStatusChanged(String clusterName) {
      boolean statusStale = false;

      for (NodeEntity node : clusterEntityMgr.findAllNodes(clusterName)) {
         if (node.isPowerStatusChanged()) {
            switch (node.getStatus()) {
            case VM_READY:
               statusStale = true;
               break;
            case SERVICE_READY:
            case BOOTSTRAP_FAILED:
               node.setPowerStatusChanged(false);
               break;
            }
         }
      }

      return statusStale;
   }

   private void refreshClusterStatus(String clusterName) {
      List<String> clusterNames = new ArrayList<String>();
      clusterNames.add(clusterName);
      refreshClusterStatus(clusterNames);
   }

   private void refreshClusterStatus(List<String> clusterNames) {
      for (String clusterName : clusterNames) {
         Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
         param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(
               new Date()));
         param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
               clusterName));
         JobParameters jobParameters = new JobParameters(param);
         try {
            long jobExecutionId =
                  jobManager.runJob(JobConstants.QUERY_CLUSTER_JOB_NAME,
                        jobParameters);
            TaskRead status = jobManager.getJobExecutionStatus(jobExecutionId);
            while (status.getStatus() != TaskRead.Status.COMPLETED
                  && status.getStatus() != TaskRead.Status.FAILED
                  && status.getStatus() != TaskRead.Status.ABANDONED
                  && status.getStatus() != TaskRead.Status.STOPPED) {
               Thread.sleep(1000);
               status = jobManager.getJobExecutionStatus(jobExecutionId);
            }
         } catch (Exception ex) {
            logger.error("failed to run query cluster job: " + clusterName, ex);
         }
      }
   }

   public ClusterRead getClusterByName(String clusterName, boolean realTime) {
      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      // return the latest data from db
      if (realTime
            && (cluster.getStatus() == ClusterStatus.RUNNING || cluster
                  .getStatus() == ClusterStatus.VHM_RUNNING)
            // for not running cluster, we don't sync up status from chef
            && checkAndResetNodePowerStatusChanged(clusterName)) {
         refreshClusterStatus(clusterName);
      }

      return clusterEntityMgr.toClusterRead(clusterName);
   }

   public ClusterCreate getClusterSpec(String clusterName) {
      ClusterCreate spec = clusterConfigMgr.getClusterConfig(clusterName);
      spec.setVcClusters(null);
      spec.setTemplateId(null);
      spec.setDistroMap(null);
      spec.setSharedDatastorePattern(null);
      spec.setLocalDatastorePattern(null);
      spec.setNetworkings(null);
      spec.setRpNames(null);
      spec.setDsNames(null);
      spec.setNetworkConfig(null);
      spec.setName(null);
      spec.setDistro(null);
      spec.setValidateConfig(null);
      spec.setSpecFile(null);
      spec.setTopologyPolicy(null);
      spec.setHostToRackMap(null);
      spec.setHttpProxy(null);
      spec.setNoProxy(null);
      spec.setDistroVendor(null);
      spec.setDistroVersion(null);
      spec.setPassword(null);
      NodeGroupCreate[] groups = spec.getNodeGroups();
      if (groups != null) {
         for (NodeGroupCreate group : groups) {
            group.setVcClusters(null);
            group.setGroupType(null);
            group.setRpNames(null);
            group.getStorage().setDsNames(null);
            group.getStorage().setImagestoreNamePattern(null);
            group.getStorage().setDiskstoreNamePattern(null);
            group.setVmFolderPath(null);
            group.getStorage().setSplitPolicy(null);
            group.getStorage().setControllerType(null);
            group.getStorage().setAllocType(null);
            if (group.getPlacementPolicies() != null) {
               List<GroupAssociation> associations =
                     group.getPlacementPolicies().getGroupAssociations();
               if (associations != null && associations.isEmpty()) {
                  group.getPlacementPolicies().setGroupAssociations(null);
               }
            }
         }
      }
      return spec;
   }

   public List<ClusterRead> getClusters(Boolean realTime) {
      List<ClusterRead> clusters = new ArrayList<ClusterRead>();
      List<ClusterEntity> clusterEntities = clusterEntityMgr.findAllClusters();
      for (ClusterEntity entity : clusterEntities) {
         clusters.add(getClusterByName(entity.getName(), realTime));
      }
      return clusters;
   }

   public Long createCluster(ClusterCreate createSpec) throws Exception {
      if (CommonUtil.isBlank(createSpec.getDistro())) {
         setDefaultDistro(createSpec);
      }
      DistroRead distroRead =
            getDistroManager().getDistroByName(createSpec.getDistro());
      createSpec.setDistroVendor(distroRead.getVendor());
      createSpec.setDistroVersion(distroRead.getVersion());
      // create auto rps if vc cluster/rp is specified
      createAutoRps(createSpec);
      ClusterCreate clusterSpec =
            ClusterSpecFactory.getCustomizedSpec(createSpec);
      createSpec.verifyClusterNameLength();
      clusterSpec.validateNodeGroupNames();
      //Check the cpu, memory max configuration according vm hardware version
      if (clusterSpec != null && clusterSpec.getNodeGroups() != null) {
         for (NodeGroupCreate ng : clusterSpec.getNodeGroups()) {
            String templateVmId = clusteringService.getTemplateVmId();
            if (templateVmId != null) {
               VcResourceUtils.checkVmMaxConfiguration(templateVmId,
                     ng.getCpuNum() == null ? 0 : ng.getCpuNum(),
                     ng.getMemCapacityMB() == null ? 0 : ng.getMemCapacityMB());
            }
         }
      }
      String name = clusterSpec.getName();
      logger.info("ClusteringService, creating cluster " + name);

      List<String> dsNames = getUsedDS(clusterSpec.getDsNames());
      if (dsNames.isEmpty()) {
         throw ClusterConfigException.NO_DATASTORE_ADDED();
      }
      List<VcCluster> vcClusters = getUsedVcClusters(clusterSpec.getRpNames());
      if (vcClusters == null || vcClusters.isEmpty()) {
         throw ClusterConfigException.NO_RESOURCE_POOL_ADDED();
      }
      // validate accessibility
      validateDatastore(dsNames, vcClusters);
      validateNetworkAccessibility(createSpec.getNetworkNames(), vcClusters);
      //save configuration into meta-db, and extend configuration using default spec
      clusterConfigMgr.createClusterConfig(clusterSpec);
      clusterEntityMgr.updateClusterStatus(name, ClusterStatus.PROVISIONING);
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.PROVISION_ERROR.name()));
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            createSpec.getName()));
      param.put(JobConstants.VERIFY_NODE_STATUS_SCOPE_PARAM, new JobParameter(
            JobConstants.CLUSTER_NODE_SCOPE_VALUE));
      JobParameters jobParameters = new JobParameters(param);
      return jobManager.runJob(JobConstants.CREATE_CLUSTER_JOB_NAME,
            jobParameters);
   }

   private void createAutoRps(ClusterCreate createSpec) {
      if (createSpec.getVcClusters() == null) {
         return;
      }
      // user specify vc resource pools directly, create auto rp in meta db dynamically
      List<VcCluster> vcClusters = createSpec.getVcClusters();
      List<String> rpNames =
            clusterConfigMgr.getRpMgr().addAutoResourcePools(vcClusters, true);
      logger.info("added automation resource pools: " + rpNames);
      createSpec.setRpNames(rpNames);
   }

   private void validateNetworkAccessibility(List<String> networkList,
         List<VcCluster> clusters) {
      AuAssert.check(networkList != null && !networkList.isEmpty());
      Set<String> networkNames = new HashSet<String>();
      networkNames.addAll(networkList);

      logger.info("start to validate network accessibility.");
      if (!resMgr.isNetworkAccessibleByCluster(networkList, clusters)) {
         List<String> clusterNames = new ArrayList<String>();
         for (VcCluster cluster : clusters) {
            clusterNames.add(cluster.getName());
         }
         throw ClusterConfigException.NETWORK_UNACCESSIBLE(networkList,
               clusterNames);
      }
   }

   private void validateDatastore(List<String> dsNames, List<VcCluster> clusters) {
      // validate if there is any datastore is accessible by one cluster
      logger.info("start to validate accessibility for datastores: " + dsNames
            + ", and clusters: " + clusters);
      boolean found = false;
      for (String dsName : dsNames) {
         for (VcCluster vcCluster : clusters) {
            if (resMgr.isDatastoreAccessibleByCluster(dsName,
                  vcCluster.getName())) {
               found = true;
               break;
            }
         }
      }
      if (!found) {
         // no any datastore is accessible by specified cluster
         List<String> vcClusterNames = new ArrayList<String>();
         for (VcCluster vcCluster : clusters) {
            vcClusterNames.add(vcCluster.getName());
         }
         throw ClusterConfigException.DATASTORE_UNACCESSIBLE(vcClusterNames,
        		 dsNames);
      }
   }

   private List<String> getUsedDS(List<String> specifiedDsNames) {
      if (specifiedDsNames == null || specifiedDsNames.isEmpty()) {
         specifiedDsNames = new ArrayList<String>();
         specifiedDsNames.addAll(clusterConfigMgr.getDatastoreMgr()
               .getAllDatastoreNames());
      }
      return specifiedDsNames;
   }

   private List<VcCluster> getUsedVcClusters(List<String> rpNames) {
      List<VcCluster> clusters = null;
      if (rpNames == null || rpNames.isEmpty()) {
         clusters = clusterConfigMgr.getRpMgr().getAllVcResourcePool();
      } else {
         clusters = new ArrayList<VcCluster>();
         StringBuffer nonexistentRpNames = new StringBuffer();
         for (String rpName : rpNames) {
            List<VcCluster> vcClusters =
                  clusterConfigMgr.getRpMgr().getVcResourcePoolByName(rpName);

            if (vcClusters == null) {
               nonexistentRpNames.append(rpName).append(",");
            } else {
               clusters.addAll(vcClusters);
            }
         }
         if (nonexistentRpNames.length() > 0) {
            nonexistentRpNames.delete(nonexistentRpNames.length()-1, nonexistentRpNames.length());
            throw VcProviderException
                  .RESOURCE_POOL_NOT_FOUND(nonexistentRpNames.toString());
         }
      }
      return clusters;
   }

   private void setDefaultDistro(ClusterCreate createSpec) {
      List<DistroRead> distroList = distroManager.getDistros();
      DistroRead[] distros = new DistroRead[distroList.size()];
      createSpec.setDistro(createSpec.getDefaultDistroName(distroList
            .toArray(distros)));
   }

   public Long configCluster(String clusterName, ClusterCreate createSpec)
         throws Exception {
      logger.info("ClusterManager, config cluster " + clusterName);
      ClusterEntity cluster;

      if ((cluster = clusterEntityMgr.findByName(clusterName)) == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())
            && !ClusterStatus.CONFIGURE_ERROR.equals(cluster.getStatus())) {
         logger.error("can not config cluster: " + clusterName + ", "
               + cluster.getStatus());
         throw ClusterManagerException.UPDATE_NOT_ALLOWED_ERROR(clusterName,
               "To update a cluster, its status must be RUNNING");
      }
      clusterConfigMgr.updateAppConfig(clusterName, createSpec);

      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.CONFIGURE_ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName,
            ClusterStatus.CONFIGURING);
      try {
         return jobManager.runJob(JobConstants.CONFIG_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to configure cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.CONFIGURE_ERROR);
         throw e;
      }
   }

   public Long resumeClusterCreation(String clusterName) throws Exception {
      logger.info("ClusterManager, resume cluster creation " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);

      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (cluster.getStatus() != ClusterStatus.PROVISION_ERROR) {
         logger.error("can not resume creation of cluster: " + clusterName
               + ", " + cluster.getStatus());
         throw ClusterManagerException.UPDATE_NOT_ALLOWED_ERROR(clusterName,
               "To update a cluster, its status must be PROVISION_ERROR");
      }
      List<String> dsNames = getUsedDS(cluster.getVcDatastoreNameList());
      if (dsNames.isEmpty()) {
         throw ClusterConfigException.NO_RESOURCE_POOL_ADDED();
      }
      List<VcCluster> vcClusters = getUsedVcClusters(cluster.getVcRpNameList());
      if (vcClusters.isEmpty()) {
         throw ClusterConfigException.NO_DATASTORE_ADDED();
      }
      // validate accessibility
      validateDatastore(dsNames, vcClusters);
      validateNetworkAccessibility(cluster.fetchNetworkNameList(), vcClusters);
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.PROVISION_ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName,
            ClusterStatus.PROVISIONING);
      clusterEntityMgr.cleanupActionError(clusterName);
      try {
         return jobManager.runJob(JobConstants.RESUME_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to resume cluster creation for cluster "
               + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.PROVISION_ERROR);
         throw e;
      }
   }

   public Long deleteClusterByName(String clusterName) throws Exception {
      logger.info("ClusterManager, deleting cluster " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);

      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())
            && !ClusterStatus.STOPPED.equals(cluster.getStatus())
            && !ClusterStatus.ERROR.equals(cluster.getStatus())
            && !ClusterStatus.PROVISION_ERROR.equals(cluster.getStatus())
            && !ClusterStatus.CONFIGURE_ERROR.equals(cluster.getStatus())) {
         logger.error("cluster: " + clusterName
               + " cannot be deleted, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.DELETION_NOT_ALLOWED_ERROR(clusterName,
               "To delete a cluster, its status must be RUNNING, STOPPED, ERROR, or PROVISION_ERROR");
      }
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.DELETING);
      clusterEntityMgr.cleanupActionError(clusterName);
      try {
         return jobManager.runJob(JobConstants.DELETE_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to delete cluster " + clusterName, e);
         cluster = clusterEntityMgr.findByName(clusterName);
         if (cluster != null) {
            clusterEntityMgr.updateClusterStatus(clusterName,
                  ClusterStatus.ERROR);
         }
         throw e;
      }
   }

   public Long startCluster(String clusterName) throws Exception {
      logger.info("ClusterManager, starting cluster " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (ClusterStatus.RUNNING.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName + " is running already");
         throw ClusterManagerException.ALREADY_STARTED_ERROR(clusterName);
      }

      if (!ClusterStatus.STOPPED.equals(cluster.getStatus())
            && !ClusterStatus.ERROR.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName
               + " cannot be started, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.START_NOT_ALLOWED_ERROR(clusterName,
               "To start a cluster, its status must be STOPPED");
      }

      cluster.setVhmTargetNum(-1);
      clusterEntityMgr.update(cluster);
      clusterEntityMgr.cleanupActionError(clusterName);
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.STARTING);
      try {
         return jobManager.runJob(JobConstants.START_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to start cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.ERROR);
         throw e;
      }
   }

   public Long stopCluster(String clusterName) throws Exception {
      logger.info("ClusterManager, stopping cluster " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (ClusterStatus.STOPPED.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName + " is stopped already");
         throw ClusterManagerException.ALREADY_STOPPED_ERROR(clusterName);
      }

      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())
            && !ClusterStatus.ERROR.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName
               + " cannot be stopped, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.STOP_NOT_ALLOWED_ERROR(clusterName,
               "To stop a cluster, its status must be RUNNING");
      }
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.STOPPED.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.STOPPING);
      clusterEntityMgr.cleanupActionError(clusterName);
      try {
         return jobManager.runJob(JobConstants.STOP_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to stop cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.ERROR);
         throw e;
      }
   }

   public Long resizeCluster(String clusterName, String nodeGroupName,
         int instanceNum) throws Exception {
      logger.info("ClusterManager, updating node group " + nodeGroupName
            + " in cluster " + clusterName + " reset instance number to "
            + instanceNum);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      List<String> dsNames = getUsedDS(cluster.getVcDatastoreNameList());
      if (dsNames.isEmpty()) {
         throw ClusterConfigException.NO_RESOURCE_POOL_ADDED();
      }

      List<VcCluster> vcClusters = getUsedVcClusters(cluster.getVcRpNameList());
      if (vcClusters.isEmpty()) {
         throw ClusterConfigException.NO_DATASTORE_ADDED();
      }

      // validate accessibility
      validateDatastore(dsNames, vcClusters);
      validateNetworkAccessibility(cluster.fetchNetworkNameList(), vcClusters);

      NodeGroupEntity group =
            clusterEntityMgr.findByName(cluster, nodeGroupName);
      if (group == null) {
         logger.error("nodegroup " + nodeGroupName + " of cluster "
               + clusterName + " does not exist");
         throw ClusterManagerException.NODEGROUP_NOT_FOUND_ERROR(nodeGroupName);
      }

      // resize of job tracker and name node is not supported
      List<String> roles = group.getRoleNameList();
      List<String> unsupportedRoles = new ArrayList<String>();
      AuAssert.check(!roles.isEmpty(), "roles should not be empty");
      if (roles.contains(HadoopRole.HADOOP_NAMENODE_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.HADOOP_NAMENODE_ROLE.toString());
      }
      if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString());
      }
      if (roles.contains(HadoopRole.ZOOKEEPER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.ZOOKEEPER_ROLE.toString());
      }
      if (!unsupportedRoles.isEmpty()) {
         logger.info("can not resize node group with role: " + unsupportedRoles);
         throw ClusterManagerException.ROLES_NOT_SUPPORTED(unsupportedRoles);
      }

      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName
               + " can be resized only in RUNNING status, it is now in "
               + cluster.getStatus() + " status");
         throw ClusterManagerException.UPDATE_NOT_ALLOWED_ERROR(clusterName,
               "To update a cluster, its status must be RUNNING");
      }

      if (instanceNum <= group.getDefineInstanceNum()) {
         logger.error("node group " + nodeGroupName
               + " cannot be shrinked from " + group.getDefineInstanceNum()
               + " to " + instanceNum + " nodes");
         throw ClusterManagerException.SHRINK_OP_NOT_SUPPORTED(nodeGroupName,
               instanceNum, group.getDefineInstanceNum());
      }

      Integer instancePerHost = group.getInstancePerHost();
      if (instancePerHost != null && instanceNum % instancePerHost != 0) {
         throw BddException
               .INVALID_PARAMETER(
                     "instance number",
                     new StringBuilder(100)
                           .append(instanceNum)
                           .append(
                                 ".instanceNum must be evenly divisible by instancePerHost")
                           .toString());
      }

      ValidationUtils.validHostNumber(clusterEntityMgr, group, instanceNum);
      ValidationUtils.hasEnoughHost(rackInfoMgr, clusterEntityMgr, group,
            instanceNum);

      int oldInstanceNum = group.getDefineInstanceNum();
      group.setDefineInstanceNum(instanceNum);

      clusterEntityMgr.update(group);
      clusterEntityMgr.cleanupActionError(clusterName);
      // create job
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.GROUP_NAME_JOB_PARAM, new JobParameter(
            nodeGroupName));
      param.put(JobConstants.GROUP_INSTANCE_NEW_NUMBER_JOB_PARAM,
            new JobParameter(Long.valueOf(instanceNum)));
      param.put(JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM,
            new JobParameter(Long.valueOf(oldInstanceNum)));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.VERIFY_NODE_STATUS_SCOPE_PARAM, new JobParameter(
            JobConstants.GROUP_NODE_SCOPE_VALUE));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.UPDATING);
      try {
         return jobManager.runJob(JobConstants.RESIZE_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to resize cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.RUNNING);
         group.setDefineInstanceNum(oldInstanceNum);
         clusterEntityMgr.update(group);
         throw e;
      }
   }

   /**
    * set cluster parameters synchronously
    *
    * @param clusterName
    * @param activeComputeNodeNum
    * @param minComputeNodeNum
    * @param maxComputeNodeNum
    * @param enableAuto
    * @param ioPriority
    * @return
    * @throws Exception
    */
   @SuppressWarnings("unchecked")
   public List<String> syncSetParam(String clusterName,
         Integer activeComputeNodeNum, Integer minComputeNodeNum, Integer maxComputeNodeNum,
         Boolean enableAuto, Priority ioPriority) throws Exception {

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      ClusterRead clusterRead = getClusterByName(clusterName, false);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      //update vm ioshares
      if (ioPriority != null) {
         prioritizeCluster(clusterName, ioPriority);
      }

      // as prioritizeCluster will update clusterEntity, here we need to refresh to avoid overriding
      cluster = clusterEntityMgr.findByName(clusterName);

      if (enableAuto != null && enableAuto != cluster.getAutomationEnable()) {
         if (enableAuto && cluster.getDistro().equalsIgnoreCase(Constants.MAPR_VENDOR)) {
            logger.error("cluster " + clusterName + " is a MAPR distro, which cannot be auto scaled");
            throw BddException.NOT_ALLOWED_SCALING("cluster", clusterName);
         }
         cluster.setAutomationEnable(enableAuto);
      }

      if (minComputeNodeNum != null
            && minComputeNodeNum != cluster.getVhmMinNum()) {
         cluster.setVhmMinNum(minComputeNodeNum);
      }

      if (maxComputeNodeNum != null
            && maxComputeNodeNum != cluster.getVhmMaxNum()) {
         cluster.setVhmMaxNum(maxComputeNodeNum);
      }

      List<String> nodeGroupNames = new ArrayList<String>();
      if ((enableAuto != null || minComputeNodeNum != null || maxComputeNodeNum != null || activeComputeNodeNum != null)
            && !clusterRead.validateSetManualElasticity(nodeGroupNames)) {
         throw BddException.INVALID_PARAMETER("cluster", clusterName);
      }

      if (activeComputeNodeNum != null) {
         if (!activeComputeNodeNum.equals(cluster.getVhmTargetNum())) {
            cluster.setVhmTargetNum(activeComputeNodeNum);
         }
      }

      //enableAuto is only set during cluster running status and
      //other elasticity attributes are only set during cluster running/stop status
      if ((enableAuto != null)
            && !ClusterStatus.RUNNING.equals(cluster.getStatus())) {
         logger.error("Cannot change elasticity mode, when cluster "
               + clusterName + " is in " + cluster.getStatus() + " status");
         throw ClusterManagerException.SET_AUTO_ELASTICITY_NOT_ALLOWED_ERROR(
               clusterName, "The cluster's status must be RUNNING");
      }
      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())
            && !ClusterStatus.STOPPED.equals(cluster.getStatus())) {
         logger.error("Cannot change elasticity parameters, when cluster "
               + clusterName + " is in " + cluster.getStatus() + " status");
         throw ClusterManagerException.SET_AUTO_ELASTICITY_NOT_ALLOWED_ERROR(
               clusterName, "The cluster's status must be RUNNING or STOPPED");
      }

      clusterEntityMgr.update(cluster);

      //update vhm extra config file
      if (enableAuto != null || minComputeNodeNum != null || maxComputeNodeNum != null) {
         boolean success =
               clusteringService.setAutoElasticity(clusterName, false);
         if (!success) {
				throw ClusterManagerException
						.FAILED_TO_SET_AUTO_ELASTICITY_ERROR(clusterName,
								"Could not update elasticity configuration file");
         }
      }

      //waitForManual if switch to Manual and targetNodeNum is null
      if (enableAuto != null && !enableAuto
            && cluster.getVhmTargetNum() == null) {
         JobUtils.waitForManual(clusterName, executionService);
      }

      return nodeGroupNames;
   }

   /**
    * set cluster parameters asynchronously
    *
    * @param clusterName
    * @param activeComputeNodeNum
    * @param minComputeNodeNum
    * @param maxComputeNodeNum
    * @param enableAuto
    * @param ioPriority
    * @return
    * @throws Exception
    */
   public Long asyncSetParam(String clusterName, Integer activeComputeNodeNum,
         Integer minComputeNodeNum, Integer maxComputeNodeNum, Boolean enableAuto,
         Priority ioPriority)
         throws Exception {

      syncSetParam(clusterName, activeComputeNodeNum, minComputeNodeNum, maxComputeNodeNum,
            enableAuto, ioPriority);

      ClusterRead cluster = getClusterByName(clusterName, false);
      // cluster must be running status
      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())) {
         String msg = "Cluster "+ clusterName +" is not running.";
         logger.error(msg);
         throw ClusterManagerException
               .SET_MANUAL_ELASTICITY_NOT_ALLOWED_ERROR(msg);
      }

      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      // TODO: transfer SET_TARGET/UNLIMIT from CLI directly
      if (activeComputeNodeNum == null) {
         param.put(JobConstants.VHM_ACTION_JOB_PARAM, new JobParameter(
               LimitInstruction.actionWaitForManual));
      } else if (activeComputeNodeNum == -1) {
         param.put(JobConstants.VHM_ACTION_JOB_PARAM, new JobParameter(
               LimitInstruction.actionUnlimit));
      } else {
         param.put(JobConstants.VHM_ACTION_JOB_PARAM, new JobParameter(
               LimitInstruction.actionSetTarget));
      }
      if (activeComputeNodeNum != null) {
         param.put(JobConstants.ACTIVE_COMPUTE_NODE_NUMBER_JOB_PARAM,
               new JobParameter(Long.valueOf(activeComputeNodeNum)));
      }
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      JobParameters jobParameters = new JobParameters(param);
      try {
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.VHM_RUNNING);
         return jobManager.runJob(JobConstants.SET_MANUAL_ELASTICITY_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to set manual elasticity for cluster "
               + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.RUNNING);
         throw e;
      }
   }

   /*
    * Change the disk I/O priority of the cluster or a node group
    */
   public void prioritizeCluster(String clusterName, Priority ioShares)
         throws Exception {
      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (ioShares.equals(cluster.getIoShares())) {
         return;
      }

      logger.info("Change all nodes' disk I/O shares to " + ioShares
            + " in the cluster " + clusterName);

      // cluster must be in RUNNING or STOPPEED status
      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())
            && !ClusterStatus.STOPPED.equals(cluster.getStatus())) {
         String msg = "The cluster's status must be RUNNING or STOPPED";
         logger.error(msg);
         throw ClusterManagerException.PRIORITIZE_CLUSTER_NOT_ALLOWED_ERROR(
               clusterName, msg);
      }

      // get target nodeuster
      List<NodeEntity> targetNodes;
      targetNodes = clusterEntityMgr.findAllNodes(clusterName);

      if (targetNodes.isEmpty()) {
         throw ClusterManagerException.PRIORITIZE_CLUSTER_NOT_ALLOWED_ERROR(
               clusterName, "Target node set is empty");
      }

      clusterEntityMgr.cleanupActionError(clusterName);
      // call clustering service to set the io shares
      Map<String, String> failedNodes =
            clusteringService
                  .configIOShares(clusterName, targetNodes, ioShares);
      if (failedNodes.isEmpty()) {
         logger.info("configured " + targetNodes.size() + " nodes' IO share level to "
               + ioShares.toString());
      } else {
         // update node table
         for (String name : failedNodes.keySet()) {
            NodeEntity node = clusterEntityMgr.findNodeByName(name);
            node.setActionFailed(true);
            node.setErrMessage(failedNodes.get(name));
            clusterEntityMgr.update(node);
         }
         throw ClusterManagerException.PRIORITIZE_CLUSTER_FAILED(clusterName,
               failedNodes.size(), targetNodes.size());
      }

      // update io shares in db
      cluster.setIoShares(ioShares);
      clusterEntityMgr.update(cluster);
   }

   public Long fixDiskFailures(String clusterName, String groupName)
         throws Exception {
      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      ClusterStatus oldStatus = cluster.getStatus();

      if (ClusterStatus.RUNNING != oldStatus) {
         throw ClusterHealServiceException.NOT_SUPPORTED(clusterName,
               "The cluster status must be RUNNING");
      }

      List<NodeGroupEntity> nodeGroups;

      if (groupName != null) {
         NodeGroupEntity nodeGroup =
               clusterEntityMgr.findByName(clusterName, groupName);
         if (nodeGroup == null) {
            logger.error("node group " + groupName + " does not exist");
            throw BddException.NOT_FOUND("group", groupName);
         }

         nodeGroups = new ArrayList<NodeGroupEntity>(1);
         nodeGroups.add(nodeGroup);
      } else {
         nodeGroups = clusterEntityMgr.findAllGroups(clusterName);
      }

      // only fix worker nodes that have datanode or tasktracker roles
      boolean workerNodesFound = false;
      JobParametersBuilder parametersBuilder = new JobParametersBuilder();
      List<JobParameters> jobParameterList = new ArrayList<JobParameters>();

      for (NodeGroupEntity nodeGroup : nodeGroups) {
         List<String> roles = nodeGroup.getRoleNameList();

         // TODO: more fine control on node roles
         if (HadoopRole.hasMgmtRole(roles)) {
            logger.info("node group " + nodeGroup.getName()
                  + " contains management roles, pass it");
            continue;
         }

         workerNodesFound = true;
         for (NodeEntity node : clusterEntityMgr.findAllNodes(clusterName,
               nodeGroup.getName())) {
            if (node.isObsoleteNode()) {
               logger.info("Ingore node " + node.getVmName()
                     + ", for it violate VM name convention."
                     + "or exceed defined group instance number. ");
               continue;
            }
            if (clusterHealService.hasBadDisks(node.getVmName())) {
               logger.warn("node " + node.getVmName()
                     + " has bad disks. Fixing it..");

               boolean vmPowerOn =
                     (node.getStatus().ordinal() != NodeStatus.POWERED_OFF
                           .ordinal());

               JobParameters nodeParameters =
                     parametersBuilder
                           .addString(JobConstants.CLUSTER_NAME_JOB_PARAM,
                                 clusterName)
                           .addString(JobConstants.TARGET_NAME_JOB_PARAM,
                                 node.getVmName())
                           .addString(JobConstants.GROUP_NAME_JOB_PARAM,
                                 nodeGroup.getName())
                           .addString(JobConstants.SUB_JOB_NODE_NAME,
                                 node.getVmName())
                           .addString(JobConstants.IS_VM_POWER_ON,
                                 String.valueOf(vmPowerOn)).toJobParameters();
               jobParameterList.add(nodeParameters);
            }
         }
      }

      if (!workerNodesFound) {
         throw ClusterHealServiceException
               .NOT_SUPPORTED(clusterName,
                     "only support fixing disk failures for worker/non-management nodes");
      }

      // all target nodes are healthy, simply return
      if (jobParameterList.isEmpty()) {
         logger.info("all target nodes are healthy, simply return");
         throw ClusterHealServiceException.NOT_NEEDED(clusterName);
      }

      try {
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.MAINTENANCE);
         return jobManager.runSubJobForNodes(
               JobConstants.FIX_NODE_DISK_FAILURE_JOB_NAME, jobParameterList,
               clusterName, oldStatus, oldStatus);
      } catch (Exception e) {
         logger.error("failed to fix disk failures, " + e.getMessage());
         throw e;
      }
   }
}
