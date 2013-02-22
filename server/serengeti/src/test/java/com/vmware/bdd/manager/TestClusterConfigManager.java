/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved. 
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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterType;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NodeGroup.InstanceType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.specpolicy.ClusterSpecFactory;
import com.vmware.bdd.utils.Constants;

public class TestClusterConfigManager {
   private static ClusterConfigManager clusterMgr = new ClusterConfigManager();
   private static VcProviderManager vcProvider = new VcProviderManager();
   private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

   static {
      clusterMgr.setRpMgr(new VcResourcePoolManager());
      VcResourcePoolManager rpMgr = new VcResourcePoolManager();
      vcProvider.setRpMgr(rpMgr);
      NetworkManager netMgr = new NetworkManager();
      clusterMgr.setNetworkMgr(netMgr);
      DistroManager distroMgr = new DistroManager();
      clusterMgr.setDistroMgr(distroMgr);
      VcDataStoreManager datastoreMgr = new VcDataStoreManager();
      vcProvider.setDatastoreMgr(datastoreMgr);
      clusterMgr.setDatastoreMgr(datastoreMgr);
      RackInfoManager rackInfoMgr = new RackInfoManager();
      clusterMgr.setRackInfoMgr(rackInfoMgr);
   }

   @BeforeClass
   public static void setup() {
      clusterMgr.getRpMgr().addResourcePool("myRp1", "cluster1", "rp1");
      clusterMgr.getRpMgr().addResourcePool("myRp2", "cluster1", "rp2");
      clusterMgr.getRpMgr().addResourcePool("myRp3", "cluster2", "rp1");
      clusterMgr.getRpMgr().addResourcePool("myRp4", "cluster2", "rp2");
      clusterMgr.getRpMgr().addResourcePool("myRp5", "cluster4", "rp1");
      clusterMgr.getRpMgr().addResourcePool("myRp6", "cluster4", "rp2");
      DAL.inTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() throws Exception {
            clusterMgr.getNetworkMgr().addDhcpNetwork("dhcpNet1", "CFNetwork");
            return null;
         }
      });
      List<String> sharedStores = new ArrayList<String>();
      sharedStores.add("share1");
      sharedStores.add("share2");
      vcProvider.getDatastoreMgr().addDataStores("testSharedStore", DatastoreType.SHARED, sharedStores);
      List<String> localStores = new ArrayList<String>();
      localStores.add("local1");
      localStores.add("vmfs*");
      vcProvider.getDatastoreMgr().addDataStores("testLocalStore", DatastoreType.LOCAL, localStores);
      DAL.inTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() throws Exception {
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
            clusterMgr.getNetworkMgr().addIpPoolNetwork("ipPool1", "CFNetwork1", "255.255.0.0", "192.168.1.254", "2.2.2.2", null, ipBlocks);
            return null;
         }
      });
   }

   @Test
   public void testString() {
      String s1 = "{\"name\":\"my-cluster\",\"groups\":[{\"name\":\"master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"cpu\":2,\"memory\":7500,\"storage\":{\"type\":\"shared\",\"size\":50},\"ha\":\"on\"},{\"name\":\"worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"cpu\":1,\"memory\":3748,\"storage\":{\"type\":\"local\",\"size\":50},\"ha\":\"off\"},{\"name\":\"client\",\"roles\":[\"hive\",\"hadoop_client\",\"pig\"],\"instance_num\":1,\"cpu\":1,\"memory\":3748,\"storage\":{\"type\":\"shared\",\"size\":50},\"ha\":\"off\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}],\"distro_map\":{\"hadoop\":\"http://localhost/distros/apache/1.0.1/hadoop-1.0.1.tar.gz\",\"pig\":\"http://localhost/distros/apache/1.0.1/pig-0.9.2.tar.gz\",\"hive\":\"http://localhost/distros/apache/1.0.1/hive-0.8.1.tar.gz\"},\"vc_shared_datastore_pattern\":[\"share1\",\"share2\"],\"vc_local_datastore_pattern\":[\"vmfs*\",\"local1\"]}";
      String s2 = "{\"name\":\"my-cluster\",\"groups\":[{\"name\":\"master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\"},{\"name\":\"worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\"},{\"name\":\"client\",\"roles\":[\"hive\",\"hadoop_client\",\"pig\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}],\"distro_map\":{\"hadoop\":\"http://localhost/distros/apache/1.0.1/hadoop-1.0.1.tar.gz\",\"pig\":\"http://localhost/distros/apache/1.0.1/pig-0.9.2.tar.gz\",\"hive\":\"http://localhost/distros/apache/1.0.1/hive-0.8.1.tar.gz\"},\"vc_shared_datastore_pattern\":[\"share1\",\"share2\"],\"vc_local_datastore_pattern\":[\"vmfs*\",\"local1\"]}";
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

   @Test
   public void testClusterConfig() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkName("dhcpNet1");
      spec.setDistro("apache");
      spec.setDistroVendor(Constants.DEFAULT_VENDOR);
      spec.setType(ClusterType.HDFS_MAPRED);
      spec = ClusterSpecFactory.getCustomizedSpec(spec);
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster = ClusterEntity.findClusterEntityById(1l);
      List<ClusterEntity> cs = DAL.findAll(ClusterEntity.class);
      for (ClusterEntity c : cs ) {
         System.out.println(c.getId());
      }
      cluster =
            ClusterEntity.findClusterEntityByName("my-cluster");
      Assert.assertTrue(cluster != null);
      Assert.assertEquals(cluster.isAutomationEnable(), null); //not a D/C seperation cluster

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("master") != -1);
      //Assert.assertTrue("manifest is inconsistent",
        //    manifest.indexOf("{\"name\":\"my-cluster\",\"groups\":[{\"name\":\"master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"bisect\":false,\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster/master\"},{\"name\":\"worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"bisect\":false,\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster/worker\"},{\"name\":\"client\",\"roles\":[\"hive\",\"hadoop_client\",\"pig\",\"hive\",\"hive_server\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"bisect\":false,\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster/client\"}],\"distro\":\"apache\",\"http_proxy\":\"\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1);
   }

   @Test
   public void testClusterConfigWithExternalHDFS() throws Exception {
      String[] hdfsArray = new String[] {
            "hdfs://168.192.0.70:8020", "hdfs://168.192.0.71:8020",
            "hdfs://168.192.0.72:8020", "hdfs://168.192.0.73:8020" };
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster-external-hdfs");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkName("dhcpNet1");
      spec.setDistro("apache");
      spec.setDistroVendor(Constants.DEFAULT_VENDOR);
      spec.setExternalHDFS(hdfsArray[0]);
      String clusterConfigJson = 
         "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\"" + hdfsArray[1] + "\"}}}}";
      Map clusterConfig = (new Gson()).fromJson(clusterConfigJson, Map.class);
      spec.setConfiguration((Map<String, Object>)(clusterConfig.get("configuration")));
      //build a jobtracker group, two compute node groups.
      NodeGroupCreate ng0 = new NodeGroupCreate();
      List<String> jobtrackerRole = new ArrayList<String>();
      jobtrackerRole.add("hadoop_jobtracker");
      ng0.setRoles(jobtrackerRole);
      ng0.setName("jobtracker");
      ng0.setInstanceNum(1);
      ng0.setInstanceType(InstanceType.LARGE);
      String ng0ConfigJson = 
         "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\"" + hdfsArray[2] + "\"}}}}";
      Map ng0Config = (new Gson()).fromJson(ng0ConfigJson, Map.class);
      ng0.setConfiguration((Map<String, Object>)(ng0Config.get("configuration")));

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
         "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\"" + hdfsArray[3] + "\"}}}}";
      Map ng1Config = (new Gson()).fromJson(ng1ConfigJson, Map.class);
      ng1.setConfiguration((Map<String, Object>)(ng1Config.get("configuration")));
      NodeGroupCreate ng2 = new NodeGroupCreate();
      ng2.setRoles(computeRoles);
      ng2.setName("compute2");
      ng2.setInstanceNum(2);
      ng2.setInstanceType(InstanceType.MEDIUM);
      StorageRead storageCompute = new StorageRead();
      storageCompute.setType("LOCAL");
      storageCompute.setSizeGB(10);
      ng2.setStorage(storageCompute);

      NodeGroupCreate[] ngs =  new NodeGroupCreate[]{ng0, ng1, ng2};
      spec.setNodeGroups(ngs);
      spec = ClusterSpecFactory.getCustomizedSpec(spec);
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster = ClusterEntity.findClusterEntityById(1l);
      List<ClusterEntity> cs = DAL.findAll(ClusterEntity.class);
      for (ClusterEntity c : cs ) {
         System.out.println(c.getId());
      }
      cluster =
            ClusterEntity.findClusterEntityByName("my-cluster-external-hdfs");
      Assert.assertTrue(cluster != null);
      
      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster-external-hdfs");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("\"fs.default.name\" must be coved with external HDFS uri in both of cluster and group configuration.",
            Pattern.compile("([\\s\\S]*" + hdfsArray[0] + "[\\s\\S]*){3}").matcher(manifest).matches());
      Assert.assertTrue("\"fs.default.name\" must be coved under the cluster level", manifest.indexOf(hdfsArray[1]) == -1);
      Assert.assertTrue("\"fs.default.name\" must be coved under the node group 1 level", manifest.indexOf(hdfsArray[2]) == -1);
      Assert.assertTrue("\"fs.default.name\" must be coved under the node group 2 level", manifest.indexOf(hdfsArray[3]) == -1);
      
   }

   @Test
   public void testClusterConfigWithExternalHDFSFailure() throws Exception {
      String[] hdfsArray = new String[] {
            "hdfs://168.192.0.70:8020", "hdfs://168.192.0.71:8020",
            "hdfs://168.192.0.72:8020", "hdfs://168.192.0.73:8020" };
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster-external-hdfs-failure");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkName("dhcpNet1");
      spec.setDistro("apache");
      spec.setDistroVendor(Constants.DEFAULT_VENDOR);
      String clusterConfigJson = 
         "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\"" + hdfsArray[1] + "\"}}}}";
      Map clusterConfig = (new Gson()).fromJson(clusterConfigJson, Map.class);
      spec.setConfiguration((Map<String, Object>)(clusterConfig.get("configuration")));
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
         "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\"" + hdfsArray[2] + "\"}}}}";
      Map ng0Config = (new Gson()).fromJson(ng0ConfigJson, Map.class);
      ng0.setConfiguration((Map<String, Object>)(ng0Config.get("configuration")));

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
         "{\"configuration\":{\"hadoop\":{\"core-site.xml\":{\"fs.default.name\":\"" + hdfsArray[3] + "\"}}}}";
      Map ng1Config = (new Gson()).fromJson(ng1ConfigJson, Map.class);
      ng1.setConfiguration((Map<String, Object>)(ng1Config.get("configuration")));
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

      NodeGroupCreate[] ngs =  new NodeGroupCreate[]{ng0, ng1, ng2};
      spec.setNodeGroups(ngs);
      spec = ClusterSpecFactory.getCustomizedSpec(spec);
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster = ClusterEntity.findClusterEntityById(1l);
      List<ClusterEntity> cs = DAL.findAll(ClusterEntity.class);
      for (ClusterEntity c : cs ) {
         System.out.println(c.getId());
      }
      cluster =
            ClusterEntity.findClusterEntityByName("my-cluster-external-hdfs-failure");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster-external-hdfs-failure");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("\"fs.default.name\" must be coved with external HDFS uri in both of cluster and group configuration.",
            Pattern.compile("([\\s\\S]*" + hdfsArray[0] + "[\\s\\S]*){3}").matcher(manifest).matches()==false);
      Assert.assertTrue("\"fs.default.name\" must be coved under the cluster level", manifest.indexOf(hdfsArray[1]) != -1);
      Assert.assertTrue("\"fs.default.name\" must be coved under the node group 1 level", manifest.indexOf(hdfsArray[2]) != -1);
      Assert.assertTrue("\"fs.default.name\" must be coved under the node group 2 level", manifest.indexOf(hdfsArray[3]) != -1);
      
   }
   @Test
   public void testClusterConfigWithTempfs() throws Exception {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster-dc-tempfs");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkName("dhcpNet1");
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
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster = ClusterEntity.findClusterEntityById(1l);
      List<ClusterEntity> cs = DAL.findAll(ClusterEntity.class);
      for (ClusterEntity c : cs ) {
         System.out.println(c.getId());
      }
      cluster =
            ClusterEntity.findClusterEntityByName("my-cluster-dc-tempfs");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster-dc-tempfs");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertEquals(cluster.isAutomationEnable(), Boolean.FALSE);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("master") != -1);
      Assert.assertTrue("manifest is inconsistent",
            manifest.indexOf("[\"tempfs_server\",\"hadoop_datanode\"]") != -1);
      Assert.assertTrue("manifest is inconsistent",
            manifest.indexOf("[\"tempfs_client\",\"hadoop_tasktracker\"]") != -1);
   }

   public void testClusterConfigWithGroupSlave() {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster1");
      spec.setNetworkName("ipPool1");
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
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName("my-cluster1");
      Assert.assertTrue(cluster != null);
      
      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster1");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("master") != -1 && manifest.indexOf("slave") != -1);

      Assert.assertTrue("manifest is inconsistent.",
            manifest.indexOf(
                  "{\"name\":\"my-cluster1\",\"groups\":[{\"name\":\"expanded_master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster1/expanded_master\"},{\"name\":\"slave\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":10,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":3,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster1/slave\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork1\",\"type\":\"static\",\"gateway\":\"192.168.1.254\",\"netmask\":\"255.255.0.0\",\"dns\":[\"2.2.2.2\"],\"ip\":[\"192.168.1.1-192.168.1.3\",\"192.168.1.102\",\"192.168.1.104-192.168.1.110\"]}]") != -1);
   }

   public void testClusterConfigWithGroupSlave2() {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster-slave2");
      spec.setNetworkName("dhcpNet1");
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
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName("my-cluster-slave2");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster-slave2");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("master") != -1 && manifest.indexOf("slave") != -1);

      Assert.assertTrue("manifest is inconsistent.",
            manifest.indexOf(
                  "{\"name\":\"my-cluster-slave2\",\"groups\":[{\"name\":\"expanded_master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster-slave2/expanded_master\"},{\"name\":\"slave\",\"roles\":[\"hadoop_tasktracker\",\"hadoop_datanode\"],\"instance_num\":10,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":3,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster-slave2/slave\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1);
   }

   public void testClusterCreateWithGroupMaster() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkName("dhcpNet1");
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
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName("my-cluster2");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster2");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("main_group") != -1 && manifest.indexOf("expanded_master") != -1
            && manifest.indexOf("expanded_worker") != -1);
      Assert.assertTrue("manifest is inconsistent",
            manifest.indexOf("{\"name\":\"my-cluster2\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":100},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster2/main_group\"},{\"name\":\"expanded_master\",\"roles\":[\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster2/expanded_master\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster2/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1);
   }

   public void testClusterConfigWithGroupMasterNeg() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkName("dhcpNet1");
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
         clusterMgr.createClusterConfig(spec);

         ClusterEntity cluster =
               ClusterEntity.findClusterEntityByName("my-cluster3");
         Assert.assertTrue(cluster != null);
         ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster3");
         String manifest = gson.toJson(attrs);
         System.out.println(manifest);
         Assert.assertTrue("should get exception", false);
      } catch (BddException e) {
         Assert.assertTrue("get expected exception.", true);
      }
   }

   public void testClusterConfigWithGroupMasterNeg1() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkName("dhcpNet1");
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
         clusterMgr.createClusterConfig(spec);

         ClusterEntity cluster =
               ClusterEntity.findClusterEntityByName("my-cluster3");
         Assert.assertTrue(cluster != null);
         ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster3");
         String manifest = gson.toJson(attrs);
         System.out.println(manifest);
         Assert.assertTrue("should get exception", false);
      } catch (BddException e) {
         Assert.assertTrue("should get ClusterConfigException.MORE_THAN_ONE_NAMENODE_GROUP exception", 
               e.getErrorId().equals("MORE_THAN_ONE_NAMENODE_GROUP") && e.getSection().equals("CLUSTER_CONFIG"));
         Assert.assertTrue("get expected exception.", true);
      }
   }

   public void testClusterConfigWithClusterStorage() {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster4");
      spec.setNetworkName("dhcpNet1");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      List<String> dsNames = new ArrayList<String>();
      dsNames.add("testSharedStore");
      dsNames.add("testLocalStore");
      spec.setDsNames(dsNames);
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster = ClusterEntity.findClusterEntityById(1l);
      List<ClusterEntity> cs = DAL.findAll(ClusterEntity.class);
      for (ClusterEntity c : cs ) {
         System.out.println(c.getId());
      }
      cluster =
            ClusterEntity.findClusterEntityByName("my-cluster4");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster4");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("master") != -1);
      Assert.assertTrue("manifest is inconsistent",
            manifest.indexOf("{\"name\":\"my-cluster4\",\"groups\":[{\"name\":\"master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster4/master\"},{\"name\":\"worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster4/worker\"},{\"name\":\"client\",\"roles\":[\"hive\",\"hadoop_client\",\"hive_server\",\"pig\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster4/client\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1);
   }

   public void testClusterConfigWithGroupStorage() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkName("dhcpNet1");
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
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName("my-cluster5");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster5");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("main_group") != -1 && manifest.indexOf("expanded_master") != -1
            && manifest.indexOf("expanded_worker") != -1);
      Assert.assertTrue("manifest is inconsistent",
            manifest.indexOf("{\"name\":\"my-cluster5\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\"],\"instance_num\":1,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster5/main_group\"},{\"name\":\"expanded_master\",\"roles\":[\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster5/expanded_master\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster5/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1);
   }

   public void testClusterConfigWithGroupStoragePattern() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkName("dhcpNet1");
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
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName("my-cluster6");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster6");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("main_group") != -1 && manifest.indexOf("expanded_master") != -1
            && manifest.indexOf("expanded_worker") != -1);
      Assert.assertTrue("manifest is inconsistent",
            manifest.indexOf("{\"name\":\"my-cluster6\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\"],\"instance_num\":1,\"storage\":{\"type\":\"local\",\"size\":100,\"name_pattern\":[\"vmfs*\",\"local1\"]},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster6/main_group\"},{\"name\":\"expanded_master\",\"roles\":[\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster6/expanded_master\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster6/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1);
   }

   public void testClusterConfigWithNoSlave() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkName("dhcpNet1");
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
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName("my-cluster7");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster7");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("main_group") != -1
            && manifest.indexOf("expanded_worker") != -1);
      Assert.assertTrue("manifest is inconsistent",
            manifest.indexOf("{\"name\":\"my-cluster7\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":100},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster7/main_group\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster7/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1);
   }

   public void testClusterAppConfig() {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("my-cluster8");
      List<String> rps = new ArrayList<String>();
      rps.add("myRp1");
      spec.setRpNames(rps);
      spec.setNetworkName("dhcpNet1");
      String configJson = 
         "{\"cluster_configuration\":{\"hadoop\":{\"core-site.xml\":{\"hadoop.security.group.mapping\":\"xyz\",\"hadoop.security.authorization\":true}}}}";
      Map config = (new Gson()).fromJson(configJson, Map.class);
      spec.setConfiguration((Map<String, Object>)(config.get("cluster_configuration")));
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster = ClusterEntity.findClusterEntityById(1l);
      List<ClusterEntity> cs = DAL.findAll(ClusterEntity.class);
      for (ClusterEntity c : cs ) {
         System.out.println(c.getId());
      }
      cluster =
            ClusterEntity.findClusterEntityByName("my-cluster8");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster8");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("master") != -1);
      Assert.assertTrue("manifest is inconsistent",
            manifest.indexOf("{\"name\":\"my-cluster8\",\"groups\":[{\"name\":\"master\",\"roles\":[\"hadoop_namenode\",\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster8/master\"},{\"name\":\"worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster8/worker\"},{\"name\":\"client\",\"roles\":[\"hive\",\"hadoop_client\",\"hive_server\",\"pig\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster8/client\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1
            && manifest.indexOf("\"cluster_configuration\":{\"hadoop\":{\"core-site.xml\":{\"hadoop.security.group.mapping\":\"xyz\",\"hadoop.security.authorization\":true}}}") != -1);
   }

   public void testGroupMasterWithGroupAppConfig() {
      ClusterCreate spec = new ClusterCreate();
      spec.setNetworkName("dhcpNet1");
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
      group.setConfiguration((Map<String, Object>)(config.get("cluster_configuration")));

      spec.setNodeGroups(nodegroups);
      clusterMgr.createClusterConfig(spec);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName("my-cluster9");
      Assert.assertTrue(cluster != null);

      ClusterCreate attrs = clusterMgr.getClusterConfig("my-cluster9");
      String manifest = gson.toJson(attrs);
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains nodegroups",
            manifest.indexOf("main_group") != -1 && manifest.indexOf("expanded_master") != -1
            && manifest.indexOf("expanded_worker") != -1);
      Assert.assertTrue("manifest is inconsistent",
            manifest.indexOf("{\"name\":\"my-cluster9\",\"groups\":[{\"name\":\"main_group\",\"roles\":[\"hadoop_namenode\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":100},\"cpu\":3,\"memory\":15000,\"ha\":\"off\",\"cluster_configuration\":{\"hadoop\":{\"core-site.xml\":{\"hadoop.security.group.mapping\":\"xxx\",\"hadoop.security.authorization\":false}}},\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster9/main_group\"},{\"name\":\"expanded_master\",\"roles\":[\"hadoop_jobtracker\"],\"instance_num\":1,\"storage\":{\"type\":\"shared\",\"size\":50},\"cpu\":2,\"memory\":7500,\"ha\":\"on\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster9/expanded_master\"},{\"name\":\"expanded_worker\",\"roles\":[\"hadoop_datanode\",\"hadoop_tasktracker\"],\"instance_num\":3,\"storage\":{\"type\":\"local\",\"size\":50},\"cpu\":1,\"memory\":3748,\"ha\":\"off\",\"vm_folder_path\":\"SERENGETI-xxx-uuid/my-cluster9/expanded_worker\"}],\"distro\":\"apache\",\"vc_clusters\":[{\"name\":\"cluster1\",\"vc_rps\":[\"rp2\"]},{\"name\":\"cluster2\",\"vc_rps\":[\"rp1\",\"rp2\"]},{\"name\":\"cluster4\",\"vc_rps\":[\"rp1\"]}],\"template_id\":\"vm-001\",\"networking\":[{\"port_group\":\"CFNetwork\",\"type\":\"dhcp\"}]") != -1);
   }
}

