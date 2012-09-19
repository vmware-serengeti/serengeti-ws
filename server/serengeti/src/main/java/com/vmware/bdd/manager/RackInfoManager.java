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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import com.vmware.bdd.apitypes.RackInfo;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.PhysicalHostEntity;
import com.vmware.bdd.entity.RackEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.exception.BddException;

public class RackInfoManager {

   private void validateRaskInfoList(final List<RackInfo> racksInfo) {
      Set<String> racks = new TreeSet<String>();
      Set<String> hosts = new TreeSet<String>();

      for (RackInfo rack : racksInfo) {
         if (racks.contains(rack.getName())) {
            throw BddException.INVALID_PARAMETER("duplicated rack", rack.getName());
         }
         racks.add(rack.getName());
         for (String host : rack.getHosts()) {
            if (hosts.contains(host)) {
               throw BddException.INVALID_PARAMETER("duplicated host", host);
            }
            hosts.add(host);
         }
      }
   }

   public void importRackInfo(final List<RackInfo> racksInfo) {
      validateRaskInfoList(racksInfo);

      DAL.inRwTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() throws Exception {
            List<RackEntity> racks = RackEntity.findAllRacks();
            // clean all racks
            for (RackEntity rack : racks) {
               rack.delete();
            }

            for (RackInfo rack : racksInfo) {
               RackEntity.addRack(rack.getName(), rack.getHosts());
            }

            return null;
         }
      });
   }

   public List<RackInfo> exportRackInfo() {
      return DAL.autoTransactionDo(new Saveable<List<RackInfo>>() {
         @Override
         public List<RackInfo> body() throws Exception {
            List<RackInfo> racksInfo = new ArrayList<RackInfo>();

            List<RackEntity> racks = RackEntity.findAllRacks();
            for (RackEntity rack : racks) {
               List<PhysicalHostEntity> hostEntities = rack.getHosts();
               if (hostEntities != null && !hostEntities.isEmpty()) {
                  List<String> hosts = new ArrayList<String>(hostEntities.size());
                  for (PhysicalHostEntity he : hostEntities) {
                     hosts.add(he.getName());
                  }
                  RackInfo rackInfo = new RackInfo();
                  rackInfo.setName(rack.getName());
                  rackInfo.setHosts(hosts);
                  racksInfo.add(rackInfo);
               }
            }

            return racksInfo;
         }
      });
   }

   public Map<String, String> exportHostRackMap() {
      Map<String, String> hostRackMap = new TreeMap<String, String>();
      List<RackInfo> rackInfo = exportRackInfo();

      for (RackInfo ri : rackInfo) {
         for (String host : ri.getHosts()) {
            hostRackMap.put(host, ri.getName());
         }
      }

      return hostRackMap;
   }
}