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
package com.vmware.bdd.software.mgmt.impl;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.software.mgmt.thrift.ClusterAction;
import com.vmware.bdd.software.mgmt.thrift.ClusterData;
import com.vmware.bdd.software.mgmt.thrift.ClusterOperation;
import com.vmware.bdd.software.mgmt.thrift.GroupData;
import com.vmware.bdd.software.mgmt.thrift.OperationStatus;
import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;
import com.vmware.bdd.software.mgmt.thrift.ServerData;


public class SoftwareManagementClientTest {

   private static final String clusterName = "cc2";
   private static final String configFileName =
         "/opt/serengeti/logs/task/3/1/cc2.json";

   private SoftwareManagementClient client;

   @BeforeClass(groups = { "software-management" })
   public void init() {
      client = new SoftwareManagementClient();
      client.init();
   }

   @AfterClass(groups = { "software-management" })
   public void close() {
      client.close();
   }

   @Test(groups = { "software-management" })
   public void createCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.CREATE);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      client.runClusterOperation(clusterOperation);
   }

   @Test(groups = { "software-management" })
   public void queryCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.QUERY);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      client.runClusterOperation(clusterOperation);
   }

   @Test(groups = { "software-management" })
   public void updateCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.UPDATE);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      client.runClusterOperation(clusterOperation);
   }

   @Test(groups = { "software-management" })
   public void startCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.START);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      client.runClusterOperation(clusterOperation);
   }

   @Test(groups = { "software-management" })
   public void stopCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.STOP);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      client.runClusterOperation(clusterOperation);
   }

   @Test(groups = { "software-management" })
   public void deleteCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.DESTROY);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      client.runClusterOperation(clusterOperation);
   }

   @Test(groups = { "software-management" })
   public void configureCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.CONFIGURE);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      client.runClusterOperation(clusterOperation);
   }

   @Test(groups = { "software-management" })
   public void enableChefClientFlag() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.ENABLE_CHEF_CLIENT);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      client.runClusterOperation(clusterOperation);
   }

   @Test(groups = { "software-management" })
   public void getOperationStatusWithDetail() {
      OperationStatusWithDetail status =
            client.getOperationStatusWithDetail(clusterName);
      Assert.assertNotNull(status);
      ClusterData clusterData = status.getClusterData();
      String clusterName = clusterData.getClusterName();
      System.out.println("clusterName : " + clusterName);
      Map<String, GroupData> groups = clusterData.getGroups();
      for (String groupName : groups.keySet()) {
         System.out.println("group name key: " + groupName);
         GroupData groupData = groups.get(groupName);
         System.out.println("group data name: " + groupData.getGroupName());
         List<ServerData> servers = groupData.getInstances();
         for (ServerData serverData : servers) {
            System.out.println("server data - name:" + serverData.getName()
                  + ", status: " + serverData.getStatus() + ", action: "
                  + serverData.getStatus());
         }
      }
   }

}
