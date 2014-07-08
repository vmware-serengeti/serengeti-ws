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
      ApiParcel parcel2 = new ApiParcel();
      parcel2.setProduct("CDH");
      parcel2.setVersion("4.7.0-1.cdh4.7.0.p0.13");
      parcel2.setStage(AvailableParcelStage.AVAILABLE_REMOTELY.toString());
      ApiParcelState state2 = new ApiParcelState();
      state2.setProgress(0);
      state2.setTotalProgress(100);
      parcel2.setState(state2);
      parcels.add(parcel2);
      ApiParcel parcel3 = new ApiParcel();
      parcel3.setProduct("CDH");
      parcel3.setVersion("5.0.1-1.cdh5.0.1.p0.13");
      parcel3.setStage(AvailableParcelStage.AVAILABLE_REMOTELY.toString());
      ApiParcelState state3 = new ApiParcelState();
      state3.setProgress(0);
      state3.setTotalProgress(100);
      parcel3.setState(state3);
      parcels.add(parcel3);
      parcelResource = new FakeParcelResource(parcel);
   }


   @Override
   public ApiParcelUsage getParcelUsage() {
      return null;
   }

   @Override
   public ApiParcelList readParcels(@DefaultValue("summary") DataView dataView) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return parcels;
   }

   @Override
   public ParcelResource getParcelResource(String s, String s2) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      System.out.println("repo: " + s + ", exact version: " + s2);
      return parcelResource;
   }
}
