/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.ClusterNetConfigInfo;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.security.EncryptionGuard;
import com.vmware.bdd.utils.ConfigInfo;

@NamedQueries({
   @NamedQuery(
      name = "cluster.findClusterWithNgs",
      query = "from ClusterEntity c left join fetch c.nodeGroups ng where c.name=:clusterName"
   ),
   @NamedQuery(
      name = "cluster.findClusterWithNodes",
      query = "from ClusterEntity c"
            + " left join fetch c.nodeGroups ng"
            + " left join fetch ng.nodes n"
            + " left join fetch ng.groupAssociations ga"
            + " where c.name=:clusterName"
   ),
   @NamedQuery(
      name = "cluster.findClusterWithVolumes",
      query = "from ClusterEntity c"
            + " left join fetch c.nodeGroups ng"
            + " left join fetch ng.nodes n"
            + " left join fetch ng.groupAssociations ga"
            + " left join fetch n.disks d"
            + " where c.name=:clusterName"
   )
})

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

   @Column(name = "appmanager")
   private String appmanager;

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

   @Column(name = "password")
   private String password;

   /*
    * The moid of node template
    */
   @Column(name = "template_id")
   private String templateId;

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

   @Column(name = "network_config")
   @Type(type = "text")
   private String networkConfig;

   @Column(name = "configuration")
   @Type(type = "text")
   private String hadoopConfig;

   @Column(name = "advanced_properties")
   @Type(type = "text")
   private String advancedProperties;

   @Column(name = "automation_enable")
   private Boolean automationEnable;

   @Column(name = "vhm_min_num")
   private int vhmMinNum;

   @Column(name = "vhm_max_num")
   private int vhmMaxNum;

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

   @Column(name = "version")
   private String version;

   @Enumerated(EnumType.STRING)
   @Column(name = "last_status")
   private ClusterStatus lastStatus;

   @Column(name = "infrastructure_config")
   private String infraConfig;

   static final Logger logger = Logger.getLogger(ClusterEntity.class);

   ClusterEntity() {
      this.ioShares = Priority.NORMAL;
   }

   public ClusterEntity(String name) {
      super();
      this.name = name;
      this.status = ClusterStatus.NA;
      this.ioShares = Priority.NORMAL;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getAppManager() {
      return appmanager;
   }

   public void setAppManager(String appmanager) {
      this.appmanager = appmanager;
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

   public List<NodeGroupEntity> getNodeGroupsSortedById() {
      List<NodeGroupEntity> sortedNg = new ArrayList<>(getNodeGroups());
      Collections.sort(sortedNg, new Comparator<NodeGroupEntity> () {
         @Override
         public int compare(NodeGroupEntity ng1, NodeGroupEntity ng2) {
            return (int)(ng1.getId() - ng2.getId());
         }
      });
      return sortedNg;
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

   public List<String> fetchNetworkNameList() {
      List<String> networkNames = new ArrayList<String>();
      for (Entry<NetTrafficType, List<ClusterNetConfigInfo>> netConfig : getNetworkConfigInfo().entrySet()) {
         for (ClusterNetConfigInfo net : netConfig.getValue()) {
            if (!networkNames.contains(net.getNetworkName())) {
               networkNames.add(net.getNetworkName());
            }
         }
      }
      return networkNames;
   }

   public String getNetworkConfig() {
      return networkConfig;
   }

   @SuppressWarnings("unchecked")
   public Map<NetTrafficType, List<ClusterNetConfigInfo>> getNetworkConfigInfo() {
      return (new Gson()).fromJson(networkConfig,
            new TypeToken<HashMap<NetTrafficType, List<ClusterNetConfigInfo>>>() {}.getType());
   }

   public void setNetworkConfig(String networkConfig) {
      this.networkConfig = networkConfig;
   }

   public void setNetworkConfig(Map<NetTrafficType, List<ClusterNetConfigInfo>> networkConfig) {
      this.networkConfig = (new Gson()).toJson(networkConfig);
   }

   public int getRealInstanceNum() {
      return getRealInstanceNum(false);
   }

   public int getRealInstanceNum(boolean ignoreObsolete) {
      int instanceNum = 0;
      for (NodeGroupEntity group : nodeGroups) {
         instanceNum += group.getRealInstanceNum(ignoreObsolete);
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

   public int getVhmMaxNum() {
      return vhmMaxNum;
   }

   public void setVhmMaxNum(int vhmMaxNum) {
      this.vhmMaxNum = vhmMaxNum;
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

   public String getPassword() {
      if (this.password == null) {
         return null;
      }

      String password = null;
      try {
         password = EncryptionGuard.decode(this.password);
      } catch (UnsupportedEncodingException e) {
         //TODO(qjin): need to handle this two exceptions more carefully
         e.printStackTrace();
      } catch (GeneralSecurityException e) {
         e.printStackTrace();
      }
      return password;
   }

   public void setPassword(String password) {
      if (password == null) {
         this.password = null;
      }
      try {
         this.password = EncryptionGuard.encode(password);
      } catch (UnsupportedEncodingException e) {
         //TODO(qjin): need to handle this two exceptions more carefully
         e.printStackTrace();
      } catch (GeneralSecurityException e) {
         e.printStackTrace();
      }
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public ClusterStatus getLastStatus() {
      return lastStatus;
   }

   public void setLastStatus(ClusterStatus lastStatus) {
      this.lastStatus = lastStatus;
   }

   public String getAdvancedProperties() {
      return advancedProperties;
   }

   public void setAdvancedProperties(String advancedProperties) {
      this.advancedProperties = advancedProperties;
   }


   public void setInfraConfig(String infraConfig) {
      this.infraConfig = infraConfig;
   }

   public String getInfraConfig() {
      return infraConfig;
   }

   public String getTemplateId() {
      return templateId;
   }

   public void setTemplateId(String templateId) {
      this.templateId = templateId;
   }
}
