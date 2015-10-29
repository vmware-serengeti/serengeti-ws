/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "physical_host_seq", allocationSize = 1)
@Table(name = "physical_host")
public class PhysicalHostEntity extends EntityBase {
   @Column(name = "name", nullable = false)
   private String name;

   @ManyToOne
   @JoinColumn(name = "rack_id")
   private RackEntity rack;

   public PhysicalHostEntity() {
   }

   public PhysicalHostEntity(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public RackEntity getRack() {
      return rack;
   }

   public void setRack(RackEntity rack) {
      this.rack = rack;
   }

}
