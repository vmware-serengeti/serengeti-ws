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

import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiParcel;
import com.cloudera.api.model.ApiParcelList;
import com.cloudera.api.model.ApiParcelState;
import com.cloudera.api.model.ApiParcelUsage;
import com.cloudera.api.v3.ParcelResource;
import com.cloudera.api.v5.ParcelsResourceV5;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableParcelStage;

import javax.ws.rs.DefaultValue;

/**
 * Author: Xiaoding Bian
 * Date: 7/7/14
 * Time: 2:11 PM
 */
public class FakeParcelsResource implements ParcelsResourceV5 {

   public ApiParcelList parcels;
   public ParcelResource parcelResource;

   public FakeParcelsResource() {
      parcels = new ApiParcelList();
      ApiParcel parcel = new ApiParcel();
      parcel.setProduct("CDH");
      parcel.setVersion("5.0.2-1.cdh5.0.2.p0.13");
      parcel.setStage(AvailableParcelStage.AVAILABLE_REMOTELY.toString());
      ApiParcelState state = new ApiParcelState();
      state.setProgress(0);
      state.setTotalProgress(100);
      parcel.setState(state);
      parcels.add(parcel);
      parcelResource = new FakeParcelResource(parcel);
   }


   @Override
   public ApiParcelUsage getParcelUsage() {
      return null;
   }

   @Override
   public ApiParcelList readParcels(@DefaultValue("summary") DataView dataView) {
      return parcels;
   }

   @Override
   public ParcelResource getParcelResource(String s, String s2) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return parcelResource;
   }
}
