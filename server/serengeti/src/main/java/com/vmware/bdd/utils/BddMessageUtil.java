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
package com.vmware.bdd.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.HadoopNodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.entity.VcResourcePoolEntity;

@SuppressWarnings("unchecked")
public class BddMessageUtil {
   private static final Logger logger = Logger.getLogger(BddMessageUtil.class);

   public static final String ERROR_CODE_FIELD = "error_code";
   public static final String ERROR_MSG_FIELD = "error_msg";
   public static final String FINISH_FIELD = "finished";
   public static final String SUCCEED_FIELD = "succeed";
   public static final String PROGRESS_FIELD = "progress";
   public static final String PROGRESS_MESSAGE_FIELD = "progress_msg";
   public static final String CLUSTER_DATA_FIELD = "cluster_data";
   public static final String CLUSTER_NAME_FIELD = "name";
   public static final String GROUP_FIELD = "groups";
   public static final String GROUP_NAME_FIELD = "name";
   public static final String INSTANCE_FIELD = "instances";
   public static final String INSTANCE_NAME_FIELD = "name";
   public static final String INSTANCE_MOID_FIELD = "moid";
   public static final String INSTANCE_RACK_FIELD = "rack";
   public static final String INSTANCE_HOSTNAME_FIELD = "hostname";
   public static final String INSTANCE_IP_FIELD = "ip_address";
   public static final String INSTANCE_STATUS_FIELD = "status";
   public static final String INSTANCE_ACTION_FIELD = "action";
   public static final String VC_CLUSTER_FIELD = "vc_cluster";
   public static final String VC_CLUSTER_NAME_FIELD = "name";
   public static final String VC_CLUSTER_VC_RP_FIELD = "vc_rp";
   public static final String DATASTORE_FIELD = "datastores";
   public static final String DATASTORE_NAME_FIELD = "name";

   public static boolean validate(Map<String, Object> mMap, String clusterName) {
      if (mMap.get(FINISH_FIELD) instanceof Boolean
            && mMap.get(SUCCEED_FIELD) instanceof Boolean
            && mMap.get(PROGRESS_FIELD) instanceof Double
            && (Double) mMap.get(PROGRESS_FIELD) <= 100
            && mMap.get(CLUSTER_DATA_FIELD) != null
            && ((HashMap<String, Object>) mMap.get(CLUSTER_DATA_FIELD)).get(
                  CLUSTER_NAME_FIELD).equals(clusterName)) {
         return true;
      }

      return false;
   }

   public static void processClusterData(final String clusterName,
         final String msg) {
      DAL.inRwTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() {
            HashMap<String, Object> clusterData =
                  (new Gson()).fromJson(msg,
                        (new HashMap<String, Object>()).getClass());

            List<HashMap<String, Object>> groups =
                  (ArrayList<HashMap<String, Object>>) clusterData
                        .get(GROUP_FIELD);
            ClusterEntity cluster =
                  ClusterEntity.findClusterEntityByName(clusterName);
            AuAssert.check(cluster.getId() != null);

            if (groups == null) {
               logger.debug("no group information in the message.");
               return null;
            }
            for (NodeGroupEntity group : cluster.getNodeGroups()) {
               for (HashMap<String, Object> groupMap : groups) {
                  if (groupMap != null
                        && group.getName().equals(
                              groupMap.get(GROUP_NAME_FIELD))) {
                     Set<HadoopNodeEntity> nodes =
                           new HashSet<HadoopNodeEntity>();

                     if (groupMap.get(INSTANCE_FIELD) == null)
                        continue;

                     for (HashMap<String, Object> instance : (ArrayList<HashMap<String, Object>>) groupMap
                           .get(INSTANCE_FIELD)) {
                        // ignore instances that don't have vm name field
                        if (instance.get(INSTANCE_NAME_FIELD) == null)
                           continue;

                        logger.debug("found node "
                              + instance.get(INSTANCE_NAME_FIELD)
                              + " in message");

                        HadoopNodeEntity node = new HadoopNodeEntity();

                        node.setVmName((String) instance
                              .get(INSTANCE_NAME_FIELD));
                        node.setMoId((String) instance
                              .get(INSTANCE_MOID_FIELD));
                        node.setRack((String) instance
                              .get(INSTANCE_RACK_FIELD));
                        node.setHostName((String) instance
                              .get(INSTANCE_HOSTNAME_FIELD));
                        node.setIpAddress((String) instance
                              .get(INSTANCE_IP_FIELD));
                        node.setStatus((String) instance
                              .get(INSTANCE_STATUS_FIELD));
                        node.setAction((String) instance
                              .get(INSTANCE_ACTION_FIELD));

                        if ((instance.get(VC_CLUSTER_FIELD)) != null) {
                           String vcCluster =
                                 (String) ((HashMap<String, Object>) instance
                                       .get(VC_CLUSTER_FIELD))
                                       .get(VC_CLUSTER_NAME_FIELD);

                           String vcRp =
                                 (String) ((HashMap<String, Object>) instance
                                       .get(VC_CLUSTER_FIELD))
                                       .get(VC_CLUSTER_VC_RP_FIELD);
                           node.setVcRp(VcResourcePoolEntity
                                 .findByClusterAndRp(vcCluster, vcRp));
                        }

                        if (instance.get(DATASTORE_FIELD) != null) {
                           List<String> dsNames = new ArrayList<String>();
                           for (HashMap<String, Object> ds : (ArrayList<HashMap<String, Object>>) instance
                                 .get(DATASTORE_FIELD)) {
                              if (ds != null)
                                 dsNames.add((String) ds
                                       .get(DATASTORE_NAME_FIELD));
                           }
                           node.setDatastoreNameList(dsNames);
                        }
                        node.setNodeGroup(group);
                        nodes.add(node);
                     }

                     // copy the new status if the instance exist
                     for (HadoopNodeEntity newNode : nodes) {
                        Iterator<HadoopNodeEntity> iter =
                              group.getHadoopNodes().iterator();
                        boolean found = false;
                        while (iter.hasNext()) {
                           HadoopNodeEntity oldNode = iter.next();
                           if (oldNode.getVmName().equals(newNode.getVmName())) {
                              oldNode.copy(newNode);
                              logger.debug("update old node "
                                    + oldNode.getVmName() + " with new info");
                              found = true;
                              break;
                           }
                        }
                        // insert new instance
                        if (!found) {
                           group.getHadoopNodes().add(newNode);
                           logger.debug("insert new node "
                                 + newNode.getVmName());
                        }
                     }
                  }
               }
            }

            return null;
         }
      });
   }

}
