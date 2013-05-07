/***************************************************************************
 *   Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.vmware.bdd.cli.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterPriority;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ElasticityRequestBody;
import com.vmware.bdd.apitypes.FixDiskRequestBody;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.ResourceScale;
import com.vmware.bdd.cli.commands.CommandsUtils;
import com.vmware.bdd.cli.commands.Constants;

@Component
public class ClusterRestClient {
   @Autowired
   private RestClient restClient;

   public void create(ClusterCreate clusterCreate) {
      final String path = Constants.REST_PATH_CLUSTERS;
      final HttpMethod httpverb = HttpMethod.POST;

      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, clusterCreate.getName());
      restClient.createObject(clusterCreate, path, httpverb, outputCallBack);
   }

   public void configCluster(ClusterCreate clusterConfig) {
      String clusterName = clusterConfig.getName();
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/"
                  + Constants.REST_PATH_CONFIG;
      final HttpMethod httpverb = HttpMethod.PUT;
      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, clusterName);
      restClient.update(clusterConfig, path, httpverb, outputCallBack);
   }

   public ClusterRead get(String id, Boolean detail) {
      final String path = Constants.REST_PATH_CLUSTER;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient
            .getObject(id, ClusterRead.class, path, httpverb, detail);
   }

   public ClusterCreate getSpec(String id) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + id + "/"
                  + Constants.REST_PATH_SPEC;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getObjectByPath(ClusterCreate.class, path, httpverb,
            false);
   }

   public ClusterRead[] getAll(Boolean detail) {
      final String path = Constants.REST_PATH_CLUSTERS;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getAllObjects(ClusterRead[].class, path, httpverb,
            detail);
   }

   public void actionOps(String id, String callbackId,
         Map<String, String> queryStrings) {
      final String path = Constants.REST_PATH_CLUSTER;
      final HttpMethod httpverb = HttpMethod.PUT;

      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, callbackId);
      restClient.actionOps(id, path, httpverb, queryStrings, outputCallBack);
   }

   public void actionOps(String id, Map<String, String> queryStrings) {
      actionOps(id, id, queryStrings);
   }

   public void resize(String clusterName, String nodeGroup, int instanceNum) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/"
                  + Constants.REST_PATH_NODEGROUP + "/" + nodeGroup
                  + "/instancenum";
      final HttpMethod httpverb = HttpMethod.PUT;

      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, clusterName);
      restClient.update(Integer.valueOf(instanceNum), path, httpverb,
            outputCallBack);
   }
   
   public void scale(ResourceScale scale){
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + scale.getClusterName() + "/"
                  + Constants.REST_PATH_NODEGROUP + "/" + scale.getNodeGroupName()
                  + "/scale";
      final HttpMethod httpverb = HttpMethod.PUT;

      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, scale.getClusterName());
      restClient.update(scale, path, httpverb,
            outputCallBack);
   }

   public void delete(String id) {
      final String path = Constants.REST_PATH_CLUSTER;
      final HttpMethod httpverb = HttpMethod.DELETE;

      PrettyOutput outputCallBack = getClusterPrettyOutputCallBack(this, id);
      restClient.deleteObject(id, path, httpverb, outputCallBack);
   }

   public void setElasticity(String clusterName,
         ElasticityRequestBody requestBody) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/"
                  + Constants.REST_PATH_ELASTICITY;
      final HttpMethod httpverb = HttpMethod.PUT;
      try {
         restClient.update(requestBody, path, httpverb);
      } catch (CliRestException e) {
         if (e.getMessage() != null) {
            throw e;
         }
      }
   }

   public void prioritizeCluster(String clusterName, String nodeGroupName,
         Priority diskIOPriority) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/" + "priority";
      final HttpMethod httpverb = HttpMethod.PUT;

      ClusterPriority requestBody = new ClusterPriority();
      requestBody.setDiskIOPriority(diskIOPriority);
      requestBody.setNodeGroupName(nodeGroupName);
      restClient.update(requestBody, path, httpverb);
   }

   public void fixDisk(final String clusterName, FixDiskRequestBody requestBody) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/"
                  + Constants.REST_PATH_FIX + "/"
                  + Constants.REST_PATH_FIX_DISK;
      final HttpMethod httpverb = HttpMethod.PUT;
      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, clusterName);
      restClient.update(requestBody, path, httpverb, outputCallBack);
   }

   private PrettyOutput getClusterPrettyOutputCallBack(
         final ClusterRestClient clusterRestClient, final String id,
         final String... completedTaskSummary) {
      return new PrettyOutput() {
         private String ngSnapshotInJson = null;
         private boolean needUpdate = true;
         private ClusterRead cluster = null;

         public void prettyOutput() throws Exception {
            try {
               if (cluster != null) {
                  List<NodeGroupRead> nodeGroups = cluster.getNodeGroups();

                  //pretty output
                  if (nodeGroups != null) {
                     for (NodeGroupRead nodeGroup : nodeGroups) {
                        System.out.printf(
                              "node group: %s,  instance number: %d\n",
                              nodeGroup.getName(), nodeGroup.getInstanceNum());
                        System.out.printf("roles:%s\n", nodeGroup.getRoles());

                        printNodesInfo(nodeGroup.getInstances());
                     }
                  }
               }
            } catch (Exception e) {
               throw e;
            }
         }

         public boolean isRefresh(boolean realTime) throws Exception {
            try {
               cluster = clusterRestClient.get(id, realTime);
               if (cluster != null) {
                  List<NodeGroupRead> nodeGroups = cluster.getNodeGroups();
                  if (nodeGroups != null) {
                     return checkOutputUpdate(nodeGroups);
                  }
               }
               return false;
            } catch (CliRestException expectedException) {
               //for some creation/deletion operations, we may get the entity not found error, but this is we expected.
               cluster = null;
               return false;
            } catch (Exception e) {
               throw e;
            }
         }

         private void printNodesInfo(List<NodeRead> nodes) throws Exception {
            if (nodes != null && nodes.size() > 0) {
               LinkedHashMap<String, List<String>> columnNamesWithGetMethodNames =
                     new LinkedHashMap<String, List<String>>();
               columnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_NAME,
                     Arrays.asList("getName"));
               columnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_IP, Arrays.asList("getIp"));
               columnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_STATUS,
                     Arrays.asList("getStatus"));
               columnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_TASK,
                     Arrays.asList("getAction"));
               CommandsUtils.printInTableFormat(columnNamesWithGetMethodNames,
                     nodes.toArray(), Constants.OUTPUT_INDENT);
            } else {
               System.out.println();
            }
         }

         private boolean checkOutputUpdate(List<NodeGroupRead> nodeGroups)
               throws JsonGenerationException, IOException {
            ObjectMapper mapper = new ObjectMapper();
            String ngCurrentInJson = mapper.writeValueAsString(nodeGroups);
            if (ngSnapshotInJson != null
                  && ngSnapshotInJson.equals(ngCurrentInJson)) {
               needUpdate = false;
            } else {
               ngSnapshotInJson = ngCurrentInJson;
               needUpdate = true;
            }
            return needUpdate;
         }

         public String[] getCompletedTaskSummary() {
            return completedTaskSummary;
         }
      };
   }
}
