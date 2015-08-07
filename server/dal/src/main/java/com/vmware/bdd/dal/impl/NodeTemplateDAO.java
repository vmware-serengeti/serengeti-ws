/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.dal.INodeTemplateDAO;
import com.vmware.bdd.entity.NodeTemplateEntity;

@Repository
@Transactional(readOnly = true)
public class NodeTemplateDAO extends BaseDAO<NodeTemplateEntity> implements INodeTemplateDAO {

   @Override
   public NodeTemplateEntity findByMoid(String vmMoid) {
      return findUniqueByCriteria(Restrictions.eq("moid", vmMoid).ignoreCase());
   }

   @Override
   public List<NodeTemplateEntity> findByName(String name) {
      return this.findByCriteria(Restrictions.eq("name", name).ignoreCase());
   }

   @Override
   public List<NodeTemplateEntity> findAllOrderByName() {
      return this.findAll("name", true);
   }

}
