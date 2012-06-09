/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
package com.vmware.bdd.apitypes;

import java.util.List;
import java.util.Set;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.spectypes.HadoopDistroMap;
import com.vmware.bdd.spectypes.VcCluster;

/**
 * Cluster creation parameters
 */
public class ClusterCreate {

   @Expose
   private String name;
   @Expose
   @SerializedName("groups")
   private NodeGroupCreate[] nodeGroupCreates;
   @Expose
   private String distro;
   private List<String> rpNames;
   @Expose
   @SerializedName("vc_clusters")
   private List<VcCluster> vcClusters;
   @Expose
   @SerializedName("template_id")
   private String templateId;
   @Expose
   @SerializedName("deploy_policy")
   private String deployPolicy;
   private List<String> dsNames;
   private String networkName;
   @Expose
   private List<NetworkAdd> networking;
   @Expose
   @SerializedName("distro_map")
   private HadoopDistroMap distroMap;
   @Expose
   @SerializedName("vc_shared_datastore_pattern")
   private Set<String> sharedPattern;
   @Expose
   @SerializedName("vc_local_datastore_pattern")
   private Set<String> localPattern;

   public ClusterCreate() {
      
   }

   public ClusterCreate(ClusterCreate cluster) {
      this.deployPolicy = cluster.deployPolicy;
      this.distro = cluster.distro;
      this.name = cluster.name;
      this.networkName = cluster.networkName;
      this.nodeGroupCreates = cluster.nodeGroupCreates;
      this.rpNames = cluster.rpNames;
      this.templateId = cluster.templateId;
      this.vcClusters = cluster.vcClusters;
      this.networking = cluster.networking;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

   public List<String> getRpNames() {
      return rpNames;
   }

   public void setRpNames(List<String> rpNames) {
      this.rpNames = rpNames;
   }

   public String getNetworkName() {
      return networkName;
   }

   public void setNetworkName(String networkName) {
      this.networkName = networkName;
   }

   public NodeGroupCreate[] getNodeGroupCreates() {
      return nodeGroupCreates;
   }

   public void setNodeGroupCreates(NodeGroupCreate[] nodeGroupCreates) {
      this.nodeGroupCreates = nodeGroupCreates;
   }

   public List<String> getDsNames() {
      return dsNames;
   }

   public void setDsNames(List<String> dsNames) {
      this.dsNames = dsNames;
   }

   public List<VcCluster> getVcClusters() {
      return vcClusters;
   }

   public void setVcClusters(List<VcCluster> vcClusters) {
      this.vcClusters = vcClusters;
   }

   public String getTemplateId() {
      return templateId;
   }

   public void setTemplateId(String templateId) {
      this.templateId = templateId;
   }

   public String getDeployPolicy() {
      return deployPolicy;
   }

   public void setDeployPolicy(String deployPolicy) {
      this.deployPolicy = deployPolicy;
   }

   public List<NetworkAdd> getNetworking() {
      return networking;
   }

   public void setNetworking(List<NetworkAdd> networking) {
      this.networking = networking;
   }

   public HadoopDistroMap getDistroMap() {
      return distroMap;
   }

   public void setDistroMap(HadoopDistroMap distroMap) {
      this.distroMap = distroMap;
   }

   public Set<String> getSharedPattern() {
      return sharedPattern;
   }

   public void setSharedPattern(Set<String> sharedPattern) {
      this.sharedPattern = sharedPattern;
   }

   public Set<String> getLocalPattern() {
      return localPattern;
   }

   public void setLocalPattern(Set<String> localPattern) {
      this.localPattern = localPattern;
   }
}
