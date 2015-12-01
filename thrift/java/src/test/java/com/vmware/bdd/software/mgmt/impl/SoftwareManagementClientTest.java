/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.software.mgmt.thrift.ClusterAction;
import com.vmware.bdd.software.mgmt.thrift.ClusterData;
import com.vmware.bdd.software.mgmt.thrift.ClusterOperation;
import com.vmware.bdd.software.mgmt.thrift.GroupData;
import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;
import com.vmware.bdd.software.mgmt.thrift.SoftwareManagement;
import com.vmware.bdd.software.mgmt.thrift.SoftwareManagement.Iface;
import com.vmware.bdd.software.mgmt.thrift.SoftwareManagement.Processor;


public class SoftwareManagementClientTest {

   private static final String clusterName = "cc2";
   private static final String configFileName = "/opt/serengeti/logs/task/3/1/cc2.json";

   private SoftwareManagementClient client;
   private TServer server;
   private SoftwareManagement.Iface handler;

   @BeforeClass(groups = { "software-management" })
   public void init() throws TException {
      handler = mock(SoftwareManagement.Iface.class);
      Processor<Iface> processor = new SoftwareManagement.Processor<>(handler);
      TServerTransport serverTransport = new TServerSocket(Configuration.getInt("management.thrift.port"));
      server = new TSimpleServer(new Args(serverTransport).processor(processor));
      new Thread() {
         public void run() {
            server.serve();
         }
      }.start();
      client = new SoftwareManagementClient();
      client.init();
   }

   @AfterClass(groups = { "software-management" })
   public void close() {
      client.close();
      server.stop();
   }

   @Test(groups = { "software-management" })
   public void createCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.CREATE);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      try {
         when(handler.runClusterOperation(clusterOperation)).thenReturn(0);
      } catch (TException e) {
         fail("Exception when calling runClusterOperation", e);
      }
      assertEquals(client.runClusterOperation(clusterOperation), 0);
   }

   @Test(groups = { "software-management" })
   public void queryCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.QUERY);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      try {
         when(handler.runClusterOperation(clusterOperation)).thenReturn(0);
      } catch (TException e) {
         fail("Exception when calling runClusterOperation", e);
      }
      assertEquals(client.runClusterOperation(clusterOperation), 0);
   }

   @Test(groups = { "software-management" })
   public void updateCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.UPDATE);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      try {
         when(handler.runClusterOperation(clusterOperation)).thenReturn(0);
      } catch (TException e) {
         fail("Exception when calling runClusterOperation", e);
      }
      assertEquals(client.runClusterOperation(clusterOperation), 0);
   }

   @Test(groups = { "software-management" })
   public void startCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.START);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      try {
         when(handler.runClusterOperation(clusterOperation)).thenReturn(0);
      } catch (TException e) {
         fail("Exception when calling runClusterOperation", e);
      }
      assertEquals(client.runClusterOperation(clusterOperation), 0);
   }

   @Test(groups = { "software-management" })
   public void stopCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.STOP);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      try {
         when(handler.runClusterOperation(clusterOperation)).thenReturn(0);
      } catch (TException e) {
         fail("Exception when calling runClusterOperation", e);
      }
      assertEquals(client.runClusterOperation(clusterOperation), 0);
   }

   @Test(groups = { "software-management" })
   public void deleteCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.DESTROY);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      try {
         when(handler.runClusterOperation(clusterOperation)).thenReturn(0);
      } catch (TException e) {
         fail("Exception when calling runClusterOperation", e);
      }
      assertEquals(client.runClusterOperation(clusterOperation), 0);
   }

   @Test(groups = { "software-management" })
   public void configureCluster() {
      ClusterOperation clusterOperation = new ClusterOperation();
      clusterOperation.setAction(ClusterAction.CONFIGURE);
      clusterOperation.setTargetName(clusterName);
      clusterOperation.setSpecFileName(configFileName);
      try {
         when(handler.runClusterOperation(clusterOperation)).thenReturn(0);
      } catch (TException e) {
         fail("Exception when calling runClusterOperation", e);
      }
      assertEquals(client.runClusterOperation(clusterOperation), 0);
   }

   @Test(groups = { "software-management" })
   public void getOperationStatusWithDetail() {
      ClusterData serverClusterData = new ClusterData();
      serverClusterData.setClusterName(clusterName);
      Map<String, GroupData> serverGroups = new HashMap<String, GroupData>();
      GroupData serverGroup1 = new GroupData();
      serverGroup1.setGroupName("group1");
      serverGroups.put("group1", serverGroup1);
      serverClusterData.setGroups(serverGroups);
      OperationStatusWithDetail serverStatus = new OperationStatusWithDetail();
      serverStatus.setClusterData(serverClusterData);
      try {
         when(handler.getOperationStatusWithDetail(clusterName)).thenReturn(serverStatus);
      } catch (TException e) {
         fail("Exception when calling getOperationStatusWithDetail", e);
      }
      OperationStatusWithDetail status = client.getOperationStatusWithDetail(clusterName);
      assertNotNull(status);
      ClusterData clientClusterData = status.getClusterData();
      assertEquals(serverClusterData.getClusterName(), clientClusterData.getClusterName());
      Map<String, GroupData> clientGroups = clientClusterData.getGroups();
      assertNotNull(clientGroups);
      assertEquals(serverGroups.size(), clientGroups.size());
      for (String groupName : clientGroups.keySet()) {
         GroupData clientGroupData = clientGroups.get(groupName);
         GroupData serverGroupData = serverGroups.get(groupName);
         assertEquals(serverGroupData.getGroupName(), clientGroupData.getGroupName());
      }
   }

}
