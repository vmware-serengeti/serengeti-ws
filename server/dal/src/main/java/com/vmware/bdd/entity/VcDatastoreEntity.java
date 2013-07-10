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
package com.vmware.bdd.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;

/**
 * Work as a message queue
 *
 */
@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "vc_datastore_seq", allocationSize = 1)
@Table(name = "vc_datastore")
public class VcDatastoreEntity extends EntityBase {

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
}
