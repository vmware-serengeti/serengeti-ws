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
import java.util.List;
import java.util.Set;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.dal.IClusterDAO;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;


/**
 * @author Terry Li
 * @since 0.8
 * @version 0.8
 * 
 */
@Repository
@Transactional(readOnly = true)
public class ClusterDAO extends BaseDAO<ClusterEntity> implements IClusterDAO {

   @Override
   public ClusterEntity findByName(String name) {
      return findUniqueByCriteria(Restrictions.eq("name", name));
   }

   @Override
   public List<String> findClustersByUsedResourcePool(String rpName) {
      List<String> clusterNames = new ArrayList<String>();

      // query cluster entities to check if resource pool is used by them.
      List<ClusterEntity> clusters = findAll();
      for (ClusterEntity cluster : clusters) {
         Set<VcResourcePoolEntity> usedRps = cluster.getUsedRps();
         for (VcResourcePoolEntity vcRp : usedRps) {
            if (rpName.equals(vcRp.getName())) {
               clusterNames.add(cluster.getName());
               break;
            }
         }
      }
      return clusterNames;
   }

   @Override
   public List<String> findClustersByUsedDatastores(Set<String> patterns) {
      List<String> clusterNames = new ArrayList<String>();

      // query cluster entities to check if the datastore is used by them.
      List<ClusterEntity> clusters = findAll();
      for (ClusterEntity cluster : clusters) {
         Set<String> usedDS = cluster.getUsedVcDatastores();
         if (CommonUtil.matchDatastorePattern(patterns, usedDS)) {
            clusterNames.add(cluster.getName());
         }
      }
      return clusterNames;
   }

   @Override
   public boolean isExist(String name) {
      if (findByName(name) == null)
         return true;
      return false;
   }

   @Override
   @Transactional
   public void updateStatus(String name, ClusterStatus status) {
      ClusterEntity cluster = findByName(name);
      AuAssert.check(cluster != null);
      cluster.setStatus(status);
   }

   @Override
   public List<NodeEntity> getAllNodes(String name) {
      List<NodeEntity> nodes = new ArrayList<NodeEntity>();

      ClusterEntity cluster = findByName(name);
      for (NodeGroupEntity group : cluster.getNodeGroups()) {
         nodes.addAll(group.getNodes());
      }
      return nodes;
   }

   @Override
   @Transactional
   public ClusterStatus getStatus(String clusterName) {
      ClusterEntity cluster = findByName(clusterName);
      AuAssert.check(cluster != null);
      return cluster.getStatus();
   }

   @Override
   @Transactional
   public void updateLastStatus(String clusterName, ClusterStatus status) {
      ClusterEntity cluster = findByName(clusterName);
      AuAssert.check(cluster != null);
      cluster.setLastStatus(status);
   }

}
