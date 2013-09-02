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

import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.BddException;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
@Repository
public class ResourcePoolDAO extends BaseDAO<VcResourcePoolEntity> implements
      IResourcePoolDAO {

   private static final Logger logger = Logger.getLogger(ResourcePoolDAO.class);

   @Override
   @Transactional(readOnly = true)
   public VcResourcePoolEntity findByName(String name) {
      return findUniqueByCriteria(Restrictions.eq("name", name));
   }

   @Override
   @Transactional(readOnly = true)
   public boolean isRPAdded(String vcCluster, String vcRP) {
      VcResourcePoolEntity entity = findByClusterAndRp(vcCluster, vcRP);
      return (entity != null);
   }

   @Override
   @Transactional(readOnly = true)
   public List<VcResourcePoolEntity> findAllOrderByClusterName() {
      Order order = Order.asc("vcCluster");
      return this.findByCriteria(new Order[] { order }, null, null);
   }

   @Override
   @Transactional(readOnly = true)
   public String getNameByClusterAndRp(String vcCluster, String vcRp) {
      VcResourcePoolEntity entity = findByClusterAndRp(vcCluster, vcRp);
      if (entity != null) {
         return entity.getName();
      }
      return null;
   }

   @Override
   @Transactional(readOnly = true)
   public VcResourcePoolEntity findByClusterAndRp(String vcCluster, String vcRp) {
      logger.debug("findByClusterAndRp. cluster:" + vcCluster);
      Map<String, String> propertyNameValues = new HashMap<String, String>();
      propertyNameValues.put("vcCluster", vcCluster);
      propertyNameValues.put("vcResourcePool", vcRp);

      VcResourcePoolEntity entity =
            findUniqueByCriteria(Restrictions.allEq(propertyNameValues));
      return entity;
   }

   @Override
   @Transactional
   public void addResourcePoolEntity(final String rpName,
         final String vcClusterName, final String vcResourcePool) {

      if (findByName(rpName) != null) {
         logger.info("resource pool name " + rpName + " is already existed.");
         throw BddException.ALREADY_EXISTS("Resource pool", rpName);
      }
      VcResourcePoolEntity entity = new VcResourcePoolEntity();
      entity.setName(rpName);
      entity.setVcCluster(vcClusterName);
      entity.setVcResourcePool(vcResourcePool);
      this.insert(entity);
      logger.info("add resource pool " + rpName);
   }

}
