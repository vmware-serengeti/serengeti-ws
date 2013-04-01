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
package com.vmware.bdd.dal.impl;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.dal.IRackDAO;
import com.vmware.bdd.entity.PhysicalHostEntity;
import com.vmware.bdd.entity.RackEntity;

/**
 * @author Terry Li
 * @since 0.8
 * @version 0.8
 * 
 */
@Repository
public class RackDAO extends BaseDAO<RackEntity> implements IRackDAO {

   @Override
   @Transactional
   public RackEntity addRack(String name, List<String> hosts) {
      RackEntity entity = new RackEntity(name);
      for (String host : hosts) {
         PhysicalHostEntity hostEntity = new PhysicalHostEntity(host);
         hostEntity.setRack(entity);
         entity.getHosts().add(hostEntity);
      }
      insert(entity);
      return entity;
   }

}
