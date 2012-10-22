#!/usr/bin/ruby
require "rexml/document"
include REXML
require "socket"
require "fileutils"
require "fog"
require "yaml"

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
VHM_START="/opt/serengeti/sbin/vhm-start.sh"

HTTPD_CONF="/etc/httpd/conf/httpd.conf"

SERENGETI_CERT_FILE="/opt/serengeti/.certs/serengeti.pem"
SERENGETI_PRIVATE_KEY="/opt/serengeti/.certs/private.pem"
SERENGETI_KEYSTORE_PATH="/opt/serengeti/.certs/serengeti.jks"
SERENGETI_KEYSTORE_PWD=%x[openssl rand -base64 6].strip

ENTERPRISE_EDITION_FLAG="/opt/serengeti/etc/enterprise"

VCEXT_TOOL_DIR="/opt/serengeti/vcext"
VCEXT_TOOL_JAR=%x[ls #{VCEXT_TOOL_DIR}/vcext-*.jar].strip

GENERATE_CERT_SCRIPT="/opt/serengeti/sbin/generate-certs.sh"

def is_enterprise_edition?
  File.exist? ENTERPRISE_EDITION_FLAG
end

def get_extension_id
  if File.exist? SERENGETI_CLOUD_MANAGER_CONF
    vc_info = YAML.load(File.open(SERENGETI_CLOUD_MANAGER_CONF))
    return vc_info["extension_key"] unless vc_info["extension_key"].nil?
  end
  "com.vmware.serengeti." + %x[uuidgen].strip[0..7]
end

SERENGETI_VCEXT_ID=get_extension_id

CLEAR_OVF_ENV_SCRIPT="/opt/serengeti/sbin/clear-ovf-env.sh"

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

h["evs_url"] =
  properties_doc.elements["*/ve:vServiceEnvironmentSection/evs:GuestApi/evs:URL"].text
puts("evs URL: " + "#{h["evs_url"]}")

h["evs_token"] =
  properties_doc.elements["*/ve:vServiceEnvironmentSection/evs:GuestApi/evs:Token"].text
puts("evs Token: " + "*** HIDDEN ***")

h["evs_thumbprint"] =
  properties_doc.elements["*/ve:vServiceEnvironmentSection/evs:GuestApi/evs:X509Thumbprint"].text
puts("evs X509Thumbprint: " + "#{h["evs_thumbprint"]}")

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

# link ~/.chef to /opt/serengeti/.chef so knife can be run in any directory
ln -sf "#{SERENGETI_HOME}/.chef" /home/serengeti/.chef
chown -h serengeti:serengeti /home/serengeti/.chef
chmod 755 /home/serengeti/.chef

# update yum server url
sed -i "s|yum_server_ip|#{ethip}|" "#{SERENGETI_HOME}/www/yum/repos/base/serengeti-base.repo"
sed -i "s|yum_repos_url|'http://#{ethip}/yum/repos/base/serengeti-base.repo'|" "#{SERENGETI_HOME}/.chef/knife.rb"

chmod +x "#{SERENGETI_SCRIPTS_HOME}/serengeti-chef-init.sh"
su - "#{SERENGETI_USER}" -s /bin/bash "#{SERENGETI_SCRIPTS_HOME}/serengeti-chef-init.sh"

#get serengeti cli jar name automatically
clijarfullname=`find /opt/serengeti/cli -name serengeti-cli*.jar`
clijarname=${clijarfullname##*\/}
echo ${clijarname}

#touch serengeti cli bash
chown serengeti:serengeti "#{SERENGETI_CLI_HOME}" -R #
touch "#{SERENGETI_SCRIPTS_HOME}/serengeti"
chown serengeti:serengeti "#{SERENGETI_SCRIPTS_HOME}/serengeti"
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
echo "CLOUD_MANAGER_CONFIG_DIR=\"#{SERENGETI_HOME}\"/conf" >> /etc/profile

# register serengeti server as vc extension service
if [ -e #{ENTERPRISE_EDITION_FLAG} -a "#{VCEXT_TOOL_JAR}" != "" ]; then
  # generate certificate and private key
  bash #{GENERATE_CERT_SCRIPT} #{SERENGETI_KEYSTORE_PWD}
  echo "registering serengeti server as vc ext service"
  java -jar #{VCEXT_TOOL_JAR} \
    -evsURL "#{h["evs_url"]}" \
    -evsToken "#{h["evs_token"]}" \
    -evsThumbprint "#{h["evs_thumbprint"]}" \
    -extKey "#{SERENGETI_VCEXT_ID}" \
    -cert "#{SERENGETI_CERT_FILE}"
  ret=$?
  if [ $ret != 0 ]; then
    echo "failed to register serengeti server as vc ext service"
    exit 1
  fi
fi

EOF

def get_connection_info(vc_info) 
  cloud_server = 'vsphere'
  info = {:provider => cloud_server,
    :vsphere_server => vc_info["evs_IP"]
  }

  if is_enterprise_edition? 
    info[:cert] = SERENGETI_CERT_FILE
    info[:key] = SERENGETI_PRIVATE_KEY
    info[:extension_key] = SERENGETI_VCEXT_ID
  else
    info[:vsphere_username] = vc_info["vcusername"]
    info[:vsphere_password] = vc_info["vcpassword"]
  end
  info
end

conn_info = get_connection_info(h)
connection = Fog::Compute.new(conn_info)

mob = connection.get_vm_mob_ref_by_moid(h["evs_SelfMoRef"])
vmdatastores = mob.datastore.map {|ds| "#{ds.info.name}"}     #serengeti server Datastore name
puts("serengeti server datastore: " + "#{vmdatastores[0]}")
vmrp = "#{mob.resourcePool.parent.name}"		      #serengeti server resource pool name
puts("serengeti server resource pool: " + "#{vmrp}")
vApp_name = "#{mob.parentVApp.name}"                               #serengeti vApp name
puts("serengeti vApp name: " + "#{vApp_name}")
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

# update serengeti uuid
sed -i "s/serengeti.uuid =.*/serengeti.uuid = #{vApp_name}/" "#{SERENGETI_WEBAPP_CONF}"

#update serengeti.properties for web service
sed -i "s/distro_root =.*/#{distroip}/" "#{SERENGETI_WEBAPP_CONF}"
sed -i "s/vc_datacenter = .*/#{vcdatacenterline}/" "#{SERENGETI_WEBAPP_CONF}"
sed -i "s/template_id = .*/#{templateid}/" "#{SERENGETI_WEBAPP_CONF}"

echo "vc_addr: #{h["evs_IP"]}" > "#{SERENGETI_CLOUD_MANAGER_CONF}"

if [ -e #{ENTERPRISE_EDITION_FLAG} ]; then
  echo "key: #{SERENGETI_PRIVATE_KEY}" >> "#{SERENGETI_CLOUD_MANAGER_CONF}"
  echo "cert: #{SERENGETI_CERT_FILE}" >> "#{SERENGETI_CLOUD_MANAGER_CONF}"
  echo "extension_key: #{SERENGETI_VCEXT_ID}" >> "#{SERENGETI_CLOUD_MANAGER_CONF}"
else
  echo "vc_user: #{vcuser}" >> "#{SERENGETI_CLOUD_MANAGER_CONF}"
  echo "vc_pwd: #{updateVCPassword}" >> "#{SERENGETI_CLOUD_MANAGER_CONF}"
fi

chmod 400 "#{SERENGETI_CLOUD_MANAGER_CONF}"
chown serengeti:serengeti "#{SERENGETI_CLOUD_MANAGER_CONF}"

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

# generate random password for root/serengeti user when lockdown
if [ -e /opt/serengeti/etc/lock_down ]; then
  /opt/serengeti/sbin/set-password -a
fi

# remove ovf env file
rm -f "#{SERENGETI_VC_PROPERTIES}"

# generate ssh key pair
rm -rf "/home/#{SERENGETI_USER}/.ssh"
su - "#{SERENGETI_USER}" -c "ssh-keygen -t rsa -N '' -f /home/#{SERENGETI_USER}/.ssh/id_rsa"

# init vhm property file
if [ -e "#{VHM_CONF}" ]; then
  sed -i "s|vCenterId=.*$|vCenterId=#{h["evs_IP"]}|g" "#{VHM_CONF}"
  sed -i "s|vCenterUser=.*$|vCenterUser=#{vcuser}|g"  "#{VHM_CONF}"
  sed -i "s|vCenterPwd=.*$|vCenterPwd=#{updateVCPassword}|g" "#{VHM_CONF}"
  sed -i "s|keyStorePath=.*$|keyStorePath=#{SERENGETI_KEYSTORE_PATH}|g" "#{VHM_CONF}"
  sed -i "s|keyStorePwd=.*$|keyStorePwd=#{SERENGETI_KEYSTORE_PWD}|g" "#{VHM_CONF}"
  sed -i "s|extensionKey=.*$|extensionKey=#{SERENGETI_VCEXT_ID}|g" "#{VHM_CONF}"
  sed -i "s|vHadoopUser=.*$|vHadoopUser=root|g" "#{VHM_CONF}"
  sed -i "s|vHadoopPwd=.*$|vHadoopPwd=password|g" "#{VHM_CONF}"
  sed -i "s|vHadoopHome=.*$|vHadoopHome=/usr/lib/hadoop|g" "#{VHM_CONF}"
  sed -i "s|vHadoopExcludeTTFile=.*$|vHadoopExcludeTTFile=/usr/lib/hadoop/conf/mapred.hosts.exclude|g" "#{VHM_CONF}"
  chmod 400 "#{VHM_CONF}"
  chown serengeti:serengeti "#{VHM_CONF}"
fi

# start vhm service on everyboot
if [ -e "#{VHM_START}" ]; then
  echo "su serengeti -c \\"bash #{VHM_START}\\"" >> /etc/rc.local
fi

# remove the path in Serengeti UI URL
if [ -e "#{HTTPD_CONF}" ]; then
  sed -i "s|# Redirect permanent.*$|Redirect permanent /datadirector http://#{ethip}:8080/datadirector|g" "#{HTTPD_CONF}"
  service httpd restart
fi

# remove vc token in ovf env
bash #{CLEAR_OVF_ENV_SCRIPT}
EOF
