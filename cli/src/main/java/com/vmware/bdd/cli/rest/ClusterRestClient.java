/***************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ElasticityRequestBody;
import com.vmware.bdd.apitypes.FixDiskRequestBody;
import com.vmware.bdd.apitypes.NetConfigInfo;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.ResourceScale;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.apitypes.ValidateResult;
import com.vmware.bdd.apitypes.VcResourceMap;
import com.vmware.bdd.cli.commands.CommandsUtils;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.utils.CommonUtil;

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

   public void upgradeCluster(String clusterName) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/"
                  + Constants.REST_PATH_UPGRADE;
      final HttpMethod httpverb = HttpMethod.PUT;
      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, clusterName);
      restClient.update(clusterName, path, httpverb, outputCallBack);
   }

   public ClusterRead get(String id, Boolean detail) {
      id = CommonUtil.encode(id);
      final String path = Constants.REST_PATH_CLUSTER;
      final HttpMethod httpverb = HttpMethod.GET;
      return restClient
            .getObject(id, ClusterRead.class, path, httpverb, detail);
   }

   public ValidateResult validateBlueprint(ClusterCreate cluster) {
      String name = CommonUtil.encode(cluster.getName());
      final String path = Constants.REST_PATH_CLUSTER + "/" + name + "/validate";
      final HttpMethod httpverb = HttpMethod.POST;
      return restClient.getObject(path, ValidateResult.class, httpverb, cluster);
   }

   public ClusterCreate getSpec(String id) {
      id = CommonUtil.encode(id);
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + id + "/"
                  + Constants.REST_PATH_SPEC;
      final HttpMethod httpverb = HttpMethod.GET;

      return restClient.getObjectByPath(ClusterCreate.class, path, httpverb,
            false);
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getRackTopology(String clusterName, String topology) {
      clusterName = CommonUtil.encode(clusterName);
      StringBuilder path = new StringBuilder();
      path.append(Constants.REST_PATH_CLUSTER).append("/").append(clusterName).append("/").append(Constants.REST_PATH_CLUSTER_RACK);
      if (!CommonUtil.isBlank(topology)) {
         path.append("?");
         path.append(Constants.REST_PATH_CLUSTER_RACK_PARAM_TOPOLOGY).append("=").append(topology);
      }

      final HttpMethod httpverb = HttpMethod.GET;
      return restClient.getObjectByPath(Map.class, path.toString(), httpverb, false);
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
      id = CommonUtil.encode(id);
      actionOps(id, id, queryStrings);
   }

   public void updateCluster(ClusterCreate clusterUpdate, boolean ignoreWarning) {
      StringBuilder path = new StringBuilder(Constants.REST_PATH_CLUSTER)
            .append("/").append(clusterUpdate.getName())
            .append("?").append("ignorewarning").append('=')
            .append(ignoreWarning);
      final HttpMethod httpverb = HttpMethod.PUT;
      restClient.update(clusterUpdate, path.toString(), httpverb);
   }

   public void resize(String clusterName, String nodeGroup, int instanceNum, Map<String, String> queryStrings) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/"
                  + Constants.REST_PATH_NODEGROUP + "/" + nodeGroup
                  + "/instancenum";
      final HttpMethod httpverb = HttpMethod.PUT;

      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, clusterName);
      restClient.updateWithQueryStrings(Integer.valueOf(instanceNum), path, queryStrings, httpverb,
            outputCallBack);
   }

   public TaskRead scale(ResourceScale scale){
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + scale.getClusterName() + "/"
                  + Constants.REST_PATH_NODEGROUP + "/" + scale.getNodeGroupName()
                  + "/scale";
      final HttpMethod httpverb = HttpMethod.PUT;

      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, scale.getClusterName());
      return restClient.updateWithReturn(scale, path, httpverb,
            outputCallBack);
   }

   public void delete(String id) {
      final String path = Constants.REST_PATH_CLUSTER;
      final HttpMethod httpverb = HttpMethod.DELETE;
      id = CommonUtil.encode(id);
      PrettyOutput outputCallBack = getClusterPrettyOutputCallBack(this, id);
      restClient.deleteObject(id, path, httpverb, outputCallBack);
   }

   public void setParam(ClusterRead cluster, ElasticityRequestBody requestBody) {
      String clusterName = cluster.getName();
      if (cluster.needAsyncUpdateParam(requestBody)) {
         asyncSetParam(clusterName, requestBody);
      } else {
         syncSetParam(clusterName, requestBody);
      }
   }

   private void asyncSetParam(String clusterName, ElasticityRequestBody requestBody) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/"
                  + Constants.REST_PATH_ASYNC_PARAM;
      final HttpMethod httpverb = HttpMethod.PUT;
      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, clusterName);
      restClient.update(requestBody, path, httpverb, outputCallBack);
   }

   private void syncSetParam(String clusterName, ElasticityRequestBody requestBody) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/"
                  + Constants.REST_PATH_SYNC_PARAM;
      final HttpMethod httpverb = HttpMethod.PUT;
      restClient.update(requestBody, path, httpverb);
   }

   public TaskRead fixDisk(final String clusterName, FixDiskRequestBody requestBody) {
      final String path =
            Constants.REST_PATH_CLUSTER + "/" + clusterName + "/"
                  + Constants.REST_PATH_FIX + "/"
                  + Constants.REST_PATH_FIX_DISK;
      final HttpMethod httpverb = HttpMethod.PUT;
      PrettyOutput outputCallBack =
            getClusterPrettyOutputCallBack(this, clusterName);
      return restClient.updateWithReturn(requestBody, path, httpverb, outputCallBack);
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
                     CommandsUtils.prettyOutputErrorNode(nodeGroups);
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
                     Constants.FORMAT_TABLE_COLUMN_IP, Arrays.asList("fetchMgtIp"));
               if (nodes.get(0).getIpConfigs() != null
                     && (nodes.get(0).getIpConfigs().containsKey(NetConfigInfo.NetTrafficType.HDFS_NETWORK)
                     || nodes.get(0).getIpConfigs().containsKey(NetConfigInfo.NetTrafficType.MAPRED_NETWORK))) {
                  columnNamesWithGetMethodNames.put(
                        Constants.FORMAT_TABLE_COLUMN_HDFS_IP, Arrays.asList("fetchHdfsIp"));
                  columnNamesWithGetMethodNames.put(
                        Constants.FORMAT_TABLE_COLUMN_MAPRED_IP, Arrays.asList("fetchMapredIp"));
               }
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

   public void recover(VcResourceMap vcResMap) {
      final String path = "/" + Constants.REST_PATH_CLUSTER_RECOVER;
      final HttpMethod httpverb = HttpMethod.PUT;
      restClient.update(vcResMap, path, httpverb);
   }

}
