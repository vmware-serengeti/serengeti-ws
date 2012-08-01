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
package com.vmware.bdd.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.dal.DAL;

/**
 * Work as a message queue
 * 
 */
@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "vc_datastore_seq", allocationSize = 1)
@Table(name = "vc_datastore")
public class VcDataStoreEntity extends EntityBase {

   @Column(name = "name", nullable = false)
   private String name;

   @Column(name = "vc_datastore", nullable = false)
   private String vcDatastore;

   @Enumerated(EnumType.STRING)
   @Column(name = "type", nullable = false)
   private DatastoreType type;

   public DatastoreType getType() {
      return type;
   }

   public void setType(DatastoreType type) {
      this.type = type;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getVcDatastore() {
      return vcDatastore;
   }

   public void setVcDatastore(String vcDatastore) {
      this.vcDatastore = vcDatastore;
   }

   public static List<VcDataStoreEntity> findAllSortByName() {
      Order order = Order.asc("name");
      return DAL.findAll(VcDataStoreEntity.class, new Order[] { order });
   }

   public static List<VcDataStoreEntity> findByType(DatastoreType type) {
      return DAL.findByCriteria(VcDataStoreEntity.class,
            Restrictions.eq("type", type));
   }

   public static List<VcDataStoreEntity> findByName(String name) {
      return DAL.findByCriteria(VcDataStoreEntity.class,
            Restrictions.eq("name", name));
   }

   public static List<VcDataStoreEntity> findByNameAndType(DatastoreType type, String name) {
      Map<String, Object> propertyNameValues = new HashMap<String, Object>();
      propertyNameValues.put("name", name);
      propertyNameValues.put("type", type);
      return DAL.findByCriteria(VcDataStoreEntity.class,
                  Restrictions.allEq(propertyNameValues));
   }

   public static boolean nameExisted(String name) {
      List<VcDataStoreEntity> entities = findByName(name);
      return (entities != null && entities.size() > 0);
   }

   public static VcDataStoreEntity findByNameAndDatastore(String name, String datastoreName) {
      Map<String, String> propertyNameValues = new HashMap<String, String>();
      propertyNameValues.put("name", name);
      propertyNameValues.put("vcDatastore", datastoreName);

      return DAL.findUniqueByCriteria(VcDataStoreEntity.class,
                  Restrictions.allEq(propertyNameValues));
   }
}
