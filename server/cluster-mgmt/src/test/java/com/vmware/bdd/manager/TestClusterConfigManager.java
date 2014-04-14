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
package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mockit;

import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterType;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.apitypes.NodeGroup.InstanceType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.dal.IServerInfoDAO;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.ServerInfoEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.DistroManager.PackagesExistStatus;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.MockVcVmUtil;
import com.vmware.bdd.service.impl.ClusteringService;
import com.vmware.bdd.service.resmgmt.IDatastoreService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.service.resmgmt.impl.ResourceInitializerService;
import com.vmware.bdd.specpolicy.ClusterSpecFactory;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.ChefServerUtils;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.TestResourceCleanupUtils;

public class TestClusterConfigManager {
   private static final Logger logger = Logger
         .getLogger(TestClusterConfigManager.class);
   private static ClusterConfigManager clusterConfigMgr;

   private static TestResourceCleanupUtils cleanUpUtils;
   private static IClusterEntityManager clusterEntityMgr;

   private static Gson gson = new GsonBuilder()
         .excludeFieldsWithoutExposeAnnotation().create();

   private ResourceInitializerService service;
   private IServerInfoDAO serverInfoDao;

   @BeforeClass
   public void beforeClass() {
      service = new ResourceInitializerService();
      serverInfoDao = new MockUp<IServerInfoDAO>() {
         @Mock
         List<ServerInfoEntity> findAll() {
            List<ServerInfoEntity> serverInfos =
                  new ArrayList<ServerInfoEntity>();
            ServerInfoEntity serverInfo = new ServerInfoEntity();
            serverInfo.setResourceInitialized(true);
            serverInfos.add(serverInfo);
            return serverInfos;
         }
      }.getMockInstance();
      service.setServerInfoDao(serverInfoDao);
   }

   @AfterMethod(groups = { "TestClusterConfigManager" })
   public void tearDownMockup() {
      Mockit.tearDownMocks();
   }

   @BeforeMethod(groups = { "TestClusterConfigManager" })
   public void setMockup() {
      Mockit.setUpMock(MockResourceManager.class);
      Mockit.setUpMock(MockVcVmUtil.class);
   }

   @AfterClass(groups = { "TestClusterConfigManager" })
   public static void tearDown() {
      cleanupResources();
   }

   public static void mockChefServerRoles() {
      List<String> rolesList =
            Arrays.asList("hadoop", "hadoop_client", "hadoop_datanode",
                  "hadoop_initial_bootstrap", "hadoop_jobtracker",
                  "hadoop_journalnode", "hadoop_master", "hadoop_namenode",
                  "hadoop_nodemanager", "hadoop_resourcemanager",
                  "hadoop_secondarynamenode", "hadoop_tasktracker",
                  "hadoop_worker", "hawq-cluster", "hawq-master-facet",
                  "hawq-worker-facet", "hawq_master", "hawq_worker",
                  "hbase_client", "hbase_master", "hbase_regionserver", "hive",
                  "hive_server", "mapr", "mapr_cldb", "mapr_client",
                  "mapr_fileserver", "mapr_hbase_client", "mapr_hbase_master",
                  "mapr_hbase_regionserver", "mapr_hive", "mapr_hive_server",
                  "mapr_jobtracker", "mapr_metrics", "mapr_mysql_server",
                  "mapr_nfs", "mapr_pig", "mapr_tasktracker", "mapr_webserver",
                  "mapr_zookeeper", "pig", "postgresql_server",
                  "tempfs_client", "tempfs_server", "zookeeper");
      HashSet<String> roles = new HashSet<String>();
      roles.addAll(rolesList);
      ChefServerUtils.setAllRoles(roles);
   }

   @BeforeClass(groups = { "TestClusterConfigManager" }, dependsOnGroups = { "TestClusteringService" })
   public static void setup() {
      Mockit.setUpMock(MockResourceManager.class);
      ApplicationContext context =
            new FileSystemXmlApplicationContext(
                  "../serengeti/WebContent/WEB-INF/spring/root-context.xml",
                  "../serengeti/WebContent/WEB-INF/spring/datasource-context.xml",
                  "../serengeti/WebContent/WEB-INF/spring/spring-batch-context.xml",
                  "../serengeti/WebContent/WEB-INF/spring/tx-context.xml",
                  "../serengeti/WebContent/WEB-INF/spring/serengeti-jobs-context.xml",
                  "../serengeti/WebContent/WEB-INF/spring/manager-context.xml");
      clusterConfigMgr = context.getBean(ClusterConfigManager.class);
      DistroManager distroMgr = Mockito.mock(DistroManager.class);
      ClusteringService clusteringService =
            Mockito.mock(ClusteringService.class);
      mockChefServerRoles();
      clusterConfigMgr.setDistroMgr(distroMgr);
      clusterConfigMgr.setClusteringService(clusteringService);
      clusterEntityMgr =
            context
                  .getBean("clusterEntityManager", IClusterEntityManager.class);
      DistroRead distro = new DistroRead();
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_namenode");
      roles.add("hadoop_jobtracker");
      roles.add("hadoop_datanode");
      roles.add("hadoop_tasktracker");
      roles.add("hadoop_client");
      roles.add("hive");
      roles.add("hive_server");
      roles.add("pig");
      distro.setRoles(roles);
      Mockito.when(clusteringService.getTemplateVmId()).thenReturn("vm-1234");
      Mockito.when(clusteringService.getTemplateVmName()).thenReturn(
            "hadoop-template");
      Mockito.when(distroMgr.getDistroByName("apache")).thenReturn(distro);
      Mockito.when(distroMgr.checkPackagesExistStatus("apache")).thenReturn(
            PackagesExistStatus.TARBALL);
      Mockito.when(
            distroMgr.getPackageUrlByDistroRole("apache",
                  HadoopRole.HADOOP_NAMENODE_ROLE.toString())).thenReturn(
            "http://localhost/distros/apache/1.0.1/hadoop-1.0.1.tar.gz");
      Mockito.when(
            distroMgr.getPackageUrlByDistroRole("apache",
                  HadoopRole.HIVE_ROLE.toString())).thenReturn(
            "http://localhost/distros/apache/1.0.1/hive-0.8.1.tar.gz");
      Mockito.when(
            distroMgr.getPackageUrlByDistroRole("apache",
                  HadoopRole.PIG_ROLE.toString())).thenReturn(
            "http://localhost/distros/apache/1.0.1/pig-0.9.2.tar.gz");
      Mockito.when(
            distroMgr.getPackageUrlByDistroRole("apache",
                  HadoopRole.HBASE_MASTER_ROLE.toString())).thenReturn(
            "http://localhost/distros/apache/1.0.1/hbase-0.94.0.tar.gz");
      Mockito.when(
            distroMgr.getPackageUrlByDistroRole("apache",
                  HadoopRole.ZOOKEEPER_ROLE.toString())).thenReturn(
            "http://localhost/distros/apache/1.0.1/zookeeper-3.4.3.tar.gz");
      IResourcePoolService resPoolSvc =
            context.getBean("resourcePoolService", IResourcePoolService.class);
      IDatastoreService dsSvc =
            context.getBean("datastoreService", IDatastoreService.class);
      INetworkService netSvc =
            context.getBean("networkService", INetworkService.class);
      cleanUpUtils = new TestResourceCleanupUtils();
      cleanUpUtils.setDsSvc(dsSvc);
      cleanUpUtils.setNetSvc(netSvc);
      cleanUpUtils.setResPoolSvc(resPoolSvc);
      cleanupResources();

      try {
         Set<String> rpNames = resPoolSvc.getAllRPNames();
         logger.info("available resource pools: " + rpNames);
         resPoolSvc.addResourcePool("myRp1", "cluster1", "rp1");
      } catch (Exception e) {
         logger.error("ignore create resource pool myRp1 exception. ", e);
      }
      try {
         resPoolSvc.addResourcePool("myRp2", "cluster1", "rp2");
      } catch (Exception e) {
         logger.error("ignore create resource pool myRp2 exception. ", e);
      }
      try {
         resPoolSvc.addResourcePool("myRp3", "cluster2", "rp1");
      } catch (Exception e) {
         logger.error("ignore create resource pool myRp3 exception. ", e);
      }
      try {
         resPoolSvc.addResourcePool("myRp4", "cluster2", "rp2");
      } catch (Exception e) {
         logger.error("ignore create resource pool myRp4 exception. ", e);
      }
      try {
         resPoolSvc.addResourcePool("myRp5", "cluster4", "rp1");
      } catch (Exception e) {
         logger.error("ignore create resource pool myRp5 exception. ", e);
      }
      try {
         resPoolSvc.addResourcePool("myRp6", "cluster4", "rp2");
      } catch (Exception e) {
         logger.error("ignore create resource pool myRp6 exception. ", e);
      }
      try {
         netSvc.addDhcpNetwork("dhcpNet1", "CFNetwork");
      } catch (Exception e) {
         logger.error("ignore create network dhcpNet1 exception. ", e);
      }
      List<String> sharedStores = new ArrayList<String>();
      sharedStores.add("share1");
      sharedStores.add("share2");
      try {
         clusterConfigMgr.getDatastoreMgr().addDatastores("testSharedStore",
               DatastoreType.SHARED, sharedStores, false);
      } catch (Exception e) {
         logger.error("ignore create datastore testSharedStore exception. ", e);
      }
      List<String> localStores = new ArrayList<String>();
      localStores.add("local1");
      localStores.add("vmfs*");
      try {
         clusterConfigMgr.getDatastoreMgr().addDatastores("testLocalStore",
               DatastoreType.LOCAL, localStores, false);
      } catch (Exception e) {
         logger.error("ignore create datastore testLocalStore exception. ", e);
      }
      List<IpBlock> ipBlocks = new ArrayList<IpBlock>();
      IpBlock ip1 = new IpBlock();
      ip1.setBeginIp("192.168.1.1");
      ip1.setEndIp("192.168.1.3");
      ipBlocks.add(ip1);
      IpBlock ip2 = new IpBlock();
      ip2.setBeginIp("192.168.1.102");
      ip2.setEndIp("192.168.1.102");
      ipBlocks.add(ip2);
      IpBlock ip3 = new IpBlock();
      ip3.setBeginIp("192.168.1.104");
      ip3.setEndIp("192.168.1.204");
      ipBlocks.add(ip3);
      try {
         netSvc.addIpPoolNetwork("ipPool1", "CFNetwork1", "255.255.0.0",
               "192.168.1.254", "2.2.2.2", null, ipBlocks);
      } catch (Exception e) {
         logger.error("ignore create network ipPool1 exception. ", e);
      }
   }

   private static void cleanupResources() {
      cleanUpUtils.removeClusters(clusterEntityMgr, "my-cluster.*");
      cleanUpUtils.removeRPs("myRp.");
      cleanUpUtils.removeDatastore("testSharedStore");
      cleanUpUtils.removeDatastore("testLocalStore");
      cleanUpUtils.removeNetwork("dhcpNet1");
      cleanUpUtils.removeNetwork("ipPool1");
   }

   @Test(groups = { "TestClusterConfigManager" })
   public void testString() {
      String s1 =
            "{\"name\":\"my-cluster\",\"groups\":[{\"name\":\"master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"cpu\":2,\"memory\":7500,\"storage\":{\"type\":\"shared\",\"size\":50},\"ha\":\"on\"},{\"name\":\"worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"cpu\":1,\"memory\":3748,\"storage\":{\"type\":\"local\",\"size\":50},\"ha\":\"off\"},{\"name\":\"client\",\"roles\":[\"hive\",\"hadoop_client\",\"pig\"],\"instance_num\":1,\"cpu\":1,\"memory\":3748,\"storage\":{\"type\":\"shared\",\"size\":50},\"ha\":\"off\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}],\"distro_map\":{\"hadoop\":\"http://localhost/distros/apache/1.0.1/hadoop-1.0.1.tar.gz\",\"pig\":\"http://localhost/distros/apache/1.0.1/pig-0.9.2.tar.gz\",\"hive\":\"http://localhost/distros/apache/1.0.1/hive-0.8.1.tar.gz\"},\"vc_shared_datastore_pattern\":[\"share1\",\"share2\"],\"vc_local_datastore_pattern\":[\"vmfs*\",\"local1\"]}";
      String s2 =
            "{\"name\":\"my-cluster\",\"groups\":[{\"name\":\"master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\"},{\"name\":\"worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\"},{\"name\":\"client\",\"roles\":[\"hive\",\"hadoop_client\",\"pig\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}],\"distro_map\":{\"hadoop\":\"http://localhost/distros/apache/1.0.1/hadoop-1.0.1.tar.gz\",\"pig\":\"http://localhost/distros/apache/1.0.1/pig-0.9.2.tar.gz\",\"hive\":\"http://localhost/distros/apache/1.0.1/hive-0.8.1.tar.gz\"},\"vc_shared_datastore_pattern\":[\"share1\",\"share2\"],\"vc_local_datastore_pattern\":[\"vmfs*\",\"local1\"]}";
      System.out.println("string length: " + s1.length());
      int i = 0;
      for (; i < s1.length(); i++) {
         if (s1.charAt(i) != s2.charAt(i)) {
            System.out.println("different at " + i);
            break;
         }
      }

      System.out.println("sub string: " + s1.substring(i));
   }

   private Map<NetTrafficType, List<String>> createNetConfigs() {
      Map<NetTrafficType, List<String>> netConfigs =
            new HashMap<NetTrafficType, List<String>>();
      List<String> netConfig = new ArrayList<String>();
      netConfig.add("dhcpNet1");
      netConfigs.put(NetTrafficType.MGT_NETWORK, netConfig);
      return netConfigs;
   }

   @Test(groups = { "TestClusterConfigManager" })
   public void testClusterConfig() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkConfig(createNetConfigs());
      spec.setDistro("apache");
      spec.setDistroVendor(Constants.DEFAULT_VENDOR);
      spec.setType(ClusterType.HDFS_MAPRED);
      spec = ClusterSpecFactory.getCustomizedSpec(spec);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findClusterById(1l);
      List<ClusterEntity> cs = clusterEntityMgr.findAllClusters();
      for (ClusterEntity c : cs) {
         System.out.println(c.getId());
      }
      cluster = clusterEntityMgr.findByName("my-cluster");
      Assert.assertTrue(cluster != null);
      ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(manifest.indexOf("master") != -1,
            "manifest should contains nodegroups");
   }

   @Test(groups = { "TestClusterConfigManager" })
   public void testCDHMapReduceV2CreateDefaultSpec() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkConfig(createNetConfigs());
      spec.setDistro("cdh4");
      spec.setDistroVendor(Constants.CDH_VENDOR);
      spec.setDistroVersion("4.4.0");
      spec.setType(ClusterType.HDFS_MAPRED);
      ClusterCreate newSpec = ClusterSpecFactory.getCustomizedSpec(spec);
      Assert.assertTrue(newSpec.getNodeGroups().length == 3);
      List<String> masterRoles = newSpec.getNodeGroups()[0].getRoles();
      Assert.assertTrue(
            masterRoles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString()),
            "expected role " + HadoopRole.HADOOP_JOBTRACKER_ROLE.toString()
            + ", but got " + masterRoles);

      spec.setDistro("cdh5");
      spec.setDistroVersion("5.0.0");
      newSpec = ClusterSpecFactory.getCustomizedSpec(spec);
      Assert.assertTrue(newSpec.getNodeGroups().length == 3);
      masterRoles = newSpec.getNodeGroups()[0].getRoles();
      Assert.assertTrue(
            masterRoles.contains(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()),
            "expected role " + HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()
            + ", but got " + masterRoles);
   }

   @Test(groups = { "TestClusterConfigManager" })
   public void testHDPMapReduceV2CreateDefaultSpec() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkConfig(createNetConfigs());
      spec.setDistro("hdp1");
      spec.setDistroVendor(Constants.HDP_VENDOR);
      spec.setDistroVersion("1.3");
      spec.setType(ClusterType.HDFS_MAPRED);
      ClusterCreate newSpec = ClusterSpecFactory.getCustomizedSpec(spec);
      Assert.assertTrue(newSpec.getNodeGroups().length == 3);
      List<String> masterRoles = newSpec.getNodeGroups()[0].getRoles();
      Assert.assertTrue(
            masterRoles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString()),
            "expected role " + HadoopRole.HADOOP_JOBTRACKER_ROLE.toString()
            + ", but got " + masterRoles);

      spec.setDistro("hdp2");
      spec.setDistroVersion("2.0");
      newSpec = ClusterSpecFactory.getCustomizedSpec(spec);
      Assert.assertTrue(newSpec.getNodeGroups().length == 3);
      masterRoles = newSpec.getNodeGroups()[0].getRoles();
      Assert.assertTrue(
            masterRoles.contains(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()),
            "expected role " + HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()
            + ", but got " + masterRoles);
   }

   @Test(groups = { "TestClusterConfigManager" })
   public void testBigTopMapReduceV2CreateDefaultSpec() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkConfig(createNetConfigs());
      spec.setDistro("bigtop");
      spec.setDistroVendor(Constants.BIGTOP_VENDOR);
      spec.setDistroVersion("0.7");
      spec.setType(ClusterType.HDFS_MAPRED);
      ClusterCreate newSpec = ClusterSpecFactory.getCustomizedSpec(spec);
      Assert.assertTrue(newSpec.getNodeGroups().length == 3);
      List<String> masterRoles = newSpec.getNodeGroups()[0].getRoles();
      Assert.assertTrue(
            masterRoles.contains(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()),
            "expected role " + HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()
            + ", but got " + masterRoles);
   }

   @Test(groups = { "TestClusterConfigManager" })
   public void testIntelMapReduceV2CreateDefaultSpec() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkConfig(createNetConfigs());
      spec.setDistro("intel2");
      spec.setDistroVendor(Constants.INTEL_VENDOR);
      spec.setDistroVersion("2.6");
      spec.setType(ClusterType.HDFS_MAPRED);
      ClusterCreate newSpec = ClusterSpecFactory.getCustomizedSpec(spec);
      Assert.assertTrue(newSpec.getNodeGroups().length == 3);
      List<String> masterRoles = newSpec.getNodeGroups()[0].getRoles();
      Assert.assertTrue(
            masterRoles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString()),
            "expected role " + HadoopRole.HADOOP_JOBTRACKER_ROLE.toString()
            + ", but got " + masterRoles);

      spec.setDistro("intel3");
      spec.setDistroVersion("3.1");
      newSpec = ClusterSpecFactory.getCustomizedSpec(spec);
      Assert.assertTrue(newSpec.getNodeGroups().length == 3);
      masterRoles = newSpec.getNodeGroups()[0].getRoles();
      Assert.assertTrue(
            masterRoles.contains(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()),
            "expected role " + HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString()
            + ", but got " + masterRoles);
   }
   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Test(groups = { "TestClusterConfigManager" })
   public void testClusterConfigWithExternalHDFS() throws Exception {
      String[] hdfsArray =
            new String[] { "hdfs://168.192.0.70:8020",
                  "hdfs://168.192.0.71:8020", "hdfs://168.192.0.72:8020",
                  "hdfs://168.192.0.73:8020" };
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster-external-hdfs");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkConfig(createNetConfigs());
      spec.setDistro("apache");
      spec.setDistroVendor(Constants.DEFAULT_VENDOR);
      spec.setExternalHDFS(hdfsArray[0]);
      String clusterConfigJson =
            "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\""
                  + hdfsArray[1] + "\"}}}}";
      Map clusterConfig = (new Gson()).fromJson(clusterConfigJson, Map.class);
      spec.setConfiguration((Map<String, Object>) (clusterConfig
            .get("configuration")));
      //build a jobtracker group, two compute node groups.
      NodeGroupCreate ng0 = new NodeGroupCreate();
      List<String> jobtrackerRole = new ArrayList<String>();
      jobtrackerRole.add("hadoop_jobtracker");
      ng0.setRoles(jobtrackerRole);
      ng0.setName("jobtracker");
      ng0.setInstanceNum(1);
      ng0.setInstanceType(InstanceType.LARGE);
      String ng0ConfigJson =
            "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\""
                  + hdfsArray[2] + "\"}}}}";
      Map ng0Config = (new Gson()).fromJson(ng0ConfigJson, Map.class);
      ng0.setConfiguration((Map<String, Object>) (ng0Config
            .get("configuration")));

      NodeGroupCreate ng1 = new NodeGroupCreate();
      List<String> computeRoles = new ArrayList<String>();
      computeRoles.add("hadoop_tasktracker");
      ng1.setRoles(computeRoles);
      ng1.setName("compute1");
      ng1.setInstanceNum(4);
      ng1.setInstanceType(InstanceType.MEDIUM);
      StorageRead storage = new StorageRead();
      storage.setType("LOCAL");
      storage.setSizeGB(10);
      ng1.setStorage(storage);
      String ng1ConfigJson =
            "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\""
                  + hdfsArray[3] + "\"}}}}";
      Map ng1Config = (new Gson()).fromJson(ng1ConfigJson, Map.class);
      ng1.setConfiguration((Map<String, Object>) (ng1Config
            .get("configuration")));
      NodeGroupCreate ng2 = new NodeGroupCreate();
      ng2.setRoles(computeRoles);
      ng2.setName("compute2");
      ng2.setInstanceNum(2);
      ng2.setInstanceType(InstanceType.MEDIUM);
      StorageRead storageCompute = new StorageRead();
      storageCompute.setType("LOCAL");
      storageCompute.setSizeGB(10);
      ng2.setStorage(storageCompute);

      NodeGroupCreate[] ngs = new NodeGroupCreate[] { ng0, ng1, ng2 };
      spec.setNodeGroups(ngs);
      spec = ClusterSpecFactory.getCustomizedSpec(spec);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findClusterById(1l);
      List<ClusterEntity> cs = clusterEntityMgr.findAllClusters();
      for (ClusterEntity c : cs) {
         System.out.println(c.getId());
      }
      cluster = clusterEntityMgr.findByName("my-cluster-external-hdfs");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs =
            clusterConfigMgr.getClusterConfig("my-cluster-external-hdfs");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(
            Pattern.compile("([\\s\\S]*" + hdfsArray[0] + "[\\s\\S]*){3}")
                  .matcher(manifest).matches(),
            "\"fs.default.name\" must be coved with external HDFS uri in both of cluster and group configuration.");
      Assert.assertTrue(manifest.indexOf(hdfsArray[1]) == -1,
            "\"fs.default.name\" must be coved under the cluster level");
      Assert.assertTrue(manifest.indexOf(hdfsArray[2]) == -1,
            "\"fs.default.name\" must be coved under the node group 1 level");
      Assert.assertTrue(manifest.indexOf(hdfsArray[3]) == -1,
            "\"fs.default.name\" must be coved under the node group 2 level");

   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Test(groups = { "TestClusterConfigManager" })
   public void testClusterConfigWithExternalHDFSFailure() throws Exception {
      String[] hdfsArray =
            new String[] { "hdfs://168.192.0.70:8020",
                  "hdfs://168.192.0.71:8020", "hdfs://168.192.0.72:8020",
                  "hdfs://168.192.0.73:8020" };
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster-external-hdfs-failure");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkConfig(createNetConfigs());
      spec.setDistro("apache");
      spec.setDistroVendor(Constants.DEFAULT_VENDOR);
      String clusterConfigJson =
            "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\""
                  + hdfsArray[1] + "\"}}}}";
      Map clusterConfig = (new Gson()).fromJson(clusterConfigJson, Map.class);
      spec.setConfiguration((Map<String, Object>) (clusterConfig
            .get("configuration")));
      //build a master group, a compute node group and a datanode.
      NodeGroupCreate ng0 = new NodeGroupCreate();
      List<String> masterRole = new ArrayList<String>();
      masterRole.add("hadoop_namenode");
      masterRole.add("hadoop_jobtracker");
      ng0.setRoles(masterRole);
      ng0.setName("jobtracker");
      ng0.setInstanceNum(1);
      ng0.setInstanceType(InstanceType.LARGE);
      String ng0ConfigJson =
            "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\""
                  + hdfsArray[2] + "\"}}}}";
      Map ng0Config = (new Gson()).fromJson(ng0ConfigJson, Map.class);
      ng0.setConfiguration((Map<String, Object>) (ng0Config
            .get("configuration")));

      NodeGroupCreate ng1 = new NodeGroupCreate();
      List<String> computeRoles = new ArrayList<String>();
      computeRoles.add("hadoop_tasktracker");
      ng1.setRoles(computeRoles);
      ng1.setName("compute1");
      ng1.setInstanceNum(4);
      ng1.setCpuNum(2);
      ng1.setMemCapacityMB(7500);
      ng1.setInstanceType(InstanceType.MEDIUM);
      StorageRead storage = new StorageRead();
      storage.setType("LOCAL");
      storage.setSizeGB(10);
      ng1.setStorage(storage);
      String ng1ConfigJson =
            "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\""
                  + hdfsArray[3] + "\"}}}}";
      Map ng1Config = (new Gson()).fromJson(ng1ConfigJson, Map.class);
      ng1.setConfiguration((Map<String, Object>) (ng1Config
            .get("configuration")));
      NodeGroupCreate ng2 = new NodeGroupCreate();
      List<String> dataRoles = new ArrayList<String>();
      dataRoles.add("hadoop_datanode");
      ng2.setRoles(dataRoles);
      ng2.setName("data1");
      ng2.setInstanceNum(2);
      ng2.setInstanceType(InstanceType.MEDIUM);
      StorageRead storageCompute = new StorageRead();
      storageCompute.setType("LOCAL");
      storageCompute.setSizeGB(10);
      ng2.setStorage(storageCompute);

      NodeGroupCreate[] ngs = new NodeGroupCreate[] { ng0, ng1, ng2 };
      spec.setNodeGroups(ngs);
      spec = ClusterSpecFactory.getCustomizedSpec(spec);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findClusterById(1l);
      List<ClusterEntity> cs = clusterEntityMgr.findAllClusters();
      for (ClusterEntity c : cs) {
         System.out.println(c.getId());
      }
      cluster = clusterEntityMgr.findByName("my-cluster-external-hdfs-failure");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs =
            clusterConfigMgr
                  .getClusterConfig("my-cluster-external-hdfs-failure");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(
            Pattern.compile("([\\s\\S]*" + hdfsArray[0] + "[\\s\\S]*){3}")
                  .matcher(manifest).matches() == false,
            "\"fs.default.name\" must be coved with external HDFS uri in both of cluster and group configuration.");
      Assert.assertTrue(manifest.indexOf(hdfsArray[1]) != -1,
            "\"fs.default.name\" must be coved under the cluster level");
      Assert.assertTrue(manifest.indexOf(hdfsArray[2]) != -1,
            "\"fs.default.name\" must be coved under the node group 1 level");
      Assert.assertTrue(manifest.indexOf(hdfsArray[3]) != -1,
            "\"fs.default.name\" must be coved under the node group 2 level");

   }

   @Test(groups = { "TestClusterConfigManager" })
   public void testClusterConfigWithTempfs() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster-dc-tempfs");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkConfig(createNetConfigs());
      spec.setDistro("apache");
      spec.setDistroVendor(Constants.DEFAULT_VENDOR);

      //build a master group, a datanode group, a compute node group with strict association and tempfs.
      NodeGroupCreate[] ngs = new NodeGroupCreate[3];
      NodeGroupCreate ng0 = new NodeGroupCreate();
      ngs[0] = ng0;
      List<String> masterRoles = new ArrayList<String>();
      masterRoles.add("hadoop_namenode");
      masterRoles.add("hadoop_jobtracker");
      ngs[0].setRoles(masterRoles);
      ngs[0].setName("master");
      ngs[0].setInstanceNum(1);
      ngs[0].setInstanceType(InstanceType.LARGE);

      NodeGroupCreate ng1 = new NodeGroupCreate();
      ngs[1] = ng1;
      List<String> dataNodeRoles = new ArrayList<String>();
      dataNodeRoles.add("hadoop_datanode");
      ngs[1].setRoles(dataNodeRoles);
      ngs[1].setName("data");
      ngs[1].setInstanceNum(4);
      ngs[1].setInstanceType(InstanceType.MEDIUM);
      StorageRead storage = new StorageRead();
      storage.setType("LOCAL");
      storage.setSizeGB(50);
      ngs[1].setStorage(storage);

      NodeGroupCreate ng2 = new NodeGroupCreate();
      ngs[2] = ng2;
      List<String> computeNodeRoles = new ArrayList<String>();
      computeNodeRoles.add("hadoop_tasktracker");
      ngs[2].setRoles(computeNodeRoles);
      ngs[2].setName("compute");
      ngs[2].setInstanceNum(8);
      ngs[2].setInstanceType(InstanceType.MEDIUM);
      StorageRead storageCompute = new StorageRead();
      storageCompute.setType("TEMPFS");
      storageCompute.setSizeGB(50);
      ngs[2].setStorage(storageCompute);
      PlacementPolicy policy = new PlacementPolicy();
      policy.setInstancePerHost(2);
      List<GroupAssociation> associates = new ArrayList<GroupAssociation>();
      GroupAssociation associate = new GroupAssociation();
      associate.setReference("data");
      associate.setType(GroupAssociationType.STRICT);
      associates.add(associate);
      policy.setGroupAssociations(associates);
      ngs[2].setPlacementPolicies(policy);

      spec.setNodeGroups(ngs);
      spec = ClusterSpecFactory.getCustomizedSpec(spec);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findClusterById(1l);
      List<ClusterEntity> cs = clusterEntityMgr.findAllClusters();
      for (ClusterEntity c : cs) {
         System.out.println(c.getId());
      }
      cluster = clusterEntityMgr.findByName("my-cluster-dc-tempfs");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs =
            clusterConfigMgr.getClusterConfig("my-cluster-dc-tempfs");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(manifest.indexOf("master") != -1,
            "manifest should contains nodegroups");
      Assert.assertTrue(
            manifest.indexOf("[\"tempfs_server\",\"hadoop_datanode\"]") != -1,
            "manifest is inconsistent");
      Assert.assertTrue(
            manifest.indexOf("[\"tempfs_client\",\"hadoop_tasktracker\"]") != -1,
            "manifest is inconsistent");
   }

   public void testClusterConfigWithGroupSlave() {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster1");
      spec.setNetworkConfig(createNetConfigs());
      List<String> rps = new ArrayList<String>();
      rps.add("myRp2");
      rps.add("myRp3");
      rps.add("myRp4");
      rps.add("myRp5");
      spec.setRpNames(rps);

      NodeGroupCreate[] nodegroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      nodegroups[0] = group;
      group.setCpuNum(3);
      group.setInstanceNum(10);
      group.setInstanceType(InstanceType.SMALL);
      group.setHaFlag("off");
      group.setName("slave");
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_datanode");
      group.setRoles(roles);

      spec.setNodeGroups(nodegroups);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findByName("my-cluster1");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster1");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(
            manifest.indexOf("master") != -1 && manifest.indexOf("slave") != -1,
            "manifest should contains nodegroups");

      Assert.assertTrue(
            manifest
                  .indexOf("{\"name\":\"my-cluster1\",\"groups\":[{\"name\":\"expanded_master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-null/my-cluster1/expanded_master\"},{\"name\":\"slave\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":10,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":3,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster1/slave\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork1\",\"type\":\"static\",\"gateway\":\"192.168.1.254\",\"netmask\":\"255.255.0.0\",\"dns\":[\"2.2.2.2\"],\"ip\":[\"192.168.1.1-192.168.1.3\",\"192.168.1.102\",\"192.168.1.104-192.168.1.110\"]}]") != -1,
            "manifest is inconsistent.");
   }

   public void testClusterConfigWithGroupSlave2() {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster-slave2");
      spec.setNetworkConfig(createNetConfigs());
      List<String> rps = new ArrayList<String>();
      rps.add("myRp2");
      rps.add("myRp3");
      rps.add("myRp4");
      rps.add("myRp5");
      spec.setRpNames(rps);

      NodeGroupCreate[] nodegroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      nodegroups[0] = group;
      group.setCpuNum(3);
      group.setInstanceNum(10);
      group.setInstanceType(InstanceType.SMALL);
      group.setHaFlag("off");
      group.setName("slave");
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_tasktracker");
      group.setRoles(roles);

      spec.setNodeGroups(nodegroups);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findByName("my-cluster-slave2");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs =
            clusterConfigMgr.getClusterConfig("my-cluster-slave2");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(
            manifest.indexOf("master") != -1 && manifest.indexOf("slave") != -1,
            "manifest should contains nodegroups");

      Assert.assertTrue(
            manifest
                  .indexOf("{\"name\":\"my-cluster-slave2\",\"groups\":[{\"name\":\"expanded_master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-null/my-cluster-slave2/expanded_master\"},{\"name\":\"slave\",\"roles\":[\"hadoop_tasktracker\",\"hadoop_datanode\"],\"instance_num\":10,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":3,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster-slave2/slave\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1,
            "manifest is inconsistent.");
   }

   public void testClusterCreateWithGroupMaster() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkConfig(createNetConfigs());
      spec.setName("my-cluster2");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp2");
      rps.add("myRp3");
      rps.add("myRp4");
      rps.add("myRp5");
      spec.setRpNames(rps);

      NodeGroupCreate[] nodegroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      nodegroups[0] = group;
      group.setCpuNum(3);
      group.setInstanceNum(1);
      group.setInstanceType(InstanceType.LARGE);
      group.setHaFlag("off");
      group.setName("main_group");
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_namenode");
      group.setRoles(roles);

      spec.setNodeGroups(nodegroups);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findByName("my-cluster2");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster2");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(
            manifest.indexOf("main_group") != -1
                  && manifest.indexOf("expanded_master") != -1
                  && manifest.indexOf("expanded_worker") != -1,
            "manifest should contains nodegroups");
      Assert.assertTrue(
            manifest
                  .indexOf("{\"name\":\"my-cluster2\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":100},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster2/main_group\"},{\"name\":\"expanded_master\",\"roles\":[\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-null/my-cluster2/expanded_master\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster2/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1,
            "manifest is inconsistent");
   }

   public void testClusterConfigWithGroupMasterNeg() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkConfig(createNetConfigs());
      spec.setName("my-cluster3");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp2");
      rps.add("myRp3");
      rps.add("myRp4");
      rps.add("myRp5");
      spec.setRpNames(rps);

      NodeGroupCreate[] nodegroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      nodegroups[0] = group;
      group.setCpuNum(3);
      group.setInstanceNum(10);
      group.setInstanceType(InstanceType.LARGE);
      group.setName("main_group");
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_namenode");
      group.setRoles(roles);

      spec.setNodeGroups(nodegroups);
      try {
         clusterConfigMgr.createClusterConfig(spec);

         ClusterEntity cluster = clusterEntityMgr.findByName("my-cluster3");
         Assert.assertTrue(cluster != null);
         ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster3");
         String manifest = gson.toJson(attrs);
         System.out.println(manifest);
         Assert.assertTrue(false, "should get exception");
      } catch (BddException e) {
         Assert.assertTrue(true, "get expected exception.");
      }
   }

   public void testClusterConfigWithGroupMasterNeg1() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkConfig(createNetConfigs());
      spec.setName("my-cluster3");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp2");
      rps.add("myRp3");
      rps.add("myRp4");
      rps.add("myRp5");
      spec.setRpNames(rps);

      NodeGroupCreate[] nodegroups = new NodeGroupCreate[2];
      NodeGroupCreate group = new NodeGroupCreate();
      nodegroups[0] = group;
      group.setCpuNum(3);
      group.setInstanceNum(1);
      group.setInstanceType(InstanceType.LARGE);
      group.setName("main_group");
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_namenode");
      group.setRoles(roles);

      group = new NodeGroupCreate();
      nodegroups[1] = group;
      group.setCpuNum(3);
      group.setInstanceNum(1);
      group.setInstanceType(InstanceType.LARGE);
      group.setName("main_group1");
      roles = new ArrayList<String>();
      roles.add("hadoop_namenode");
      group.setRoles(roles);

      spec.setNodeGroups(nodegroups);
      try {
         clusterConfigMgr.createClusterConfig(spec);

         ClusterEntity cluster = clusterEntityMgr.findByName("my-cluster3");
         Assert.assertTrue(cluster != null);
         ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster3");
         String manifest = gson.toJson(attrs);
         System.out.println(manifest);
         Assert.assertTrue(false, "should get exception");
      } catch (BddException e) {
         Assert.assertTrue(e.getErrorId()
               .equals("MORE_THAN_ONE_NAMENODE_GROUP")
               && e.getSection().equals("CLUSTER_CONFIG"),
               "should get ClusterConfigException.MORE_THAN_ONE_NAMENODE_GROUP exception");
         Assert.assertTrue(true, "get expected exception.");
      }
   }

   @Test(groups = { "TestClusterConfigManager" })
   public void testClusterConfigWithClusterStorage() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster4");
      spec.setNetworkConfig(createNetConfigs());
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      List<String> dsNames = new ArrayList<String>();
      dsNames.add("testSharedStore");
      dsNames.add("testLocalStore");
      spec.setDsNames(dsNames);
      spec.setType(ClusterType.HDFS_MAPRED);
      spec.setDistro("apache");
      spec.setDistroVendor(Constants.DEFAULT_VENDOR);
      spec = ClusterSpecFactory.getCustomizedSpec(spec);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findClusterById(1l);
      List<ClusterEntity> cs = clusterEntityMgr.findAllClusters();
      for (ClusterEntity c : cs) {
         System.out.println(c.getId());
      }
      cluster = clusterEntityMgr.findByName("my-cluster4");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster4");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(manifest.indexOf("master") != -1,
            "manifest should contains nodegroups");
      //      Assert.assertTrue("manifest is inconsistent",
      //            manifest.indexOf("{\"name\":\"my-cluster4\",\"groups\":[{\"name\":\"master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-null/my-cluster4/master\"},{\"name\":\"worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster4/worker\"},{\"name\":\"client\",\"roles\":[\"hadoop_client\",\"pig\",\"hive\",\"hive_server\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster4/client\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1);
   }

   public void testClusterConfigWithGroupStorage() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkConfig(createNetConfigs());
      spec.setName("my-cluster5");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp2");
      rps.add("myRp3");
      rps.add("myRp4");
      rps.add("myRp5");
      spec.setRpNames(rps);

      NodeGroupCreate[] nodegroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      nodegroups[0] = group;
      group.setCpuNum(3);
      group.setInstanceNum(1);
      group.setInstanceType(InstanceType.LARGE);
      group.setHaFlag("off");
      group.setName("main_group");
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_namenode");
      group.setRoles(roles);
      StorageRead storage = new StorageRead();
      storage.setSizeGB(50);
      storage.setType(DatastoreType.LOCAL.toString());
      group.setStorage(storage);
      spec.setNodeGroups(nodegroups);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findByName("my-cluster5");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster5");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(
            manifest.indexOf("main_group") != -1
                  && manifest.indexOf("expanded_master") != -1
                  && manifest.indexOf("expanded_worker") != -1,
            "manifest should contains nodegroups");
      Assert.assertTrue(
            manifest
                  .indexOf("{\"name\":\"my-cluster5\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\"],\"instance_num\":1,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster5/main_group\"},{\"name\":\"expanded_master\",\"roles\":[\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-null/my-cluster5/expanded_master\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster5/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1,
            "manifest is inconsistent");
   }

   public void testClusterConfigWithGroupStoragePattern() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkConfig(createNetConfigs());
      spec.setName("my-cluster6");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp2");
      rps.add("myRp3");
      rps.add("myRp4");
      rps.add("myRp5");
      spec.setRpNames(rps);

      NodeGroupCreate[] nodegroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      nodegroups[0] = group;
      group.setCpuNum(3);
      group.setInstanceNum(1);
      group.setInstanceType(InstanceType.LARGE);
      group.setHaFlag("off");
      group.setName("main_group");
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_namenode");
      group.setRoles(roles);
      StorageRead storage = new StorageRead();
      storage.setType(DatastoreType.LOCAL.toString());
      List<String> dsNames = new ArrayList<String>();
      dsNames.add("testSharedStore");
      dsNames.add("testLocalStore");
      storage.setDsNames(dsNames);
      group.setStorage(storage);
      spec.setNodeGroups(nodegroups);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findByName("my-cluster6");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster6");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(
            manifest.indexOf("main_group") != -1
                  && manifest.indexOf("expanded_master") != -1
                  && manifest.indexOf("expanded_worker") != -1,
            "manifest should contains nodegroups");
      Assert.assertTrue(
            manifest
                  .indexOf("{\"name\":\"my-cluster6\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\"],\"instance_num\":1,\"storage\":{\"type\":\"local\",\"size\":100,\"name_pattern\":[\"vmfs*\",\"local1\"]},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster6/main_group\"},{\"name\":\"expanded_master\",\"roles\":[\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-null/my-cluster6/expanded_master\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster6/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1,
            "manifest is inconsistent");
   }

   public void testClusterConfigWithNoSlave() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkConfig(createNetConfigs());
      spec.setName("my-cluster7");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp2");
      rps.add("myRp3");
      rps.add("myRp4");
      rps.add("myRp5");
      spec.setRpNames(rps);

      NodeGroupCreate[] nodegroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      nodegroups[0] = group;
      group.setCpuNum(3);
      group.setInstanceNum(1);
      group.setInstanceType(InstanceType.LARGE);
      group.setHaFlag("off");
      group.setName("main_group");
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_namenode");
      roles.add("hadoop_jobtracker");
      group.setRoles(roles);

      spec.setNodeGroups(nodegroups);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findByName("my-cluster7");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster7");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(
            manifest.indexOf("main_group") != -1
                  && manifest.indexOf("expanded_worker") != -1,
            "manifest should contains nodegroups");
      Assert.assertTrue(
            manifest
                  .indexOf("{\"name\":\"my-cluster7\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":100},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster7/main_group\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster7/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1,
            "manifest is inconsistent");
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Test(groups = { "TestClusterConfigManager" })
   public void testClusterAppConfig() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster8");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkConfig(createNetConfigs());
      spec.setType(ClusterType.HDFS_MAPRED);
      spec.setDistro("apache");
      spec.setDistroVendor(Constants.DEFAULT_VENDOR);
      spec = ClusterSpecFactory.getCustomizedSpec(spec);
      spec.setType(null);
      String configJson =
            "{\"cluster_configuration\":{\"hadoop\":{\"core-site.xml\":{\"hadoop.security.group.mapping\":\"xyz\",\"hadoop.security.authorization\":true}}}}";
      Map config = (new Gson()).fromJson(configJson, Map.class);
      spec.setConfiguration((Map<String, Object>) (config
            .get("cluster_configuration")));
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findClusterById(1l);
      List<ClusterEntity> cs = clusterEntityMgr.findAllClusters();
      for (ClusterEntity c : cs) {
         System.out.println(c.getId());
      }
      cluster = clusterEntityMgr.findByName("my-cluster8");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster8");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(manifest.indexOf("master") != -1,
            "manifest should contains nodegroups");
      Assert.assertTrue(
            //            manifest.indexOf("{\"name\":\"my-cluster8\",\"groups\":[{\"name\":\"master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-null/my-cluster8/master\"},{\"name\":\"worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster8/worker\"},{\"name\":\"client\",\"roles\":[\"hadoop_client\",\"pig\",\"hive\",\"hive_server\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster8/client\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1 &&
            manifest
                  .indexOf("\"cluster_configuration\":{\"hadoop\":{\"core-site.xml\":{\"hadoop.security.group.mapping\":\"xyz\",\"hadoop.security.authorization\":true}}}") != -1,
            "manifest is inconsistent");
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
   public void testGroupMasterWithGroupAppConfig() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkConfig(createNetConfigs());

      spec.setName("my-cluster9");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp2");
      rps.add("myRp3");
      rps.add("myRp4");
      rps.add("myRp5");
      spec.setRpNames(rps);

      NodeGroupCreate[] nodegroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      nodegroups[0] = group;
      group.setCpuNum(3);
      group.setInstanceNum(1);
      group.setInstanceType(InstanceType.LARGE);
      group.setHaFlag("off");
      group.setName("main_group");
      List<String> roles = new ArrayList<String>();
      roles.add("hadoop_namenode");
      group.setRoles(roles);
      String configJson =
            "{\"cluster_configuration\":{\"hadoop\":{\"core-site.xml\":{\"hadoop.security.group.mapping\":\"xxx\",\"hadoop.security.authorization\":false}}}}";
      Map config = (new Gson()).fromJson(configJson, Map.class);
      group.setConfiguration((Map<String, Object>) (config
            .get("cluster_configuration")));

      spec.setNodeGroups(nodegroups);
      clusterConfigMgr.createClusterConfig(spec);

      ClusterEntity cluster = clusterEntityMgr.findByName("my-cluster9");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterConfigMgr.getClusterConfig("my-cluster9");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue(
            manifest.indexOf("main_group") != -1
                  && manifest.indexOf("expanded_master") != -1
                  && manifest.indexOf("expanded_worker") != -1,
            "manifest should contains nodegroups");
      Assert.assertTrue(
            manifest
                  .indexOf("{\"name\":\"my-cluster9\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":100},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"cluster_configuration\":{\"hadoop\":{\"core-site.xml\":{\"hadoop.security.group.mapping\":\"xxx\",\"hadoop.security.authorization\":false}}},\"vm_folder_path\":\"SERENGETI-null/my-cluster9/main_group\"},{\"name\":\"expanded_master\",\"roles\":[\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-null/my-cluster9/expanded_master\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-null/my-cluster9/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1,
            "manifest is inconsistent");
   }
}
