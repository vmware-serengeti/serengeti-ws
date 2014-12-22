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
package com.vmware.bdd.service.collection.impl;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mockit;
import mockit.Tested;

import org.apache.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;

import com.vmware.bdd.dal.IServerInfoDAO;
import com.vmware.bdd.entity.ServerInfoEntity;
import com.vmware.bdd.service.collection.impl.CollectionInitializerService;
import com.vmware.bdd.utils.CommonUtil;

public class TestCollectionInitializerService {

   private static final Logger logger = Logger
         .getLogger(TestCollectionInitializerService.class);
   @Tested
   private CollectionInitializerService collectionInitializerService;
   private IServerInfoDAO serverInfoDao;
   private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   final String DEPLOY_TIME = "2014-12-12 07:59:55";

   @BeforeClass
   public void beforeClass() {
      collectionInitializerService = new CollectionInitializerService();
   }

   @AfterMethod
   public void tearDown() {
      Mockit.tearDownMocks();
   }

   @Test(groups = { "TestCollectionInitializerService" })
   public void testSetDeployTime() {
      serverInfoDao = new MockUp<IServerInfoDAO>() {
         @Mock
         List<ServerInfoEntity> findAll() {
            List<ServerInfoEntity> serverInfos =
                  new ArrayList<ServerInfoEntity>();
            ServerInfoEntity serverInfo = new ServerInfoEntity();
            serverInfo.setInstanceId(CommonUtil.getUUID());
            serverInfos.add(serverInfo);
            return serverInfos;
         }

         @Mock(invocations = 1)
         void update(ServerInfoEntity serverInfo) {
            logger.info("mock update server info");
         }

      }.getMockInstance();
      collectionInitializerService.setServerInfoDao(serverInfoDao);
      collectionInitializerService.setDeployTime(new Date());
   }

   @Test(groups = { "TestCollectionInitializerService" })
   public void testGetDeployTime() {
      serverInfoDao = new MockUp<IServerInfoDAO>() {
         @Mock
         List<ServerInfoEntity> findAll() {
            List<ServerInfoEntity> serverInfos =
                  new ArrayList<ServerInfoEntity>();
            ServerInfoEntity serverInfo = new ServerInfoEntity();
            serverInfo.setDeployTime(Timestamp.valueOf(DEPLOY_TIME));
            serverInfos.add(serverInfo);
            return serverInfos;
         }
      }.getMockInstance();
      collectionInitializerService.setServerInfoDao(serverInfoDao);
      Date date = collectionInitializerService.getDeployTime();
      assertEquals(df.format(date), DEPLOY_TIME);
   }

   @Test(groups = { "TestCollectionInitializerService" })
   public void testGenerateInstanceId() {
      serverInfoDao = new MockUp<IServerInfoDAO>() {
         @Mock
         List<ServerInfoEntity> findAll() {
            List<ServerInfoEntity> serverInfos =
                  new ArrayList<ServerInfoEntity>();
            ServerInfoEntity serverInfo = new ServerInfoEntity();
            serverInfo.setDeployTime(Timestamp.valueOf(DEPLOY_TIME));
            serverInfos.add(serverInfo);
            return serverInfos;
         }

         @Mock(invocations = 1)
         void update(ServerInfoEntity serverInfo) {
            logger.info("mock update server info");
         }

      }.getMockInstance();
      collectionInitializerService.setServerInfoDao(serverInfoDao);
      collectionInitializerService.generateInstanceId();
   }

   @Test(groups = { "TestCollectionInitializerService" })
   public void testGetInstanceId() {
      final String instanceId = CommonUtil.getUUID();
      serverInfoDao = new MockUp<IServerInfoDAO>() {
         @Mock
         List<ServerInfoEntity> findAll() {
            List<ServerInfoEntity> serverInfos =
                  new ArrayList<ServerInfoEntity>();
            ServerInfoEntity serverInfo = new ServerInfoEntity();
            serverInfo.setInstanceId(instanceId);
            serverInfos.add(serverInfo);
            return serverInfos;
         }
      }.getMockInstance();
      collectionInitializerService.setServerInfoDao(serverInfoDao);
      assertEquals(collectionInitializerService.getInstanceId(), instanceId);
   }
}
