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
package com.vmware.bdd.utils;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

public class AppConfigValidationFactoryTest {
   private ClusterCreate cluster;
   @BeforeMethod
   public void setup(){
      cluster=new ClusterCreate();
      Map<String,Object> hadoopMap=new HashMap<String,Object>();
      Map<String,Object> hadoopFileMap=new HashMap<String,Object>();
      Map<String,Object> corePopertysMap=new HashMap<String,Object>();
      Map<String,Object> hdfsPopertysMap=new HashMap<String,Object>();
      Map<String,Object> mapredPopertysMap=new HashMap<String,Object>();
      Map<String,Object> hbaseFileMap=new HashMap<String,Object>();
      Map<String,Object> hbaseSiteMap=new HashMap<String,Object>();
      Map<String,Object> zookeeperFileMap=new HashMap<String,Object>();
      Map<String,Object> zookeeperEnvMap=new HashMap<String,Object>();
      corePopertysMap.put("hadoop.tmp.dir", "/tmp");
      hdfsPopertysMap.put("dfs.http.address", "localhost");
      mapredPopertysMap.put("mapred.task.cache.levels","3");
      hbaseSiteMap.put("hbase.cluster.distributed", "false");
      zookeeperEnvMap.put("JVMFLAGS", "-Xmx1g");
      zookeeperEnvMap.put("other", "error");
      hadoopFileMap.put("core-site.xml", corePopertysMap);
      hadoopFileMap.put("hdfs-site.xml", hdfsPopertysMap);
      hadoopFileMap.put("mapred-site.xml", mapredPopertysMap);
      hbaseFileMap.put("hbase-site.xml", hbaseSiteMap);
      zookeeperFileMap.put("java.env", zookeeperEnvMap);
      hadoopMap.put("hadoop", hadoopFileMap);
      hadoopMap.put("hbase", hbaseFileMap);
      hadoopMap.put("zookeeper", zookeeperFileMap);
      cluster.setConfiguration(hadoopMap);
      NodeGroupCreate nodeGroup1=new NodeGroupCreate();
      NodeGroupCreate nodeGroup2=new NodeGroupCreate();
      NodeGroupCreate nodeGroup3=new NodeGroupCreate();
      hadoopMap=new HashMap<String,Object>();
      Map<String, Object> zookeeperMap = new HashMap<String,Object>();
      Map<String, Object> noExistingFileZookeeperMap = new HashMap<String,Object>();
      hadoopFileMap=new HashMap<String,Object>();
      corePopertysMap=new HashMap<String,Object>();
      hdfsPopertysMap=new HashMap<String,Object>();
      mapredPopertysMap=new HashMap<String,Object>();
      corePopertysMap.put("hadoop.tmp.dir", "/tmp");
      hdfsPopertysMap.put("dfs.namenode.test.level", 4);
      hdfsPopertysMap.put("dfs.namenode.logger.level", 5);
      mapredPopertysMap.put("mapred.cluster.map.memory.mb",200);
      hadoopFileMap.put("core-site.xml", corePopertysMap);
      hadoopFileMap.put("hdfs-site.xml", hdfsPopertysMap);
      hadoopFileMap.put("mapred-site.xml", mapredPopertysMap);
      hadoopMap.put("hadoop", hadoopFileMap);
      zookeeperMap.put("zookeeper", zookeeperFileMap);
      noExistingFileZookeeperMap.put("zookeeper", hadoopFileMap);
      nodeGroup1.setConfiguration(hadoopMap);
      nodeGroup2.setConfiguration(zookeeperMap);
      nodeGroup3.setConfiguration(noExistingFileZookeeperMap);
      cluster.setNodeGroups(new NodeGroupCreate[]{nodeGroup1, nodeGroup2, nodeGroup3});
   }

   @Test
   public void testWhiteListHandle() {
      ValidateResult hadoopValidateResult=AppConfigValidationFactory.whiteListHandle(cluster.getNodeGroups()[0].getConfiguration());
      assertEquals(hadoopValidateResult.getType(),ValidateResult.Type.WHITE_LIST_INVALID_NAME);
      assertEquals(hadoopValidateResult.getFailureNames().get(0), "dfs.namenode.test.level");
      assertEquals(hadoopValidateResult.getFailureNames().get(1), "dfs.namenode.logger.level");

      ValidateResult zookeeperValidateResult=AppConfigValidationFactory.whiteListHandle(cluster.getNodeGroups()[1].getConfiguration());
      assertEquals(zookeeperValidateResult.getType(),ValidateResult.Type.WHITE_LIST_INVALID_NAME);
      assertEquals(zookeeperValidateResult.getFailureNames().size(), 1);
      assertEquals(zookeeperValidateResult.getFailureNames().get(0), "other");

      ValidateResult noExistingValidateResult=AppConfigValidationFactory.whiteListHandle(cluster.getNodeGroups()[2].getConfiguration());
      assertEquals(noExistingValidateResult.getType(),ValidateResult.Type.WHITE_LIST_INVALID_NAME);
      assertEquals(noExistingValidateResult.getNoExistFileNames().get("zookeeper").size(),3);

      //test wrong values
      Map<String, Object> fairSchedulerMap = new HashMap<String,Object>();
      fairSchedulerMap.put("text", "<?xml version=\"1.0\"?>" +
            "<allocations> <pool name=\"sample_pool\"><minMaps>5</minMaps>" +
            "<minReduces>5</minReduces><maxMaps>25</maxMaps><maxReduces>25</maxReduces>" +
            "<minSharePreemptionTimeout>300</minSharePreemptionTimeout></pool>" +
            "<user name=\"sample_user\"><maxRunningJobs>6</maxRunningJobs></user>" +
            "<userMaxJobsDefault>3</userMaxJobsDefault><fairSharePreemptionTimeout>600k" +
            "</fairSharePreemptionTimeout></allocations>");
      ((Map<String, Object>)cluster.getNodeGroups()[0].getConfiguration().get("hadoop")).put("fair-scheduler.xml", fairSchedulerMap);
      hadoopValidateResult=AppConfigValidationFactory.whiteListHandle(cluster.getNodeGroups()[0].getConfiguration());
      assertEquals(hadoopValidateResult.getType(),ValidateResult.Type.WHITE_LIST_INVALID_VALUE);
      assertEquals(hadoopValidateResult.getFailureValues().get(0), "600k");

      //test wrong xml format
      fairSchedulerMap.put("text",
            "<allocations> <pool name=\"sample_pool\"><minMaps>5</minMaps>" +
            "<minReduces>5</minReduces><maxMaps>25</maxMaps><maxReduces>25</maxReduces>" +
            "<minSharePreemptionTimeout>300</minSharePreemptionTimeout></pool>" +
            "<user name=\"sample_user\"><maxRunningJobs>6</maxRunningJobs></user>" +
            "<userMaxJobsDefault>3</userMaxJobsDefault><fairSharePreemptionTimeout>600k" +
            "</fairSharePreemptionTimeout>");
      ((Map<String, Object>)cluster.getNodeGroups()[0].getConfiguration().get("hadoop")).put("fair-scheduler.xml", fairSchedulerMap);
      hadoopValidateResult=AppConfigValidationFactory.whiteListHandle(cluster.getNodeGroups()[0].getConfiguration());
      assertEquals(hadoopValidateResult.getType(),ValidateResult.Type.WHITE_LIST_INVALID_VALUE);
      assertEquals(hadoopValidateResult.getFailureValues().get(0),
            "<allocations> <pool name=\"sample_pool\"><minMaps>5</minMaps>" +
                  "<minReduces>5</minReduces><maxMaps>25</maxMaps><maxReduces>25</maxReduces>" +
                  "<minSharePreemptionTimeout>300</minSharePreemptionTimeout></pool>" +
                  "<user name=\"sample_user\"><maxRunningJobs>6</maxRunningJobs></user>" +
                  "<userMaxJobsDefault>3</userMaxJobsDefault><fairSharePreemptionTimeout>600k" +
                  "</fairSharePreemptionTimeout>");
   }

   @Test
   public void testBlackListHandle() {
      ValidateResult validateResult=AppConfigValidationFactory.blackListHandle(cluster.getConfiguration());
      assertEquals(validateResult.getType(),ValidateResult.Type.NAME_IN_BLACK_LIST);
      assertEquals(validateResult.getFailureNames().get(0), "dfs.http.address");
      assertEquals(validateResult.getFailureNames().get(1), "mapred.task.cache.levels");
      assertEquals(validateResult.getFailureNames().get(2), "hbase.cluster.distributed");
   }

   @Test
   public void testConfigurationTypeValidation1() {
      Map<String, Object> configurations = new HashMap<String, Object>();
      configurations.put("hadoop", new Object());

      List<String> warningMsgList = new ArrayList<String>();
      AppConfigValidationFactory.validateConfigType(configurations, warningMsgList);

      assertEquals(0, warningMsgList.size());
   }

   @Test
   public void testConfigurationTypeValidation2() {
      Map<String, Object> configurations = new HashMap<String, Object>();
      configurations.put("hadoop", new Object());
      configurations.put("xyz", new Object());

      List<String> warningMsgList = new ArrayList<String>();
      AppConfigValidationFactory.validateConfigType(configurations, warningMsgList);
      assertEquals(1, warningMsgList.size());

      assertEquals("Warning: The type: xyz is not a regular cluster configuration.",
            warningMsgList.get(0));
   }

   @Test
   public void testConfigurationTypeValidation3() {
      Map<String, Object> configurations = new HashMap<String, Object>();
      configurations.put("hadoop", new Object());
      configurations.put("xyz", new Object());
      configurations.put("abc", new Object());

      List<String> warningMsgList = new ArrayList<String>();
      AppConfigValidationFactory.validateConfigType(configurations, warningMsgList);
      assertEquals(1, warningMsgList.size());

      assertEquals("Warning: The types: abc,xyz are not regular cluster configurations.",
            warningMsgList.get(0));
   }
}
