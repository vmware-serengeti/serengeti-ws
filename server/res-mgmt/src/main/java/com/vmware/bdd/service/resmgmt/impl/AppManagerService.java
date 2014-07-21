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

package com.vmware.bdd.service.resmgmt.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.dal.IAppManagerDAO;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.service.resmgmt.IAppManagerService;


/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 5:46 PM
 */
@Service
public class AppManagerService implements IAppManagerService{

   private static Logger logger = Logger.getLogger(AppManagerService.class);

   private IAppManagerDAO appManagerDAO;

   @Override
   public synchronized void addAppManager(AppManagerAdd appManagerAdd) {
      logger.info((new Gson()).toJson(appManagerAdd));
      // TODO: validation
      AppManagerEntity appManagerEntity = new AppManagerEntity(appManagerAdd);
      appManagerDAO.insert(appManagerEntity);
   }

   @Override
   public AppManagerEntity findAppManagerByName(String name) {
      return appManagerDAO.findByName(name);
   }

   public IAppManagerDAO getAppManagerDAO() {
      return appManagerDAO;
   }

   @Autowired
   public void setAppManagerDAO(IAppManagerDAO appManagerDAO) {
      this.appManagerDAO = appManagerDAO;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.IAppManagerService#getAppManagerRead(java.lang.String)
    */
   @Override
   @Transactional(readOnly = true)
   public AppManagerRead getAppManagerRead(String name) {
      AppManagerEntity entity = appManagerDAO.findByName(name);
      if (entity == null) {
         return null;
      }

      return toAppManagerRead(entity);
   }

   /**
    * @param entity
    * @return
    */
   private AppManagerRead toAppManagerRead(AppManagerEntity entity) {
      AppManagerRead read = new AppManagerRead();
      read.setName(entity.getName());
      read.setDescription(entity.getDescription());
      read.setType(entity.getType());
      read.setUrl(entity.getUrl());
      read.setUsername(entity.getUsername());
      read.setPassword(entity.getPassword());
      read.setSslCertificate(entity.getSslCertificate());
      return read;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.IAppManagerService#getAllAppManagerReads()
    */
   @Override
   @Transactional(readOnly = true)
   public List<AppManagerRead> getAllAppManagerReads() {
      List<AppManagerEntity> entities = appManagerDAO.findAllSortByName();
      List<AppManagerRead> reads = new ArrayList<AppManagerRead>();
      AppManagerRead read;
      for (AppManagerEntity entity : entities) {
         reads.add(toAppManagerRead(entity));
      }
      return reads;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.IAppManagerService#findAll()
    */
   @Override
   public List<AppManagerEntity> findAll() {
      return appManagerDAO.findAll();
   }
}
