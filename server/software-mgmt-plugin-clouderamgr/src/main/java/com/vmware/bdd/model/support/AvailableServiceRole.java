/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.model.support;

import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.util.List;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 3:37 PM
 */
public class AvailableServiceRole {

   public static final int VERSION_UNBOUNDED = -1;
   public static final String ROOT_SERVICE = "CLUSTER";

   @Expose
   private String name;

   @Expose(serialize = false)
   private AvailableServiceRole parent;

   @Expose
   private AvailableParcelRepo repository;

   @Expose
   private int versionApiMin;

   @Expose
   private int versionApiMax;

   @Expose
   private int versionCdhMin;

   @Expose
   private int versionCdhMax;

   @Expose
   private List<AvailableConfiguration> availableConfigurations; // TODO: use map instead

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public AvailableServiceRole getParent() {
      return parent;
   }

   public void setParentObject(AvailableServiceRole parent) {
      this.parent = parent;
   }

   public String getParentName() {
      if (parent != null) {
         return parent.getName();
      }
      return null;
   }

   public void setParent(String parent) {
      try {
         this.parent = null;
         if (parent != null) {
            this.parent = AvailableServiceRoleLoader.getServiceRole(parent);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public AvailableParcelRepo getRepository() {
      return repository;
   }

   public void setRepository(String repository) {
      this.repository = AvailableParcelRepo.valueOf(repository);
   }

   public int getVersionApiMin() {
      return versionApiMin;
   }

   public void setVersionApiMin(int versionApiMin) {
      this.versionApiMin = versionApiMin;
   }

   public int getVersionApiMax() {
      return versionApiMax;
   }

   public void setVersionApiMax(int versionApiMax) {
      this.versionApiMax = versionApiMax;
   }

   public int getVersionCdhMin() {
      return versionCdhMin;
   }

   public void setVersionCdhMin(int versionCdhMin) {
      this.versionCdhMin = versionCdhMin;
   }

   public int getVersionCdhMax() {
      return versionCdhMax;
   }

   public void setVersionCdhMax(int versionCdhMax) {
      this.versionCdhMax = versionCdhMax;
   }

   public List<AvailableConfiguration> getAvailableConfigurations() {
      return availableConfigurations;
   }

   public void setAvailableConfigurations(List<AvailableConfiguration> availableConfigurations) {
      this.availableConfigurations = availableConfigurations;
   }

   public boolean isService() {
      return getParentName().equalsIgnoreCase(ROOT_SERVICE);
   }

   public boolean isRole() {
      return getParent() != null && getParent().isService();
   }
}
