/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.DatastoreAdd;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.apitypes.DatastoreReadDetail;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.entity.VcDataStoreEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.VcProviderException;

public class VcDataStoreManager {
   private static final Logger logger = Logger
         .getLogger(VcDataStoreManager.class);

   public Set<String> getAllSharedDatastores() {
      return getAllDatastoresByType(DatastoreType.SHARED);
   }

   public Set<String> getSharedDatastoresByNames(List<String> nameList) {
      if (nameList == null) {
         return null;
      }
      Set<String> result = new HashSet<String>();
      for (String name : nameList) {
         result.addAll(getAllDatastoresByTypeAndName(DatastoreType.SHARED, name));
      }
      return result;
   }

   public Set<String> getLocalDatastoresByNames(List<String> nameList) {
      if (nameList == null) {
         return null;
      }
      Set<String> result = new HashSet<String>();
      for (String name : nameList) {
         result.addAll(getAllDatastoresByTypeAndName(DatastoreType.LOCAL, name));
      }
      return result;
   }

   public Set<String> getAllLocalDatastores() {
      return getAllDatastoresByType(DatastoreType.LOCAL);
   }

   private Set<String> getAllDatastoresByType(DatastoreType type) {
      // load vc resource pools
      List<VcDataStoreEntity> datastores =
         VcDataStoreEntity.findByType(type);
      Set<String> result = getDatastorePattern(datastores);
      logger.debug("got " + type + " datastores: " + result);
      return result;
   }

   private Set<String> getAllDatastoresByTypeAndName(DatastoreType type, String name) {
      // load vc resource pools
      List<VcDataStoreEntity> datastores =
         VcDataStoreEntity.findByNameAndType(type, name);
      if (datastores == null) {
         logger.warn("No datastore found for name " + name + ", type" + type);
      }
      Set<String> result = getDatastorePattern(datastores);
      logger.debug("got datastores for type : " + type + ", name: " + name + result);
      return result;
   }

   public Set<String> getDatastoresByName(String name) {
      List<VcDataStoreEntity> datastores = VcDataStoreEntity.findByName(name);
      if (datastores == null) {
         logger.warn("No datastore found for name " + name);
      }

      return getDatastorePattern(datastores);
   }

   public Set<String> getDatastoresByNameList(List<String> nameList) {
      if (nameList == null) {
         return null;
      }
      Set<String> result = new HashSet<String>();
      for (String name : nameList) {
         result.addAll(getDatastoresByName(name));
      }
      return result;
   }

   private Set<String> getDatastorePattern(List<VcDataStoreEntity> datastores) {
      Set<String> result = new HashSet<String>();
      for (VcDataStoreEntity store : datastores) {
         result.add(store.getVcDatastore());
      }
      return result;
   }

   private void addDatastoreEntity(final DatastoreType type,
         final List<String> datastores, final String name) {
      DAL.inTransactionDo(new Saveable<Void>() {
         public Void body() {
            if (VcDataStoreEntity.nameExisted(name)) {
               throw BddException.ALREADY_EXISTS("datastore", name);
            }
            for (String ds : datastores) {
               VcDataStoreEntity entity =
                     new VcDataStoreEntity();
               entity.setType(type);
               entity.setName(name);
               entity.setVcDatastore(ds);
               entity.insert();
               logger.info("add shared datastore " + ds);
            }
            return null;
         }
      });
   }

   public void addDataStores(DatastoreAdd datastore) {
      addDatastoreEntity(datastore.getType(), datastore.getSpec(), datastore.getName());
   }

   public void addDataStores(String name, DatastoreType type, List<String> spec) {
      addDatastoreEntity(type, spec, name);
   }

   public Set<String> getAllDataStoreName() {
      List<VcDataStoreEntity> datastores =
         VcDataStoreEntity.findAllSortByName();
      Set<String> result = new HashSet<String>();
      for (VcDataStoreEntity ds : datastores) {
         result.add(ds.getName());
      }
      return result;
   }

   public DatastoreRead getDatastoreRead(String name) {
      logger.debug("get datastore " + name);
      List<VcDataStoreEntity> entities = VcDataStoreEntity.findByName(name);
      if (entities.isEmpty()) {
         return null;
      }

      DatastoreRead read = new DatastoreRead();
      read.setName(name);
      read.setType(entities.get(0).getType());
      read.setDatastoreReadDetails(new ArrayList<DatastoreReadDetail>());
      for (VcDataStoreEntity entity : entities) {
         DatastoreReadDetail detail = new DatastoreReadDetail();
         detail.setVcDatastoreName(entity.getVcDatastore());
         read.getDatastoreReadDetails().add(detail);
      }
      logger.debug("found datastore: " + read);
      return read;
   }

   public List<DatastoreRead> getAllDatastoreReads() {
      logger.debug("get all datastores.");
      List<VcDataStoreEntity> entities =
         VcDataStoreEntity.findAllSortByName();
      if (entities.isEmpty()) {
         return null;
      }

      List<DatastoreRead> result = new ArrayList<DatastoreRead>();
      DatastoreRead read = new DatastoreRead();
      read.setName(entities.get(0).getName());
      read.setType(entities.get(0).getType());
      read.setDatastoreReadDetails(new ArrayList<DatastoreReadDetail>());
      result.add(read);
      for (VcDataStoreEntity entity : entities) {
         if (!entity.getName().equals(read.getName())) {
            // new datastore 
            read = new DatastoreRead();
            read.setName(entity.getName());
            read.setType(entity.getType());
            read.setDatastoreReadDetails(new ArrayList<DatastoreReadDetail>());
            result.add(read);
         }
         DatastoreReadDetail detail = new DatastoreReadDetail();
         detail.setVcDatastoreName(entity.getVcDatastore());
         read.getDatastoreReadDetails().add(detail);
      }
      logger.debug("found datastores: " + result);
      return result;
   }

   public void deleteDatastore(String name) {
      logger.debug("delete datastore " + name);
      final List<VcDataStoreEntity> entities = VcDataStoreEntity.findByName(name);
      if (entities.isEmpty()) {
         throw VcProviderException.DATASTORE_NOT_FOUND(name);
      }

      // query cluster configuration to check if data store is used by cluster spec.
      List<ClusterEntity> clusters = ClusterEntity.findClusterEntityByDatastore(name);
      if (!clusters.isEmpty()) {
         List<String> clusterNames = new ArrayList<String>();
         for (ClusterEntity cluster : clusters) {
            clusterNames.add(cluster.getName());
         }
         logger.error("cannot remove datastore, since following cluster spec referenced this datastore: " + clusterNames);
         throw VcProviderException.DATASTORE_IS_REFERENCED_BY_CLUSTER(clusterNames);
      }

      DAL.inTransactionDo(new Saveable<Void>() {
         public Void body() throws VcProviderException {
            for (VcDataStoreEntity entity : entities) {
               entity.delete();
            }
            return null;
         }
      });
      logger.debug("successfully deleted datastore " + name);
   }
}
