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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.IpConfigInfo;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.StorageRead.DiskType;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.spectypes.NicSpec;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

/**
 * Hadoop Node Entity class: describes hadoop node info
 * 
 */
@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "node_seq", allocationSize = 1)
@Table(name = "node")
public class NodeEntity extends EntityBase {
   private static final Logger logger = Logger.getLogger(NodeEntity.class);
   @Column(name = "vm_name", unique = true, nullable = false)
   private String vmName;

   @Column(name = "moid", unique = true)
   private String moId;

   @Column(name = "rack")
   private String rack;

   @Column(name = "host_name")
   private String hostName;

   // vm status, poweredOn/poweredOff
   @Enumerated(EnumType.STRING)
   @Column(name = "status")
   private NodeStatus status = NodeStatus.NOT_EXIST;

   // this field means VM status changed from powered on to powered off or vice visa.
   // if status changed is true, when user query status, will start a query to Ironfan
   // to get VM bootstrap status
   @Column(name = "power_status_changed")
   private boolean powerStatusChanged;

   @Column(name = "action")
   private String action;

   @Column(name = "action_failed")
   private boolean actionFailed;

   @Column(name = "error_message")
   private String errMessage;

   @Column(name = "guest_host_name")
   private String guestHostName;

   @Column(name = "cpu_number")
   private Integer cpuNum;

   @Column(name = "memory")
   private Long memorySize;

   @Column(name = "version")
   private String version;

   @ManyToOne
   @JoinColumn(name = "node_group_id")
   private NodeGroupEntity nodeGroup;

   @ManyToOne
   @JoinColumn(name = "vc_rp_id")
   private VcResourcePoolEntity vcRp;

   @OneToMany(mappedBy = "nodeEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   private Set<DiskEntity> disks;

   @OneToMany(mappedBy = "nodeEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   private Set<NicEntity> nics;

   public NodeEntity() {
      this.disks = new HashSet<DiskEntity>();
      this.nics = new HashSet<NicEntity>();
   }

   public NodeEntity(String vmName, String rack, String hostName,
         NodeStatus status) {
      super();
      this.vmName = vmName;
      this.rack = rack;
      this.hostName = hostName;
      this.status = status;
      this.disks = new HashSet<DiskEntity>();
      this.nics = new HashSet<NicEntity>();
   }

   public List<String> getVolumns() {
      List<String> volumns = new ArrayList<String>();
      for (DiskEntity disk : disks) {
         if (DiskType.DATA_DISK.getType().equals(disk.getDiskType())
               || DiskType.SWAP_DISK.getType().equals(disk.getDiskType()))
            volumns.add(disk.getDiskType() + ":" + disk.getHardwareUUID());
      }
      return volumns;
   }

   public String getGuestHostName() {
      return guestHostName;
   }

   public void setGuestHostName(String guestHostName) {
      this.guestHostName = guestHostName;
   }

   public boolean isPowerStatusChanged() {
      return powerStatusChanged;
   }

   public void setPowerStatusChanged(boolean powerStatusChanged) {
      this.powerStatusChanged = powerStatusChanged;
   }

   public String getVmName() {
      return vmName;
   }

   public void setVmName(String vmName) {
      this.vmName = vmName;
   }

   public String getMoId() {
      return moId;
   }

   public void setMoId(String moId) {
      this.moId = moId;
   }

   public String getRack() {
      return rack;
   }

   public void setRack(String rack) {
      this.rack = rack;
   }

   public String getHostName() {
      return hostName;
   }

   public void setHostName(String hostName) {
      this.hostName = hostName;
   }

   public NodeStatus getStatus() {
      return status;
   }

   public void setUnavailableConnection() {
      this.status = NodeStatus.DISCONNECTED;
      logger.debug("node " + getVmName() + " status changed to " + status);
   }

   public boolean isDisconnected() {
      return (this.status == NodeStatus.DISCONNECTED);
   }

   /*
    * This method will compare the setting status with existing status.
    * If they are different, powerChanged field will be set accordingly.
    * To turn off statusChanged, user need to call setStatusChanged explicitly.
    * 
    */
   public void setStatus(NodeStatus status) {
      if (status.ordinal() >= NodeStatus.POWERED_ON.ordinal()
            && this.status.ordinal() >= NodeStatus.POWERED_ON.ordinal()) {
         if (status.ordinal() > this.status.ordinal()) {
            this.status = status;
            logger.debug("node " + getVmName() + " status changed to " + status);
         }
         return;
      } else if (status.ordinal() <= NodeStatus.POWERED_OFF.ordinal()
            && this.status.ordinal() <= NodeStatus.POWERED_OFF.ordinal()) {
         // the new status and old status are in same range, both powered on, or both powered off status
         // don't think that power status is changed
         if (this.status != status) {
            this.status = status;
            logger.debug("node " + getVmName() + " status changed to " + status);
         }
         return;
      }

      if (this.status != status) {
         powerStatusChanged = true;
         logger.debug("node " + getVmName() + " status changed to " + status);
         this.status = status;
      }
   }

   public void setStatus(NodeStatus status, boolean validation) {
      if (validation) {
         setStatus(status);
      } else {
         logger.debug("Set node " + getVmName() + " status to" + status
               + " without validation");
         this.status = status;
      }
   }

   public String getAction() {
      return action;
   }

   public void setAction(String action) {
      if (this.action != action) {
         logger.debug("node " + getVmName() + " action changed to " + action);
         this.action = action;
      }
   }

   public NicEntity getPrimaryMgtNic() {
      if (nics == null || nics.isEmpty()) {
         return null;
      }
      for (NicEntity nicEntity : nics) {
         for (NicSpec.NetTrafficDefinition netDef : nicEntity.getNetTrafficDefs()) {
            if (netDef.getTrafficType().equals(NetTrafficType.MGT_NETWORK)
                  && netDef.getIndex() == 0) {
               return nicEntity;
            }
         }
      }
      return (NicEntity) nics.toArray()[0];
   }

   public String getPrimaryMgtIpV4() {
      NicEntity nicEntity = getPrimaryMgtNic();
      if (nicEntity == null) {
         return Constants.NULL_IPV4_ADDRESS;
      }
      return nicEntity.getIpv4Address();
   }

   /**
    * @return the cpuNum
    */
   public Integer getCpuNum() {
      return cpuNum;
   }

   /**
    * @param cpuNum
    *           the cpuNum to set
    */
   public void setCpuNum(Integer cpuNum) {
      this.cpuNum = cpuNum;
   }

   /**
    * @return the memorySize
    */
   public Long getMemorySize() {
      return memorySize;
   }

   /**
    * @param memorySize
    *           the memorySize to set
    */
   public void setMemorySize(Long memorySize) {
      this.memorySize = memorySize;
   }

   public NodeGroupEntity getNodeGroup() {
      return nodeGroup;
   }

   public void setNodeGroup(NodeGroupEntity nodeGroup) {
      this.nodeGroup = nodeGroup;
   }

   public VcResourcePoolEntity getVcRp() {
      return vcRp;
   }

   public void setVcRp(VcResourcePoolEntity vcRp) {
      this.vcRp = vcRp;
   }

   public List<String> getDatastoreNameList() {
      Set<String> datastores = new HashSet<String>(disks.size());
      for (DiskEntity disk : disks) {
         datastores.add(disk.getDatastoreName());
      }
      return new ArrayList<String>(datastores);
   }

   public Set<DiskEntity> getDisks() {
      return disks;
   }

   public void setDisks(Set<DiskEntity> disks) {
      this.disks = disks;
   }

   public void copy(NodeEntity newNode) {
      this.status = newNode.getStatus();
      this.action = newNode.getAction();
      this.memorySize = newNode.getMemorySize();
      this.cpuNum = newNode.getCpuNum();

      if (newNode.getRack() != null) {
         this.rack = newNode.getRack();
      }
      if (newNode.getHostName() != null) {
         this.hostName = newNode.getHostName();
      }
      if (newNode.getMoId() != null) {
         this.moId = newNode.getMoId();
      }
      if (newNode.getVcRp() != null) {
         this.vcRp = newNode.getVcRp();
      }

      if (newNode.getDisks() != null && !newNode.getDisks().isEmpty()) {
         if (this.disks == null)
            this.disks = new HashSet<DiskEntity>(newNode.disks.size());

         for (DiskEntity disk : newNode.getDisks()) {
            DiskEntity clone = disk.copy(disk);
            clone.setNodeEntity(this);
            this.disks.add(clone);
         }
      }
   }

   public Set<String> fetchAllPortGroups() {
      return fetchPortGroupToIpMap().keySet();
   }

   public Map<String, String> fetchPortGroupToIpMap() {
      Map<String, String> pgToIpMap = new HashMap<String, String>();
      if (nics != null && !nics.isEmpty()) {
         for (NicEntity nicEntity : nics) {
            pgToIpMap.put(nicEntity.getNetworkEntity().getPortGroup(), nicEntity.getIpv4Address());
         }
      }
      return pgToIpMap;
   }

   public boolean nicsReady() {
      if (nics == null || nics.isEmpty()) {
         return false;
      }

      for (NicEntity nicEntity : nics) {
         if (!nicEntity.isReady()) {
            return false;
         }
      }
      return true;
   }

   public void resetNicsInfo() {
      if (nics == null || nics.isEmpty()) {
         return;
      }

      for (NicEntity nicEntity : nics) {
         nicEntity.setConnected(false);
         nicEntity.setIpv4Address(Constants.NULL_IPV4_ADDRESS);
         nicEntity.setIpv6Address(Constants.NULL_IPV6_ADDRESS);
      }
   }

   public NicEntity findNic(NetworkEntity networkEntity) {
      AuAssert.check(networkEntity != null);
      for (NicEntity nicEntity : nics) {
         if (nicEntity.getNetworkEntity() != null && nicEntity.getNetworkEntity().equals(networkEntity)) {
            return nicEntity;
         }
      }
      return null;
   }

   public DiskEntity findDisk(String diskName) {
      AuAssert.check(diskName != null && !diskName.isEmpty()
            && this.disks != null);
      for (DiskEntity disk : this.disks) {
         if (disk.getName().equals(diskName))
            return disk;
      }

      return null;
   }

   public DiskEntity findSystemDisk() {
      AuAssert.check(this.disks != null);
      for (DiskEntity disk : this.disks) {
         if (DiskType.SYSTEM_DISK.getType().equals(disk.getDiskType()))
            return disk;
      }

      return null;
   }

   public boolean isObsoleteNode() {
      // if resize failed, some node is out of cluster scope.
      try {
         long index = CommonUtil.getVmIndex(vmName);
         if (index < nodeGroup.getDefineInstanceNum()) {
            return false;
         } else {
            return true;
         }
      } catch (BddException e) {
         logger.warn("VM " + vmName + " violate name convention");
         return true;
      }
   }

   // if includeVolumes is true, this method must be called inside a transaction
   public NodeRead toNodeRead(boolean includeVolumes) {
      NodeRead node = new NodeRead();
      node.setRack(this.rack);
      node.setHostName(this.hostName);
      // For class NodeRead, keep "ipConfigsInfo" structure since it's used by software provision
      node.setIpConfigs(convertToIpConfigInfo());
      node.setName(this.vmName);
      node.setMoId(this.moId);
      node.setStatus(this.status != null ? this.status.toString() : null);
      node.setAction(this.action);
      node.setVersion(this.version);
      if (this.cpuNum != null) {
         node.setCpuNumber(this.cpuNum);
      }
      if (this.memorySize != null) {
         node.setMemory(this.memorySize);
      }
      List<String> roleNames = nodeGroup.getRoleNameList();
      node.setRoles(roleNames);
      if (includeVolumes)
         node.setVolumes(this.getVolumns());
      if (actionFailed) {
         node.setActionFailed(true);
      }
      if (errMessage != null && !errMessage.isEmpty()) {
         node.setErrMessage(errMessage);
      }
      return node;
   }

   /**
    * convert "nics" to NodeRead's "ipConfigInfo" field
    *
    * @return
    */
   public Map<NetTrafficType, List<IpConfigInfo>> convertToIpConfigInfo() {
      Map<NetTrafficType, List<IpConfigInfo>> ipConfigInfo = new HashMap<NetTrafficType, List<IpConfigInfo>>();
      if (nics != null) {
         for (NicEntity nicEntity : nics) {
            for (NicSpec.NetTrafficDefinition netDef : nicEntity.getNetTrafficDefs()) {
               if (!ipConfigInfo.containsKey(netDef.getTrafficType())) {
                  ipConfigInfo.put(netDef.getTrafficType(), new ArrayList<IpConfigInfo>());
               }
               List<IpConfigInfo> ipInfo = ipConfigInfo.get(netDef.getTrafficType());
               IpConfigInfo newIpConfig = new IpConfigInfo(netDef.getTrafficType(), 
                     nicEntity.getNetworkEntity().getName(), nicEntity.getNetworkEntity().getPortGroup(),
                     nicEntity.getIpv4Address());
               if (netDef.getIndex() + 1 > ipInfo.size()) {
                  while (ipInfo.size() < netDef.getIndex() + 1) {
                     ipInfo.add(null);
                  }
               }
               ipInfo.remove(netDef.getIndex());
               ipInfo.add(netDef.getIndex(), newIpConfig);
            }
         }
      }
      return ipConfigInfo;
   }

   @Override
   public int hashCode() {
      if (this.vmName != null) {
         return this.vmName.hashCode();
      }
      if (this.id != null) {
         return this.id.intValue();
      }
      return 0;
   }

   @Override
   public boolean equals(Object node) {
      if (!(node instanceof NodeEntity))
         return false;
      if (this.vmName != null && ((NodeEntity) node).getVmName() != null) {
         return ((NodeEntity) node).getVmName().equals(this.vmName);
      }
      return false;
   }

   public void cleanupErrorMessage() {
      this.actionFailed = false;
      this.errMessage = null;
      this.action = null;
   }

   public Set<NicEntity> getNics() {
      return nics;
   }

   public void setNics(Set<NicEntity> nics) {
      this.nics = nics;
   }

   public boolean isActionFailed() {
      return actionFailed;
   }

   public void setActionFailed(boolean actionFailed) {
      this.actionFailed = actionFailed;
   }

   public String getErrMessage() {
      return errMessage;
   }

   public void setErrMessage(String errMessage) {
      this.errMessage = errMessage;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public boolean needUpgrade(String serverVersion) {
      return (this.getMoId() != null && (this.getVersion() == null || !serverVersion.equals(this.getVersion())));
   }

   public boolean canBeUpgrade() {
      String nodeIp = this.getPrimaryMgtIpV4();
      return this.status.ordinal() >= NodeStatus.POWERED_ON.ordinal() && nodeIp != null && !Constants.NULL_IPV4_ADDRESS.equals(nodeIp);
   }

   public void cleanupErrorMessageForUpgrade() {
      this.actionFailed = false;
      this.errMessage = null;
      if (!canBeUpgrade()) {
         this.action = null;
      }
   }

   public boolean isVmReady() {
      return NodeStatus.VM_READY.equals(this.status);
   }

   public String getVmNameWithIP() {
      return this.getVmName() + "(" + this.getPrimaryMgtIpV4() + ")";
   }
}
