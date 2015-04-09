/***************************************************************************
 * Copyright (c) 2013-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.specpolicy;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NetworkDnsType;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.HostnameManager;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.IpAddressUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 10/12/13
 * Time: 4:09 PM
 */
public class GuestMachineIdSpec {
   @Expose
   @SerializedName("bootupUUID")
   private String bootupUuid;

   @Expose
   @SerializedName("nics")
   private NicDeviceConfigSpec[] nics;

   private String defaultPg;

   @Expose
   @SerializedName("managementServerIP")
   private String managementServerIP;

   public GuestMachineIdSpec() {}

   public GuestMachineIdSpec(List<NetworkAdd> networkAdds,
         Map<String, String> ipInfo, String defaultPg) {
      int nicNumber = networkAdds.size();
      this.nics = new NicDeviceConfigSpec[nicNumber];
      for (int i = 0; i < nicNumber; i++) {
         this.nics[i] = new NicDeviceConfigSpec(networkAdds.get(i), ipInfo);
      }

      this.defaultPg = defaultPg;
      this.bootupUuid = null;
      this.managementServerIP = IpAddressUtil.getHostManagementIp();
   }

   public GuestMachineIdSpec(List<NetworkAdd> networkAdds,
         Map<String, String> ipInfo, String defaultPg, BaseNode vNode, INetworkService networkMgr) {
      int nicNumber = networkAdds.size();
      this.nics = new NicDeviceConfigSpec[nicNumber];
      for (int i = 0; i < nicNumber; i++) {
         this.nics[i] = new NicDeviceConfigSpec(networkAdds.get(i), ipInfo, vNode, networkMgr);
      }

      this.defaultPg = defaultPg;
      this.bootupUuid = null;
      this.managementServerIP = IpAddressUtil.getHostManagementIp();
   }

   public GuestMachineIdSpec(List<NetworkAdd> networkAdds,
         Map<String, String> ipInfo, String defaultPg, NodeEntity node, INetworkService networkMgr) {
      int nicNumber = networkAdds.size();
      this.nics = new NicDeviceConfigSpec[nicNumber];
      for (int i = 0; i < nicNumber; i++) {
         this.nics[i] = new NicDeviceConfigSpec(networkAdds.get(i), ipInfo, node, networkMgr);
      }

      this.defaultPg = defaultPg;
      this.bootupUuid = null;
      this.managementServerIP = IpAddressUtil.getHostManagementIp();
   }

   public Map<String, String> toGuestVariable() {
      Map<String, String> guestVarialbe = new HashMap<String, String>();
      Gson gson = new Gson();
      guestVarialbe.put(Constants.GUEST_VARIABLE_NIC_DEVICES, gson.toJson(nics));
      guestVarialbe.put(Constants.MANAGEMENT_SERVER_IP, this.managementServerIP);
      NicDeviceConfigSpec defaultNic = null;
      if (defaultPg == null) {
         defaultNic = nics[0];
      } else {
         for (NicDeviceConfigSpec nic : nics) {
            if (nic.getPortGroupName().equals(defaultPg)) {
               defaultNic = nic;
               break;
            }
         }
      }

      AuAssert.check(defaultNic != null);
      /*
       * The following 7 attributes are used to be compatible with BDE 1.0.0;
       * If user upgrade from BDE 1.0.0 to latest version, the cluster nodes deployed before
       * are still using old version setup-ip.py, so we should be compatible with old machine id format.
       */
      guestVarialbe.put(Constants.GUEST_VARIABLE_POLICY_KEY, defaultNic.getBootProto());
      guestVarialbe.put(Constants.GUEST_VARIABLE_PORT_GROUP, defaultNic.getPortGroupName());
      guestVarialbe.put(Constants.GUEST_VARIABLE_IP_KEY, defaultNic.getIpAddress());
      guestVarialbe.put(Constants.GUEST_VARIABLE_GATEWAY_KEY, defaultNic.getGateway());
      guestVarialbe.put(Constants.GUEST_VARIABLE_NETMASK_KEY, defaultNic.getNetmask());
      guestVarialbe.put(Constants.GUEST_VARIABLE_DNS_KEY_0, defaultNic.getDnsServer0());
      guestVarialbe.put(Constants.GUEST_VARIABLE_DNS_KEY_1, defaultNic.getDnsServer1());

      return guestVarialbe;
   }

   @Override
   public String toString() {
      return (new Gson()).toJson(this);
   }

   public String getBootupUuid() {
      return bootupUuid;
   }

   public void setBootupUuid(String bootupUuid) {
      this.bootupUuid = bootupUuid;
   }
   public NicDeviceConfigSpec[] getNics() {
      return nics;
   }

   public void setNics(NicDeviceConfigSpec[] nics) {
      this.nics = nics;
   }

   public static class NicDeviceConfigSpec {
      /*
       * It is not robust to assume OS assign NIC names(eth0, eth1...)
       * according to the order of nic devices in Node#NetworkSchema we
       * configured(it is not actually), so we need to transfer the
       * <portgroup, ipconfig> pairs to control Ips configuration accurately
       */
      @Expose
      @SerializedName(Constants.GUEST_VARIABLE_PORT_GROUP)
      private String portGroupName;

      @Expose
      @SerializedName(Constants.GUEST_VARIABLE_POLICY_KEY)
      private String bootProto;

      @Expose
      @SerializedName(Constants.GUEST_VARIABLE_IP_KEY)
      private String ipAddress;

      @Expose
      @SerializedName(Constants.GUEST_VARIABLE_GATEWAY_KEY)
      private String gateway;

      @Expose
      @SerializedName(Constants.GUEST_VARIABLE_NETMASK_KEY)
      private String netmask;

      @Expose
      @SerializedName(Constants.GUEST_VARIABLE_DNS_KEY_0)
      private String dnsServer0;

      @Expose
      @SerializedName(Constants.GUEST_VARIABLE_DNS_KEY_1)
      private String dnsServer1;

      @Expose
      @SerializedName(Constants.GUEST_VARIABLE_DNS_TYPE)
      private NetworkDnsType dnsType;

      @Expose
      @SerializedName(Constants.GUEST_VARIABLE_DHCP_HOSTNAME)
      private String dhcpHostname;

      public NicDeviceConfigSpec() {}

      public NicDeviceConfigSpec(NetworkAdd networkAdd, Map<String, String> ipInfo) {
         this.portGroupName = networkAdd.getPortGroup();
         this.bootProto = networkAdd.getType();
         this.ipAddress = ipInfo.get(portGroupName);
         this.gateway = networkAdd.getGateway();
         this.netmask = networkAdd.getNetmask();
         this.dnsServer0 = networkAdd.getDns1();
         this.dnsServer1 = networkAdd.getDns2();
      }

      public NicDeviceConfigSpec(NetworkAdd networkAdd, Map<String, String> ipInfo, BaseNode vNode, INetworkService networkMgr) {
         this(networkAdd, ipInfo);
         NetworkEntity networkEntity = networkMgr.getNetworkEntityByName(networkAdd.getName());
         this.dhcpHostname = HostnameManager.generateHostname(networkEntity, vNode);
         this.dnsType = networkEntity.getDnsType();
      }

      public NicDeviceConfigSpec(NetworkAdd networkAdd, Map<String, String> ipInfo, NodeEntity node, INetworkService networkMgr) {
         this(networkAdd, ipInfo);
         NetworkEntity networkEntity = networkMgr.getNetworkEntityByName(networkAdd.getName());
         this.dhcpHostname = HostnameManager.generateHostname(networkEntity, node);
         this.dnsType = NetworkDnsType.DYNAMIC;
      }

      public String getPortGroupName() {
         return portGroupName;
      }

      public void setPortGroupName(String portGroupName) {
         this.portGroupName = portGroupName;
      }

      public String getIpAddress() {
         return ipAddress;
      }

      public void setIpAddress(String ipAddress) {
         this.ipAddress = ipAddress;
      }

      public String getBootProto() {
         return bootProto;
      }

      public void setBootProto(String bootProto) {
         this.bootProto = bootProto;
      }

      public String getGateway() {
         return gateway;
      }

      public void setGateway(String gateway) {
         this.gateway = gateway;
      }

      public String getNetmask() {
         return netmask;
      }

      public void setNetmask(String netmask) {
         this.netmask = netmask;
      }

      public String getDnsServer0() {
         return dnsServer0;
      }

      public void setDnsServer0(String dnsServer0) {
         this.dnsServer0 = dnsServer0;
      }

      public String getDnsServer1() {
         return dnsServer1;
      }

      public void setDnsServer1(String dnsServer1) {
         this.dnsServer1 = dnsServer1;
      }

      public NetworkDnsType getDnsType() {
         return dnsType;
      }

      public void setDnsType(NetworkDnsType dnsType) {
         this.dnsType = dnsType;
      }

      public String getDhcpHostname() {
         return dhcpHostname;
      }

      public void setDhcpHostname(String dhcpHostname) {
         this.dhcpHostname = dhcpHostname;
      }

   }
}
