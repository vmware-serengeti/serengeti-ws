/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.clouderamgr.service;

import javax.ws.rs.NotFoundException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.software.mgmt.plugin.utils.SerialUtils;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import com.cloudera.api.ApiRootResource;
import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiBulkCommandList;
import com.cloudera.api.model.ApiCluster;
import com.cloudera.api.model.ApiClusterList;
import com.cloudera.api.model.ApiClusterVersion;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiConfig;
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiConfigStalenessStatus;
import com.cloudera.api.model.ApiHealthSummary;
import com.cloudera.api.model.ApiHost;
import com.cloudera.api.model.ApiHostInstallArguments;
import com.cloudera.api.model.ApiHostNameList;
import com.cloudera.api.model.ApiHostRef;
import com.cloudera.api.model.ApiHostRefList;
import com.cloudera.api.model.ApiParcel;
import com.cloudera.api.model.ApiRole;
import com.cloudera.api.model.ApiRoleConfigGroup;
import com.cloudera.api.model.ApiRoleList;
import com.cloudera.api.model.ApiRoleNameList;
import com.cloudera.api.model.ApiRoleRef;
import com.cloudera.api.model.ApiRoleState;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.model.ApiServiceConfig;
import com.cloudera.api.model.ApiServiceList;
import com.cloudera.api.model.ApiServiceState;
import com.cloudera.api.v3.ParcelResource;
import com.cloudera.api.v6.RootResourceV6;
import com.cloudera.api.v6.ServicesResourceV6;
import com.cloudera.api.v7.RootResourceV7;
import com.google.common.collect.ImmutableList;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.plugin.clouderamgr.exception.ClouderaManagerException;
import com.vmware.bdd.plugin.clouderamgr.exception.CommandExecFailException;
import com.vmware.bdd.plugin.clouderamgr.model.CmClusterDef;
import com.vmware.bdd.plugin.clouderamgr.model.CmNodeDef;
import com.vmware.bdd.plugin.clouderamgr.model.CmRoleDef;
import com.vmware.bdd.plugin.clouderamgr.model.CmServiceDef;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableParcelStage;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRole;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRoleContainer;
import com.vmware.bdd.plugin.clouderamgr.poller.ParcelProvisionPoller;
import com.vmware.bdd.plugin.clouderamgr.poller.host.HostInstallPoller;
import com.vmware.bdd.plugin.clouderamgr.spectypes.HadoopRole;
import com.vmware.bdd.plugin.clouderamgr.utils.CmUtils;
import com.vmware.bdd.plugin.clouderamgr.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.AbstractSoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;
import com.vmware.bdd.software.mgmt.plugin.utils.ReflectionUtils;
import com.vmware.bdd.software.mgmt.plugin.utils.SSHUtil;
import com.vmware.bdd.software.mgmt.plugin.utils.ValidateRolesUtil;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Version;

/**
 * Author: Xiaoding Bian
 * Date: 6/11/14
 * Time: 5:57 PM
 */
public class ClouderaManagerImpl extends AbstractSoftwareManager implements SoftwareManager {

   private static final Logger logger = Logger.getLogger(ClouderaManagerImpl.class);

   private final String UNKNOWN_VERSION = "UNKNOWN";
   public final String MIN_SUPPORTED_VERSION = "5.0.0";
   private final String usernameForHosts = "serengeti";
   private final int sshPort = 22;
   private final String  sudoCmd = CommonUtil.getCustomizedSudoCmd();

   private final String stopAgentCmd = sudoCmd + " service cloudera-scm-agent stop";
   private final String privateKeyFile = "/home/serengeti/.ssh/id_rsa";
   private String privateKey;
   private RootResourceV6 apiResourceRootV6;
   private RootResourceV7 apiResourceRootV7;
   private String cmServerHostId;
   private String domain;
   private String cmServerHost;
   private int cmPort;
   private String cmUsername;
   private String cmPassword;

   private final static int INVALID_PROGRESS = -1;

   public RootResourceV6 getApiResourceRootV6() {
      return apiResourceRootV6;
   }

   private enum ProgressSplit {
      INSPECT_HOSTS(10),
      INSTALL_HOSTS_AGENT(40),
      VALIDATE_PARCELS_AVAILABILITY(45),
      DOWNLOAD_PARCEL(60),
      DISTRIBUTE_PARCEL(70),
      ACTIVATE_PARCEL(75),
      CONFIGURE_SERVICES(80),
      START_SERVICES(100),
      STOP_SERVICES(100);
      private int progress;
      private ProgressSplit(int progress) {
         this.progress = progress;
      }

      public int getProgress() {
         return progress;
      }
   }

   public ClouderaManagerImpl() {}

   public ClouderaManagerImpl(String cmServerHost, int port, String user,
         String password, String privateKey) throws ClouderaManagerException {
      this.cmServerHost = cmServerHost;
      this.cmPort = port;
      this.domain = "http://" + cmServerHost + ":" + cmPort;
      this.cmUsername = user;
      this.cmPassword = password;
      ApiRootResource apiRootResource = new ClouderaManagerClientBuilder().withHost(cmServerHost)
            .withPort(port).withUsernamePassword(user, password).build();
      initApiResource(apiRootResource);
      this.privateKey = privateKey;
   }

   public ClouderaManagerImpl(URL url, String user, String password,
         String privateKey) throws ClouderaManagerException {
      this.cmServerHost = url.getHost();
      this.cmPort = url.getPort();
      this.domain = url.getProtocol() + "://" + cmServerHost + ":" + cmPort;
      this.cmUsername = user;
      this.cmPassword = password;
      ApiRootResource apiRootResource =
            new ClouderaManagerClientBuilder().withBaseURL(url)
                  .withUsernamePassword(user, password).build();
      initApiResource(apiRootResource);
      this.privateKey = privateKey;
   }

   private void initApiResource(ApiRootResource apiRootResource) {
      String apiVersion = apiRootResource.getCurrentVersion();
      logger.info("api version: " + apiVersion);
      int apiVersionNum = apiVersion.charAt(1) - '0';
      assert(apiVersionNum >= 6);
      this.apiResourceRootV6 = apiRootResource.getRootV6();
      String cmVersion = getVersion();
      if (cmVersion.equals(UNKNOWN_VERSION)) {
         return;
      }
      logger.info("cm version: " + cmVersion);
      DefaultArtifactVersion cmVersionInfo = new DefaultArtifactVersion(cmVersion);
      assert(cmVersionInfo.getMajorVersion() >= 5);

      if (apiVersionNum >= 7 && isCmSupported(7, cmVersionInfo)) {
         this.apiResourceRootV7 = apiRootResource.getRootV7();
      }
   }

   private boolean isCmSupported(int apiVersion, DefaultArtifactVersion cmVersionInfo) {
      DefaultArtifactVersion sinceVersion = new DefaultArtifactVersion(Constants.API_VERSION_SINCE_OF_CM_VERSION.get(apiVersion));
      if (cmVersionInfo.getMajorVersion() > sinceVersion.getMajorVersion()
            || (cmVersionInfo.getMajorVersion() == sinceVersion.getMajorVersion()
            && cmVersionInfo.getMinorVersion() >= sinceVersion.getMinorVersion())) {
         return true;
      }
      return false;
   }

   @Override
   public boolean validateServerVersion() throws SoftwareManagerCollectorException {
      String cmVersion = getVersion();
      DefaultArtifactVersion cmVersionInfo = new DefaultArtifactVersion(cmVersion);
      DefaultArtifactVersion minSupportedVersionInfo = new DefaultArtifactVersion(MIN_SUPPORTED_VERSION);
      logger.info("Min supported version of " + getType() + " is: " + MIN_SUPPORTED_VERSION);
      logger.info("Version of new software manager is: " + cmVersion);
      if (cmVersion.equals(UNKNOWN_VERSION) || cmVersionInfo.getMajorVersion() < minSupportedVersionInfo.getMajorVersion()) {
         throw SoftwareManagerCollectorException.INVALID_VERSION(Constants.CDH_PLUGIN_NAME, cmVersion);
      }
      return true;
   }

   @Override
   public String getName() {
      return Constants.CDH_PLUGIN_NAME;
   }

   @Override
   public String getDescription() {
      return null;
   }

   @Override
   public String getType() {
      return com.vmware.bdd.utils.Constants.CLOUDERA_MANAGER_PLUGIN_TYPE;
   }

   @Override
   public List<HadoopStack> getSupportedStacks() throws SoftwareManagementPluginException {
      String randomClusterName = UUID.randomUUID().toString();
      final ApiClusterList clusterList = new ApiClusterList();
      ApiCluster apiCluster = new ApiCluster();
      apiCluster.setName(randomClusterName);
      apiCluster.setVersion(ApiClusterVersion.CDH5);
      clusterList.add(apiCluster);
      try {
         List<HadoopStack> hadoopStacks = new ArrayList<HadoopStack>();
         apiResourceRootV6.getClustersResource().createClusters(clusterList);
         for (ApiParcel apiParcel : apiResourceRootV6.getClustersResource().getParcelsResource(randomClusterName)
               .readParcels(DataView.SUMMARY).getParcels()) {
            if (apiParcel.getProduct().equals(Constants.CDH_REPO_PREFIX)) {
               DefaultArtifactVersion parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
               HadoopStack stack = new HadoopStack();
               stack.setDistro(apiParcel.getProduct(), parcelVersion.getMajorVersion() + "."
                     + parcelVersion.getMinorVersion() + "." + parcelVersion.getIncrementalVersion());
               stack.setFullVersion(apiParcel.getVersion());
               stack.setVendor(Constants.CDH_DISTRO_VENDOR);
               List<String> roles = new ArrayList<String>();
               for (String role : AvailableServiceRoleContainer.allRoles(apiParcel.getVersion())) {
                  roles.add(role);
               }
               stack.setHveSupported(true);
               stack.setRoles(roles);
               hadoopStacks.add(stack);
            }
         }
         apiResourceRootV6.getClustersResource().deleteCluster(randomClusterName);
         return hadoopStacks;
      } catch (Exception e) {
         throw SoftwareManagementPluginException.RETRIEVE_SUPPORTED_STACKS_FAIL(e, Constants.CDH_PLUGIN_NAME);
      }
   }

   @Override
   public String getSupportedConfigs(HadoopStack hadoopStack)
         throws SoftwareManagementPluginException {
      try {
         return AvailableServiceRoleContainer.getSupportedConfigs(CmUtils.distroVersionOfHadoopStack(hadoopStack));
      } catch (IOException e) {
         throw ClouderaManagerException.GET_SUPPORT_CONFIGS_EXCEPTION(e);
      }
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint) throws ValidationException {
      return (new CmClusterValidator()).validateBlueprint(blueprint);
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
      boolean success = false;
      CmClusterDef clusterDef = null;
      try {
         clusterDef = new CmClusterDef(blueprint);
         ReflectionUtils.getPreStartServicesHook().preStartServices(blueprint.getName());

         validateBlueprint(blueprint);
         provisionCluster(clusterDef, null, reportQueue);
         provisionParcels(clusterDef, null, reportQueue);
         configureServices(clusterDef, reportQueue, true);
         startServices(clusterDef, reportQueue, true);
         success = true;
         clusterDef.getCurrentReport().setAction("");
         clusterDef.getCurrentReport().setProgress(100);
         clusterDef.getCurrentReport().setSuccess(true);
      } catch (SoftwareManagementPluginException ex) {
         clusterDef.getCurrentReport().setAction("Failed to Create Cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         throw ex;
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to Create Cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         logger.error(e.getMessage());
         throw SoftwareManagementPluginException.CREATE_CLUSTER_EXCEPTION(e, Constants.CDH_PLUGIN_NAME, clusterDef.getName());
      } finally {
         clusterDef.getCurrentReport().setFinished(true);

         if(success) {
            clusterDef.getCurrentReport().setClusterAndNodesServiceStatus(ServiceStatus.STARTED);
         }

         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
      }

      return success;
   }

   @Override
   /**
    * @TODO better use a event-listener mode to decouple the reportQueue. lixl
    */
   public boolean reconfigCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
      boolean success = false;
      CmClusterDef clusterDef = null;
      try {
         clusterDef = new CmClusterDef(blueprint);
         logger.info("Reconfig cluster, blueprint is: " + new Gson().toJson(blueprint, ClusterBlueprint.class));
         ReflectionUtils.getPreStartServicesHook().preStartServices(clusterDef.getName());
         logger.info("Pre start service succeeded");
         syncHostsId(clusterDef);
         logger.info("Sync hosts Id succeed");
         configureServices(clusterDef, reportQueue, false);
         success = true;
         clusterDef.getCurrentReport().setAction("Successfully Reconfigure Cluster");
         clusterDef.getCurrentReport().setProgress(100);
         clusterDef.getCurrentReport().setSuccess(true);
      } catch (SoftwareManagementPluginException ex) {
         clusterDef.getCurrentReport().setAction("Failed to Reconfigure Cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         throw ex;
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to Reconfigure Cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         logger.error(e.getMessage());
         throw SoftwareManagementPluginException.RECONFIGURE_CLUSTER_FAILED(e, Constants.CDH_PLUGIN_NAME, clusterDef.getName());
      } finally {
         clusterDef.getCurrentReport().setFinished(true);
         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
      }
      return success;
   }

   @Override
   public boolean scaleOutCluster(ClusterBlueprint blueprint, List<String> addedNodeNames,
         ClusterReportQueue reportQueue, boolean forceScaleOut) throws SoftwareManagementPluginException {
      boolean success = false;
      CmClusterDef clusterDef = null;
      try {
         clusterDef = new CmClusterDef(blueprint);
         ReflectionUtils.getPreStartServicesHook().preStartServices(blueprint.getName(), forceScaleOut);

         provisionCluster(clusterDef, addedNodeNames, reportQueue, true, forceScaleOut);
         provisionParcels(clusterDef, addedNodeNames, reportQueue);
         Map<String, List<ApiRole>> roles = configureNodeServices(
               clusterDef, reportQueue, addedNodeNames);
         startNodeServices(clusterDef, addedNodeNames, roles, reportQueue);
         success = true;
         clusterDef.getCurrentReport().setProgress(100);
         clusterDef.getCurrentReport().setAction("");
         clusterDef.getCurrentReport().setClusterAndNodesServiceStatus(ServiceStatus.STARTED);
      } catch (SoftwareManagementPluginException ex) {
         if (ex instanceof CommandExecFailException) {
            String hostId = ((CommandExecFailException)ex).getRefHostId();
            CmNodeDef nodeDef = clusterDef.idToHosts().get(hostId);
            String errMsg = null;
            if (nodeDef != null) {
               errMsg = "Failed to start role for node "  + nodeDef.getName() + " for "
                     + ((ex.getMessage() == null) ? "" : (", " + ex.getMessage()));
               // reset all node actions.
               clusterDef.getCurrentReport().setNodesAction("", addedNodeNames);
               clusterDef.getCurrentReport().setNodesStatus(ServiceStatus.STOPPED, addedNodeNames);

               // set error message for specified node
               clusterDef.getCurrentReport().getNodeReports()
                     .get(nodeDef.getName()).setErrMsg(errMsg);
               throw  SoftwareManagementPluginException.START_SERVICE_FAILED(ex, Constants.CDH_PLUGIN_NAME, clusterDef.getName());
            }
         }
         clusterDef.getCurrentReport().setNodesError(ex.getMessage(), addedNodeNames);
         clusterDef.getCurrentReport().setNodesStatus(ServiceStatus.FAILED, addedNodeNames);
         throw ex;
      } catch (Exception e) {
         clusterDef.getCurrentReport().setNodesError(
               "Failed to bootstrap nodes for " + e.getMessage(), addedNodeNames);
         clusterDef.getCurrentReport().setNodesStatus(ServiceStatus.FAILED, addedNodeNames);
         logger.error(e.getMessage());
         throw SoftwareManagementPluginException.SCALE_OUT_CLUSTER_FAILED(e, Constants.CDH_PLUGIN_NAME, clusterDef.getName());
      } finally {
         clusterDef.getCurrentReport().setSuccess(success);
         clusterDef.getCurrentReport().setFinished(true);
         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
      }
      return success;
   }

   private Map<String, List<ApiRole>> configureNodeServices(final CmClusterDef cluster,
         final ClusterReportQueue reportQueue, final List<String> addedNodeNames)
               throws SoftwareManagementPluginException {

      Map<String, String> nodeRefToName = cluster.hostIdToName();
      Map<String, List<CmRoleDef>> serviceRolesMap = new HashMap<String, List<CmRoleDef>>();
      Set<String> addedNodeNameSet = new HashSet<String>();
      addedNodeNameSet.addAll(addedNodeNames);
      for (CmServiceDef serviceDef : cluster.getServices()) {
         List<CmRoleDef> roles = serviceDef.getRoles();
         for (CmRoleDef role : roles) {
            String nodeId = role.getNodeRef();
            String nodeName = nodeRefToName.get(nodeId);
            if (addedNodeNameSet.contains(nodeName)) {
               // new added hosts
               List<CmRoleDef> roleDefs = serviceRolesMap.get(serviceDef.getName());
               if (roleDefs == null) {
                  roleDefs = new ArrayList<CmRoleDef>();
                  serviceRolesMap.put(serviceDef.getName(), roleDefs);
               }
               roleDefs.add(role);
            }
         }
      }

      Map<String, List<ApiRole>> result = new HashMap<>();
      try {
         ApiServiceList apiServiceList = apiResourceRootV6.getClustersResource()
               .getServicesResource(cluster.getName()).readServices(DataView.SUMMARY);
         for (ApiService apiService : apiServiceList.getServices()) {
            if (!serviceRolesMap.containsKey(apiService.getName())) {
               // not touched by this resize, continue
               continue;
            }
            result.put(apiService.getName(), new ArrayList<ApiRole>());
            List<CmRoleDef> roleDefs = serviceRolesMap.get(apiService.getName());
            List<ApiRole> apiRoles =
                  apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName())
                  .getRolesResource(apiService.getName()).readRoles()
                  .getRoles();
            logger.debug("Existing roles " + apiRoles);
            for (ApiRole apiRole : apiRoles) {
               for (Iterator<CmRoleDef> ite = roleDefs.iterator(); ite.hasNext(); ) {
                  CmRoleDef roleDef = ite.next();
                  if (apiRole.getHostRef().getHostId().equals(roleDef.getNodeRef())) {
                     ite.remove();
                     result.get(apiService.getName()).add(apiRole);
                     break;
                  }
               }
            }
            if (!roleDefs.isEmpty()) {
               List<ApiRole> newRoles = new ArrayList<>();
               for (CmRoleDef roleDef : roleDefs) {
                  ApiRole apiRole = createApiRole(roleDef);
                  newRoles.add(apiRole);
               }
               String action = "Configuring service " + apiService.getDisplayName();
               cluster.getCurrentReport().setNodesAction(action, addedNodeNames);
               reportQueue.addClusterReport(cluster.getCurrentReport().clone());

               logger.debug("Creating roles " + newRoles);
               ApiRoleList roleList =
                     apiResourceRootV6.getClustersResource()
                     .getServicesResource(cluster.getName())
                     .getRolesResource(apiService.getName())
                     .createRoles(new ApiRoleList(newRoles));
               result.get(apiService.getName()).addAll(roleList.getRoles());
            }
         }
         logger.info("Finished configure services");

         syncRolesId(cluster);

         preDeployConfig(cluster);

         // deploy client config for new added roles only
         for (String serviceName : result.keySet()) {
            final ApiRoleNameList roleNameList = new ApiRoleNameList();
            final String sName = serviceName;
            List<String> roleNames = new ArrayList<>();
            for (ApiRole apiRole : result.get(serviceName)) {
               roleNames.add(apiRole.getName());
            }
            roleNameList.setRoleNames(roleNames);
            retry(5, new Retriable() {
               @Override
               public void doWork() throws Exception {
                  executeAndReport("Deploying client config", addedNodeNames,
                        apiResourceRootV6.getClustersResource()
                              .getServicesResource(cluster.getName())
                              .deployClientConfigCommand(sName, roleNameList),
                        ProgressSplit.CONFIGURE_SERVICES.getProgress(),
                        cluster.getCurrentReport(), reportQueue, true);
               }
            });
         }

         return result;
      } catch (Exception e) {
         String errMsg = "Failed to configure services" + ((e.getMessage() == null) ? "" : (", " + e.getMessage()));
         logger.error(errMsg);
         throw SoftwareManagementPluginException.CONFIGURE_SERVICE_FAILED(e);
      }
   }

   private ApiRole createApiRole(CmRoleDef role) {
      ApiRole apiRole = new ApiRole();
      apiRole.setType(role.getType().getName());
      apiRole.setHostRef(new ApiHostRef(role.getNodeRef()));
      ApiConfigList roleConfigList = new ApiConfigList();
      if (role.getConfiguration() != null) {
         for (String key : role.getConfiguration().keySet()) {
            roleConfigList.add(new ApiConfig(key, role.getConfiguration().get(key)));
         }
      }
      apiRole.setConfig(roleConfigList);
      return apiRole;
   }

   @Override
   public boolean deleteCluster(ClusterBlueprint clusterBlueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      String clusterName = clusterBlueprint.getName();
      try {
         if (!echo()) {
            logWarningWhenForceDeleteCluster(clusterName);
            return true;
         }
         if (!isProvisioned(clusterName)) {
            return true;
         }

         ApiHostRefList hosts = apiResourceRootV6.getClustersResource().listHosts(clusterName);

         apiResourceRootV6.getClustersResource().deleteCluster(clusterName);

         for (ApiHostRef host : hosts.getHosts()) {
            apiResourceRootV6.getHostsResource().deleteHost(host.getHostId());
         }

      } catch (Exception e) {
         throw SoftwareManagementPluginException.DELETE_CLUSTER_FAILED(e, Constants.CDH_PLUGIN_NAME, clusterName);
      }
      return true;
   }

   @Override
   public boolean onStopCluster(ClusterBlueprint clusterBlueprint,
         ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
      return stopServices(clusterBlueprint, reportQueue);
   }

   private void logWarningWhenForceDeleteCluster(String clusterName) {
      logger.warn("Cloudera manager server was unavailable when deleting cluster " + clusterName + ". Will delete VMs forcely.");
      logger.warn("You may need to delete cluster resource on cloudera manager server manually.");
   }

   @Override
   public boolean onDeleteCluster(ClusterBlueprint clusterBlueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      if (!echo()) {
         logWarningWhenForceDeleteCluster(clusterBlueprint.getName());
         return true;
      }
      // just stop this cluster
      return onStopCluster(clusterBlueprint, reports);
   }

   @Override
   public boolean onDeleteNodes(ClusterBlueprint blueprint, List<String> nodeNames) throws SoftwareManagementPluginException {
      CmClusterDef clusterDef = null;
      try {
         clusterDef = new CmClusterDef(blueprint);
         syncHostsId(clusterDef);
         List<ApiHost> hosts = new ArrayList<>();
         for (CmNodeDef nodeDef : clusterDef.getNodes()) {
            if (nodeNames.contains(nodeDef.getName())
                  && !nodeDef.getName().equals(nodeDef.getNodeId())) {
               try {
                  ApiHost host = apiResourceRootV6.getHostsResource().readHost(nodeDef.getNodeId());
                  if (host != null) {
                     hosts.add(host);
                  }
               } catch (NotFoundException e) {
                  logger.debug("Host " + nodeDef.getNodeId() + " is not found from Cloudera Manager.");
                  continue;
               }
            }
         }
         removeHosts(clusterDef, hosts);
         clusterDef.getCurrentReport().setProgress(100);
         clusterDef.getCurrentReport().setAction("");
      } catch (Exception e) {
         logger.error("Failed to remove hosts " + nodeNames, e);
      }
      return true;
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public void decommissionNode(ClusterBlueprint blueprint, String nodeGroupName, String nodeName, ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      //decommissionNode host
      CmClusterDef clusterDef = null;
      List<String> hostNames = new ArrayList<String>();
      CmNodeDef nodeDef;
      ApiHost apiHost;
      try {
         clusterDef = new CmClusterDef(blueprint);
         syncHostsId(clusterDef);
         nodeDef = null;
         for (CmNodeDef nodeDefIter: clusterDef.getNodes()) {
            if (nodeDefIter.getName().equals(nodeName)) {
               nodeDef = nodeDefIter;
            }
         }
         assert (nodeDef != null);
         apiHost = apiResourceRootV6.getHostsResource().readHost(nodeDef.getNodeId());
         if (apiHost == null) {
            return;
         }
         String hostName = apiHost.getHostname();
         hostNames.add(hostName);
         ClusterReport report = clusterDef.getCurrentReport();
         executeAndReport("Decommission node",
               apiResourceRootV6.getClouderaManagerResource().hostsDecommissionCommand(new ApiHostNameList(hostNames)), ProgressSplit.STOP_SERVICES.getProgress(),
               report,
               reportQueue,
               true);
         logger.info("Decommission node " + hostName + " successed");
      } catch (Exception e) {
         logger.error("Failed to decommissionNode node " + nodeName, e);
         throw SoftwareManagementPluginException.DECOMISSION_FAILED(clusterDef.getName(), nodeGroupName, nodeName, e.getMessage());
      }

      //stop agent on that vm
      SSHUtil sshUtil = new SSHUtil();
      String hostIP = nodeDef.getIpAddress();
      boolean stopAgentSucceed = false;
      String errMsg = null;
      try {
         stopAgentSucceed = sshUtil.execCmd(usernameForHosts, privateKeyFile, hostIP, sshPort, stopAgentCmd, null, null);
      } catch (Exception e) {
         logger.error("Got exception when stop agent on " + hostIP, e);
         errMsg = e.getMessage();
      } finally {
         if (!stopAgentSucceed) {
            logger.error("Stop agent failed");
            throw SoftwareManagementPluginException.STOP_AGENT_FAILED("ClouderaManager", nodeName, errMsg);
         } else {
            logger.info("Stop agent succeed");
         }
      }

      //delete host from the cluster
      logger.info("Start to remove host " + nodeName + " " + hostIP + " " + nodeDef.getNodeId());
      try {
         List<ApiHost> hosts = new ArrayList<>();
         hosts.add(apiHost);
         removeHosts(clusterDef, hosts);
      } catch (Exception e) {
         logger.error( "Faield to delete host " + nodeName, e);
         throw SoftwareManagementPluginException.DELETE_HOST_FAILED(clusterDef.getName(), nodeGroupName, nodeName, e);
      }
   }

   @Override
   public boolean recomissionNode(String clusterName, NodeInfo node, ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public boolean comissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public boolean startNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public boolean stopNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public String exportBlueprint(String clusterName) {
      return null;
   }

   @Override
   public ClusterReport queryClusterStatus(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException {
      if (blueprint == null) {
         logger.info("Empty blueprint is passed to query cluster status. Return null.");
         return null;
      }
      try {
         CmClusterDef cluster = new CmClusterDef(blueprint);
         syncHostsId(cluster);
         boolean allStarted = true;
         boolean allStopped = true;
         for (ApiService apiService : apiResourceRootV6.getClustersResource()
               .getServicesResource(cluster.getName()).readServices(DataView.SUMMARY)) {
            ApiServiceState serviceState = apiService.getServiceState();
            if (!ApiServiceState.STARTED.equals(serviceState)) {
               allStarted = false;
            } else {
               allStopped = false;
            }
         }
         if (allStopped) {
            logger.debug("Cluster " + blueprint.getName() + " services are stopped.");
            cluster.getCurrentReport().setStatus(ServiceStatus.STOPPED);
         } else if (allStarted) {
            ApiHealthSummary summary = getExistingServiceHealthStatus(cluster.getName());
            switch (summary) {
            case GOOD:
               cluster.getCurrentReport().setStatus(ServiceStatus.STARTED);
               logger.debug("Cluster " + blueprint.getName() + " is healthy.");
               break;
            case BAD:
            case CONCERNING:
               cluster.getCurrentReport().setStatus(ServiceStatus.ALERT);
               logger.debug("Cluster " + blueprint.getName() + " is concerning.");
               break;
            default:
               cluster.getCurrentReport().setStatus(ServiceStatus.STARTED);
               logger.debug("Cluster " + blueprint.getName() + " healthy is unavailable.");
               break;
            }
         } else {
            cluster.getCurrentReport().setStatus(ServiceStatus.ALERT);
            logger.debug("Cluster " + blueprint.getName() + " services are not all started yet.");
         }
         queryNodesStatus(cluster);
         return cluster.getCurrentReport().clone();
      } catch (Exception e) {
         throw SoftwareManagementPluginException.QUERY_CLUSTER_STATUS_FAILED(
               blueprint.getName(), e);
      }
   }

   private ApiHealthSummary getExistingServiceHealthStatus(String clusterName) {
      ApiHealthSummary summary = ApiHealthSummary.DISABLED;
      for (ApiService apiService : apiResourceRootV6.getClustersResource()
            .getServicesResource(clusterName).readServices(DataView.SUMMARY)) {
         ApiHealthSummary health = apiService.getHealthSummary();
         logger.debug("Cluster " + clusterName + " Service "
         + apiService.getType() + " status is " + health);
         if (health.ordinal() > summary.ordinal()) {
            summary = health;
         }
      }
      logger.debug("Cluster " + clusterName + " Service status is " + summary);
      return summary;
   }

   private boolean isExistingServiceStarted(String clusterName) {
      boolean started = true;
      for (ApiService apiService : apiResourceRootV6.getClustersResource()
            .getServicesResource(clusterName).readServices(DataView.SUMMARY)) {
         ApiServiceState serviceState = apiService.getServiceState();
         if (!ApiServiceState.STARTED.equals(serviceState)) {
            started = false;
            break;
         }
      }
      return started;
   }

   private void queryNodesStatus(CmClusterDef cluster) {
      for (CmNodeDef node : cluster.getNodes()) {
         Map<String, NodeReport> nodeReports = cluster.getCurrentReport().getNodeReports();
         NodeReport nodeReport = nodeReports.get(node.getName());
         try {
            ApiHost host = apiResourceRootV6.getHostsResource().readHost(node.getNodeId());
            ApiHealthSummary health = host.getHealthSummary();
            switch(health) {
            case GOOD:
               List<ApiRoleRef> roleRefs = host.getRoleRefs();
               boolean hasStarted = false;
               boolean hasStopped = false;
               for (ApiRoleRef roleRef : roleRefs) {
                  if (isRoleStarted(roleRef.getClusterName(),
                        roleRef.getServiceName(), roleRef.getRoleName())) {
                     hasStarted = true;
                  } else {
                     hasStopped = true;
                  }
               }
               if (hasStopped && !hasStarted) {
                  nodeReport.setStatus(ServiceStatus.STOPPED);
               } else if (hasStopped && hasStarted) {
                  nodeReport.setStatus(ServiceStatus.ALERT);
               } else if (!hasStopped && hasStarted){
                  nodeReport.setStatus(ServiceStatus.STARTED);
               } else {
                  nodeReport.setStatus(ServiceStatus.STOPPED);
               }
               logger.debug("Node " + nodeReport.getName() + " is good.");
               break;
            case CONCERNING:
               nodeReport.setStatus(ServiceStatus.UNHEALTHY);
               logger.debug("Node " + nodeReport.getName() + " is concerning.");
               break;
            case BAD:
               logger.debug("Node " + nodeReport.getName() + " is not running well.");
               nodeReport.setStatus(ServiceStatus.ALERT);
               break;
            default:
               logger.debug("Node " + nodeReport.getName() + " is unknown.");
               nodeReport.setStatus(ServiceStatus.UNKONWN);
               break;
            }
         } catch (NotFoundException e) {
            logger.debug("Node " + node.getName() + " is not found in Cloudera Manager.");
            nodeReport.setStatus(ServiceStatus.FAILED);
         }
      }
   }

   @Override
   public boolean echo() {
      try {
         String message = "Hello";
         return apiResourceRootV6.getToolsResource().echo(message).getMessage().equals(message);
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public HealthStatus getStatus() {
      return HealthStatus.Connected;
   }

   private boolean isProvisioned(String clusterName) throws ClouderaManagerException {
      for (ApiCluster apiCluster : apiResourceRootV6.getClustersResource().readClusters(DataView.SUMMARY)) {
         if (apiCluster.getName().equals(clusterName)) {
            return true;
         }
      }
      return false;
   }

   private boolean isStarted(CmClusterDef cluster) throws ClouderaManagerException {
      final Set<String> servicesNotStarted = new HashSet<String>();
      try {

         if (!isConfigured(cluster)) {
            return false;
         }

         for (CmServiceDef serviceDef : cluster.getServices()) {
            servicesNotStarted.add(serviceDef.getName());
         }

         for (ApiService apiService : apiResourceRootV6.getClustersResource()
               .getServicesResource(cluster.getName())
               .readServices(DataView.SUMMARY)) {
            ApiServiceState serviceState = apiService.getServiceState();
            if (serviceState == ApiServiceState.STARTED) {
               servicesNotStarted.remove(apiService.getName());
            }
         }

      } catch (Exception e) {
         throw SoftwareManagementPluginException.START_CLUSTER_FAILED(e, Constants.CDH_PLUGIN_NAME, cluster.getName());
      }
      return servicesNotStarted.isEmpty();
   }

   private boolean needStop(String clusterName) {
      if (!isProvisioned(clusterName)) {
         return false;
      }

      for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(clusterName).readServices(DataView.SUMMARY)) {
         if (apiService.getServiceState().equals(ApiServiceState.STARTED) || apiService.getServiceState().equals(ApiServiceState.STARTING)) {
            return true;
         }
      }

      return false;
   }

   // When stop command failed to execute, we should not ignore it, because that will
   // make the cloudera manager in inconsistent state with bde. So when we fail, we throw exception
   private boolean stopServices(ClusterBlueprint clusterBlueprint, ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
      assert(clusterBlueprint != null && clusterBlueprint.getName() != null && !clusterBlueprint.getName().isEmpty());
      String clusterName = clusterBlueprint.getName();
      CmClusterDef clusterDef = null;
      ClusterReport report = null;
      boolean succeed = false;
      try {
         if (!isProvisioned(clusterName)) {
            return true;
         }
         clusterDef = new CmClusterDef(clusterBlueprint);
         report = clusterDef.getCurrentReport();
         if (isStopped(clusterName) || !needStop(clusterName)) {
            succeed = true;
            return true;
         }
         executeAndReport("Stopping Services",
               apiResourceRootV6.getClustersResource().stopCommand(clusterName),
               ProgressSplit.STOP_SERVICES.getProgress(),
               report,
               reportQueue,
               true);
         succeed = true;
         return true;
      } catch (Exception e) {
         logger.error("Got an exception when cloudera manager stopping services", e);
         report.setClusterAndNodesServiceStatus(ServiceStatus.STOP_FAILED);
         HashMap<String, Set<String>> unstoppedRoles = getFailedRoles(clusterName, ApiRoleState.STOPPED);
         setRolesErrorMsg(report, unstoppedRoles, "stopping");
         throw SoftwareManagementPluginException.STOP_CLUSTER_EXCEPTION(e, Constants.CDH_PLUGIN_NAME, clusterBlueprint.getName());
      } finally {
         if (clusterDef != null) {
            if (succeed) {
               report.setClusterAndNodesServiceStatus(ServiceStatus.STOPPED);
               report.setProgress(ProgressSplit.STOP_SERVICES.getProgress());
               logger.info("Cloudera Manager stopped all services successfully.");
            }
            report.setClusterAndNodesAction("");
            report.setFinished(true);
            report.setSuccess(succeed);
            reportQueue.addClusterReport(report.clone());
         }
      }
   }

   /*
    * get roles that is not in the expected state
    */
   private HashMap<String, Set<String>> getFailedRoles(String clusterName, ApiRoleState roleState) {
      HashMap<String, Set<String>> failedRoles = null;
      ApiServiceList serviceList = apiResourceRootV6.getClustersResource().getServicesResource(clusterName).readServices(
            DataView.FULL);
      if (serviceList != null && serviceList.getServices() != null && !serviceList.getServices().isEmpty()) {
         for (ApiService service : serviceList.getServices()) {
            for (ApiRole role : apiResourceRootV6.getClustersResource().getServicesResource(clusterName)
                  .getRolesResource(service.getName()).readRoles()) {
               logger.info("role " + role.getName() + " is in state " + role.getRoleState());
               if (!roleState.equals(role.getRoleState())) {
                  if (failedRoles == null) {
                     failedRoles = new HashMap<>();
                  }
                  String hostId = role.getHostRef().getHostId();
                  if (!failedRoles.containsKey(hostId)) {
                     failedRoles.put(hostId, new HashSet<String>());
                  }
                  String roleDisplayName = getRoleDisplayName(role.getType());
                  if (roleDisplayName == null) {
                     roleDisplayName = "UNKNOWN_ROLE";
                  }
                  failedRoles.get(hostId).add(roleDisplayName);
               }
            }
         }
      }
      if(failedRoles != null) {
         logger.info("Roles not in " + roleState + " state are: " + failedRoles.toString());
      }
      return failedRoles;
   }

   private String getRoleDisplayName(String type) {
      try {
         Map<String, String> nameMap = AvailableServiceRoleContainer.nameToDisplayName();
         if (nameMap == null || nameMap.isEmpty()) {
            return null;
         }
         return nameMap.get(type);
      } catch (Exception e) {
         return null;
      }
   }

   public boolean startCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
      return startCluster(clusterBlueprint, reportQueue, false);
   }

   @Override
   public boolean startCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reportQueue, boolean forceStart) throws SoftwareManagementPluginException {
      assert(clusterBlueprint != null && clusterBlueprint.getName() != null && !clusterBlueprint.getName().isEmpty());
      String clusterName = clusterBlueprint.getName();
      CmClusterDef clusterDef = null;
      ClusterReport report = null;
      boolean succeed = false;
      try {
         if (!isProvisioned(clusterName)) {
            return true;
         }
         clusterDef = new CmClusterDef(clusterBlueprint);
         report = clusterDef.getCurrentReport();
         if (isExistingServiceStarted(clusterName)) {
            succeed = true;
            return true;
         }
         ReflectionUtils.getPreStartServicesHook().preStartServices(clusterDef.getName());
         executeAndReport("Starting Services",
                           apiResourceRootV6.getClustersResource().startCommand(clusterName),
                           ProgressSplit.START_SERVICES.getProgress(),
                           report,
                           reportQueue,
                           true);
         succeed = true;
         return true;
      } catch (Exception e) {
         report.setClusterAndNodesServiceStatus(ServiceStatus.STARTUP_FAILED);
         logger.error("Got an exception when cloudera manager starting cluster", e);
         HashMap<String, Set<String>> unstartedRoles = getFailedRoles(clusterName, ApiRoleState.STARTED);
         setRolesErrorMsg(report, unstartedRoles, "starting");
         throw SoftwareManagementPluginException.START_CLUSTER_FAILED(e, Constants.CDH_PLUGIN_NAME, clusterDef.getName());
      } finally {
         if (clusterDef != null) {
            report.setFinished(true);
            if (succeed) {
               report.setProgress(ProgressSplit.START_SERVICES.getProgress());
               report.setClusterAndNodesServiceStatus(ServiceStatus.STARTED);
               logger.info("Cloudera Manager started all services successfully.");
            }
            report.setClusterAndNodesAction("");//clean action field
            report.setFinished(true);
            report.setSuccess(succeed);
            reportQueue.addClusterReport(report.clone());
         }
      }
   }

   private void setRolesErrorMsg(ClusterReport report, HashMap<String, Set<String>> failedRoles, String action) {
      if (failedRoles == null || failedRoles.isEmpty()) {
         return;
      }
      Map<String, NodeReport> nodeReports = report.getNodeReports();
      for (String hostId: failedRoles.keySet()) {
         String ip = hostId2IP(hostId);
         for (String nodeReportKey: nodeReports.keySet()) {
            NodeReport nodeReport = nodeReports.get(nodeReportKey);
            if (nodeReport.getIpAddress().equals(ip)) {
               logger.info("added " + failedRoles.get(hostId).toString() + " to " + nodeReport.getIpAddress());
               nodeReport.setUseClusterMsg(false);
               nodeReport.setErrMsg("Failed to " + action + " roles " + failedRoles.get(hostId));
            }
         }
      }
      failedRoles.clear();
   }

   private boolean isStopped(String clusterName) throws ClouderaManagerException {
      if (!isProvisioned(clusterName)) {
         return false;
      }

      for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(clusterName).readServices(DataView.SUMMARY)) {
         if (apiService.getServiceState() != null && !apiService.getServiceState().equals(ApiServiceState.STOPPED)) {
            return false;
         }
      }
      return true;
   }

   private boolean isConfigured(CmClusterDef cluster) throws ClouderaManagerException {
      boolean executed = false;
      final Set<String> servicesNotConfigured = new HashSet<String>();
      try {
         if (isProvisioned(cluster.getName())) {
            for (CmServiceDef serviceDef : cluster.getServices()) {
               servicesNotConfigured.add(serviceDef.getName());
            }

            for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .readServices(DataView.SUMMARY)) {
               ApiConfigStalenessStatus stale = apiService.getConfigStalenessStatus();
               if (stale == ApiConfigStalenessStatus.FRESH) {
                  servicesNotConfigured.remove(apiService.getName());
               }
            }
            executed = true;
         }
      } catch (Exception e) {
         throw ClouderaManagerException.CHECK_CONFIGURED_EXCEPTION(e, cluster.getName());
      }
      return executed && servicesNotConfigured.size() == 0;
   }

   /**
    * install hosts agent, Reentrant
    * @param cluster
    * @param reportQueue
    * @throws Exception
    */
   private void installHosts(final CmClusterDef cluster, final List<String> addedNodes,
         final ClusterReportQueue reportQueue, boolean force) throws Exception {
      logger.info("Installing agent for each node of cluster: " + cluster.getName());
      List<String> ips = new ArrayList<String>();
      List<String> hostnames = new ArrayList<String>();

      // for resize
      Set<String> existIps = new HashSet<String>();
      for (ApiHost apiHost : apiResourceRootV6.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
         existIps.add(apiHost.getIpAddress());
      }

      for (CmNodeDef node : cluster.getNodes()) {
         if (existIps.contains(node.getIpAddress())) { // TODO: check health status, delete it if not available
            continue;
         }
         hostnames.add(node.getFqdn());
         ips.add(node.getIpAddress());
      }

      if (hostnames.size() != 0) {
         ApiHostInstallArguments apiHostInstallArguments = new ApiHostInstallArguments();
         apiHostInstallArguments.setHostNames(hostnames);
         apiHostInstallArguments.setSshPort(22);
         apiHostInstallArguments.setUserName(usernameForHosts);
         apiHostInstallArguments.setPrivateKey(privateKey);
         apiHostInstallArguments.setParallelInstallCount(20);

         // Install CM agents. TODO: show steps msg
         StatusPoller hostInstallPoller = null;
         try {

            final ApiCommand cmd =  apiResourceRootV6.getClouderaManagerResource().hostInstallCommand(apiHostInstallArguments);
            logger.info("install command id: " + cmd.getId());

            hostInstallPoller = new HostInstallPoller(apiResourceRootV6, cmd.getId(), cluster.getCurrentReport(), reportQueue,
                  ProgressSplit.INSTALL_HOSTS_AGENT.getProgress(), addedNodes, domain, cmUsername, cmPassword);
            //when force enabled, we will not check return value and ignore partial failures
            boolean checkReturn = !force;
            if (force) {
               logger.warn("force cluster operation, will ignore failures");
            }
            executeAndReport("Installing Host Agents", addedNodes, cmd, ProgressSplit.INSTALL_HOSTS_AGENT.getProgress(),
                  cluster.getCurrentReport(), reportQueue, hostInstallPoller, checkReturn);
         } catch (Exception e) {
            logger.info(e.getMessage());

            Set<String> installedIPs = new HashSet<String>();
            for (ApiHost apiHost : apiResourceRootV6.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
               installedIPs.add(apiHost.getIpAddress());
            }
            List<String> failedIps = new ArrayList<String>();
            for (String ip : ips) {
               if (!installedIPs.contains(ip)) {
                  failedIps.add(ip);
               }
            }

            String errMsg = "Failed to install agents on nodes: " + failedIps.toString()
                  + ((e.getMessage() == null) ? "" : (", " + e.getMessage()));
            logger.error(errMsg);
            throw ClouderaManagerException.INSTALL_AGENTS_FAIL(e);
         }
      } else {
         cluster.getCurrentReport().setProgress(ProgressSplit.INSTALL_HOSTS_AGENT.getProgress());
         reportQueue.addClusterReport(cluster.getCurrentReport().clone());
      }
   }

   private void syncHostsId(final CmClusterDef clusterDef) {
      Map<String, CmNodeDef> ipToNode = clusterDef.ipToNode();
      Map<String, List<CmRoleDef>> ipToRoles = clusterDef.ipToRoles();
      for (ApiHost apiHost : apiResourceRootV6.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
         if (ipToNode.containsKey(apiHost.getIpAddress())) {
            ipToNode.get(apiHost.getIpAddress()).setNodeId(apiHost.getHostId());
         }
         if (ipToRoles.containsKey(apiHost.getIpAddress())) {
            for (CmRoleDef role : ipToRoles.get(apiHost.getIpAddress())) {
               role.setNodeRef(apiHost.getHostId());
            }
         }
      }
   }

   /**
    * assume host IDs are already synced
    * @param clusterDef
    */
   private void updateRackId(final CmClusterDef clusterDef, List<String> addedNodes) {
      List<CmNodeDef> nodesToUpdateId = new ArrayList<CmNodeDef>();
      for (CmNodeDef node : clusterDef.getNodes()) {
         //for cluster create, we need to update all nodes, but for cluster resize, we only need to update
         // new addedNodes
         if (addedNodes == null) {
            nodesToUpdateId.add(node);
         } else if (addedNodes.contains(node.getName())) {
            nodesToUpdateId.add(node);
         }
      }
      for (CmNodeDef node: nodesToUpdateId) {
         logger.info("Updating RackId for node " + node.getName());
         ApiHost host = apiResourceRootV6.getHostsResource().readHost(node.getNodeId());
         host.setRackId(node.getRackId());
         apiResourceRootV6.getHostsResource().updateHost(host.getHostId(), host);
      }
   }

   /**
    * sync roles' name by role type and hostRef
    * @param clusterDef
    */
   private void syncRolesId(final CmClusterDef clusterDef) {
      Map<String, List<CmRoleDef>> nodeRefToRoles = clusterDef.nodeRefToRoles();
      for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(clusterDef.getName())
            .readServices(DataView.SUMMARY)) {
         for (ApiRole apiRole : apiResourceRootV6.getClustersResource().getServicesResource(clusterDef.getName())
               .getRolesResource(apiService.getName()).readRoles()) {
            List<CmRoleDef> roleDefs = nodeRefToRoles.get(apiRole.getHostRef().getHostId());
            if (roleDefs != null) {
               for (CmRoleDef roleDef : roleDefs) {
                  if (apiRole.getType().equalsIgnoreCase(roleDef.getType().getName())) {
                     roleDef.setName(apiRole.getName());
                  }
               }
            }
         }
      }
   }

   private void provisionCluster(final CmClusterDef cluster, final List<String> addedNodes,
         final ClusterReportQueue reportQueue) throws Exception {
      provisionCluster(cluster, addedNodes, reportQueue, false);
   }

   /**
    * Reentrant
    * @param cluster
    * @param reportQueue
    * @throws Exception
    */
   private void provisionCluster(final CmClusterDef cluster, final List<String> addedNodes,
         final ClusterReportQueue reportQueue, boolean removeBadHosts) throws Exception {
      provisionCluster(cluster, addedNodes, reportQueue, removeBadHosts, false);
   }

   private void provisionCluster(final CmClusterDef cluster, final List<String> addedNodes,
         final ClusterReportQueue reportQueue, boolean removeBadHosts, final boolean force) throws Exception {

      if (!isProvisioned(cluster.getName())) {
         executeAndReport("Inspecting Hosts", addedNodes, apiResourceRootV6.getClouderaManagerResource().inspectHostsCommand(),
               ProgressSplit.INSPECT_HOSTS.getProgress(), cluster.getCurrentReport(), reportQueue, false);

         final ApiClusterList clusterList = new ApiClusterList();
         ApiCluster apiCluster = new ApiCluster();
         apiCluster.setName(cluster.getName());
         apiCluster.setVersion(ApiClusterVersion.valueOf(cluster.getVersion()));
         apiCluster.setFullVersion(cluster.getFullVersion());
         clusterList.add(apiCluster);

         apiResourceRootV6.getClustersResource().createClusters(clusterList);
      } else {
         /*
         For cluster resume/resize, the cluster is already exist, we need to check if this cluster is created by BDE.
         So far, just check if all IPs exist in Cloudera Cluster are included in given blueprint
          */
         Set<String> ips = new HashSet<String>();
         for (CmNodeDef node : cluster.getNodes()) {
            ips.add(node.getIpAddress());
         }
         List<ApiHost> hosts = new ArrayList<>();
         for (ApiHostRef hostRef : apiResourceRootV6.getClustersResource().listHosts(cluster.getName())) {
            ApiHost host = apiResourceRootV6.getHostsResource().readHost(hostRef.getHostId());
            if (!ips.contains(host.getIpAddress())) {
               if (host.getHealthSummary().equals(ApiHealthSummary.BAD) ||
                     host.getHealthSummary().equals(ApiHealthSummary.NOT_AVAILABLE)) {
                  hosts.add(host);
                  logger.info("Host " + host.getHostname() + " should be removed for it's not in cluster " + cluster.getName()
                        + " and in " + host.getHealthSummary() + " status");
                  continue;
               }
               throw SoftwareManagementPluginException.CLUSTER_ALREADY_EXIST(cluster.getName());
            }
         }

         try {
            removeHosts(cluster, hosts);
         } catch (Exception e) {
            logger.error("Failed to remove bad hosts " + hosts, e);
         }
      }

      retry(2, new Retriable() {
         @Override
         public void doWork() throws Exception {
            installHosts(cluster, addedNodes, reportQueue, force);
         }
      });

      syncHostsId(cluster);
      updateRackId(cluster, addedNodes);
      logger.debug("cluster spec after synced hosts Id: " + new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .create().toJson(cluster));

      Set<ApiHostRef> toAddHosts = new HashSet<ApiHostRef>();
      for (CmNodeDef node : cluster.getNodes()) {
         if (addedNodes == null) {
            //cluster creation
            toAddHosts.add(new ApiHostRef(node.getNodeId()));
         } else if (addedNodes.contains(node.getName())) {
            //cluster resize
            toAddHosts.add(new ApiHostRef(node.getNodeId()));
         }
      }

      for (ApiHostRef hostRef : apiResourceRootV6.getClustersResource().listHosts(cluster.getName())) {
         if (toAddHosts.contains(hostRef)) {
            toAddHosts.remove(hostRef);
         }
      }

      if (!toAddHosts.isEmpty()) {
         logger.info("apiHosts to add: " + toAddHosts.toString());

         // Add hosts to this cluster
         apiResourceRootV6.getClustersResource().addHosts(cluster.getName(), new ApiHostRefList(new ArrayList<ApiHostRef>(toAddHosts)));
      }
   }

   private void removeHosts(final CmClusterDef cluster, List<ApiHost> hosts) throws Exception {
      for (ApiHost apiHost : hosts) {
         List<ApiRoleRef> apiRoles = apiHost.getRoleRefs();
         for (ApiRoleRef apiRoleRef : apiRoles) {
            logger.debug("Start to remove role " + apiRoleRef.getRoleName() + " from host " + apiHost.getHostname());
            apiResourceRootV6.getClustersResource()
                  .getServicesResource(apiRoleRef.getClusterName())
                  .getRolesResource(apiRoleRef.getServiceName())
                  .deleteRole(apiRoleRef.getRoleName());
         }
         logger.debug("Start to remove host " + apiHost.getHostId());
         apiResourceRootV6.getHostsResource().deleteHost(apiHost.getHostId());
      }
   }

   /** provision parcels, Reentrant
    * A Parcel encapsulate a specific product and version. For example, (CDH 4.1).
    * A parcel is downloaded, distributed to all the machines of a cluster and then allowed to be activated.
    *
    * @param cluster
    * @param reportQueue
    * @throws Exception
    */
   private void provisionParcels(final CmClusterDef cluster, final List<String> addedNodes,
         final ClusterReportQueue reportQueue) throws Exception {

      if (isConfigured(cluster)) {
         return;
      }

      apiResourceRootV6.getClouderaManagerResource().updateConfig(
            new ApiConfigList(Arrays.asList(new ApiConfig[]{new ApiConfig("PARCEL_UPDATE_FREQ", "1")})));

      final Set<String> repositoriesRequired = new HashSet<String>();

      for (CmServiceDef serviceDef : cluster.getServices()) {
         repositoriesRequired.add(serviceDef.getType().getRepository().toString(cluster.getVersion()));
      }

      logger.info("parcel repo required: " + repositoriesRequired + " cluster: " + cluster.getName());

      final List<String> repositoriesRequiredOrdered = new ArrayList<String>();
      for (String repository : repositoriesRequired) {
         if (repository.equals(Constants.CDH_REPO_PREFIX)) {
            repositoriesRequiredOrdered.add(0, repository);
         } else {
            repositoriesRequiredOrdered.add(repository);
         }
      }

      // validate this cluster has access to all Parcels it requires
      executeAndReport("Validating parcels availability", addedNodes, null, ProgressSplit.VALIDATE_PARCELS_AVAILABILITY.getProgress(),
            cluster.getCurrentReport(), reportQueue, new StatusPoller() {
         @Override
         public boolean poll() {
            for (ApiParcel parcel : apiResourceRootV6.getClustersResource().getParcelsResource(cluster.getName())
                  .readParcels(DataView.FULL).getParcels()) {
               try {
                  repositoriesRequired.remove(parcel.getProduct());
               } catch (IllegalArgumentException e) {
                  // ignore
               }
            }
            // TODO: if one required parcel is not available, will run forever, need timeout/validation
            return repositoriesRequired.isEmpty();
         }
      }, false);

      apiResourceRootV6.getClouderaManagerResource().updateConfig(
            new ApiConfigList(Arrays.asList(new ApiConfig[]{new ApiConfig("PARCEL_UPDATE_FREQ", "60")})));

      DefaultArtifactVersion expectVersion = null;
      if (cluster.getFullVersion() != null) {
         expectVersion = new DefaultArtifactVersion(cluster.getFullVersion());
      }

      for (String repository : repositoriesRequiredOrdered) {
         DefaultArtifactVersion parcelVersion = null;
         for (ApiParcel apiParcel : apiResourceRootV6.getClustersResource().getParcelsResource(cluster.getName())
               .readParcels(DataView.FULL).getParcels()) {
            DefaultArtifactVersion parcelVersionTmp = new DefaultArtifactVersion(apiParcel.getVersion());
            if (apiParcel.getProduct().equals(repository)) {
               if (apiParcel.getProduct().equals(Constants.CDH_REPO_PREFIX)) {
                  /*
                   * Policy for "CDH" parcel:
                   * 1) If specify fullVersion, try to find that parcel, if cannot, select the latest parcel(with highest version).
                   * 2) If fullVersion not specified, select the latest parcel
                   */
                  if (parcelVersion == null || parcelVersion.compareTo(parcelVersionTmp) < 0) {
                     parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
                  }
                  if (expectVersion != null && parcelVersionTmp.getMajorVersion() == expectVersion.getMajorVersion()
                        && parcelVersionTmp.getMinorVersion() == expectVersion.getMinorVersion()
                        && parcelVersionTmp.getIncrementalVersion() == expectVersion.getIncrementalVersion()) {
                     parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
                     break;
                  }
               }

               if (!apiParcel.getProduct().equals(Constants.CDH_REPO_PREFIX)) {
                  // For non-CDH parcel, just select the latest one
                  if (parcelVersion == null || parcelVersion.compareTo(parcelVersionTmp) < 0) {
                     parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
                  }
               }
            }
         }

         final ParcelResource apiParcelResource = apiResourceRootV6.getClustersResource()
               .getParcelsResource(cluster.getName()).getParcelResource(repository, parcelVersion.toString());
         String refMsg = referCmfUrlMsg(domain + "/cmf/parcel/status");
         if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DOWNLOADED.ordinal()) {
            String action = "Downloading parcel...";

            ParcelProvisionPoller poll = new ParcelProvisionPoller(apiParcelResource, AvailableParcelStage.DOWNLOADED, cluster.getCurrentReport(),
                  reportQueue, ProgressSplit.DOWNLOAD_PARCEL.getProgress());

            if (apiParcelResource.readParcel().getStage().equals(AvailableParcelStage.DOWNLOADING.toString())) {
               // Another thread is downloading this parcel, just wait for its completion
               executeAndReport(action, null, ProgressSplit.DOWNLOAD_PARCEL.getProgress(),
                     cluster.getCurrentReport(), reportQueue, poll, false);
            } else {
               // the ApiCommand instance for parcel is inaccessible, so do not check the return value
               executeAndReport(action, apiParcelResource.startDownloadCommand(),
                     ProgressSplit.DOWNLOAD_PARCEL.getProgress(),
                     cluster.getCurrentReport(), reportQueue, poll, false);
            }
            if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DOWNLOADED.ordinal()) {
               throw ClouderaManagerException.DOWNLOAD_PARCEL_FAIL(apiParcelResource.readParcel().getProduct(),
                     apiParcelResource.readParcel().getVersion(), refMsg);
            }
         }

         if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DISTRIBUTED.ordinal()) {
            String action = "Distributing parcel...";

            final StatusPoller poller = new ParcelProvisionPoller(apiParcelResource, AvailableParcelStage.DISTRIBUTED, cluster.getCurrentReport(),
                  reportQueue, ProgressSplit.DISTRIBUTE_PARCEL.getProgress());

            executeAndReport(action, apiParcelResource.startDistributionCommand(),
                  ProgressSplit.DISTRIBUTE_PARCEL.getProgress(),
                  cluster.getCurrentReport(), reportQueue, poller, false);

            if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DISTRIBUTED.ordinal()) {
               throw ClouderaManagerException.DISTRIBUTE_PARCEL_FAIL(apiParcelResource.readParcel().getProduct(),
                     apiParcelResource.readParcel().getVersion(), refMsg);
            }
         }
         if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.ACTIVATED.ordinal()) {
            String action = "Activating parcel...";

            executeAndReport(action, apiParcelResource.activateCommand(), ProgressSplit.ACTIVATE_PARCEL.getProgress(),
                  cluster.getCurrentReport(), reportQueue, new StatusPoller() {
               @Override
               public boolean poll() {
                  // activate parcel is pretty fast, so suppose we are no need to do much error handling/progress monitoring
                  // TODO: set a timeout
                  return apiParcelResource.readParcel().getStage().equals(AvailableParcelStage.ACTIVATED.toString());
               }
            }, false);

            if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.ACTIVATED.ordinal()) {
               throw ClouderaManagerException.ACTIVATE_PARCEL_FAIL(apiParcelResource.readParcel().getProduct(),
                     apiParcelResource.readParcel().getVersion(), refMsg);
            }
         }
      }
   }

   private void configureServices(final CmClusterDef cluster, final ClusterReportQueue reportQueue, final boolean skipIfConfigured) {
      boolean servicesConfigured = isConfigured(cluster);
      if (servicesConfigured) {
         syncRolesId(cluster);
         if (skipIfConfigured) {
            return;
         }
      }
      String action = "Configuring cluster services";
      cluster.getCurrentReport().setAction(action);
      reportQueue.addClusterReport(cluster.getCurrentReport().clone());

      Collections.sort(cluster.getServices(), new Comparator<CmServiceDef>() {
         @Override
         public int compare(CmServiceDef o1, CmServiceDef o2) {
            return o1.getType().compareTo(o2.getType());
         }
      });

      Iterator<CmServiceDef> serviceDefIterator = cluster.getServices().iterator();
      CmServiceDef hueService = null;
      while (serviceDefIterator.hasNext() || hueService != null) {
         ApiServiceList serviceList = new ApiServiceList();
         try {
            if (hueService != null) {
               CmServiceDef hdfsService = cluster.serviceDefOfType("HDFS");
               CmRoleDef namenodeRole = null;
               for (CmRoleDef roleDef : hdfsService.getRoles()) {
                  if (roleDef.getType().getDisplayName().equals("HDFS_NAMENODE")) {
                     namenodeRole = roleDef;
                     break;
                  }
               }
               if (namenodeRole != null) {
                  // namenode role def should already synced the role ID from CM
                  hueService.addConfig(Constants.CONFIG_HUE__WEBHDFS, namenodeRole.getName());
                  addApiService(serviceList, cluster, hueService, servicesConfigured);
               }
               hueService = null;
            }

            while (serviceDefIterator.hasNext()) {
               CmServiceDef serviceDef = serviceDefIterator.next();
               if (serviceDef.getType().getDisplayName().equals("HUE")) {
                  hueService = serviceDef;
                  break;
               }
               addApiService(serviceList, cluster, serviceDef, servicesConfigured);
            }

            if (!servicesConfigured) {
               apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).createServices(serviceList);
               logger.info("Finished create services");

               syncRolesId(cluster);
            }
         } catch (Exception e) {
            String errMsg = "Failed to configure services" + ((e.getMessage() == null) ? "" : (", " + e.getMessage()));
            logger.error(errMsg);
            throw SoftwareManagementPluginException.CONFIGURE_SERVICE_FAILED(e);
         }
      }

      try {
         updateRoleConfigGroups(cluster.getName());
         logger.info("Updated roles config groups");

         preDeployConfig(cluster);

         executeAndReport("Deploying client config", apiResourceRootV6.getClustersResource().deployClientConfig(cluster.getName()),
               ProgressSplit.CONFIGURE_SERVICES.getProgress(), cluster.getCurrentReport(), reportQueue);

      } catch (Exception e) {
         String errMsg = "Failed to configure services" + ((e.getMessage() == null) ? "" : (", " + e.getMessage()));
         logger.error(errMsg);
         throw SoftwareManagementPluginException.CONFIGURE_SERVICE_FAILED(e);
      }
   }

   private void addApiService(ApiServiceList serviceList, CmClusterDef cluster, CmServiceDef serviceDef, boolean servicesConfigured) {
      ApiService apiService = new ApiService();
      apiService.setType(serviceDef.getType().getName());
      apiService.setName(serviceDef.getName());
      apiService.setDisplayName(serviceDef.getDisplayName());

      ApiServiceConfig apiServiceConfig = new ApiServiceConfig();

      Set<String> serviceTypes = cluster.allServiceTypes();
      if (serviceDef.getType().getDependencies() != null) {
         for (AvailableServiceRole.Dependency dependency : serviceDef.getType().getDependencies()) {
            for (String dependService : dependency.getServices()) {
               if (serviceDef.getType().getDisplayName().equals("IMPALA") && dependService.equals("YARN")) {
                  // Impala needs Llama role to be able to use YARN for resource management, but this
                  // is a new added role and not yet supported well
                  continue;
               }
               if (serviceTypes.contains(dependService)) {
                  apiServiceConfig.add(new ApiConfig(dependency.getConfigKey(), cluster.serviceNameOfType(dependService)));
               }
            }
         }
      }

      if (serviceDef.getConfiguration() != null) {
         for (String key : serviceDef.getConfiguration().keySet()) {
            apiServiceConfig.add(new ApiConfig(key, serviceDef.getConfiguration().get(key)));
         }
      }

      //add service user and group to service config
      addServiceUserConfig(serviceDef, apiServiceConfig);

      // update configs if service already exist
      if (servicesConfigured
            && apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).readService(serviceDef.getName()) != null) {
         apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
               .updateServiceConfig(serviceDef.getName(), "update configs for role " + serviceDef.getName(), apiServiceConfig);
         logger.info("Finished reconfigure service " + serviceDef.getName());
      }

      apiService.setConfig(apiServiceConfig);

      List<ApiRole> apiRoles = new ArrayList<ApiRole>();
      for (CmRoleDef roleDef : serviceDef.getRoles()) {
         ApiRole apiRole = createApiRole(roleDef);

            /*
            update configs of this role if services already exist,
            the roleDef's roleName is already synced up at the beginning of this function
             */
         if (servicesConfigured
               && apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).readService(serviceDef.getName()) != null
               && apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).getRolesResource(serviceDef.getName()).readRole(roleDef.getName()) != null) {
            apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).getRolesResource(serviceDef.getName())
                  .updateRoleConfig(roleDef.getName(), "update config for role " + roleDef.getDisplayName(), apiRole.getConfig());
            logger.info("Finished reconfigure role " + roleDef.getDisplayName());
         }

         apiRoles.add(apiRole);
      }

      apiService.setRoles(apiRoles);
      serviceList.add(apiService);
   }

   //Todo(qjin): add service user config to each configured service
   private void addServiceUserConfig(CmServiceDef serviceDef, ApiServiceConfig apiServiceConfig) {
      logger.info("in addServiceUserGonfig, userName is " + serviceDef.getProcessUserName() + ", groupName is " + serviceDef.getProcessGroupName());
      if (!CommonUtil.isBlank(serviceDef.getProcessUserName())) {
            apiServiceConfig.add(new ApiConfig("process_username", serviceDef.getProcessUserName()));
      }
      if (!CommonUtil.isBlank(serviceDef.getProcessGroupName())) {
         apiServiceConfig.add(new ApiConfig("process_groupname", "hadoop_group"));
      }
   }

   private void preDeployConfig(final CmClusterDef cluster) throws Exception {
      // Necessary, since createServices a habit of kicking off async commands (eg ZkAutoInit )
      for (CmServiceDef serviceDef : cluster.getServices()) {
         for (ApiCommand command : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
               .listActiveCommands(serviceDef.getName(), DataView.SUMMARY)) {
            execute(command);
         }
      }
   }

   /**
    * Update base role config groups to avoid showing error msg on CM GUI, these configurations do not take
    * effect actually, will be overridden by each role's configuration.
    * @param clusterName
    * @throws IOException
    */
   private void updateRoleConfigGroups(String clusterName) throws IOException {
      Map<String, String> nameMap = AvailableServiceRoleContainer.nameToDisplayName();
      if (nameMap == null || nameMap.isEmpty()) {
         return;
      }
      ServicesResourceV6 servicesResource = apiResourceRootV6.getClustersResource().getServicesResource(clusterName);
      for (ApiService service : servicesResource.readServices(DataView.SUMMARY)) {
         for (ApiRoleConfigGroup roleConfigGroup : servicesResource.getRoleConfigGroupsResource(service.getName())
               .readRoleConfigGroups()) {
            if (roleConfigGroup == null || !nameMap.containsKey(roleConfigGroup.getRoleType())) {
               continue;
            }

            String roleDisplayName = nameMap.get(roleConfigGroup.getRoleType());
            ApiConfigList configList = new ApiConfigList();
            boolean needUpdate = true;
            switch (roleDisplayName) {
               case "HDFS_NAMENODE":
                  configList.add(new ApiConfig(Constants.CONFIG_DFS_NAME_DIR_LIST, "/tmp/dfs/nn"));
                  break;
               case "HDFS_DATANODE":
                  configList.add(new ApiConfig(Constants.CONFIG_DFS_DATA_DIR_LIST, "/tmp/dfs/dn"));
                  break;
               case "HDFS_SECONDARY_NAMENODE":
                  configList.add(new ApiConfig(Constants.CONFIG_FS_CHECKPOINT_DIR_LIST, "/tmp/dfs/snn"));
                  break;
               case "HDFS_JOURNALNODE":
                  configList.add(new ApiConfig(Constants.CONFIG_DFS_JOURNALNODE_EDITS_DIR, "/tmp/dfs/jn"));
                  break;
               case "YARN_NODE_MANAGER":
                  configList.add(new ApiConfig(Constants.CONFIG_NM_LOCAL_DIRS, "/tmp/yarn/nm"));
                  break;
               case "MAPREDUCE_JOBTRACKER":
                  configList.add(new ApiConfig(Constants.CONFIG_MAPRED_JT_LOCAL_DIR_LIST, "/tmp/mapred/jt"));
                  break;
               case "MAPREDUCE_TASKTRACKER":
                  configList.add(new ApiConfig(Constants.CONFIG_MAPRED_TT_LOCAL_DIR_LIST, "/tmp/mapred/tt"));
                  break;
               default:
                  needUpdate = false;
                  break;
            }

            if (needUpdate) {
               logger.info("Updating base role config group of type: " + roleDisplayName);
               roleConfigGroup.setConfig(configList);
               servicesResource.getRoleConfigGroupsResource(service.getName()).updateRoleConfigGroup(roleConfigGroup.getName(),
                     roleConfigGroup, Constants.ROLE_CONFIG_GROUP_UPDATE_NOTES);
            }
         }
      }
   }

   /**
    *
    * @param cluster
    * @param reportQueue
    * @param endProgress
    * @throws Exception
    */
   private void startNnHA(final CmClusterDef cluster, final ClusterReportQueue reportQueue, final int endProgress) throws Exception {
      // Initialize Zookeeper
      CmServiceDef zkService = cluster.serviceDefOfType("ZOOKEEPER");

      executeAndReport("Initializing Zookeeper", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
            .zooKeeperInitCommand(zkService.getName()),
            INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);

      startService(cluster, zkService, (cluster.getCurrentReport().getProgress() + endProgress) / 2, reportQueue);

      // Initialize High Availability state in Zookeeper
      CmServiceDef hdfsService = cluster.serviceDefOfType("HDFS");
      for (CmRoleDef roleDef : hdfsService.getRoles()) {
         if (roleDef.getType().getDisplayName().equals("HDFS_FAILOVER_CONTROLLER") && roleDef.isActive()) {
            executeAndReport("Initialize High Availability state in Zookeeper", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .getRoleCommandsResource(hdfsService.getName())
                  .hdfsInitializeAutoFailoverCommand(new ApiRoleNameList(ImmutableList.<String>builder().add(roleDef.getName()).build())),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);
            break;
         }
      }

      // Start JournalNodes
      ApiRoleNameList jnRoles = new ApiRoleNameList();
      for (CmRoleDef roleDef : hdfsService.getRoles()) {
         if (roleDef.getType().getDisplayName().equals("HDFS_JOURNALNODE")) {
            jnRoles.add(roleDef.getName());
         }
      }
      executeAndReport("Starting JournalNodes", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
            .getRoleCommandsResource(hdfsService.getName()).startCommand(jnRoles),
            INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);

      /*
      1) Format active NN
      2) Intialize shared edits directory of NameNode
      3) Start active NN
      */
      for (CmRoleDef roleDef : hdfsService.getRoles()) {
         if (roleDef.getType().getDisplayName().equals("HDFS_NAMENODE") && roleDef.isActive()) {
            ApiRoleNameList nnRoles = new ApiRoleNameList();
            nnRoles.add(roleDef.getName());
            try {
               executeAndReport("Formatting Active Namenode",
                     apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                           .getRoleCommandsResource(hdfsService.getName()).formatCommand(nnRoles),
                     INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);
            } catch (Exception e){
               // ignore
            }

            try {
               executeAndReport("Initializing Shared Edits Directory of Namenode",
                     apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                           .getRoleCommandsResource(hdfsService.getName()).hdfsInitializeSharedDirCommand(nnRoles),
                     INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);
            } catch (Exception e) {
               // ignore
            }

            executeAndReport("Starting Active Namenode", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .getRoleCommandsResource(hdfsService.getName()).startCommand(nnRoles),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);

            break;
         }
      }

      // Wait for active NameNode start up
      Thread.sleep(10 * 1000); // TODO: wait until standby NN started responding to RPCs

      /*
      1) Bootstrapping standby NN
      2) Start standby NN
       */
      for (CmRoleDef roleDef : hdfsService.getRoles()) {
         if (roleDef.getType().getDisplayName().equals("HDFS_NAMENODE") && !roleDef.isActive()) {

            ApiRoleNameList nnRoles = new ApiRoleNameList();
            nnRoles.add(roleDef.getName());
            executeAndReport("Boostrapping Standby Namenode",
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .getRoleCommandsResource(hdfsService.getName()).hdfsBootstrapStandByCommand(nnRoles),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);

            executeAndReport("Starting Standby Namenode", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .getRoleCommandsResource(hdfsService.getName()).startCommand(nnRoles),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);

            break;
         }
      }

      // Start Failover controllers
      List<String> failOverRoles = new ArrayList<String>();
      for (CmRoleDef roleDef : hdfsService.getRoles()) {
         if (roleDef.getType().getDisplayName().equals("HDFS_FAILOVER_CONTROLLER")) {
            failOverRoles.add(roleDef.getName());
         }
      }

      executeAndReport("Starting Failover Controllers", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
            .getRoleCommandsResource(hdfsService.getName()).startCommand(new ApiRoleNameList(failOverRoles)),
            INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);

      // Wait for standby NameNode start up
      Thread.sleep(10 * 1000); // TODO: wait until standby NN started responding to RPCs

      // Create HDFS /tmp directory
      executeAndReport("Creating HDFS Temp Dir", apiResourceRootV6.getClustersResource()
            .getServicesResource(cluster.getName()).hdfsCreateTmpDir(hdfsService.getName()),
            INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);

      // Start all other roles;
      startService(cluster, hdfsService, endProgress, reportQueue);
   }

   private boolean startNodeServices(final CmClusterDef cluster, final List<String> addedNodes,
         final Map<String, List<ApiRole>> roles, final ClusterReportQueue reportQueue)
               throws Exception {
      boolean executed = true;
      int endProgress = ProgressSplit.START_SERVICES.getProgress();
      for (String serviceName : roles.keySet()) {
         Set<String> roleDisplayNames = new HashSet<>();
         List<String> roleNames = new ArrayList<>();
         for (ApiRole role : roles.get(serviceName)) {
            if (isRoleStarted(cluster.getName(), serviceName, role.getName())) {
               continue;
            }
            roleNames.add(role.getName());
            roleDisplayNames.add(role.getType());
         }
         executeAndReport("Starting Roles " + roleDisplayNames, addedNodes,
               apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
               .getRoleCommandsResource(serviceName).startCommand(new ApiRoleNameList(roleNames)),
               INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
         cluster.getCurrentReport().setProgress(endProgress);
         reportQueue.addClusterReport(cluster.getCurrentReport().clone());
      }
      return executed;
   }

   private boolean isRoleStarted(String clusterName, String serviceName, String roleName) {
      ApiRoleState roleState = apiResourceRootV6.getClustersResource()
            .getServicesResource(clusterName)
            .getRolesResource(serviceName)
            .readRole(roleName).getRoleState();
      return roleState.equals(ApiRoleState.STARTED);
   }

   /**
    * start services/roles, assume the roles' IDs are already synched
    * @param cluster
    * @param isFirstStart
    * @return
    * @throws com.vmware.bdd.plugin.clouderamgr.exception.ClouderaManagerException
    */
   private boolean startServices(final CmClusterDef cluster, final ClusterReportQueue reportQueue, final boolean isFirstStart) throws ClouderaManagerException {
      boolean executed = true;
      int endProgress = ProgressSplit.START_SERVICES.getProgress();
      try {
         ReflectionUtils.getPreStartServicesHook().preStartServices(cluster.getName());
         if (!cluster.isEmpty()) {
            if (!isConfigured(cluster)) {
               configureServices(cluster, reportQueue, true);
            }
            if (!isStarted(cluster)) {
               // sort the services based on their dependency relationship.
               Collections.sort(cluster.getServices(), new Comparator<CmServiceDef>() {
                  @Override
                  public int compare(CmServiceDef o1, CmServiceDef o2) {
                     return o1.getType().compareTo(o2.getType());
                  }
               });
               int leftServices = cluster.getServices().size();
               if (cluster.isFailoverEnabled()) {
                  int haProgress = cluster.getCurrentReport().getProgress()
                        + (endProgress - cluster.getCurrentReport().getProgress()) * 2 / leftServices;
                  startNnHA(cluster, reportQueue, haProgress);
                  leftServices -= 2;
               }

               for (CmServiceDef serviceDef : cluster.getServices()) {
                  if (apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .readService(serviceDef.getName()).getServiceState().equals(ApiServiceState.STARTED)) {
                     // Zookeeper and HDFS services may be already started in startNnHA()
                     continue;
                  }

                  if (!isFirstStart) {
                     startService(cluster, serviceDef, INVALID_PROGRESS, reportQueue);
                  } else if (apiResourceRootV7 != null) {
                     // TODO: CM's BUG here: the getServicesResource() should accept a cluster name instead of clusterName
                     executeAndReport("Starting " + serviceDef.getType().getDisplayName() + " Service",
                           apiResourceRootV7.getClustersResource().getServicesResource(serviceDef.getName())
                           .firstRun(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
                  } else {
                     if (Version.compare(serviceDef.getType().getVersionApiMin() , "6") > 0) {
                        // just log, do not throw exception
                        logger.error(serviceDef.getType().getDisplayName() + " service cannot be deployed by API version 6, "
                              + "please upgrade CloudeManager to version " + Constants.API_VERSION_SINCE_OF_CM_VERSION.get(6) + " or higher");
                        continue;
                     }
                     // pre start
                     preStartServices(cluster, serviceDef, reportQueue);
                     for (CmRoleDef roleDef : serviceDef.getRoles()) {
                        preStartRoles(cluster, serviceDef, roleDef, reportQueue);
                     }

                     // start
                     startService(cluster, serviceDef, INVALID_PROGRESS, reportQueue);

                     // post start
                     postStartServices(cluster, serviceDef, reportQueue);
                     for (CmRoleDef roleDef : serviceDef.getRoles()) {
                        postStartRoles(cluster, serviceDef, roleDef, reportQueue);
                     }
                  }

                  if (leftServices > 0) {
                     int currentProgress = cluster.getCurrentReport().getProgress();
                     int toProgress = currentProgress + (endProgress - currentProgress) / leftServices;
                     if (toProgress != currentProgress) {
                        cluster.getCurrentReport().setProgress(toProgress);
                        reportQueue.addClusterReport(cluster.getCurrentReport().clone());
                     }
                  }
                  leftServices -= 1;
               }
            } else {
               executed = false;
            }

            // push into provision phase once OPSAPS-13194/OPSAPS-12870 is addressed
            startManagement();

            cluster.getCurrentReport().setProgress(endProgress);
            reportQueue.addClusterReport(cluster.getCurrentReport().clone());
         }
      } catch (Exception e) {
         String errMsg = "Failed to start services";
         logger.error(errMsg, e);
         throw SoftwareManagementPluginException.START_SERVICE_FAILED(e, Constants.CDH_PLUGIN_NAME, cluster.getName());
      }

      return executed;
   }

   private void preStartServices(final CmClusterDef cluster, CmServiceDef serviceDef, final ClusterReportQueue reportQueue) throws Exception {
      switch (serviceDef.getType().getDisplayName()) {
         case "HIVE":
            /* This command is not yet exposed by ClouderaManager
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .hiveCreateMetastoreDatabaseCommand(serviceDef.getName()), false);
                  */
            executeAndReport("Creating Hive Metastore Database Tables", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .hiveCreateMetastoreDatabaseTablesCommand(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            executeAndReport("Creating Hive User Dir", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createHiveUserDirCommand(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            executeAndReport("Createing Hive Warehourse", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createHiveWarehouseCommand(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "OOZIE":
            executeAndReport("Creating Oozie Database", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .createOozieDb(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            executeAndReport("Installing Oozie ShareLib", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .installOozieShareLib(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "HBASE":
            executeAndReport("Creating HBase Root Dir", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createHBaseRootCommand(serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "ZOOKEEPER":
            executeAndReport("Initializing Zookeeper", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .zooKeeperInitCommand(serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);
            break;
         case "SOLR":
            executeAndReport("Init Solr Service",
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .initSolrCommand(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            executeAndReport("Creating Solr HDFS Home Dir", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createSolrHdfsHomeDirCommand(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "SQOOP":
            executeAndReport("Creating Sqoop User Dir", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createSqoopUserDirCommand(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "IMPALA":
            executeAndReport("Creating Impala User Dir", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createImpalaUserDirCommand(serviceDef.getName()), INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         default:
            break;
      }
   }

   private void preStartRoles(final CmClusterDef cluster, CmServiceDef serviceDef, CmRoleDef roleDef, ClusterReportQueue reportQueue) throws Exception {

      switch (roleDef.getType().getDisplayName()) {
         case "HDFS_NAMENODE":
            executeAndReport("Formatting Namenode",
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .getRoleCommandsResource(serviceDef.getName()).formatCommand(
                        new ApiRoleNameList(ImmutableList.<String>builder().add(roleDef.getName()).build())),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "YARN_RESOURCE_MANAGER":
            executeAndReport("Creating Remote Log Dir for ResourceManager", apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName()).createYarnNodeManagerRemoteAppLogDirCommand(
                        serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "YARN_JOB_HISTORY":
            executeAndReport("Creating Dir for JobHistory", apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName()).createYarnJobHistoryDirCommand(serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "HUE_SERVER":
            executeAndReport("Syncing up Hue Database", apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName()).getRoleCommandsResource(serviceDef.getName())
                  .syncHueDbCommand(new ApiRoleNameList(ImmutableList.<String>builder().add(roleDef.getName()).build())),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         default:
            break;
      }
   }

   private void postStartServices(final CmClusterDef cluster, CmServiceDef serviceDef, final ClusterReportQueue reportQueue) throws Exception {
      switch (serviceDef.getType().getDisplayName()) {
         case "HDFS":
            executeAndReport("Creating HDFS Temp Dir", apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName()).hdfsCreateTmpDir(serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);
         default:
            break;
      }
   }

   private void postStartRoles(final CmClusterDef cluster, CmServiceDef serviceDef, CmRoleDef roleDef, final ClusterReportQueue reportQueue) throws Exception {
      switch (roleDef.getType().getDisplayName()) {
         default:
            break;
      }
   }

   private void startService(CmClusterDef cluster, CmServiceDef serviceDef, int toProgress, final ClusterReportQueue reportQueue) throws Exception {
      if (apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
            .readService(serviceDef.getName()).getServiceState().equals(ApiServiceState.STARTED)) {
         return;
      }
      String serviceDisplayName = serviceDef.getType().getDisplayName();
      // in compute only usecase, we don't need to start service for isilon
      if (serviceDisplayName.equalsIgnoreCase("ISILON")) {
         return;
      }
      logger.info("Cloudera manager is starting service: " + serviceDisplayName);
      executeAndReport("Starting Service " + serviceDisplayName, apiResourceRootV6.getClustersResource()
            .getServicesResource(cluster.getName()).startCommand(serviceDef.getName()),
            toProgress, cluster.getCurrentReport(), reportQueue, true);
   }

   private void startManagement() {
      try {
         if (apiResourceRootV6.getClouderaManagerResource().getMgmtServiceResource().readService(DataView.SUMMARY)
               .getServiceState().equals(ApiServiceState.STOPPED)) {
            logger.info("Starting Cloudera Management Service");
            execute(apiResourceRootV6.getClouderaManagerResource().getMgmtServiceResource().startCommand());
         }
      } catch (Exception e) {
         // ignore
      }
   }

   public String getCmServerHostId() {
      if (cmServerHostId != null) {
         return cmServerHostId;
      }

      for (ApiHost apiHost : apiResourceRootV6.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
         if (apiHost.getIpAddress().equals(cmServerHost) || apiHost.getHostname().equalsIgnoreCase(cmServerHost)) {
            this.cmServerHostId = apiHost.getHostId();
            return cmServerHostId;
         }
      }

      throw ClouderaManagerException.FAIL_FETCH_CM_SERVER_HOST_ID();
   }

   private ApiCommand execute(final ApiBulkCommandList bulkCommand, boolean checkReturn) throws Exception {
      ApiCommand lastCommand = null;
      for (ApiCommand command : bulkCommand) {
         lastCommand = execute(command, checkReturn);
      }
      return lastCommand;
   }

   private ApiCommand execute(final ApiCommand command) throws Exception {
      return execute(command, true);
   }

   private ApiCommand execute(final ApiCommand command, boolean checkReturn) throws Exception {
      return executeAndReport(null, command, INVALID_PROGRESS, null, null, checkReturn);
   }

   private ApiCommand executeAndReport(String action, final ApiCommand command, int endProgress,
         ClusterReport currentReport, ClusterReportQueue reportQueue) throws Exception {
      return executeAndReport(action, command, endProgress, currentReport, reportQueue, true);
   }

   private ApiCommand executeAndReport(String action, final ApiBulkCommandList bulkCommand, int endProgress,
         ClusterReport currentReport, ClusterReportQueue reportQueue, boolean checkReturn) throws Exception {
      return executeAndReport(action, null, bulkCommand, endProgress, currentReport, reportQueue, checkReturn);
   }

   private ApiCommand executeAndReport(String action, List<String> addedNodes, final ApiBulkCommandList bulkCommand,
         int endProgress, ClusterReport currentReport, ClusterReportQueue reportQueue, boolean checkReturn)
               throws Exception {
      ApiCommand lastCommand = null;
      for (ApiCommand command : bulkCommand) {
         lastCommand = executeAndReport(action, addedNodes, command, endProgress, currentReport, reportQueue, checkReturn);
      }
      return lastCommand;
   }

   private ApiCommand executeAndReport(String action, final List<String> nodeNames, final ApiCommand command,
         int endProgress, ClusterReport currentReport, ClusterReportQueue reportQueue, boolean checkReturn) throws Exception {
      return executeAndReport(action, nodeNames, command, endProgress, currentReport, reportQueue,
            new StatusPoller() {
               @Override
               public boolean poll() {
                  return apiResourceRootV6.getCommandsResource().readCommand(command.getId()).getEndTime() != null;
               }
            }, checkReturn);
   }

   private ApiCommand executeAndReport(String action, final ApiCommand command, int endProgress,
         ClusterReport currentReport, ClusterReportQueue reportQueue, boolean checkReturn) throws Exception {
      return executeAndReport(action, command, endProgress, currentReport, reportQueue,
            new StatusPoller() {
               @Override
               public boolean poll() {
                  return apiResourceRootV6.getCommandsResource().readCommand(command.getId()).getEndTime() != null;
               }
            }, checkReturn);
   }

   private ApiCommand executeAndReport(String action, final ApiCommand command, int endProgress,
         ClusterReport currentReport, ClusterReportQueue reportQueue, StatusPoller poller,
         boolean checkReturn) throws Exception {
      return executeAndReport(action, null, command, endProgress, currentReport, reportQueue, poller, checkReturn);
   }

   /**
    * When checkReturn is true, we will check the return value of async cloudera command
    */
   private ApiCommand executeAndReport(String action, List<String> nodeNames, final ApiCommand command,
         int endProgress, ClusterReport currentReport, ClusterReportQueue reportQueue, StatusPoller poller,
         boolean checkReturn) throws Exception {

      if (action != null) {
         logger.info("Action: " + action);
         if (nodeNames != null) {
            currentReport.setNodesAction(action, nodeNames);
            currentReport.setNodesStatus(ServiceStatus.PROVISIONING, nodeNames);
         } else {
            currentReport.setClusterAndNodesAction(action);
            currentReport.setClusterAndNodesServiceStatus(ServiceStatus.PROVISIONING);
         }
         reportQueue.addClusterReport(currentReport.clone());
      }

      ApiCommand commandReturn = null;
      poller.waitForComplete();

      if (checkReturn && command != null
            && !(commandReturn = apiResourceRootV6.getCommandsResource().readCommand(command.getId())).getSuccess()) {
         logger.info("Failed to run command: " + command);
         String errorMsg = getSummaryErrorMsg(command, domain);
         logger.error(errorMsg);
         String hostId = (commandReturn.getHostRef() == null) ? null : commandReturn.getHostRef().getHostId();
         throw CommandExecFailException.EXECUTE_COMMAND_FAIL(hostId, errorMsg);
      }

      if (endProgress != INVALID_PROGRESS) {
         if (currentReport.getProgress() < endProgress) {
            currentReport.setProgress(endProgress);
            reportQueue.addClusterReport(currentReport.clone());
         }
      }

      return commandReturn;
   }

   private String getSummaryErrorMsg(ApiCommand command, String domain) {
      ApiCommand newCommand = apiResourceRootV6.getCommandsResource().readCommand(command.getId());
      StringBuilder errorMsg = new StringBuilder(newCommand.getResultMessage());
      if (errorMsg.length() > 0) {
         errorMsg.append(". ");
      }
      if (newCommand.getResultDataUrl() != null) {
         errorMsg = errorMsg.append(referCmfUrlMsg(newCommand.getResultDataUrl()));
      } else {
         errorMsg = errorMsg.append(referCmfUrlMsg(domain + "/cmf/command/" + newCommand.getId() + "/details"));
      }
      return errorMsg.toString();
   }

   private String hostId2IP(String hostId) {
      logger.info("hostId is " + hostId);
      ApiHost host = apiResourceRootV6.getHostsResource().readHost(hostId);
      return host.getIpAddress();
   }

   private String referCmfUrlMsg(String url) {
      return "Please refer to " + url + " for details";
   }

   private static abstract class Retriable {
      public abstract void doWork() throws Exception;
   }

   private void retry(int retryTimes, Retriable operate) throws Exception {
      int i = 0;
      while (true) {
         i += 1;
         try {
            operate.doWork();
            return;
         } catch (Exception e) {
            if (i == retryTimes) {
               logger.info("Failed for all " + retryTimes + " times");
               throw e;
            } else {
               logger.info(
                     "Failed for the " + i + "st round: " + e.getMessage() + ", have " + (retryTimes - i) + " times to retry");
            }
         }
      }
   }

   @Override
   public List<String> validateRolesForScaleOut(NodeGroupInfo group) {
      // resize of job tracker and name node is not supported
      List<String> roles = group.getRoles();
      List<String> unsupportedRoles = new ArrayList<String>();
      if (roles.isEmpty()) {
         // no unsupported roles
         return new ArrayList<String>();
      }
      if (roles.contains(HadoopRole.HDFS_NAMENODE_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.HDFS_NAMENODE_ROLE.toString());
      }
      if (roles.contains(HadoopRole.HDFS_SECONDARY_NAMENODE_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.HDFS_SECONDARY_NAMENODE_ROLE.toString());
      }
      if (roles.contains(HadoopRole.MAPREDUCE_JOBTRACKER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.MAPREDUCE_JOBTRACKER_ROLE.toString());
      }
      if (roles.contains(HadoopRole.YARN_RESOURCE_MANAGER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.YARN_RESOURCE_MANAGER_ROLE.toString());
      }
      if (roles.contains(HadoopRole.ZOOKEEPER_SERVER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.ZOOKEEPER_SERVER_ROLE.toString());
      }
      return unsupportedRoles;
   }

   public void validateRolesForShrink(NodeGroupInfo groupInfo)
         throws SoftwareManagementPluginException {
      ValidateRolesUtil.validateRolesForShrink(CmUtils.getConfDir(), groupInfo);
   }

   @Override
   public void updateInfrastructure(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean hasHbase(ClusterBlueprint blueprint) {
      boolean hasHbase = false;
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         if (HadoopRole.hasHBaseRole(group.getRoles())) {
            hasHbase = true;
            break;
         }
      }
      return hasHbase;
   }

   @Override
   public boolean hasMgmtRole(List<String> roles) {
      return HadoopRole.hasMgmtRole(roles);
   }

   @Override
   public boolean isComputeOnlyRoles(List<String> roles) {
      return false;
   }

   @Override
   public boolean twoDataDisksRequired(NodeGroupInfo group) {
      EnumSet<HadoopRole> enumRoles = HadoopRole.getEnumRoles(group.getRoles());
      if ((enumRoles.size() == 1 || (enumRoles.size() == 2 && enumRoles
            .contains(HadoopRole.HDFS_JOURNALNODE_ROLE)))
            && (enumRoles.contains(HadoopRole.ZOOKEEPER_SERVER_ROLE))) {
         return true;
      }
      return false;
   }

   @Override
   public boolean hasComputeMasterGroup(ClusterBlueprint blueprint) {
      return false;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager#getVersion()
    */
   @Override
   public String getVersion() {
      try {
         return apiResourceRootV6.getClouderaManagerResource().getVersion().getVersion();
      } catch (Exception e) {
         // we print the log here for user to check the cause.
         String errMsg = "Cannot connect to the Software Manager, check the connection information.";
         logger.error(errMsg, e);
         return UNKNOWN_VERSION;
      }
   }

   @Override
   public HadoopStack getDefaultStack()
         throws SoftwareManagementPluginException {
      List<HadoopStack> hadoopStacks = getSupportedStacks();
      Collections.<HadoopStack> sort(hadoopStacks);
      return hadoopStacks.get(0);
   }

   @Override
   public boolean hasMountPointStartwithDatax(String clusterName) {
      return false;
   }
}
