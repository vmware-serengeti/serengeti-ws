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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.dal.IDatastoreDAO;
import com.vmware.bdd.entity.VcDatastoreEntity;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
@Repository
@Transactional(readOnly = true)
public class DatastoreDAO extends BaseDAO<VcDatastoreEntity> implements
      IDatastoreDAO {

   private static final Logger logger = Logger.getLogger(DatastoreDAO.class);

   @Override
   public List<VcDatastoreEntity> findAllSortByName() {
      Order order = Order.asc("name");
      return this.findByCriteria(new Order[] { order }, null, null);
   }

   @Override
   public List<VcDatastoreEntity> findByType(DatastoreType type) {
      return this.findByCriteria(Restrictions.eq("type", type));
   }

   @Override
   public List<VcDatastoreEntity> findByName(String name) {
      return this.findByCriteria(Restrictions.eq("name", name));
   }

   @Override
   public List<VcDatastoreEntity> findByNameAndType(DatastoreType type,
         String name) {
      logger.debug("findByNameAndType, name:" + name);
      Map<String, Object> propertyNameValues = new HashMap<String, Object>();
      propertyNameValues.put("name", name);
      propertyNameValues.put("type", type);
      return this.findByCriteria(Restrictions.allEq(propertyNameValues));
   }

   @Override
   public boolean nameExisted(String name) {
      List<VcDatastoreEntity> entities = findByName(name);
      return (entities != null && entities.size() > 0);
   }

   @Override
   public VcDatastoreEntity findByNameAndDatastore(String name,
         String datastoreName) {
      Map<String, String> propertyNameValues = new HashMap<String, String>();
      propertyNameValues.put("name", name);
      propertyNameValues.put("vcDatastore", datastoreName);

      return this.findUniqueByCriteria(Restrictions.allEq(propertyNameValues));
   }
}
