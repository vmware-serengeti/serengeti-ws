/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.resmgmt;

import com.vmware.aurora.vc.*;

/**
 * Created by xiaoliangl on 8/3/15.
 */
public enum VC_RESOURCE_TYPE {
   DATA_CENTER, CLUSTER, DATA_STORE, HOST, RESOURCE_POOL;

   public static VC_RESOURCE_TYPE fromType(VcObject vcObject) {
      if(vcObject instanceof VcDatacenter) {
         return DATA_CENTER;
      } else if(vcObject instanceof VcCluster) {
         return CLUSTER;
      } else if(vcObject instanceof VcDatastore) {
         return DATA_STORE;
      } else if(vcObject instanceof VcHost) {
         return HOST;
      }  else if(vcObject instanceof VcResourcePool) {
         return RESOURCE_POOL;
      }

      return null;
   }
}