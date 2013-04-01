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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.dal.INodeGroupDAO;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeGroupEntity;

/**
 * @author Terry Li
 * @since 0.8
 * @version 0.8
 * 
 */
@Repository
@Transactional(readOnly = true)
public class NodeGroupDAO extends BaseDAO<NodeGroupEntity> implements
      INodeGroupDAO {

   @Override
   public NodeGroupEntity findByName(ClusterEntity cluster, String groupName) {
      Map<String, Object> conditions = new HashMap<String, Object>();
      conditions.put("cluster", cluster);
      conditions.put("name", groupName);
      return findUniqueByCriteria(Restrictions.allEq(conditions));
   }

   @Override
   public List<NodeGroupEntity> findAllByClusters(
         Collection<ClusterEntity> clusters) {
      if (!clusters.isEmpty()) {
         return findByCriteria(Restrictions.in("cluster", clusters));
      } else {
         return new ArrayList<NodeGroupEntity>(0);
      }
   }

}
