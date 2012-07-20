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

system <<EOF
/usr/sbin/rabbitmqctl add_vhost /chef
/usr/sbin/rabbitmqctl add_user chef testing
/usr/sbin/rabbitmqctl set_permissions -p /chef chef ".*" ".*" ".*"
mkdir -p "#{SERENGETI_TMP}"
touch "#{SERENGETI_VC_PROPERTIES}"
/usr/sbin/vmware-rpctool 'info-get guestinfo.ovfEnv' > "#{SERENGETI_VC_PROPERTIES}"
mkdir -p "#{SERENGETI_HOME}/.chef"
cp /etc/chef/*.pem "#{SERENGETI_HOME}/.chef"
cp "#{SERENGETI_HOME}/.chef/knife.rb" "#{SERENGETI_HOME}/.chef/knife.rb.bak"
chown serengeti:serengeti "#{SERENGETI_HOME}/.chef" -R
cd "#{SERENGETI_HOME}"
chmod +x "#{SERENGETI_SCRIPTS_HOME}/serengeti-knife-config"
su - "#{SERENGETI_USER}" -s /usr/bin/expect "#{SERENGETI_SCRIPTS_HOME}/serengeti-knife-config"
rm -rf "#{SERENGETI_HOME}/.chef/knife.rb"
mv "#{SERENGETI_HOME}/.chef/knife.rb.bak" "#{SERENGETI_HOME}/.chef/knife.rb"
EOF

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

#save dhcp init configuration
FileUtils.cp("#{ETH_CONFIG_FILE}", "#{ETH_CONFIG_FILE_DHCP}")
FileUtils.cp("#{DNS_CONFIG_FILE}", "#{DNS_CONFIG_FILE_DHCP}")

#network update
if ("#{h["boot_proto"]}" == "DHCP") then
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

#init resource flag, it records the initial value for initResources
#if fog or tomcat failed, we also init resources after reboot or re-configuration
if ("#{h["initResources"]}" == "True") then
   FileUtils.touch("#{SERENGETI_HOME}/logs/not-init")
end

system <<EOF
sed -i "s/chef_server_url.*/#{fqdn_url}/" "#{CHEF_CONF}" #update CHEF_URL
chmod +x "#{SERENGETI_SCRIPTS_HOME}/serengeti-chef-init.sh"
su - "#{SERENGETI_USER}" -s /bin/bash "#{SERENGETI_SCRIPTS_HOME}/serengeti-chef-init.sh"

#get serengeti cli jar name automatically
clijarfullname=`find /opt/serengeti/cli -name serengeti-cli*.jar`
clijarname=${clijarfullname##*\/}
echo ${clijarname}

#touch serengeti cli bash
chown serengeti:serengeti "#{SERENGETI_CLI_HOME}" -R #
touch "#{SERENGETI_SCRIPTS_HOME}/serengeti"
chmod +x "#{SERENGETI_SCRIPTS_HOME}/serengeti"
echo "#!/bin/bash" > "#{SERENGETI_SCRIPTS_HOME}/serengeti"
echo "clijarname=${clijarfullname##*\/}" >> "#{SERENGETI_SCRIPTS_HOME}/serengeti"
cat >> "#{SERENGETI_SCRIPTS_HOME}/serengeti" << 'SHELLEOF'
cd "#{SERENGETI_CLI_HOME}"
if [ $# == 0 ]
then
  java -jar \${clijarname}
else
  case "$1" in
    --help)
      echo "Usage: serengeti [command] [--cmdfile file] [--histsize size] [--help]"
      ;;
    --*)
      java -jar \${clijarname} $*
      ;;
    *)
      java -jar \${clijarname} $1
      ;;
  esac
fi
SHELLEOF

#write system configuration
echo "PATH=\\$PATH:\"#{SERENGETI_SCRIPTS_HOME}\"" >> /etc/profile

EOF

cloud_server = 'vsphere'
info = {:provider => cloud_server,
  :vsphere_server => h["evs_IP"],
  :vsphere_username => h["vcusername"],
  :vsphere_password => h["vcpassword"],
}
connection = Fog::Compute.new(info)
mob = connection.get_vm_mob_ref_by_moid(h["evs_SelfMoRef"])
vmdatastores = mob.datastore.map {|ds| "#{ds.info.name}"}     #serengeti server Datastore name
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
sed -i "s/vc_addr = .*/vc_addr = \"#{h["evs_IP"]}\"/" "#{SERENGETI_WEBAPP_CONF}"
sed -i "s/vc_user = .*/vc_user = \"#{vcuser}\"/" "#{SERENGETI_WEBAPP_CONF}"
sed -i "s/vc_pwd = .*/vc_pwd = \"#{updateVCPassword}\"/" "#{SERENGETI_WEBAPP_CONF}"
sed -i "s/vc_datacenter = .*/#{vcdatacenterline}/" "#{SERENGETI_WEBAPP_CONF}"
sed -i "s/template_id = .*/#{templateid}/" "#{SERENGETI_WEBAPP_CONF}"

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
connecthost="connect --host localhost:8080"
su - "#{SERENGETI_USER}" -c "#{SERENGETI_SCRIPTS_HOME}/serengeti \\"${connecthost}\\""

#add default resourcepool, datastore, and dhcp network
if [ "#{h["initResources"]}" == "True" ]; then
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

EOF
