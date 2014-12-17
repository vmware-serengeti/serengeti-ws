/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.usermgmt.persist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.dal.IBaseDAO;

/**
 * Created By xiaoliangl on 11/28/14.
 */
@Component
@Transactional(propagation = Propagation.REQUIRED)
public class MgmtVmCfgEao {

   @Autowired
   @Qualifier(value = "mgmtVmCfgDao")
   private IBaseDAO<MgmtVmCfgItemEntity> mgmtVmCfgDao;

   public Map<String, String> findAll() {
      List<MgmtVmCfgItemEntity> cfgItemList = mgmtVmCfgDao.findAll();

      Map<String, String> cfgMap = null;

      if (cfgItemList != null && !cfgItemList.isEmpty()) {
         cfgMap = new HashMap<>();
         for (MgmtVmCfgItemEntity mgmtVmCfgItemEntity : cfgItemList) {
            cfgMap.put(mgmtVmCfgItemEntity.getName(), mgmtVmCfgItemEntity.getValue());
         }
      } else {
         throw new UserMgmtPersistException("MGMT_VM_CFG.CFG_NOT_FOUND", null);
      }


      return cfgMap;
   }

   public void update(Map<String, String> newConfig) {
      for (Map.Entry<String, String> cfgEntry : newConfig.entrySet()) {
         MgmtVmCfgItemEntity cfgItemEntity = mgmtVmCfgDao.findById(cfgEntry.getKey());
         if (cfgItemEntity != null) {
            cfgItemEntity.setValue(cfgEntry.getValue());
            mgmtVmCfgDao.update(cfgItemEntity);
         } else {
            cfgItemEntity = new MgmtVmCfgItemEntity();
            cfgItemEntity.setName(cfgEntry.getKey());
            cfgItemEntity.setValue(cfgEntry.getValue());
            mgmtVmCfgDao.insert(cfgItemEntity);
         }
      }
   }
}
