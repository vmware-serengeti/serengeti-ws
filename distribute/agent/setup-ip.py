#!/usr/bin/python

# ***** BEGIN LICENSE BLOCK *****
#    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
#    Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ***** END LICENSE BLOCK *****

import os, re, sys, socket, struct, traceback

def ReadMachineId():
   try:
      machineId = os.popen('/usr/sbin/vmware-rpctool machine.id.get').read()
      if len(machineId) == 0: return None
      return machineId
   except:
      print "get machine.id exception, ignore"
      return None


def WriteFile(file, content):
   os.system("cp %s %s" % (file, file + "~"))
   fh = open(file, 'w')
   fh.write(content)
   fh.close()


def ReadNetworkPropFromMachineId(machineId, netPropertyMap, netCfg):
   def GetNetCfg(properties):
      cfg = {}
      mIdCmd='/opt/vmware/sbin/machine_id_guest_var %s'
      for key in properties:
         value = os.popen(mIdCmd % key).read().strip()
         if value != None and len(value) > 0:
            cfg[properties[key]] = value
      return cfg   

   for eth in netPropertyMap:
      if netPropertyMap[eth].has_key('src') and netPropertyMap[eth]['src'] == 'machine.id':
         netPropertyMap[eth].pop('src')
         netCfg[eth] = GetNetCfg(netPropertyMap[eth])
         

def SetEthAsDhcp(vmName, nic, hostname):
   dhcpCfgTemplate = """
ONBOOT=yes
STARTMODE=manual
DEVICE=%s
BOOTPROTO=dhcp
DHCLIENT_RELEASE_BEFORE_QUIT=yes
"""

   dhcpCfgTemplate = dhcpCfgTemplate.lstrip()

   interfaces = dhcpCfgTemplate % nic

   # support DDNS for multiple hostname/interface
   if hostname != None and len(hostname) != 0:
      interfaces += "DHCLIENT_HOSTNAME_OPTION=%s\n" % hostname
   # dhcp client should not set hostname from cms/ldap's internal/vCenter interfaces (eth1/2)
   if nic == "eth0" :
      interfaces += "DHCLIENT_SET_HOSTNAME=yes\n"
   else:
      interfaces += "DHCLIENT_SET_HOSTNAME=no\n"

   # update eth configuration file
   WriteFile('/etc/sysconfig/network-scripts/ifcfg-%s' % nic, interfaces)
   # bring up the nic
   os.system('/etc/init.d/network start %s -o manual' % nic)

         

def SetHostName(ip, hostname):
   hostNameCfgTemplate = """
%s %s
"""

   hostNameCfgTemplate = hostNameCfgTemplate.lstrip()
   interfaces = hostNameCfgTemplate % (ip, hostname)
   # update eth configuration file
   WriteFile('/etc/hosts', interfaces)
   

def SetEthAsDhcp(vmName, nic, hostname):
   dhcpCfgTemplate = """
ONBOOT=yes
STARTMODE=manual
DEVICE=%s
BOOTPROTO=dhcp
DHCLIENT_RELEASE_BEFORE_QUIT=yes
"""

   dhcpCfgTemplate = dhcpCfgTemplate.lstrip()

   interfaces = dhcpCfgTemplate % nic

   # support DDNS for multiple hostname/interface
   if hostname != None and len(hostname) != 0:
      interfaces += "DHCLIENT_HOSTNAME_OPTION=%s\n" % hostname
   # dhcp client should not set hostname from cms/ldap's internal/vCenter interfaces (eth1/2)
   if nic == "eth0" :
      interfaces += "DHCLIENT_SET_HOSTNAME=yes\n"
   else:
      interfaces += "DHCLIENT_SET_HOSTNAME=no\n"

   # update eth configuration file
   WriteFile('/etc/sysconfig/network-scripts/ifcfg-%s' % nic, interfaces)
   # bring up the nic
   os.system('/etc/init.d/network start %s -o manual' % nic)



def SetupNetwork(vmName, cfg):
   staticCfgTemplate = """
ONBOOT=yes
DEVICE=%(dev)s
BOOTPROTO=static
STARTMODE=manual
IPADDR=%(ip)s
NETMASK=%(netmask)s
GATEWAY=%(gateway)s
"""

   staticCfgTemplate = staticCfgTemplate.lstrip()

      # setup DNS
   if cfg.has_key('dns'):
      dnsServers = ['0.0.0.0']
      # remove redundant dns servers
      for dns in cfg['dns']:
         if not cfg['dns'][dns] in dnsServers:
            dnsServers.append(cfg['dns'][dns])

      resolvConf = ""
      for dns in dnsServers[1:]: 
         resolvConf += "nameserver " + dns + "\n"

      if len(resolvConf) != 0:
         print "setting up DNS rules: %s" % resolvConf
         WriteFile('/etc/resolv.conf', resolvConf)

      cfg.pop('dns')

   # setup network interfaces
   for nic in cfg:
      settings = cfg[nic]
      settings['dev'] = nic

      hostname = ""
      if settings.has_key('hostname') and len(settings['hostname']) != 0:
         hostname = settings['hostname']

      if (settings.has_key('policy') and settings['policy'] == 'dhcp') or \
         not settings.has_key('ip') or len(settings['ip']) == 0 or settings['ip'] == '0.0.0.0' or \
         not settings.has_key('netmask') or len(settings['netmask']) == 0 or settings['netmask'] == '0.0.0.0':
         # apply dhcp tempate here
         SetEthAsDhcp(vmName, nic, hostname)
         continue

      interfaces = staticCfgTemplate % settings

      if settings.has_key('gateway') and len(settings['gateway']) != 0 and settings['gateway'] != '0.0.0.0':
         #interfaces += "GATEWAY=%s\n" % settings['gateway']
         # set the last NIC's gateway as default gw
         if settings['dev'] == "eth0":
            defaultRoute = "default\t%s\t dev \t%s\n" % (settings['gateway'], settings['dev'])
            print "setting up default gw: %s" % defaultRoute
            #WriteFile('/etc/sysconfig/network/routes', defaultRoute)
            WriteFile('/etc/sysconfig/network-scripts/route-eth0', defaultRoute)

      # update eth configuration file
      WriteFile('/etc/sysconfig/network-scripts/ifcfg-%s' % nic, interfaces)
      # bring up the nic
      os.system('/etc/init.d/network start %s -o manual' % nic)
   
   ips = os.popen( "/sbin/ifconfig -a|grep inet|grep -v 127.0.0.1|grep -v inet6|awk \'{print $2}\'|tr -d \"addr:\" ").read()
   ip = ips[:-1]
#   SetHostName(ip, ip)
   os.system('hostname %s' % ip)
   #os.system('/etc/init.d/network restart')
      
###################
##  Main program ##
###################
if __name__ == "__main__":
   # Setup mapping of ip address and ovf properties
   netPropertyMap = {
      "serengeti_cv" : {
         "eth0" : {
            "src" : "machine.id",
            "bootproto" : "policy",
            "ipaddr"    : "ip",
            "netmask"   : "netmask",
            "gateway"   : "gateway",
            "hostname"  : "hostname",
            },
         "dns" : {
            "src" : "machine.id",
            "dnsserver0" : "dns0",
            "dnsserver1" : "dns1"
            }
         }
      }

   try:
      # set vm type as vhelper by default
      vmName = "serengeti_cv"
      print "VM Type: %s" % vmName 

      machineId = ReadMachineId()

      if machineId == None:
         # start the network anyway, default to use last boot's settings
         print "No machine.id found, start network with last boot's settings"
         os.system('/etc/init.d/network start -o manual')
      else: 
         netCfg = {}
         print "Reading network settings from machine.id"
         ReadNetworkPropFromMachineId(machineId, netPropertyMap[vmName], netCfg)
         print "Setting up network with cfg:"
         print netCfg
         SetupNetwork(vmName, netCfg)
   except:
      print "Unexpected error:"
      traceback.print_exc()
      sys.exit(-1)
