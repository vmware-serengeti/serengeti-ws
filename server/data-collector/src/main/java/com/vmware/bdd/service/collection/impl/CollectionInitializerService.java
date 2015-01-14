/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.service.collection.impl;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.vmware.bdd.utils.Constants;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.aop.annotation.RetryTransaction;
import com.vmware.bdd.dal.IServerInfoDAO;
import com.vmware.bdd.entity.ServerInfoEntity;
import com.vmware.bdd.service.collection.ICollectionInitializerService;
import com.vmware.bdd.utils.CommonUtil;

public class CollectionInitializerService implements
      ICollectionInitializerService {

   private static final Logger logger = Logger
         .getLogger(CollectionInitializerService.class);
   private IServerInfoDAO serverInfoDao;
   private SimpleDateFormat df = new SimpleDateFormat(Constants.DEPLOY_TIME_FORMAT);

   @Override
   @Transactional
   @RetryTransaction(2)
   public void setDeployTime(Date date) {
      String timestampStr = df.format(date);
      Timestamp timestamp = Timestamp.valueOf(timestampStr);
      List<ServerInfoEntity> entities = serverInfoDao.findAll();
      if (entities != null && entities.size() == 1) {
         ServerInfoEntity entity = entities.get(0);
         if (entity.getDeployTime() == null) {
            entity.setDeployTime(timestamp);
            serverInfoDao.update(entity);
            logger.debug("Deploy time of server info updated.");
         }
      } else {
         ServerInfoEntity entity = new ServerInfoEntity();
         entity.setDeployTime(timestamp);
         serverInfoDao.insert(entity);
         logger.debug("Deploy time of server info inserted.");
      }
   }

   @Override
   @Transactional(readOnly = true)
   public Date getDeployTime() {
      List<ServerInfoEntity> entities = serverInfoDao.findAll();
      if (entities != null && entities.size() == 1) {
         ServerInfoEntity entity = entities.get(0);
         Timestamp timestamp = entity.getDeployTime();
         if (timestamp != null) {
            String timestampStr = df.format(timestamp);
            logger.debug("deploy time of serengeti is " + timestampStr);
            try {
               return df.parse(timestampStr);
            } catch (ParseException e) {
               logger.warn("Parse timestamp failed: " + e.getLocalizedMessage());
            }
         }
      }
      return null;
   }

   @Override
   @Transactional
   @RetryTransaction(2)
   public void generateInstanceId() {
      List<ServerInfoEntity> entities = serverInfoDao.findAll();
      String newInstanceId = CommonUtil.getUUID();
      if (entities != null && entities.size() == 1) {
         ServerInfoEntity entity = entities.get(0);
         String instanceId = entity.getInstanceId();
         if (CommonUtil.isBlank(instanceId)) {
            entity.setInstanceId(newInstanceId);
            serverInfoDao.update(entity);
            logger.debug("Instance id of server info updated.");
         }
      } else {
         ServerInfoEntity entity = new ServerInfoEntity();
         entity.setInstanceId(newInstanceId);
         serverInfoDao.insert(entity);
         logger.debug("Instance id of server info inserted.");
      }
   }

   @Override
   @Transactional(readOnly = true)
   public String getInstanceId() {
      List<ServerInfoEntity> entities = serverInfoDao.findAll();
      if (entities != null && entities.size() == 1) {
         ServerInfoEntity entity = entities.get(0);
         String instanceId = entity.getInstanceId();
         if (!CommonUtil.isBlank(instanceId)) {
            logger.debug("Instance id of data collection in the serengeti server is:"
                  + instanceId);
            return instanceId;
         }
      }
      return null;
   }

   public IServerInfoDAO getServerInfoDao() {
      return serverInfoDao;
   }

   @Autowired
   public void setServerInfoDao(IServerInfoDAO serverInfoDao) {
      this.serverInfoDao = serverInfoDao;
   }

}
