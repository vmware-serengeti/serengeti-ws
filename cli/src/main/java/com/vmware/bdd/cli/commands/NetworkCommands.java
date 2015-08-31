/*****************************************************************************
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
 ****************************************************************************/
package com.vmware.bdd.cli.commands;

/**
 * <p>This class is the realization of Network command.</p>
 */
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.IpAllocEntryRead;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NetworkDnsType;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.NetworkRestClient;

@Component
public class NetworkCommands implements CommandMarker {

   @Autowired
   private NetworkRestClient networkRestClient;

   private String networkName;

   // Define network type
   private enum NetworkType {
      DHCP, IP
   }

   // Define the pattern type.
   private interface PatternType {
      // Match a single IP format,eg. 192.168.0.2 .
      final String IP =
            "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b";
      // Match a IP section format,eg. 192.168.0.2-100 .
      final String IPSEG =
            "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\-((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b";
   }

   @CliAvailabilityIndicator({ "network help" })
   public boolean isCommandAvailable() {
      return true;
   }

   /**
    * <p>
    * Add function of network command.
    * </p>
    *
    * @param name
    *           Customize the network's name.
    * @param portGroup
    *           The port group name.
    * @param dhcp
    *           The dhcp flag.
    * @param dns
    *           The frist dns information.
    * @param sedDNS
    *           The second dns information.
    * @param ip
    *           The ip range information.
    * @param gateway
    *           The gateway information.
    * @param mask
    *           The network mask information.
    */
   @CliCommand(value = "network add", help = "Add a network to Serengeti")
   public void addNetwork(
         @CliOption(key = { "name" }, mandatory = true, help = "Customize the network's name") final String name,
         @CliOption(key = { "portGroup" }, mandatory = true, help = "The port group name") final String portGroup,
         @CliOption(key = { "dhcp" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Use DHCP if specified") final boolean dhcp,
         @CliOption(key = { "dns" }, mandatory = false, help = "The master DNS server IP") final String dns,
         @CliOption(key = { "secondDNS" }, mandatory = false, help = "The secondary DNS server IP") final String sedDNS,
         @CliOption(key = { "ip" }, mandatory = false, help = "The IP address") final String ip,
         @CliOption(key = { "gateway" }, mandatory = false, help = "The gateway IP") final String gateway,
         @CliOption(key = { "mask" }, mandatory = false, help = "The subnet mask") final String mask,
         @CliOption(key = { "dnsType" }, mandatory = false, specifiedDefaultValue = "NORMAL", unspecifiedDefaultValue = "NORMAL", help = "The type of DNS server: NORMAL, DYNAMIC or OTHERS") final String dnsType) {

      NetworkType operType = null;

       CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_NETWORK, NetworkDnsType.DYNAMIC.toString());
      if (!CommandsUtils.isBlank(ip) && dhcp) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PARAMS_EXCLUSION_PAIR_NETWORK_ADD_IP_DHCP
               + Constants.PARAMS_EXCLUSION);
         return;
      } else if(!CommandsUtils.isBlank(ip) && NetworkDnsType.DYNAMIC.toString().equals(dnsType)) {
          CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                  Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
                  Constants.PARAMS_EXCLUSION_PAIR_NETWORK_ADD_STATIC_DDNS
                          + Constants.PARAMS_EXCLUSION);
          return;
      } else if (dhcp) {
         operType = NetworkType.DHCP;
      } else if (!CommandsUtils.isBlank(ip)) {
         operType = NetworkType.IP;
      } else {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PARAMS_NETWORK_ADD_IP_DHCP_NOT_NULL);
         return;
      }

      try {
         addNetwork(operType, name, portGroup, ip, dhcp, dns, sedDNS, gateway,
               mask, NetworkDnsType.valueOf(dnsType.toUpperCase()));
      } catch (IllegalArgumentException ex) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.INVALID_VALUE + " " + "dnsType=" + dnsType);
      }
   }

   /**
    * <p>
    * Delete a network from Serengeti by name
    * </p>
    *
    * @param name
    *           Customize the network's name
    */
   @CliCommand(value = "network delete", help = "Delete a network from Serengeti by name")
   public void deleteNetwork(
         @CliOption(key = { "name" }, mandatory = true, help = "Customize the network's name") final String name) {

      //rest invocation
      try {
         networkRestClient.delete(name);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_NETWORK,
               Constants.OUTPUT_OP_RESULT_DELETE);
      }catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
               Constants.OUTPUT_OP_DELETE, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   /**
    * <p>
    * get network information from Serengeti
    * </p>
    *
    * @param name
    *           Customize the network's name
    * @param detail
    *           The detail flag
    *
    */
   @CliCommand(value = "network list", help = "Get network information from Serengeti")
   public void getNetwork(
         @CliOption(key = { "name" }, mandatory = false, help = "Customize the network's name") final String name,
         @CliOption(key = { "detail" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to show node information") final boolean detail) {

      // rest invocation
      try {
         if (name == null) {
            NetworkRead[] networks = networkRestClient.getAll(detail);
            prettyOutputNetworksInfo(networks, detail);
         } else {
            NetworkRead network = networkRestClient.get(name, detail);
            prettyOutputNetworkInfo(network, detail);
         }
      }catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                 Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
      }
   }

   @CliCommand(value = "network modify", help = "Modify a network")
   public void modifyNetwork(
         @CliOption(key = { "name" }, mandatory = true, help = "The network name") final String name,
         @CliOption(key = { "addIP" }, mandatory = false, help = "The ip information") final String ip,
         @CliOption(key = { "dnsType" }, mandatory = false, specifiedDefaultValue = "NORMAL", help = "The type of DNS server: NORMAL, DYNAMIC or OTHERS") final String dnsType) {

      NetworkAdd networkAdd = new NetworkAdd();
      networkAdd.setName(name);
      try {
         if (!CommandsUtils.isBlank(ip) && dnsType.equals(NetworkDnsType.DYNAMIC.toString())) {
             CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                     Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
                     Constants.PARAMS_EXCLUSION_PAIR_NETWORK_ADD_STATIC_DDNS
                             + Constants.PARAMS_EXCLUSION);
             return;
         }
         if (ip != null) {
            if (!validateIP(ip, Constants.OUTPUT_OP_MODIFY)) {
               return;
            }
            networkAdd.setIpBlocks(transferIpInfo(ip));
         }
         if (dnsType != null) {
            networkAdd.setDnsType(NetworkDnsType.valueOf(dnsType.toUpperCase()));
         }
         networkRestClient.update(networkAdd);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_NETWORK,
                 Constants.OUTPUT_OP_RESULT_MODIFY);
      } catch (IllegalArgumentException ex) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.INVALID_VALUE + " " + "dnsType=" + dnsType);
      } catch (Exception e) {
          CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                  Constants.OUTPUT_OP_MODIFY, Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
      }
   }

   private void addNetwork(NetworkType operType, final String name,
         final String portGroup, final String ip, final boolean dhcp,
         final String dns, final String sedDNS, final String gateway,
         final String mask, final NetworkDnsType dnsType) {

      switch (operType) {
      case IP:
         try {
            addNetworkByIPModel(operType, name, portGroup, ip, dns, sedDNS,
                  gateway, mask, dnsType);
         } catch (UnknownHostException e) {
             CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                     Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL, Constants.OUTPUT_UNKNOWN_HOST);
         }
         break;
      case DHCP:
         addNetworkByDHCPModel(operType, name, portGroup, dhcp, dnsType);
      }

   }

    private void addNetworkByDHCPModel(NetworkType operType, final String name,
                                       final String portGroup, final boolean dhcp, final NetworkDnsType dnsType) {
        NetworkAdd networkAdd = new NetworkAdd();
        networkAdd.setName(name);
        networkAdd.setPortGroup(portGroup);
        networkAdd.setDhcp(true);
        networkAdd.setDnsType(dnsType);

        //rest invocation
        try {
            networkRestClient.add(networkAdd);
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_NETWORK, Constants.OUTPUT_OP_RESULT_ADD);
        } catch (CliRestException e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                    Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
        }

    }

   private void addNetworkByIPModel(NetworkType operType, final String name,
         final String portGroup, final String ip, final String dns,
         final String sedDNS, final String gateway, final String mask, final NetworkDnsType dnsType)
         throws UnknownHostException {

      // validate the network add command option.
      networkName = name;
      if (validateAddNetworkOptionByIP(ip, dns, sedDNS, gateway, mask)) {
         NetworkAdd networkAdd = new NetworkAdd();
         networkAdd.setName(name);
         networkAdd.setPortGroup(portGroup);
         networkAdd.setIpBlocks(transferIpInfo(ip));
         networkAdd.setDns1(dns);
         networkAdd.setDns2(sedDNS);
         networkAdd.setGateway(gateway);
         networkAdd.setNetmask(mask);
         networkAdd.setDnsType(dnsType);

         //rest invocation
         try {
            networkRestClient.add(networkAdd);
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_NETWORK,
                  Constants.OUTPUT_OP_RESULT_ADD);
         }catch (CliRestException e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                  Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         }
      }

   }

   private List<IpBlock> transferIpInfo(final String ip)
         throws UnknownHostException {
      List<IpBlock> ipBlockList = new ArrayList<IpBlock>();
      List<String> ipList = CommandsUtils.inputsConvert(ip);
      Pattern ipPattern = Pattern.compile(PatternType.IP);
      Pattern ipSegPattern = Pattern.compile(PatternType.IPSEG);

      if (ipList == null) {
         throw new RuntimeException(
               "[NetworkCommands:transferIpInfo]ipList is null .");
      } else if (ipList.size() == 0) {
         return ipBlockList;
      } else {
         IpBlock ipBlock = null;
         for (Iterator<String> iterator = ipList.iterator(); iterator.hasNext();) {

            String ipRangeStr = iterator.next();
            if (ipPattern.matcher(ipRangeStr).matches()) {
               ipBlock = new IpBlock();
               ipBlock.setBeginIp(ipRangeStr);
               ipBlock.setEndIp(ipRangeStr);
               ipBlockList.add(ipBlock);
            } else if (ipSegPattern.matcher(ipRangeStr).matches()) {
               ipBlock = new IpBlock();
               String[] ips = ipRangeStr.split("-");
               ipBlock.setBeginIp(ips[0]);
               ipBlock.setEndIp(new StringBuilder()
                     .append(ips[0].substring(0, ips[0].lastIndexOf(".") + 1))
                     .append(ips[1]).toString());

               ipBlockList.add(ipBlock);
            }

         }
      }
      return ipBlockList;
   }

   private boolean validateAddNetworkOptionByIP(final String ip,
         final String dns, final String sedDNS, final String gateway,
         final String mask) {

      if (!validateIP(ip, Constants.OUTPUT_OP_ADD)) {
         return false;
      }
      if (!validateDNS(dns)) {
         return false;
      }
      if (!validateDNS(sedDNS)) {
         return false;
      }
      if (!validateGateway(gateway)) {
         return false;
      }
      if (!validateMask(mask)) {
         return false;
      }
      return true;
   }

   private boolean validateIP(final String ip, final String type) {

      Pattern ipPattern = Pattern.compile(PatternType.IP);
      Pattern ipSegPattern = Pattern.compile(PatternType.IPSEG);

      List<String> ipPrarams = CommandsUtils.inputsConvert(ip);

      if (ipPrarams.size() == 0) {
         StringBuilder errorMessage = new StringBuilder().append(Constants.PARAMS_NETWORK_ADD_FORMAT_ERROR);
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                 type, Constants.OUTPUT_OP_RESULT_FAIL, Constants.INVALID_VALUE + " "
                         + "ip=" + ipPrarams + errorMessage.toString());
         return false;
      }

      for (Iterator<String> iterator = ipPrarams.iterator(); iterator.hasNext();) {
         String ipParam = iterator.next();
         if (!ipPattern.matcher(ipParam).matches()
               && !ipSegPattern.matcher(ipParam).matches()) {
            StringBuilder errorMessage = new StringBuilder().append(Constants.PARAMS_NETWORK_ADD_FORMAT_ERROR);
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                    type, Constants.OUTPUT_OP_RESULT_FAIL,
                    Constants.INVALID_VALUE + " " + "ip=" + ipPrarams + errorMessage.toString());
            return false;
         }
      }
      return true;
   }

   private boolean validateDNS(final String dns) {

      Pattern ipPattern = Pattern.compile(PatternType.IP);

      if (!CommandsUtils.isBlank(dns) && !ipPattern.matcher(dns).matches()) {
         StringBuilder errorMessage = new StringBuilder().append(Constants.PARAMS_NETWORK_ADD_IP_ERROR);

         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.INVALID_VALUE + " " + "dns=" + dns + errorMessage.toString());
         return false;
      } else
         return true;
   }


   private boolean validateGateway(final String gateway) {
      Pattern ipPattern = Pattern.compile(PatternType.IP);

      if (CommandsUtils.isBlank(gateway)) {
         return true;
      }
      if (!ipPattern.matcher(gateway).matches()) {
         StringBuilder errorMessage = new StringBuilder().append(Constants.PARAMS_NETWORK_ADD_IP_ERROR);

         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                 Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
                 Constants.INVALID_VALUE + " " + "gateway=" + gateway + errorMessage.toString());
         return false;
      }
      return true;
   }

   private boolean validateMask(final String mask) {
      Pattern ipPattern = Pattern.compile(PatternType.IP);
      if (CommandsUtils.isBlank(mask)) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                 Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
                 Constants.PARAMS_NETWORK_ADD_MASK + Constants.MULTI_INPUTS_CHECK);
         return false;
      } else if (!ipPattern.matcher(mask).matches()) {
         StringBuilder errorMessage = new StringBuilder().append(Constants.PARAMS_NETWORK_ADD_IP_ERROR);

         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NETWORK,
                 Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
                 Constants.INVALID_VALUE + " " + "mask=" + mask + errorMessage.toString());
         return false;
      } else
         return true;
   }

   private void prettyOutputNetworksInfo(NetworkRead[] networks, boolean detail) {
      if (networks != null) {
         LinkedHashMap<String, List<String>> networkIpColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         networkIpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getName"));
         networkIpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_PORT_GROUP,
               Arrays.asList("getPortGroup"));
         networkIpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_TYPE,
               Arrays.asList("findDhcpOrIp"));
         if (detail) {
            networkIpColumnNamesWithGetMethodNames.put(
                  Constants.FORMAT_TABLE_COLUMN_FREE_IPS,
                  Arrays.asList("getFreeIpBlocks"));
            networkIpColumnNamesWithGetMethodNames.put(
                  Constants.FORMAT_TABLE_COLUMN_ASSIGNED_IPS,
                  Arrays.asList("getAssignedIpBlocks"));
         } else {
            networkIpColumnNamesWithGetMethodNames.put(
                  Constants.FORMAT_TABLE_COLUMN_IP_RANGES,
                  Arrays.asList("getAllIpBlocks"));
         }
         networkIpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_DNS1, Arrays.asList("getDns1"));
         networkIpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_DNS2, Arrays.asList("getDns2"));
         networkIpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_GATEWAY,
               Arrays.asList("getGateway"));
         networkIpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_MASK, Arrays.asList("getNetmask"));
         networkIpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_DNS_TYPE, Arrays.asList("getDnsType"));
         networkIpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_GENERATE_HOSTNAME, Arrays.asList("getIsGenerateHostname"));
         try {
            if (detail) {
               LinkedHashMap<String, List<String>> networkDhcpColumnNamesWithGetMethodNames =
                     new LinkedHashMap<String, List<String>>();
               networkDhcpColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_NAME,
                     Arrays.asList("getName"));
               networkDhcpColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_PORT_GROUP,
                     Arrays.asList("getPortGroup"));
               networkDhcpColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_TYPE,
                     Arrays.asList("findDhcpOrIp"));
               networkDhcpColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_DNS_TYPE, Arrays.asList("getDnsType"));
               networkDhcpColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_GENERATE_HOSTNAME, Arrays.asList("getIsGenerateHostname"));
               for (NetworkRead network : networks) {
                  if (network.isDhcp())
                     CommandsUtils.printInTableFormat(
                           networkDhcpColumnNamesWithGetMethodNames,
                           new NetworkRead[] { network },
                           Constants.OUTPUT_INDENT);
                  else
                     CommandsUtils.printInTableFormat(
                           networkIpColumnNamesWithGetMethodNames,
                           new NetworkRead[] { network },
                           Constants.OUTPUT_INDENT);
                  System.out.println();
                  prettyOutputNetworkDetailInfo(network);
                  System.out.println();
               }
            } else {
               CommandsUtils.printInTableFormat(
                     networkIpColumnNamesWithGetMethodNames, networks,
                     Constants.OUTPUT_INDENT);
            }
         } catch (Exception e) {
            System.err.println(e.getMessage());
         }
      }

   }

   private void prettyOutputNetworkInfo(NetworkRead network, boolean detail) {
      if (network != null)
         prettyOutputNetworksInfo(new NetworkRead[] {network}, detail);
   }

   private void prettyOutputNetworkDetailInfo(NetworkRead network)
         throws Exception {
      List<IpAllocEntryRead> ipAllocEntrys = network.getIpAllocEntries();
      LinkedHashMap<String, List<String>> networkDetailColumnNamesWithGetMethodNames =
            new LinkedHashMap<String, List<String>>();
      networkDetailColumnNamesWithGetMethodNames.put(
            Constants.FORMAT_TABLE_COLUMN_IP, Arrays.asList("getIpAddress"));
      networkDetailColumnNamesWithGetMethodNames.put(
            Constants.FORMAT_TABLE_COLUMN_NODE,
            Arrays.asList("getNodeName"));
      networkDetailColumnNamesWithGetMethodNames.put(
            Constants.FORMAT_TABLE_COLUMN_NODE_GROUP_NAME,
            Arrays.asList("getNodeGroupName"));
      networkDetailColumnNamesWithGetMethodNames.put(
            Constants.FORMAT_TABLE_COLUMN_CLUSTER_NAME,
            Arrays.asList("getClusterName"));
      CommandsUtils.printInTableFormat(
            networkDetailColumnNamesWithGetMethodNames,
            ipAllocEntrys.toArray(),
            new StringBuilder().append(Constants.OUTPUT_INDENT)
                  .append(Constants.OUTPUT_INDENT).toString());
   }

}
