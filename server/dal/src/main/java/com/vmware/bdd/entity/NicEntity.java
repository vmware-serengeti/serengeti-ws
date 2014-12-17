/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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


import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.hibernate.annotations.Type;

import com.vmware.bdd.spectypes.NicSpec;
import com.vmware.bdd.utils.Constants;

/**
 * Author: Xiaoding Bian
 * Date: 12/16/13
 * Time: 11:35 AM
 */

@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "nic_seq", allocationSize = 1)
@Table(name = "nic")

/**
 * NicEntity class: network interface cards info for node entity
 */
public class NicEntity extends EntityBase{
   @Column(name = "ipv4_address")
   private String ipv4Address;

   // We may support ipv6 for management network in future
   @Column(name = "ipv6_address")
   private String ipv6Address;

   @Column(name = "mac_address")
   private String macAddress;

   @Column(name = "connected")
   private Boolean connected;

   @Column(name = "net_traffic_definition")
   @Type(type = "text")
   private String netTrafficDefs;

   @ManyToOne
   @JoinColumn(name = "node_id")
   private NodeEntity nodeEntity;

   @ManyToOne
   @JoinColumn(name = "network_id")
   private NetworkEntity networkEntity;

   public NicEntity() {}

   @Override
   public String toString() {
      return "NicEntity [networkName=" + getNetworkEntity().getName() +  ", portGroupName="
            + getNetworkEntity().getPortGroup() + ", nodeName=" + getNodeEntity().getVmName()
            + ", IPv4Address=" + getIpv4Address() + ", IPv6Address=" + getIpv6Address()
            + ", macAddress=" + getMacAddress() + ", connected=" + getConnected();
   }

   public boolean isReady() {
      return (connected && ipv4Address != null && !Constants.NULL_IPV4_ADDRESS.equals(ipv4Address));
   }

   public String getIpv4Address() {
      return ipv4Address;
   }

   public void setIpv4Address(String ipv4Address) {
      this.ipv4Address = ipv4Address;
   }

   public String getIpv6Address() {
      return ipv6Address;
   }

   public void setIpv6Address(String ipv6Address) {
      this.ipv6Address = ipv6Address;
   }

   public String getMacAddress() {
      return macAddress;
   }

   public void setMacAddress(String macAddress) {
      this.macAddress = macAddress;
   }

   public NodeEntity getNodeEntity() {
      return nodeEntity;
   }

   public void setNodeEntity(NodeEntity nodeEntity) {
      this.nodeEntity = nodeEntity;
   }

   public NetworkEntity getNetworkEntity() {
      return networkEntity;
   }

   public void setNetworkEntity(NetworkEntity networkEntity) {
      this.networkEntity = networkEntity;
   }

   public Boolean getConnected() {
      return connected;
   }

   public void setConnected(Boolean connected) {
      this.connected = connected;
   }

   public void setNetTrafficDefs(String netTrafficDefs) {
      this.netTrafficDefs = netTrafficDefs;
   }

   public void setNetTrafficDefs(Set<NicSpec.NetTrafficDefinition> netDefs) {
      this.netTrafficDefs = (new Gson()).toJson(netDefs);
   }

   public Set<NicSpec.NetTrafficDefinition> getNetTrafficDefs() {
      return (new Gson()).fromJson(netTrafficDefs,
            new TypeToken<HashSet<NicSpec.NetTrafficDefinition>>() {}.getType());
   }
}
