/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.vmware.bdd.apitypes.Priority;
import org.hibernate.annotations.Type;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.utils.ConfigInfo;

/**
 * Cluster Entity
 * 
 */
@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "cluster_seq", allocationSize = 1)
@Table(name = "cluster")
public class ClusterEntity extends EntityBase {

   @Column(name = "name", unique = true, nullable = false)
   private String name;

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false)
   private ClusterStatus status;

   @Column(name = "distro")
   private String distro;

   @Column(name = "distro_vendor")
   private String distroVendor;

   @Column(name = "distro_version")
   private String distroVersion;

   @Enumerated(EnumType.STRING)
   @Column(name = "topology", nullable = false)
   private TopologyType topologyPolicy;

   @Column(name = "start_after_deploy")
   private boolean startAfterDeploy;

   @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   private Set<NodeGroupEntity> nodeGroups;

   /*
    * cluster definition field. VCResourcePool inside this array may not be used
    * by this cluster, so we should avoid setting up the ManyToMany mapping.
    * JSON encoded VCResourcePoolEntity name array
    */
   @Column(name = "vc_rp_names")
   @Type(type = "text")
   private String vcRpNames;

   /*
    * cluster definition field. VCDataStores inside this array may not be used
    * by this cluster, so we should avoid setting up the ManyToMany mapping.
    * JSON encoded VCDataStoreEntity name array
    */
   @Column(name = "vc_datastore_names")
   @Type(type = "text")
   private String vcDatastoreNames;

   // OneToMany mapping with Network table
   @ManyToOne
   @JoinColumn(name = "network_id")
   private NetworkEntity network;

   @Column(name = "configuration")
   @Type(type = "text")
   private String hadoopConfig;

   @Column(name = "automation_enable")
   private Boolean automationEnable;

   @Column(name = "vhm_min_num")
   private int vhmMinNum;

   @Enumerated(EnumType.STRING)
   @Column(name = "ioshare_type")
   private Priority ioShares;

   @Column(name = "vhm_target_num")
   private Integer vhmTargetNum;
   
   // records the latest job id the cluster executes
   @Column(name = "latest_task_id")
   private Long latestTaskId;

   @Column(name = "vhm_master_moid")
   private String vhmMasterMoid;

   @Column(name = "vhm_jobtracker_port")
   private String vhmJobTrackerPort;

   ClusterEntity() {
      this.ioShares = Priority.NORMAL;
   }

   public ClusterEntity(String name) {
      super();
      this.name = name;
      this.status = ClusterStatus.NA;
      this.ioShares = Priority.NORMAL;
   }

   public NetworkEntity getNetwork() {
      return network;
   }

   public void setNetwork(NetworkEntity network) {
      this.network = network;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public TopologyType getTopologyPolicy() {
      return topologyPolicy;
   }

   public void setTopologyPolicy(TopologyType topologyPolicy) {
      this.topologyPolicy = topologyPolicy;
   }

   public ClusterStatus getStatus() {
      return status;
   }

   public void setStatus(ClusterStatus status) {
      this.status = status;
   }

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

   public String getDistroVendor() {
      return distroVendor;
   }

   public void setDistroVendor(String distroVendor) {
      this.distroVendor = distroVendor;
   }

   public String getDistroVersion() {
      return distroVersion;
   }

   public void setDistroVersion(String distroVersion) {
      this.distroVersion = distroVersion;
   }

   public boolean isStartAfterDeploy() {
      return startAfterDeploy;
   }

   public void setStartAfterDeploy(boolean startAfterDeploy) {
      this.startAfterDeploy = startAfterDeploy;
   }

   public Set<NodeGroupEntity> getNodeGroups() {
      return nodeGroups;
   }

   public void setNodeGroups(Set<NodeGroupEntity> nodeGroups) {
      this.nodeGroups = nodeGroups;
   }

   public String getVcRpNames() {
      return this.vcRpNames;
   }

   @SuppressWarnings("unchecked")
   public List<String> getVcRpNameList() {
      return (new Gson()).fromJson(vcRpNames,
            (new ArrayList<String>()).getClass());
   }

   public void setVcRpNameList(List<String> vcRpNameList) {
      this.vcRpNames = (new Gson()).toJson(vcRpNameList);
   }

   public String getVcDatastoreNames() {
      return this.vcDatastoreNames;
   }

   @SuppressWarnings("unchecked")
   public List<String> getVcDatastoreNameList() {
      return (new Gson()).fromJson(vcDatastoreNames,
            (new ArrayList<String>()).getClass());
   }

   public void setVcDatastoreNameList(List<String> vcDatastoreNameList) {
      this.vcDatastoreNames = (new Gson()).toJson(vcDatastoreNameList);
   }

   public int getRealInstanceNum() {
      int instanceNum = 0;
      for (NodeGroupEntity group : nodeGroups) {
         instanceNum += group.getRealInstanceNum();
      }

      return instanceNum;
   }

   public int getDefinedInstanceNum() {
      int instanceNum = 0;
      for (NodeGroupEntity group : nodeGroups) {
         instanceNum += group.getDefineInstanceNum();
      }

      return instanceNum;
   }

   public Set<VcResourcePoolEntity> getUsedRps() {
      Set<VcResourcePoolEntity> rps = new HashSet<VcResourcePoolEntity>();
      for (NodeGroupEntity group : nodeGroups) {
         rps.addAll(group.getUsedVcResourcePools());
      }

      return rps;
   }

   public Set<String> getUsedVcDatastores() {
      HashSet<String> datastores = new HashSet<String>();
      for (NodeGroupEntity group : nodeGroups) {
         datastores.addAll(group.getUsedVcDatastores());
      }

      return datastores;
   }

   public String getHadoopConfig() {
      return hadoopConfig;
   }

   public void setHadoopConfig(String hadoopConfig) {
      this.hadoopConfig = hadoopConfig;
   }

   public String getRootFolder() {
      return ConfigInfo.getSerengetiRootFolder() + "/" + this.name;
   }

   public boolean inStableStatus() {
      ClusterStatus[] stableStatus =
            new ClusterStatus[] { ClusterStatus.RUNNING, ClusterStatus.STOPPED,
                  ClusterStatus.CONFIGURE_ERROR, ClusterStatus.ERROR,
                  ClusterStatus.PROVISION_ERROR };

      return Arrays.asList(stableStatus).contains(this.status);
   }

   public Boolean getAutomationEnable() {
      return automationEnable;
   }

   public void setAutomationEnable(Boolean automationEnable) {
      this.automationEnable = automationEnable;
   }

   public Priority getIoShares() {
      return ioShares;
   }

   public void setIoShares(Priority ioShares) {
      this.ioShares = ioShares;
   }

   public int getVhmMinNum() {
      return vhmMinNum;
   }

   public void setVhmMinNum(int vhmMinNum) {
      this.vhmMinNum = vhmMinNum;
   }
   
   public Integer getVhmTargetNum() {
      return vhmTargetNum;
   }
   
   public void setVhmTargetNum(Integer vhmTargetNum) {
      this.vhmTargetNum = vhmTargetNum;
   }

   public Long getLatestTaskId() {
      return latestTaskId;
   }

   public void setLatestTaskId(Long latestTaskId) {
      this.latestTaskId = latestTaskId;
   }

   public String getVhmMasterMoid() {
      return vhmMasterMoid;
   }

   public void setVhmMasterMoid(String vhmMasterMoid) {
      this.vhmMasterMoid = vhmMasterMoid;
   }

   public String getVhmJobTrackerPort() {
      return vhmJobTrackerPort;
   }

   public void setVhmJobTrackerPort(String vhmJobTrackerPort) {
      this.vhmJobTrackerPort = vhmJobTrackerPort;
   }

}
