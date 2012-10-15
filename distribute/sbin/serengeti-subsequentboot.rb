#!/usr/bin/ruby
require "rexml/document"
include REXML
require "socket"
require "fileutils"
require "fog"

#serengeti server user
SERENGETI_USER="serengeti"

#serengeti install home
SERENGETI_HOME="/opt/serengeti"
#serengeti conf folder
SERENGETI_WEBAPP_HOME="#{SERENGETI_HOME}/conf"
#serengeti webservice properties
SERENGETI_WEBAPP_CONF="#{SERENGETI_WEBAPP_HOME}/serengeti.properties"
SERENGETI_CLOUD_MANAGER_CONF="#{SERENGETI_WEBAPP_HOME}/cloud-manager.vsphere.yaml"
#cookbook related .chef/knife.rb
CHEF_CONF="#{SERENGETI_HOME}/.chef/knife.rb"
#serengeti tmp folder
SERENGETI_TMP="#{SERENGETI_HOME}/tmp"
SERENGETI_VC_PROPERTIES="#{SERENGETI_TMP}/vcproperties.xml" #VC properities
#serengeti scriptes folder
SERENGETI_SCRIPTS_HOME="#{SERENGETI_HOME}/sbin"
#serengeti cli folder
SERENGETI_CLI_HOME="#{SERENGETI_HOME}/cli"
#for serengeti server using static ip
ETH_CONFIG_FILE="/etc/sysconfig/network-scripts/ifcfg-eth0"
ETH_CONFIG_FILE_DHCP="/etc/sysconfig/network-scripts/ifcfg-eth0.bak"
ETH_CONFIG_FILE_TMP="/etc/sysconfig/network-scripts/ifcfg-eth0.tmp"
DNS_CONFIG_FILE="/etc/resolv.conf"
DNS_CONFIG_FILE_DHCP="/etc/resolv.conf.bak"
DNS_CONFIG_FILE_TMP="/etc/resolv.conf.tmp"

VHM_CONF="/opt/serengeti/conf/vhm.properties"

HTTPD_CONF="/etc/httpd/conf/httpd.conf"

system <<EOF
#rabbitmq reconfigure
/usr/sbin/rabbitmqctl add_vhost /chef
/usr/sbin/rabbitmqctl add_user chef testing
/usr/sbin/rabbitmqctl set_permissions -p /chef chef ".*" ".*" ".*"
#get serengeti vc properties xml
mkdir -p "#{SERENGETI_TMP}"
touch "#{SERENGETI_VC_PROPERTIES}"
/usr/sbin/vmware-rpctool 'info-get guestinfo.ovfEnv' > "#{SERENGETI_VC_PROPERTIES}"
EOF

#handle vc properties file
properties_doc = Document.new File.new "#{SERENGETI_VC_PROPERTIES}"
h = Hash.new
properties_doc.elements.each("*/PropertySection/Property") do |element|
  h["#{element.attributes["oe:key"]}"] = element.attributes["oe:value"]
  puts("#{element.attributes["oe:key"]}" + ": " + h["#{element.attributes["oe:key"]}"])
end
h["networkName"] =
  properties_doc.elements["*/ve:EthernetAdapterSection/ve:Adapter"].attributes["ve:network"]
puts("Network Name: " + "#{h["networkName"]}")  
h["evs_IP"] =
  properties_doc.elements["*/ve:vServiceEnvironmentSection/evs:VCenterApi/evs:IP"].text
puts("VC IP: " + "#{h["evs_IP"]}")
h["evs_SelfMoRef"] =
  properties_doc.elements["*/ve:vServiceEnvironmentSection/evs:VCenterApi/evs:SelfMoRef"].text.split(/:/).pop
puts("VM Ref ID: " + "#{h["evs_SelfMoRef"]}")  
h["templatename"] =
  properties_doc.elements["*/Entity"].attributes["oe:id"]
puts("Node Template Name: " + "#{h["templatename"]}")

#network update
if ("#{h["boot_proto"]}" == "DHCP") then
   FileUtils.cp("#{ETH_CONFIG_FILE_DHCP}", "#{ETH_CONFIG_FILE}")
   FileUtils.cp("#{DNS_CONFIG_FILE_DHCP}", "#{DNS_CONFIG_FILE}")
   system("/etc/init.d/network restart")
   ethiptmp = `/sbin/ifconfig eth0 | grep "inet addr" | awk '{print $2}'`
   ethip = "#{ethiptmp}".strip.split(/:/).pop
else
   File.open("#{ETH_CONFIG_FILE_TMP}", 'w') { |file_tmp|
	    file_tmp.puts "DEVICE=eth0"
        file_tmp.puts "BOOTPROTO=static"
	    file_tmp.puts "IPADDR=#{h["ipAddr"]}"
		file_tmp.puts "NETMASK=#{h["netmask"]}"
        file_tmp.puts "GATEWAY=#{h["gateway"]}"
        file_tmp.puts "PEERDNS=no"
        file_tmp.puts "ONBOOT=yes"
        file_tmp.puts "TYPE=Ethernet"		
    }
    FileUtils.rm("#{ETH_CONFIG_FILE}")
    FileUtils.mv("#{ETH_CONFIG_FILE_TMP}", "#{ETH_CONFIG_FILE}")
	
	File.open("#{DNS_CONFIG_FILE_TMP}", 'w') { |file_tmp|
      file_tmp.puts "nameserver #{h["dns1"]}"
	  if "#{h["dns2"]}" != "0.0.0.0"
       file_tmp.puts "nameserver #{h["dns2"]}"
      end
    }
    FileUtils.rm("#{DNS_CONFIG_FILE}")
    FileUtils.mv("#{DNS_CONFIG_FILE_TMP}", "#{DNS_CONFIG_FILE}")
    system("/etc/init.d/network restart")
    ethip = "#{h["ipAddr"]}"
end

#get updated distroip
distroip="distro_root = " + "http:\\/\\/#{ethip}\\/distros"
puts("distro ip: #{distroip}")
#get updated fqdn_url
fqdn_url="chef_server_url         " + "'http:\\/\\/#{ethip}:4000'"
puts("fqdn_url: #{fqdn_url}")

system <<EOF
sed -i "s/chef_server_url.*/#{fqdn_url}/" "#{CHEF_CONF}" #update CHEF_URL
EOF

#get serenegeti server vsphere related information
cloud_server = 'vsphere'
info = {:provider => cloud_server,
  :vsphere_server => h["evs_IP"],
  :vsphere_username => h["vcusername"],
  :vsphere_password => h["vcpassword"],
}
connection = Fog::Compute.new(info)
mob = connection.get_vm_mob_ref_by_moid(h["evs_SelfMoRef"])
vmdatastores = mob.datastore.map {|ds| "#{ds.info.name}"}  	#serengeti server Datastore name
puts("serengeti server datastore: " + "#{vmdatastores[0]}")
vmrp = "#{mob.resourcePool.parent.name}"					#serengeti server resource pool name
puts("serengeti server resource pool: " + "#{vmrp}")
vmcluster = "#{mob.resourcePool.owner.name}" #serengeti server vc cluster name
puts("serengeti server vc cluster: " + "#{vmcluster}")
datacenter = mob.resourcePool.owner
while datacenter.parent
  break if datacenter.class.to_s == 'Datacenter'
  datacenter = datacenter.parent
end
vcdatacenter = datacenter.name #serengeti server datastore name
puts("serengeti server datacenter: " + "#{vcdatacenter}")
vms = mob.resourcePool.vm
template_mob = vms.find {|v| v.name == "#{h["templatename"]}"}
template_moid = template_mob._ref
puts("template id: " + "#{template_moid}")

vcuser = "#{h["vcusername"]}"
puts("vc user: " + "#{vcuser}")
updateVCPassword = "#{h["vcpassword"]}".gsub("$", "\\$")
templateid = "template_id = " + "#{template_moid}"
vcdatacenterline = "vc_datacenter = " + "#{vcdatacenter}"

system <<EOF

#stop tomcat for update serengeti.properties
/etc/init.d/tomcat stop

#update serengeti.properties for web service
sed -i "s/distro_root =.*/#{distroip}/" "#{SERENGETI_WEBAPP_CONF}"
sed -i "s/vc_datacenter = .*/#{vcdatacenterline}/" "#{SERENGETI_WEBAPP_CONF}"
sed -i "s/template_id = .*/#{templateid}/" "#{SERENGETI_WEBAPP_CONF}"

echo "vc_addr: #{h["evs_IP"]}" > "#{SERENGETI_CLOUD_MANAGER_CONF}"
echo "vc_user: #{vcuser}" >> "#{SERENGETI_CLOUD_MANAGER_CONF}"
echo "vc_pwd:  #{updateVCPassword}" >> "#{SERENGETI_CLOUD_MANAGER_CONF}"
chmod 400 "#{SERENGETI_CLOUD_MANAGER_CONF}"
chown serengeti:serengeti "#{SERENGETI_CLOUD_MANAGER_CONF}"

# re-init vhm property file
if [ -e "#{VHM_CONF}" ]; then
  sed -i "s|vCenterId=.*$|vCenterId=#{h["evs_IP"]}|g" "#{VHM_CONF}"
  sed -i "s|vCenterUser=.*$|vCenterUser=#{vcuser}|g"  "#{VHM_CONF}"
  sed -i "s|vCenterPwd=.*$|vCenterPwd=#{updateVCPassword}|g" "#{VHM_CONF}"
  sed -i "s|vHadoopUser=.*$|vHadoopUser=root|g" "#{VHM_CONF}"
  sed -i "s|vHadoopPwd=.*$|vHadoopPwd=password|g" "#{VHM_CONF}"
  sed -i "s|vHadoopHome=.*$|vHadoopHome=/usr/lib/hadoop|g" "#{VHM_CONF}"
  sed -i "s|vHadoopExcludeTTFile=.*$|vHadoopExcludeTTFile=/usr/lib/hadoop/conf/mapred.hosts.exclude|g" "#{VHM_CONF}"
  chmod 400 "#{VHM_CONF}"
  chown serengeti:serengeti "#{VHM_CONF}"
fi


#kill tomcat using shell direclty to avoid failing to stop tomcat
pidlist=`ps -ef|grep tomcat | grep -v "grep"|awk '{print $2}'`
echo "tomcat Id list :$pidlist"
if [ "$pidlist" = "" ]
then
   echo "no tomcat pid alive"
else
   for pid in ${pidlist}
   {
      kill -9 $pid
      echo "KILL $pid:"
      echo "service stop success"
   }
fi

#start serengeti web service
/etc/init.d/tomcat start

#serengeti cli connect first
connecthost="connect --host localhost:8080 --username serengeti --password password"
su - "#{SERENGETI_USER}" -c "#{SERENGETI_SCRIPTS_HOME}/serengeti \\"${connecthost}\\""

if [[ -f "#{SERENGETI_HOME}/logs/not-init" ]];then
   rpadd="resourcepool add --name defaultRP --vcrp \\"#{vmrp}\\" --vccluster \\"#{vmcluster}\\""
   dsadd="datastore add --name defaultDSShared --spec \\"#{vmdatastores[0]}\\" --type SHARED"
   ntadd="network add --name defaultNetwork --portGroup \\"#{h["networkName"]}\\" --dhcp"
   touch "#{SERENGETI_CLI_HOME}/initResources"
   echo ${rpadd} >> "#{SERENGETI_CLI_HOME}/initResources"
   echo ${dsadd} >> "#{SERENGETI_CLI_HOME}/initResources"
   echo ${ntadd} >> "#{SERENGETI_CLI_HOME}/initResources"
   su - "#{SERENGETI_USER}" -c "#{SERENGETI_SCRIPTS_HOME}/serengeti --cmdfile #{SERENGETI_CLI_HOME}/initResources"
   rm -rf "#{SERENGETI_HOME}/logs/not-init"
fi

# update serengeti server ip address in httpd conf
if [ -e "#{HTTPD_CONF}" ]; then
  sed -i "s|Redirect permanent.*$|Redirect permanent /datadirector http://#{ethip}:8080/serengeti|g" "#{HTTPD_CONF}"
  service httpd restart
fi

# remove ovf env file
rm -f "#{SERENGETI_VC_PROPERTIES}"
EOF
