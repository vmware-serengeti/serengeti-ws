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
package com.vmware.bdd.service.resmgmt.impl;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Created by xiaoliangl on 7/28/15.
 */

@Component
@ManagedResource(objectName= "bean:name=VcInventorySyncCountersMBean" , description= "My First Bean" )
public class VcInventorySyncCounters {
   private AtomicBoolean refreshInProgress = new AtomicBoolean(false);
   private AtomicIntegerArray counters = new AtomicIntegerArray(3);

   @ManagedAttribute(description="The RefreshInProgress Attribute")
   public boolean isRefreshInProgress() {
      return refreshInProgress.get();
   }

   protected void setRefreshInProgress(boolean flag) {
      refreshInProgress.set(flag);
   }

   @ManagedAttribute(description="The InventoryRefresh Attribute")
   public int getInventoryRefresh() {
      return counters.get(0);
   }

   protected void increaseInvRefresh() {
      counters.incrementAndGet(0);
   }

   @ManagedAttribute
   public int getFinishedRefresh() {
      return counters.get(1);
   }

   protected void increaseFinishedRefresh() {
      counters.incrementAndGet(1);
   }

   @ManagedAttribute
   public int getPendingRefresh() {
      return counters.get(2);
   }

   protected void increasePendingRefresh() {
      counters.incrementAndGet(2);
   }

   public void decreasePendingRefresh() {
      counters.decrementAndGet(2);
   }
}
