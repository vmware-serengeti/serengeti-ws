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
package com.vmware.bdd.plugin.clouderamgr.service.cm;

import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiParcel;
import com.cloudera.api.model.ApiParcelList;
import com.cloudera.api.v3.ParcelResource;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableParcelStage;

/**
 * Author: Xiaoding Bian
 * Date: 7/7/14
 * Time: 2:31 PM
 */
public class FakeParcelResource implements ParcelResource {
   public ApiParcel parcel;

   public FakeParcelResource(ApiParcel parcel) {
      this.parcel = parcel;
   }

   @Override
   public ApiParcel readParcel() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return parcel;
   }

   @Override
   public ApiCommand startDownloadCommand() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      parcel.getState().setProgress(20);
      try {
         Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      parcel.getState().setProgress(100);
      parcel.setStage(AvailableParcelStage.DOWNLOADED.toString());
      return null;
   }

   @Override
   public ApiCommand cancelDownloadCommand() {
      return null;
   }

   @Override
   public ApiCommand removeDownloadCommand() {
      return null;
   }

   @Override
   public ApiCommand startDistributionCommand() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      parcel.getState().setProgress(20);
      try {
         Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      parcel.getState().setProgress(100);
      parcel.setStage(AvailableParcelStage.DISTRIBUTED.toString());
      return null;
   }

   @Override
   public ApiCommand cancelDistributionCommand() {
      return null;
   }

   @Override
   public ApiCommand startRemovalOfDistributionCommand() {
      return null;
   }

   @Override
   public ApiCommand activateCommand() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      parcel.getState().setProgress(20);
      try {
         Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      parcel.getState().setProgress(100);
      parcel.setStage(AvailableParcelStage.ACTIVATED.toString());
      return null;
   }

   @Override
   public ApiCommand deactivateCommand() {
      return null;
   }
}
