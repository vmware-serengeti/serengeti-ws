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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.AppManagerAdd;
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
   public void setPluginDAO(IAppManagerDAO pluginDAO) {
      this.appManagerDAO = pluginDAO;
   }
}
