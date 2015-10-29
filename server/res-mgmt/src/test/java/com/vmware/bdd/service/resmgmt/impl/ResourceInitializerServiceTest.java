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
package com.vmware.bdd.service.resmgmt.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.bdd.apitypes.NetworkDnsType;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mockit;
import mockit.Tested;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.dal.IDatastoreDAO;
import com.vmware.bdd.dal.INetworkDAO;
import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.dal.IServerInfoDAO;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.ServerInfoEntity;
import com.vmware.bdd.entity.VcDatastoreEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.service.resmgmt.IDatastoreService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourceInitializerService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.service.utils.MockResourceService;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
public class ResourceInitializerServiceTest extends BaseResourceTest{

   private static final Logger logger = Logger
         .getLogger(ResourceInitializerServiceTest.class);

   @Tested
   private ResourceInitializerService service;

   private IServerInfoDAO serverInfoDao;
   private IResourcePoolService rpSvc;
   private IDatastoreService dsSvc;
   private INetworkService networkSvc;

   @BeforeClass
   public void beforeClass() {
      service = new ResourceInitializerService();
   }

   @AfterMethod(groups = { "res-mgmt" })
   public void tearDownMockup() {
      Mockit.tearDownMocks();
   }

   @BeforeMethod(groups = { "res-mgmt" })
   public void setMockup() {
      Mockit.setUpMock(MockResourceService.class);
   }

   @Test(groups = { "res-mgmt"})
   public void isResoruceInitialized() {
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
      boolean result = service.isResourceInitialized();
      Assert.assertEquals(result, true);
   }

   @Test(groups = { "res-mgmt"})
   public void isResoruceInitialized_WithoutInitialization() {
      serverInfoDao = new MockUp<IServerInfoDAO>() {
         @Mock
         List<ServerInfoEntity> findAll() {
            List<ServerInfoEntity> serverInfos =
                  new ArrayList<ServerInfoEntity>();
            ServerInfoEntity serverInfo = new ServerInfoEntity();
            serverInfo.setResourceInitialized(false);
            serverInfos.add(serverInfo);
            return serverInfos;
         }
      }.getMockInstance();
      service.setServerInfoDao(serverInfoDao);
      boolean result = service.isResourceInitialized();
      Assert.assertEquals(result, false);
   }

   @Test(groups = { "res-mgmt"})
   public void isResoruceInitialized_WithoutInitializationEntity() {
      serverInfoDao = new MockUp<IServerInfoDAO>() {
         @Mock
         List<ServerInfoEntity> findAll() {
            List<ServerInfoEntity> serverInfos =
                  new ArrayList<ServerInfoEntity>();
            return serverInfos;
         }
      }.getMockInstance();
      service.setServerInfoDao(serverInfoDao);
      boolean result = service.isResourceInitialized();
      Assert.assertEquals(result, false);
   }


   @Test(groups = { "res-mgmt"})
   public void updateServerInfo() {
      serverInfoDao = new MockUp<IServerInfoDAO>() {
         @Mock
         List<ServerInfoEntity> findAll() {
            List<ServerInfoEntity> serverInfos =
                  new ArrayList<ServerInfoEntity>();
            ServerInfoEntity serverInfo = new ServerInfoEntity();
            serverInfo.setResourceInitialized(false);
            serverInfos.add(serverInfo);
            return serverInfos;
         }

         @Mock(invocations = 1)
         void update(ServerInfoEntity serverInfo) {
            logger.info("mock update server info");
         }

      }.getMockInstance();
      service.setServerInfoDao(serverInfoDao);
      service.updateOrInsertServerInfo();
   }

   @Test(groups = { "res-mgmt"})
   public void insertServerInfo() {
      serverInfoDao = new MockUp<IServerInfoDAO>() {
         @Mock
         List<ServerInfoEntity> findAll() {
            List<ServerInfoEntity> serverInfos =
                  new ArrayList<ServerInfoEntity>();
            return serverInfos;
         }

         @Mock(invocations = 1)
         void insert(ServerInfoEntity serverInfo) {
            logger.info("mock insert server info");
         }

      }.getMockInstance();
      service.setServerInfoDao(serverInfoDao);
      service.updateOrInsertServerInfo();
   }

   @Test(groups = { "res-mgmt"})
   public void noOperationForServerInfo() {
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

         @Mock(invocations = 0)
         void insert(ServerInfoEntity serverInfo) {
            logger.info("mock insert server info");
         }
      }.getMockInstance();
      service.setServerInfoDao(serverInfoDao);
      service.updateOrInsertServerInfo();
   }

   @Test(groups = { "res-mgmt"})
   public void addResourceIntoDB() {
      rpSvc = new MockUp<IResourcePoolService>() {
         @Mock
         void addResourcePool(String rpName, String vcClusterName,
               String vcResourcePool) {
            logger.info("mock rp service to add rp");
            Assert.assertEquals(rpName, "defaultRP");
            Assert.assertEquals(vcClusterName, "testCluster");
            Assert.assertEquals(vcResourcePool, "serengetiRP");
         }
      }.getMockInstance();
      dsSvc = new MockUp<IDatastoreService>() {
         @Mock
         void addDatastores(String name, DatastoreType type, List<String> spec, boolean regex) {
            logger.info("mock datastore service to add ds");
            Assert.assertEquals(name, "defaultDSShared");
            Assert.assertEquals(type, DatastoreType.SHARED);
            Assert.assertEquals(spec.size(), 1);
            Assert.assertEquals(regex, false);
         }
      }.getMockInstance();
      networkSvc = new MockUp<INetworkService>() {
         @Mock
         NetworkEntity addDhcpNetwork(final String name,
               final String portGroup, NetworkDnsType dnsType) {
            Assert.assertEquals(dnsType, NetworkDnsType.NORMAL);
            Assert.assertEquals(name, "defaultNetwork");
            Assert.assertEquals(portGroup, "serengetiNet");
            return new NetworkEntity();
         }
      }.getMockInstance();
      service.setRpSvc(rpSvc);
      service.setDsSvc(dsSvc);
      service.setNetworkSvc(networkSvc);
      Map<DatastoreType, List<String>> dsMaps = new HashMap<DatastoreType, List<String>>();
      List<String> dsNames = new ArrayList<String>();
      dsNames.add("datastore1");
      dsMaps.put(DatastoreType.SHARED, dsNames);
      service.addResourceIntoDB("testCluster", "serengetiRP", "serengetiNet",
            dsMaps);
   }

   @Test(groups={"res-mgmt","dependsOnVC","dependsOnDB"})
   public void initResource(){
      super.init();
      IResourceInitializerService svc = ctx.getBean(IResourceInitializerService.class);

      IResourcePoolDAO rpDao = ctx.getBean(IResourcePoolDAO.class);
      IDatastoreDAO dsDao = ctx.getBean(IDatastoreDAO.class);
      INetworkDAO networkDao = ctx.getBean(INetworkDAO.class);
      VcResourcePoolEntity rpEntity = rpDao.findByName(ResourceInitializerService.DEFAULT_RP);
      if(rpEntity != null){
         rpDao.delete(rpEntity);
      }
      List<VcDatastoreEntity> dss = dsDao.findByName(ResourceInitializerService.DEFAULT_DS_SHARED);
      if(dss != null && dss.size() > 0){
         for(VcDatastoreEntity ds : dss){
            dsDao.delete(ds);
         }
      }
      dss = dsDao.findByName(ResourceInitializerService.DEFAULT_DS_LOCAL);
      if(dss != null && dss.size() > 0){
         for(VcDatastoreEntity ds : dss){
            dsDao.delete(ds);
         }
      }
      NetworkEntity network = networkDao.findNetworkByName(ResourceInitializerService.DEFAULT_NETWORK);
      if(network != null){
         networkDao.delete(network);
      }
      svc.initResource();

   }

}
