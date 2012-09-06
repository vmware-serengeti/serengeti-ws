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
