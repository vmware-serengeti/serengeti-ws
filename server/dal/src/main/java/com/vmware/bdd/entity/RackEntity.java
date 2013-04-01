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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "rack_seq", allocationSize = 1)
@Table(name = "rack")
public class RackEntity extends EntityBase {
   @Enumerated(EnumType.STRING)
   @Column(name = "name", nullable = false)
   private String name;

   @OneToMany(mappedBy = "rack", fetch = FetchType.LAZY)
   @Cascade({ CascadeType.SAVE_UPDATE, CascadeType.REMOVE })
   private List<PhysicalHostEntity> hosts;

   public RackEntity() {
      this.hosts = new ArrayList<PhysicalHostEntity>();
   }

   public RackEntity(String name) {
      this.name = name;
      this.hosts = new ArrayList<PhysicalHostEntity>();
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public List<PhysicalHostEntity> getHosts() {
      return hosts;
   }

   public void setHosts(List<PhysicalHostEntity> hosts) {
      this.hosts = hosts;
   }

}
