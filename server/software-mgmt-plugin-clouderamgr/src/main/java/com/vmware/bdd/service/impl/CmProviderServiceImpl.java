package com.vmware.bdd.service.impl;

import com.cloudera.api.ApiRootResource;
import com.cloudera.api.ClouderaManagerClientBuilder;
import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiBulkCommandList;
import com.cloudera.api.model.ApiCluster;
import com.cloudera.api.model.ApiClusterList;
import com.cloudera.api.model.ApiClusterVersion;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiConfig;
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiHost;
import com.cloudera.api.model.ApiHostInstallArguments;
import com.cloudera.api.model.ApiHostRef;
import com.cloudera.api.model.ApiHostRefList;
import com.cloudera.api.model.ApiParcel;
import com.cloudera.api.model.ApiRole;
import com.cloudera.api.model.ApiRoleList;
import com.cloudera.api.model.ApiRoleState;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.model.ApiServiceConfig;
import com.cloudera.api.model.ApiServiceList;
import com.cloudera.api.v3.ParcelResource;
import com.cloudera.api.v3.RootResourceV3;
import com.cloudera.api.v4.RootResourceV4;
import com.cloudera.api.v5.RootResourceV5;
import com.cloudera.api.v6.RootResourceV6;
import com.cloudera.api.v6.ServicesResourceV6;
import com.google.common.collect.ImmutableMap;
import com.vmware.bdd.model.CmClusterDef;
import com.vmware.bdd.model.CmMgmtServiceType;
import com.vmware.bdd.model.CmNodeDef;
import com.vmware.bdd.model.CmParcelStage;
import com.vmware.bdd.model.CmRoleDef;
import com.vmware.bdd.model.CmServiceDef;
import com.vmware.bdd.model.CmServiceRoleType;
import com.vmware.bdd.exception.CmException;
import com.vmware.bdd.service.ICmProviderService;
import com.vmware.bdd.utils.Constants;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 1:03 PM
 */
public class CmProviderServiceImpl implements ICmProviderService {
   private static final Logger logger = Logger.getLogger(CmProviderServiceImpl.class);

   public static String[][] CM_VERSION_MATRIX = new String[][] { { "cm5.0.0", "v6", "cdh5" }, { "cm5.0.0", "v6", "cdh4" } };


   public enum CmServerServiceTypeRepo {
      CDH, IMPALA, SOLR, SPARK;
      private static final ImmutableMap<String, ImmutableMap<String, String>> TYPE_TO_REPO = ImmutableMap.of("CDH4",
            ImmutableMap.of("CDH", "CDH", "IMPALA", "IMPALA", "SOLR", "SOLR", "SPARK", "SPARK"), "CDH5",
            ImmutableMap.of("CDH", "CDH", "IMPALA", "CDH", "SOLR", "CDH", "SPARK", "CDH"));
      public String toString(String cdh) {
         if (!TYPE_TO_REPO.containsKey(cdh) || !TYPE_TO_REPO.get(cdh).containsKey(this.toString())) {
            throw new RuntimeException("Could not determine repo for [" + cdh + "] and [" + this + "]");
         }
         return TYPE_TO_REPO.containsKey(cdh) ? TYPE_TO_REPO.get(cdh).get(this.toString()) : null;
      }
   };

   public static final int VERSION_UNBOUNDED = -1;
   public static final String NAME_TOKEN_DELIM = "_";
   public static final String NAME_TAG_DEFAULT = "cdh";
   public static final String NAME_QUALIFIER_DEFAULT = "1";
   public static final String NAME_QUALIFIER_GROUP = "group";

   private String version;
   private int versionApi;
   private int versionCdh;
   private RootResourceV3 apiResourceRootV3;
   private RootResourceV4 apiResourceRootV4;
   private RootResourceV5 apiResourceRootV5;
   private RootResourceV6 apiResourceRootV6;

   private boolean isFirstStartRequired = true;
   private static final String CDH_REPO_PREFIX = "CDH";

   private static final String CM_PARCEL_STAGE_DOWNLOADED = "DOWNLOADED";
   private static final String CM_PARCEL_STAGE_DISTRIBUTED = "DISTRIBUTED";
   private static final String CM_PARCEL_STAGE_ACTIVATED = "ACTIVATED";

   private static final String CM_CONFIG_UPDATE_MESSAGE = "Update base config group with defaults";

   private static int API_POLL_PERIOD_MS = 500;

   public CmProviderServiceImpl(String version, int versionApi, int versionCdh, String cmServerHost, int port,
         String user, String password) throws CmException {
      this.version = version;
      this.versionApi = versionApi;
      this.versionCdh = versionCdh;
      ApiRootResource apiRootResource = new ClouderaManagerClientBuilder().withHost(cmServerHost)
            .withPort(port).withUsernamePassword(user, password).build();
      this.apiResourceRootV3 = apiRootResource.getRootV3();
      this.apiResourceRootV4 = this.versionApi >= 4 ? apiRootResource.getRootV4() : null;
      this.apiResourceRootV5 = this.versionApi >= 5 ? apiRootResource.getRootV5() : null;
      this.apiResourceRootV6 = this.versionApi >= 6 ? apiRootResource.getRootV6() : null;
   }

   @Override
   public String getVersion() {
      return version;
   }

   @Override
   public int getVersionApi() {
      return versionApi;
   }

   @Override
   public int getVersionCdh() {
      return versionCdh;
   }

   @Override
   public boolean provision(CmClusterDef cluster) {

      boolean success = false;

      try {
         provisionManagement(cluster);
         if (!isProvisioned(cluster)) {
            provisionCluster(cluster);
            if (cluster.getIsParcel()) {
               provisionParcels(cluster);
            }
         }

      } catch (Exception e) {
         throw CmException.PROVISION_FAILED(cluster.getName());

      }

      return success;
   }

   @Override
   public boolean unprovision(CmClusterDef cluster) throws CmException {
      try {
         if (!cluster.isEmpty()) {
            if (isProvisioned(cluster)) {
               apiResourceRootV6.getClustersResource().deleteCluster(cluster.getName());
            }
         }
      } catch (Exception e) {
         throw new CmException("Failed to unprovision cluster");
      }
      return true;
   }

   @Override
   public boolean isProvisioned(CmClusterDef cluster) throws CmException {
      try {
         //apiResourceRootV6.getClustersResource().readCluster(clusterName);
         for (ApiCluster apiCluster : apiResourceRootV6.getClustersResource().readClusters(DataView.SUMMARY)) {
            if (apiCluster.getName().equals(cluster.getName())) {
               return true;
            }
         }
      } catch (Exception e) {
         throw CmException.UNSURE_CLUSTER_EXIST(cluster.getName());
      }
      return false;
   }

   @Override
   public boolean start(CmClusterDef cluster) throws CmException {
      return false;
   }

   @Override
   public boolean isStarted(CmClusterDef cluster) throws CmException {
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
            for (ApiRole apiRole : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .getRolesResource(apiService.getName()).readRoles()) {
               if (apiRole.getRoleState().equals(ApiRoleState.STARTED)) {
                  servicesNotStarted.remove(apiRole.getName());
               }
            }
         }

      } catch (Exception e) {
         //throw new CmServerException("Failed to detrermine if cluster is started", e);
         throw CmException.PROVISION_FAILED(cluster.getName());
      }
      return servicesNotStarted.isEmpty();
   }

   @Override
   public boolean stop(CmClusterDef cluster) throws CmException {
      try {
         if (!cluster.isEmpty()) {
            if (isConfigured(cluster) && !isStopped(cluster)) {
               execute(apiResourceRootV6.getClustersResource().stopCommand("cluster"));
            }
         }
      } catch (Exception e) {
         throw new CmException("Failed to stop cluster");
      }
      return true;
   }

   @Override
   public boolean isStopped(CmClusterDef cluster) throws CmException {
      final Set<String> servicesNotStopped = cluster.allServiceNames();
      try {
         if (isConfigured(cluster)) {
            for (ApiService apiService : apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName())
                  .readServices(DataView.SUMMARY)) {
               for (ApiRole apiRole : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                     .getRolesResource(apiService.getName()).readRoles()) {
                  if (apiRole.getRoleState().equals(ApiRoleState.STOPPED)) {
                     servicesNotStopped.remove(apiRole.getName());
                  }
               }
            }
         }
      } catch (Exception e) {
         //throw new CmServerException("Failed to detrermine if cluster is stopped", e);
         throw CmException.PROVISION_FAILED(cluster.getName());
      }
      return servicesNotStopped.isEmpty();
   }

   @Override
   public boolean configure(CmClusterDef cluster) throws CmException {
      boolean executed = false;
      try {
         logger.info("Start Configure cluster: " + cluster.getName());
         if (!cluster.isEmpty()) {
            if (!isProvisioned(cluster)) {
               provision(cluster);
            }
            if (!isConfigured(cluster)) {
               configureServices(cluster);
               isFirstStartRequired = true;
               executed = true;
            }
         }
         logger.info("Successfully configure cluster: " + cluster.getName());
      } catch (Exception e) {
         logger.info("Configure cluster failed: " + cluster.getName());
         throw new CmException("Failed to configure cluster");
      }

      return executed;
   }

   @Override
   public boolean isConfigured(CmClusterDef cluster) throws CmException {
      boolean executed = false;
      final Set<String> servicesNotConfigured = new HashSet<String>();
      try {
         if (isProvisioned(cluster)) {
            for (CmServiceDef serviceDef : cluster.getServices()) {
               servicesNotConfigured.add(serviceDef.getName());
            }

            for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .readServices(DataView.SUMMARY)) {
               for (ApiRole apiRole : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                     .getRolesResource(apiService.getName()).readRoles()) {
                  servicesNotConfigured.remove(apiRole.getName());
               }
            }
            executed = true;
         }

      } catch (Exception e) {
         //throw new CmServerException("Failed to detrermine if cluster is configured", e);
         throw CmException.PROVISION_FAILED(cluster.getName());
      }
      return executed && servicesNotConfigured.size() == 0;
   }

   @Override
   public boolean unconfigure(CmClusterDef cluster) throws CmException {
      return false;
   }

   @Override
   public boolean initialize(CmClusterDef cluster) throws CmException {
      boolean executed = false;
      try {

         logger.info("Cluster Initializing");

         /*
         Map<String, String> configuration = cluster.getServiceConfiguration(versionApi).get(
               CmMgmtServiceType.CM.getId());
         configuration.remove("cm_database_name");
         configuration.remove("cm_database_type");
         executed = CmMgmtServiceType.CM.getId() != null
               && provisionCmSettings(configuration).size() >= configuration.size();
               */

         logger.info("Cluster Initialized");

      } catch (Exception e) {
         logger.error("Cluster Initialize failed");
         throw new CmException("Failed to initialise cluster");
      }

      return executed;
   }

   private Map<String, String> provisionCmSettings(Map<String, String> config) throws InterruptedException {

      Map<String, String> configPostUpdate = new HashMap<String, String>();
      ApiConfigList apiConfigList = new ApiConfigList();
      if (config != null && !config.isEmpty()) {
         for (String key : config.keySet()) {
            apiConfigList.add(new ApiConfig(key, config.get(key)));
         }
         apiResourceRootV6.getClouderaManagerResource().updateConfig(apiConfigList);
      }
      apiConfigList = apiResourceRootV6.getClouderaManagerResource().getConfig(DataView.SUMMARY);
      for (ApiConfig apiConfig : apiConfigList) {
         configPostUpdate.put(apiConfig.getName(), apiConfig.getValue());
      }

      return configPostUpdate;

   }

   @Override
   public boolean getServiceConfigs(CmClusterDef cluster, File path) throws CmException {
      return false;
   }

   public void provisionManagement(final CmClusterDef cluster) {

      boolean cmsProvisionRequired = false;
      try {
         cmsProvisionRequired = apiResourceRootV6.getClouderaManagerResource().getMgmtServiceResource()
               .readService(DataView.SUMMARY) == null;
         logger.info("cmsProvisionRequired: " + cmsProvisionRequired);
         System.out.println("cmsProvisionRequired: " + cmsProvisionRequired);
      } catch (RuntimeException e) {
         cmsProvisionRequired = true;
      }

      if (cmsProvisionRequired) {
         final ApiHostRef apiHostRef = new ApiHostRef("cmCluster-master-0");
         boolean licenseDeployed = false;

         try {
            licenseDeployed = apiResourceRootV6.getClouderaManagerResource().readLicense() != null;
            logger.info("licenseDeployed: " + licenseDeployed);
            System.out.println("licenseDeployed: " + licenseDeployed);
         } catch (Exception e) {
            // ignore
         }
         if (versionApi >= 7 && !licenseDeployed) {
            apiResourceRootV6.getClouderaManagerResource().beginTrial();
            licenseDeployed = true;
         }
         final boolean enterpriseDeployed = licenseDeployed;

         if (versionApi >= 4 || licenseDeployed) {
            logger.info("Start provisioning CM mgmt services");
            System.out.println("Start provisioning CM mgmt services");
            ApiService cmsServiceApi = new ApiService();
            List<ApiRole> cmsRoleApis = new ArrayList<ApiRole>();
            cmsServiceApi.setName(CmMgmtServiceType.MANAGEMENT.getName());
            cmsServiceApi.setType(CmMgmtServiceType.MANAGEMENT.getId());

            for (CmMgmtServiceType type : CmMgmtServiceType.values()) {
               if (type.getParent() != null && (!type.requireEnterprise() || enterpriseDeployed)) {
                  ApiRole cmsRoleApi = new ApiRole();
                  cmsRoleApi.setName(type.getName());
                  cmsRoleApi.setType(type.getId());
                  cmsRoleApi.setHostRef(apiHostRef);
                  cmsRoleApis.add(cmsRoleApi);
               }
            }
            cmsServiceApi.setRoles(cmsRoleApis);
            logger.info("cmsService to setup: " + cmsServiceApi.toString());
            System.out.println("cmsService to setup: " + cmsServiceApi.toString());
            apiResourceRootV6.getClouderaManagerResource().getMgmtServiceResource().setupCMS(cmsServiceApi);
            logger.info("Finished setup CMS service");

            /* TODO: currently only use default "ApiRoleConfigGroup", support customized configGroup in future
            A role config group contains roles of the same role type sharing the same configuration.
            While each role has to belong to a group, a role config group may be empty. There exists
            a default role config group for each role type. Default groups cannot be removed nor created.
            The name of a role config group is unique and cannot be changed. The configuration of individual
            roles may be overridden on role level.
            */
         }
      }
   }

   public void addHosts(final CmClusterDef cluster) throws Exception {
      List<ApiHost> apiHosts = new ArrayList<ApiHost>();
      List<String> Ips = new ArrayList<String>();
      List<String> hostnames = new ArrayList<String>();

      Set<String> existIps = new HashSet<String>();
      for (ApiHost apiHost : apiResourceRootV6.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
         existIps.add(apiHost.getIpAddress());
      }

      for (CmNodeDef node : cluster.getNodes()) {
         if (existIps.contains(node.getIpAddress())) {
            continue;
         }
         apiHosts.add(node.toCmHost());
         Ips.add(node.getIpAddress());
         hostnames.add(node.getFqdn());
      }

      //apiResourceRootV6.getHostsResource().createHosts(new ApiHostList(apiHosts));

      ApiHostInstallArguments apiHostInstallArguments = new ApiHostInstallArguments();
      //apiHostInstallArguments.setHostNames(Ips);
      apiHostInstallArguments.setHostNames(hostnames);
      apiHostInstallArguments.setSshPort(22);
      apiHostInstallArguments.setUserName("serengeti");
      apiHostInstallArguments.setPassword("password");
      apiHostInstallArguments.setParallelInstallCount(20);

      //apiResourceRootV6.getClouderaManagerResource()
      // Install CM agents. TODO: show steps msg
      execute("InstallHosts", apiResourceRootV6.getClouderaManagerResource().hostInstallCommand(apiHostInstallArguments));
   }

   public void deleteHosts(final CmClusterDef cluster) throws Exception {
      ApiHostRefList hosts = apiResourceRootV6.getClustersResource().removeAllHosts(cluster.getName());

      for (ApiHostRef host : hosts.getHosts()) {
         apiResourceRootV6.getHostsResource().deleteHost(host.getHostId());
      }
   }

   public void provisionCluster(final CmClusterDef cluster) throws Exception {

      execute(apiResourceRootV6.getClouderaManagerResource().inspectHostsCommand());

      final ApiClusterList clusterList = new ApiClusterList();
      ApiCluster apiCluster = new ApiCluster();
      apiCluster.setName(cluster.getName());
      apiCluster.setVersion(ApiClusterVersion.valueOf(cluster.getVersion()));
      apiCluster.setFullVersion(cluster.getFullVersion());
      clusterList.add(apiCluster);

      apiResourceRootV6.getClustersResource().createClusters(clusterList);

      List<ApiHost> apiHosts = new ArrayList<ApiHost>();
      List<ApiHostRef> apiHostRefs = new ArrayList<ApiHostRef>();

      for (CmNodeDef node : cluster.getNodes()) {
         apiHostRefs.add(new ApiHostRef(node.getNodeId()));
      }

      logger.info("apiHostRefs: " + apiHostRefs.toString());

      // Add hosts to this cluster
      apiResourceRootV6.getClustersResource().addHosts(cluster.getName(), new ApiHostRefList(apiHostRefs));
   }


   /*
   A Parcel encapsulate a specific product and version. For example, (CDH 4.1).
   A parcel is downloaded, distributed to all the machines of a cluster and then allowed to be activated.
    */
   public void provisionParcels(final CmClusterDef cluster) throws InterruptedException, IOException {

      apiResourceRootV6.getClouderaManagerResource().updateConfig(
            new ApiConfigList(Arrays.asList(new ApiConfig[]{new ApiConfig("PARCEL_UPDATE_FREQ", "1")})));

      final Set<String> repositoriesRequired = new HashSet<String>();

      for (CmServiceDef serviceDef : cluster.getServices()) {
         CmServiceRoleType serviceType = CmServiceRoleType.valueOf(serviceDef.getType());
         repositoriesRequired.add(serviceType.getRepository().toString(cluster.getVersion()));
      }

      logger.info("parcel repo required: " + repositoriesRequired + " cluster: " + cluster.getName());
      System.out.println("parcel repo required: " + repositoriesRequired + " cluster: " + cluster.getName());

      final List<String> repositoriesRequiredOrdered = new ArrayList<String>();
      for (String repository : repositoriesRequired) {
         if (repository.equals(Constants.CDH_REPO_PREFIX)) {
            repositoriesRequiredOrdered.add(0, repository);
         } else {
            repositoriesRequiredOrdered.add(repository);
         }
      }

      // validate this cluster has access to all Parcels it requires
      execute("WaitForParcelsAvailability", new Callback() {
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
      });

      apiResourceRootV6.getClouderaManagerResource().updateConfig(
            new ApiConfigList(Arrays.asList(new ApiConfig[] { new ApiConfig("PARCEL_UPDATE_FREQ", "60") })));

      for (String repository : repositoriesRequiredOrdered) {
         DefaultArtifactVersion parcelVersion = null;
         for (ApiParcel apiParcel : apiResourceRootV6.getClustersResource().getParcelsResource(cluster.getName())
               .readParcels(DataView.FULL).getParcels()) {
            DefaultArtifactVersion parcelVersionTmp = new DefaultArtifactVersion(apiParcel.getVersion());
            if (apiParcel.getProduct().equals(repository)) {
               if (!apiParcel.getProduct().equals(Constants.CDH_REPO_PREFIX) || versionCdh == parcelVersionTmp.getMajorVersion()) {
                  if (parcelVersion == null || parcelVersion.compareTo(parcelVersionTmp) < 0) {
                     parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
                  }
               }
            }
         }

         logger.info("exact parcel version: " + parcelVersion);
         System.out.println("exact parcel version: " + parcelVersion);

         final ParcelResource apiParcelResource = apiResourceRootV6.getClustersResource()
               .getParcelsResource(cluster.getName()).getParcelResource(repository, parcelVersion.toString());
         if (CmParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < CmParcelStage.DOWNLOADED.ordinal()) {
            System.out.println("Downloading...");
            execute(apiParcelResource.startDownloadCommand(), new Callback() {
               @Override
               public boolean poll() {
                  // TODO: timeout
                  return apiParcelResource.readParcel().getStage().equals(CM_PARCEL_STAGE_DOWNLOADED);
                  // TODO: retrieve status
                  //apiParcelResource.readParcel().getState();
               }
            }, false);
         }
         if (CmParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < CmParcelStage.DISTRIBUTED.ordinal()) {
            System.out.println("Distributing....");
            execute(apiParcelResource.startDistributionCommand(), new Callback() {
               @Override
               public boolean poll() {
                  return apiParcelResource.readParcel().getStage().equals(CM_PARCEL_STAGE_DISTRIBUTED);
               }
            }, false);
         }
         if (CmParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < CmParcelStage.ACTIVATED.ordinal()) {
            System.out.println("Activating....");
            execute(apiParcelResource.activateCommand(), new Callback() {
               @Override
               public boolean poll() {
                  return apiParcelResource.readParcel().getStage().equals(CM_PARCEL_STAGE_ACTIVATED);
               }
            }, false);
         }
      }
   }

   public void configureServices2(final CmClusterDef cluster) throws Exception {

       ApiServiceList serviceList = new ApiServiceList();

      for (CmServiceDef serviceDef : cluster.getServices()) {
         CmServiceRoleType type = CmServiceRoleType.valueOf(serviceDef.getType());
         ApiService apiService = new ApiService();
         apiService.setType(type.getId());
         apiService.setName(serviceDef.getName());
         apiService.setDisplayName(serviceDef.getDisplayName());

         serviceList.add(apiService);
      }

      System.out.println(serviceList);
      apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).createServices(serviceList);
   }

   public void configureRoles(final CmClusterDef cluster) throws Exception {
      for (CmServiceDef serviceDef : cluster.getServices()) {
         List<ApiRole> apiRoles = new ArrayList<ApiRole>();
         for (CmRoleDef roleDef : serviceDef.getRoles()) {
            ApiRole apiRole = new ApiRole();
            //apiRole.setName(roleDef.getName());
            apiRole.setType(roleDef.getType());
            apiRole.setHostRef(new ApiHostRef(roleDef.getNodeRef()));
            // TODO: user defined configurations;
            apiRoles.add(apiRole);
         }
         System.out.println(apiRoles);
         apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).getRolesResource(serviceDef.getName()).createRoles(new ApiRoleList(apiRoles));
      }
   }

   public void configureServices(final CmClusterDef cluster) throws Exception {
      //final List<CmServerService> services = getServiceHosts();
      logger.info("CreateClusterServices");
      ApiServiceList serviceList = new ApiServiceList();

      for (CmServiceDef serviceDef : cluster.getServices()) {
         CmServiceRoleType type = CmServiceRoleType.valueOf(serviceDef.getType());
         ApiService apiService = new ApiService();
         apiService.setType(type.getId());
         apiService.setName(serviceDef.getName());
         apiService.setDisplayName(serviceDef.getDisplayName());

         ApiServiceConfig apiServiceConfig = new ApiServiceConfig();
         // TODO: support user defined configs

         // config service dependencies
         Set<CmServiceRoleType> serviceTypes = cluster.allServiceTypes();
         switch (type) {
            case YARN: //TODO: Compute Only
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               break;
            case MAPREDUCE: //TODO: Compute Only
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               break;
            case HBASE: //TODO: HBase only
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster.serviceNameOfType(CmServiceRoleType.ZOOKEEPER)));
               break;
            case SOLR:
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster.serviceNameOfType(CmServiceRoleType.ZOOKEEPER)));
               break;
            case SOLR_INDEXER:
               apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType(CmServiceRoleType.HBASE)));
               apiServiceConfig.add(new ApiConfig("solr_service", cluster.serviceNameOfType(CmServiceRoleType.SOLR)));
               break;
            case HUE:
               apiServiceConfig.add(new ApiConfig("hue_webhdfs", cluster.serviceNameOfType(CmServiceRoleType.HDFS_HTTP_FS)));
               apiServiceConfig.add(new ApiConfig("oozie_service", cluster.serviceNameOfType(CmServiceRoleType.OOZIE)));
               apiServiceConfig.add(new ApiConfig("hive_service", cluster.serviceNameOfType(CmServiceRoleType.HIVE)));
               if (serviceTypes.contains(CmServiceRoleType.HBASE)) {
                  apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType(CmServiceRoleType.HBASE)));
               }
               if (serviceTypes.contains(CmServiceRoleType.IMPALA)) {
                  apiServiceConfig.add(new ApiConfig("impala_service", cluster.serviceNameOfType(CmServiceRoleType.IMPALA)));
               }
               if (serviceTypes.contains(CmServiceRoleType.SOLR)) {
                  apiServiceConfig.add(new ApiConfig("solr_service", cluster.serviceNameOfType(CmServiceRoleType.SOLR)));
               }
               if (serviceTypes.contains(CmServiceRoleType.SQOOP)) {
                  apiServiceConfig.add(new ApiConfig("sqoop_service", cluster.serviceNameOfType(CmServiceRoleType.SQOOP)));
               }
               if (serviceTypes.contains(CmServiceRoleType.HBASE_THRIFT_SERVER)) {
                  apiServiceConfig.add(new ApiConfig("hue_hbase_thrift", cluster
                        .serviceNameOfType(CmServiceRoleType.HBASE_THRIFT_SERVER)));
               }
               break;
            case SQOOP:
               apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", serviceTypes
                     .contains(CmServiceRoleType.YARN) ? cluster.serviceNameOfType(CmServiceRoleType.YARN) : cluster
                     .serviceNameOfType(CmServiceRoleType.MAPREDUCE)));
               break;
            case OOZIE:
               apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", serviceTypes
                     .contains(CmServiceRoleType.YARN) ? cluster.serviceNameOfType(CmServiceRoleType.YARN) : cluster
                     .serviceNameOfType(CmServiceRoleType.MAPREDUCE)));
               break;
            case HIVE:
               apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", serviceTypes
                     .contains(CmServiceRoleType.YARN) ? cluster.serviceNameOfType(CmServiceRoleType.YARN) : cluster
                     .serviceNameOfType(CmServiceRoleType.MAPREDUCE)));
               if (versionApi >= 4) {
                  apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster
                        .serviceNameOfType(CmServiceRoleType.ZOOKEEPER)));
               }
               break;
            case IMPALA:
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType(CmServiceRoleType.HBASE)));
               apiServiceConfig.add(new ApiConfig("hive_service", cluster.serviceNameOfType(CmServiceRoleType.HIVE)));
               break;
            case FLUME:
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType(CmServiceRoleType.HBASE)));
            default:
               break;
         }
         apiService.setConfig(apiServiceConfig);

         List<ApiRole> apiRoles = new ArrayList<ApiRole>();
         for (CmRoleDef roleDef : serviceDef.getRoles()) {
            ApiRole apiRole = new ApiRole();
            //apiRole.setName(roleDef.getName());
            apiRole.setType(roleDef.getType());
            apiRole.setHostRef(new ApiHostRef(roleDef.getNodeRef()));
            // TODO: user defined configurations;
            apiRoles.add(apiRole);
         }

         apiService.setRoles(apiRoles);
         serviceList.add(apiService);
      }

      System.out.println(serviceList);
      apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).createServices(serviceList);
      logger.info("Finished create services");
      System.out.println("Finished create services");

      // TODO: role config group

      // Necessary, since createServices a habit of kicking off async commands (eg ZkAutoInit )
      for (CmServiceRoleType type : cluster.allServiceTypes()) {
         for (ApiCommand command : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
               .listActiveCommands(cluster.serviceNameOfType(type), DataView.SUMMARY)) {
            execute(command, false);
         }
      }

      //execute(apiResourceRootV6.getClustersResource().deployClientConfig(cluster.getName()));
   }

   public void unconfigureServices(final CmClusterDef cluster) throws Exception {
      final Set<CmServiceRoleType> types = new TreeSet<CmServiceRoleType>(Collections.reverseOrder());
      types.addAll(cluster.allServiceTypes());
      for (String serviceName : cluster.allServiceNames()) {
         ServicesResourceV6 servicesResource = apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName());
         System.out.println(servicesResource);
         if (servicesResource != null) {
            servicesResource.deleteService(serviceName);
         }
      }
   }

   private ApiCommand execute(final ApiBulkCommandList bulkCommand, boolean checkReturn) throws InterruptedException {
      ApiCommand lastCommand = null;
      for (ApiCommand command : bulkCommand) {
         lastCommand = execute(command, checkReturn);
      }
      return lastCommand;
   }

   private ApiCommand execute(final ApiCommand command) throws InterruptedException {
      return execute(command, true);
   }

   private ApiCommand execute(String label, final ApiCommand command) throws InterruptedException {
      return execute(label, command, true);
   }

   private ApiCommand execute(final ApiCommand command, boolean checkReturn) throws InterruptedException {
      return execute(command.getName(), command, checkReturn);
   }

   private ApiCommand execute(String label, final ApiCommand command, boolean checkReturn) throws InterruptedException {
      return execute(label, command, new Callback() {
         @Override
         public boolean poll() {
            return apiResourceRootV6.getCommandsResource().readCommand(command.getId()).getEndTime() != null;
            //apiResourceRootV6.getCommandsResource().readCommand(command.getId()).getResultDataUrl();
         }
      }, checkReturn);
   }

   private ApiCommand execute(ApiCommand command, Callback callback, boolean checkReturn) throws InterruptedException {
      return execute(command.getName(), command, callback, checkReturn);
   }

   private ApiCommand execute(String label, Callback callback) throws InterruptedException {
      return execute(label, null, callback, false);
   }

   private ApiCommand execute(String label, ApiCommand command, Callback callback, boolean checkReturn)
         throws InterruptedException {
      label = WordUtils.capitalize(label.replace("-", " ").replace("_", " ")).replace(" ", "");
      logger.info("label: " + label);
      if (command != null) {
         logger.info("Waiting for command: " + label);
      }
      ApiCommand commandReturn = null;
      while (true) {
         if (callback.poll()) {
            if (checkReturn && command != null
                  && !(commandReturn = apiResourceRootV6.getCommandsResource().readCommand(command.getId())).getSuccess()) {
               logger.info("Failed to run command: " + command);
               throw new RuntimeException("Command [" + command + "] failed [" + commandReturn + "]");
            }
            return commandReturn;
         }
         Thread.sleep(API_POLL_PERIOD_MS);
      }
   }

   private static abstract class Callback {
      public abstract boolean poll();
   }
}
