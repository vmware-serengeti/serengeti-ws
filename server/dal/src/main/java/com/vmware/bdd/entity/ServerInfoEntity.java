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

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "server_info_seq", allocationSize = 1)
@Table(name = "server_info")
public class ServerInfoEntity extends EntityBase{

   @Column(name = "resource_initialized", nullable = false)
   private boolean resourceInitialized;

   @Column(name = "version")
   private String version;

   @Column(name = "instance_id")
   private String instanceId;

   @Column(name = "deploy_time")
   private Timestamp deployTime;

   /**
    * @return the resourceInitalized
    */
   public boolean isResourceInitialized() {
      return resourceInitialized;
   }

   /**
    * @param resourceInitialized the resourceInitialized to set
    */
   public void setResourceInitialized(boolean resourceInitialized) {
      this.resourceInitialized = resourceInitialized;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getInstanceId() {
      return instanceId;
   }

   public void setInstanceId(String instanceId) {
      this.instanceId = instanceId;
   }

   public Timestamp getDeployTime() {
      return deployTime;
   }

   public void setDeployTime(Timestamp deployTime) {
      this.deployTime = deployTime;
   }

}
