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
package com.vmware.bdd.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.exception.BddException;
import java.util.regex.Pattern;

public class IpAddressUtil {

   public static boolean isValidIp(long addr) {
      InetAddress ip = getAddressFromLong(addr);

      return !ip.isLoopbackAddress() && !ip.isMulticastAddress();
   }

   public static boolean isValidIp(String ipAddr) {
      Long ip = getAddressAsLong(ipAddr);
      if (ipAddr == null) {
         return false;
      }

      return isValidIp(ip);
   }

   public static boolean isValidIp(long netmask, long addr) {
      long hostPart = addr & ~netmask;
      int bits = 32 - getNetworkPrefixBits(netmask);

      // should not be network and broadcast addresses (all 0 and all 1)
      if (hostPart == 0 || hostPart == (1L << bits) -1) {
         return false;
      }

      return isValidIp(addr);
   }

   public static boolean isValidIp(String netmask, String ipAddr) {
      Long mask = getAddressAsLong(netmask);
      Long ip = getAddressAsLong(ipAddr);
      if (mask == null || ipAddr == null) {
         return false;
      }

      return isValidIp(mask, ip);
   }

   public static InetAddress getAddressFromLong(Long addr) {
      if (addr == null) {
         return null;
      }

      AuAssert.check(addr >= 0 && addr < (1L << 32));

      byte[] bytes =
            new byte[] { (byte) ((addr >> 24) & 0xff), (byte) ((addr >> 16) & 0xff),
                  (byte) ((addr >> 8) & 0xff), (byte) (addr & 0xff) };

      try {
         return InetAddress.getByAddress(bytes);
      } catch (UnknownHostException e) {
         AuAssert.unreachable();
         return null;
      }
   }

   public static Long getAddressAsLong(InetAddress addr) {
      if (addr == null) {
         return null;
      }

      byte[] bytes = addr.getAddress();
      AuAssert.check(bytes.length == 4); // ipv4 only

      // byte & 0xff is a hack to use java unsigned byte
      return ((bytes[0] & 0xffL) << 24) + ((bytes[1] & 0xffL) << 16)
            + ((bytes[2] & 0xffL) << 8) + (bytes[3] & 0xffL);
   }

   /**
    * Convert an string to an internal representation as long. The accepted IP
    * address format is not strictly consistent to RFC standards.
    * 
    * @param addr
    *           address to convert
    * @return internal representation, null if the input is not a valid IP
    *         address
    */
   public static Long getAddressAsLong(String addr) {
      if (addr == null) {
         return null;
      }

      String[] parts = addr.split("\\.");
      if (parts.length != 4) {
         return null;
      }

      long ip = 0;
      try {
         for (int i = 0; i < 4; ++i) {
            long part = Integer.parseInt(parts[i]);
            if (part < 0 || part > 255) {
               return null;
            }

            ip += part << ((3 - i) * 8);
         }
      } catch (NumberFormatException ex) {
         return null;
      }

      return ip;
   }

   /**
    * Get the number of network prefix bits from a netmask. This function does
    * not validate whether the netmask is really usufull or not, for example, it
    * will treat "255.255.255.255", "255.255.255.254", "255.255.255.252" as
    * valid netmasks though they can not be used practically.
    * 
    * @param netmask
    *           netmask
    * @return a 1 - 32 integer indicates the network part length, -1 when the
    *         input is not a valid netmask.
    */
   public static int getNetworkPrefixBits(long netmask) {
      AuAssert.check(netmask >= 0 && netmask < (1L << 32));

      int i = 0;
      long tmp = netmask;
      while (i <= 32) {
         if ((tmp & 1L) == 1L) {
            long expected = ((1L << 32) - 1L) >> i;
            if ((expected & tmp) == expected) {
               return 32 - i;
            } else {
               return -1;
            }
         }
         ++i;
         tmp = tmp >> 1;
      }

      return -1;
   }

   public static boolean isValidNetmask(long netmask) {
      return getNetworkPrefixBits(netmask) != -1;
   }

   public static boolean isValidNetmask(String netmask) {
      Long internal = getAddressAsLong(netmask);
      return internal!= null && getNetworkPrefixBits(internal) != -1;
   }

   /**
    * Check whether an IP address is valid in a specified network.
    * 
    * @param network
    *           network address
    * @param netmask
    *           network netmask
    * @param ip
    *           ip to be checked
    * @return whether ip is valid in this network
    */
   public static boolean networkContains(long network, long netmask, long ip) {
      return network == (netmask & ip);
   }

   public static void verifyIPBlocks(List<IpBlock> ipBlocks, final long netmask) {
      AuAssert.check(ipBlocks != null, "Spring should guarantee this");
      for (IpBlock blk : ipBlocks) {
         Long begin = getAddressAsLong(blk.getBeginIp());
         Long end = getAddressAsLong(blk.getEndIp());
         if (begin == null || end == null || begin > end
               || !isValidIp(netmask, begin) || !isValidIp(netmask, end)) {
            throw BddException.INVALID_PARAMETER("IP block",
                  "[" + blk.getBeginIp() + ", " + blk.getEndIp() + "]");
         }
      }
   }

   /**
    * Considering one host may have more than 1 nic(each nic may have ipv4 or ipv6 address)
    * this function will traverse all the nics and get the first none loopback ipv4 address
    * @return: the first none loopback address of the host
    *          0.0.0.0 if failed
    */
   public static String getHostManagementIp() {
      Enumeration<NetworkInterface> interfaces;
      try {
         interfaces = NetworkInterface.getNetworkInterfaces();
         while (interfaces.hasMoreElements()) {
            NetworkInterface i = (NetworkInterface) interfaces.nextElement();
            for (Enumeration<InetAddress> inetAddresses = i.getInetAddresses(); inetAddresses.hasMoreElements();) {
                InetAddress addr = (InetAddress) inetAddresses.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        }
      } catch (SocketException e) {
         return Constants.NULL_IPV4_ADDRESS;
      }
      return Constants.NULL_IPV4_ADDRESS;
  }

}
