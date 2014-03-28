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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.dal.INodeDAO;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.utils.AuAssert;

/**
 * @author Terry Li
 * @since 0.8
 * @version 0.8
 * 
 */
@Repository
@Transactional(readOnly = true)
public class NodeDAO extends BaseDAO<NodeEntity> implements INodeDAO {

   @Override
   public List<NodeEntity> findByNodeGroups(Collection<NodeGroupEntity> groups) {
      if (!groups.isEmpty()) {
         return findByCriteria(Restrictions.in("nodeGroup", groups));
      } else {
         return new ArrayList<NodeEntity>(0);
      }
   }

   @Override
   public NodeEntity findByName(NodeGroupEntity group, String name) {
      AuAssert.check(group != null);
      return findUniqueByCriteria(Restrictions.and(
            Restrictions.eq("nodeGroup", group),
            Restrictions.eq("vmName", name)));
   }

   @Override
   public NodeEntity findByMobId(String vmId) {
      return findUniqueByCriteria(Restrictions.eq("moId", vmId));
   }

   @Override
   public NodeEntity findByName(String name) {
      return findUniqueByCriteria(Restrictions.eq("vmName", name));
   }

   @Override
   public List<NodeEntity> findByHostName(String hostName) {
      return findByCriteria(Restrictions.eq("hostName", hostName));
   }

   @Override
   @Transactional
   public void updateAction(String moId, String action) {
      NodeEntity node = findByMobId(moId);
      AuAssert.check(node != null);
      node.setAction(action);
   }
}
