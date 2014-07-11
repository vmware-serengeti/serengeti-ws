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

import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import junit.framework.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 7/11/14
 * Time: 3:19 PM
 */
public class TestCmClusterValidator {

   @Test
   public void testSuccess() {
      try {
         ClusterBlueprint blueprint = generateBlueprint();
         CmClusterValidator validator = new CmClusterValidator();
         Assert.assertTrue(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
      }
   }

   @Test
   public void testUnrecogRolesOfDistro() {
      try {
         ClusterBlueprint blueprint = generateBlueprint();
         blueprint.getHadoopStack().setDistro("CDH-4.0.0");
         CmClusterValidator validator = new CmClusterValidator();
         Assert.assertFalse(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
      }
   }

   @Test
   public void testUnrecogRoles() {
      try {
         ClusterBlueprint blueprint = generateBlueprint();
         blueprint.getNodeGroups().get(0).getRoles().add("FAKE_ROLE01");
         blueprint.getNodeGroups().get(1).getRoles().add("FAKE_ROLE02");
         CmClusterValidator validator = new CmClusterValidator();
         Assert.assertFalse(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
      }
   }

   @Test
   public void testUnrecogConfigTypes() {
      try {
         ClusterBlueprint blueprint = generateBlueprint();
         blueprint.getConfiguration().put("FAKE_CONFIG", new HashMap<String, String>());
         CmClusterValidator validator = new CmClusterValidator();
         Assert.assertTrue(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
      }
   }

   @Test
   public void testBadConfigItems() {
      try {
         ClusterBlueprint blueprint = generateBlueprint();
         Map<String, String> hdfsConfig = (Map<String, String>) blueprint.getConfiguration().get("HDFS");
         hdfsConfig.put("fake_config_01", "value");

         Map<String, String> nnConfig = (Map<String, String>) blueprint.getNodeGroups().get(0).getConfiguration().get("HDFS_NAMENODE");
         nnConfig.put("fake_config_02", "value");

         CmClusterValidator validator = new CmClusterValidator();
         Assert.assertFalse(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
      }
   }

   @Test
   public void testInvalidRack() {
      try {
         ClusterBlueprint blueprint = generateBlueprint();
         blueprint.getNodeGroups().get(0).getNodes().get(0).setRack("bad_rack");
         CmClusterValidator validator = new CmClusterValidator();
         Assert.assertFalse(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
      }
   }

   @Test
   public void testMissedRoles() {
      try {
         ClusterBlueprint blueprint = generateBlueprint();
         blueprint.getNodeGroups().get(1).getRoles().remove("HDFS_DATANODE");
         blueprint.getNodeGroups().get(0).getRoles().remove("YARN_RESOURCE_MANAGER");
         CmClusterValidator validator = new CmClusterValidator();
         Assert.assertFalse(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
      }
   }

   @Test
   public void testHA() {
      try {
         ClusterBlueprint blueprint = generateBlueprint();
         blueprint.getNodeGroups().get(0).getRoles().remove("HDFS_SECONDARY_NAMENODE");
         CmClusterValidator validator = new CmClusterValidator();
         Assert.assertFalse(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
      }
   }

   @Test
   public void testServiceDependency() {
      try {
         ClusterBlueprint blueprint = generateBlueprint();
         blueprint.getNodeGroups().get(0).getRoles().remove("HDFS_NAMENODE");
         blueprint.getNodeGroups().get(0).getRoles().remove("HDFS_SECONDARY_NAMENODE");
         blueprint.getNodeGroups().get(1).getRoles().remove("HDFS_DATANODE");
         CmClusterValidator validator = new CmClusterValidator();
         Assert.assertFalse(validator.validateBlueprint(blueprint));
      } catch (ValidationException e) {
         System.out.println("warning_msg_list: " + e.getWarningMsgList());
         System.out.println("error_msg_list: " + e.getFailedMsgList());
      }
   }

   private ClusterBlueprint generateBlueprint() {
      //return SerialUtils.getObjectByJsonString(ClusterBlueprint.class, CommonUtil.readJsonFile("simple_blueprint.json"));
      ClusterBlueprint blueprint = new ClusterBlueprint();
      blueprint.setName("cluster01");
      HadoopStack hadoopStack = new HadoopStack();
      hadoopStack.setDistro("CDH-5.0.2");
      blueprint.setHadoopStack(hadoopStack);

      List<NodeGroupInfo> groups = new ArrayList<NodeGroupInfo>();

      NodeGroupInfo group01 = new NodeGroupInfo();
      group01.setName("master");
      List<String> roles01 = new ArrayList<String>();
      roles01.add("HDFS_NAMENODE");
      roles01.add("HDFS_SECONDARY_NAMENODE");
      roles01.add("YARN_RESOURCE_MANAGER");
      roles01.add("YARN_JOB_HISTORY");
      group01.setRoles(roles01);
      Map<String, Object> configs = new HashMap<String, Object>();

      Map<String, String> nnConfig = new HashMap<String, String>();
      nnConfig.put("namenode_java_heapsize", "1024");
      configs.put("HDFS_NAMENODE", nnConfig);

      Map<String, String> snnConfig = new HashMap<String, String>();
      snnConfig.put("secondary_namenode_java_heapsize", "1024");
      configs.put("HDFS_SECONDARY_NAMENODE", snnConfig);
      group01.setConfiguration(configs);

      NodeInfo node01 = new NodeInfo();
      node01.setRack("/rack01");
      List<NodeInfo> nodes01 = new ArrayList<>();
      nodes01.add(node01);
      group01.setNodes(nodes01);

      NodeGroupInfo group02 = new NodeGroupInfo();
      group02.setName("worker");
      List<String> roles02 = new ArrayList<>();
      roles02.add("HDFS_DATANODE");
      roles02.add("YARN_NODE_MANAGER");
      group02.setRoles(roles02);

      Map<String, Object> configs02 = new HashMap<String, Object>();
      Map<String, String> dnConfig = new HashMap<String, String>();
      dnConfig.put("dfs_datanode_failed_volumes_tolerated", "2");
      configs02.put("HDFS_DATANODE", dnConfig);
      group02.setConfiguration(configs02);

      NodeInfo node02 = new NodeInfo();
      node02.setRack("/rack02");
      List<NodeInfo> nodes02 = new ArrayList<>();
      nodes02.add(node02);
      group02.setNodes(nodes02);

      groups.add(group01);
      groups.add(group02);
      blueprint.setNodeGroups(groups);

      Map<String, Object> clusterConfig = new HashMap<String, Object>();

      Map<String, String> hdfsConfig = new HashMap<String, String>();
      hdfsConfig.put("hdfs_namenode_health_enabled", "true");
      clusterConfig.put("HDFS", hdfsConfig);

      Map<String, String> snnConfig02 = new HashMap<String, String>();
      snnConfig02.put("secondarynamenode_java_opts", "-XX:+UseParNewGC");
      clusterConfig.put("HDFS_SECONDARY_NAMENODE", snnConfig02);

      blueprint.setConfiguration(clusterConfig);

      return blueprint;
   }
}
