/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.PhysicalHostEntity;
import com.vmware.bdd.entity.RackEntity;
import com.vmware.bdd.entity.Saveable;

public class RackInfoManager {

   public void importRackInfo(final Map<String, List<String>> rackInfo) {
      DAL.inRwTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() throws Exception {
            List<RackEntity> racks = RackEntity.findAllRacks();
            // clean all racks
            for (RackEntity rack : racks) {
               rack.delete();
            }

            for (String rackName : rackInfo.keySet()) {
               RackEntity.addRack(rackName, rackInfo.get(rackName));
            }

            return null;
         }
      });
   }

   public Map<String, List<String>> exportRackInfo() {
      return DAL.inRoTransactionDo(new Saveable<Map<String, List<String>>>() {
         @Override
         public Map<String, List<String>> body() throws Exception {
            Map<String, List<String>> rackInfo = new TreeMap<String, List<String>>();

            List<RackEntity> racks = RackEntity.findAllRacks();
            for (RackEntity rack : racks) {
               List<PhysicalHostEntity> hostEntities = rack.getHost();
               if (hostEntities != null && !hostEntities.isEmpty()) {
                  List<String> hosts = new ArrayList<String>(hostEntities.size());
                  for (PhysicalHostEntity he : hostEntities) {
                     hosts.add(he.getName());
                  }
                  rackInfo.put(rack.getName(), hosts);
               }
            }

            return rackInfo;
         }
      });
   }
}
