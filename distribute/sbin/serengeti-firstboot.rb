#!/usr/bin/ruby
require "rexml/document"
include REXML
require "socket"
require "fileutils"
require "yaml"

#serengeti server user
SERENGETI_USER="serengeti"

#serengeti install home
SERENGETI_HOME="/opt/serengeti"
#serengeti conf folder
SERENGETI_WEBAPP_HOME="#{SERENGETI_HOME}/conf"
#serengeti webservice properties
SERENGETI_WEBAPP_CONF="#{SERENGETI_WEBAPP_HOME}/serengeti.properties"
SERENGETI_VC_CONF="#{SERENGETI_WEBAPP_HOME}/vc.properties"

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

GENERATE_CERT_SCRIPT="/opt/serengeti/sbin/generate-certs.sh"
CLEAR_OVF_ENV_SCRIPT="/opt/serengeti/sbin/clear-ovf-env.sh"

def is_enterprise_edition?
  File.exist? ENTERPRISE_EDITION_FLAG
end

system <<EOF
/usr/sbin/rabbitmqctl add_vhost /chef
/usr/sbin/rabbitmqctl add_user chef testing
/usr/sbin/rabbitmqctl set_permissions -p /chef chef ".*" ".*" ".*"
mkdir -p "#{SERENGETI_HOME}/.chef"
cp /etc/chef/*.pem "#{SERENGETI_HOME}/.chef"
cp "#{SERENGETI_HOME}/.chef/knife.rb" "#{SERENGETI_HOME}/.chef/knife.rb.bak"
chown serengeti:serengeti "#{SERENGETI_HOME}/.chef" -R
cd "#{SERENGETI_HOME}"
chmod +x "#{SERENGETI_SCRIPTS_HOME}/serengeti-knife-config"
su - "#{SERENGETI_USER}" -s /usr/bin/expect "#{SERENGETI_SCRIPTS_HOME}/serengeti-knife-config"
rm -rf "#{SERENGETI_HOME}/.chef/knife.rb"
mv "#{SERENGETI_HOME}/.chef/knife.rb.bak" "#{SERENGETI_HOME}/.chef/knife.rb"

mkdir -p "#{SERENGETI_TMP}"
touch "#{SERENGETI_VC_PROPERTIES}"
/usr/sbin/vmware-rpctool 'info-get guestinfo.ovfEnv' > "#{SERENGETI_VC_PROPERTIES}"
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

VCEXT_INSTANCE_ID="#{h["evs_SelfMoRef"]}"

# see getBootstrapInstanceId() in Configuration.java
def get_extension_id moref
  vmid = Integer(moref.split('-')[1])
  # same to Java Long's hashCode
  vmid = (vmid >> 32 ^ vmid) & 0xffffffff
  # convert to hex string
  "com.vmware.aurora.vcext.instance-#{vmid.to_s(16)}"
end

SERENGETI_VCEXT_ID = get_extension_id VCEXT_INSTANCE_ID
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

system <<EOF
sed -i "s/chef_server_url.*/#{fqdn_url}/" "#{CHEF_CONF}" #update CHEF_URL

# link ~/.chef to /opt/serengeti/.chef so knife can be run in any directory
ln -sf "#{SERENGETI_HOME}/.chef" /home/serengeti/.chef
chown -h serengeti:serengeti /home/serengeti/.chef
chmod 755 /home/serengeti/.chef

# update yum server url
sed -i "s|yum_server_ip|#{ethip}|" "#{SERENGETI_HOME}/www/yum/repos/base/serengeti-base.repo"
sed -i "s|yum_repos_url|'http://#{ethip}/yum/repos/base/serengeti-base.repo'|" "#{SERENGETI_HOME}/.chef/knife.rb"
sed -i "s|yum_server_ip|#{ethip}|g" "#{SERENGETI_HOME}/www/distros/manifest.sample"

chmod +x "#{SERENGETI_SCRIPTS_HOME}/serengeti-chef-init.sh"
su - "#{SERENGETI_USER}" -s /bin/bash "#{SERENGETI_SCRIPTS_HOME}/serengeti-chef-init.sh"

# get serengeti cli jar name automatically
clijarfullname=`find /opt/serengeti/cli -name serengeti-cli*.jar`
clijarname=${clijarfullname##*\/}
echo ${clijarname}

# touch serengeti cli bash
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

# write system configuration
echo "PATH=\\$PATH:\"#{SERENGETI_SCRIPTS_HOME}\"" >> /etc/profile

# generate keystores
bash #{GENERATE_CERT_SCRIPT} #{SERENGETI_KEYSTORE_PWD}
EOF

system <<EOF

# stop tomcat for update serengeti.properties
/etc/init.d/tomcat stop

# update serengeti.properties for web service
sed -i "s/distro_root =.*/#{distroip}/" "#{SERENGETI_WEBAPP_CONF}"

if [ "#{h["initResources"]}" == "True" ]; then
  sed -i "s/init_resource = .*/init_resource = true/" "#{SERENGETI_WEBAPP_CONF}"
else
  sed -i "s/init_resource = .*/init_resource = false/" "#{SERENGETI_WEBAPP_CONF}"
fi

# update vc properties
sed -i "s|vim.host =.*$|vim.host = #{h["evs_IP"]}|g" "#{SERENGETI_VC_CONF}"
sed -i "s|vim.port =.*$|vim.port = 443|g" "#{SERENGETI_VC_CONF}"
sed -i "s|vim.evs_url =.*$|vim.evs_url = #{h["evs_url"]}|g"  "#{SERENGETI_VC_CONF}"
sed -i "s|vim.evs_token =.*$|vim.evs_token = #{h["evs_token"]}|g" "#{SERENGETI_VC_CONF}"
sed -i "s|vim.thumbprint =.*$|vim.thumbprint = #{h["evs_thumbprint"]}|g" "#{SERENGETI_VC_CONF}"
sed -i "s|cms.keystore =.*$|cms.keystore = #{SERENGETI_KEYSTORE_PATH}|g" "#{SERENGETI_VC_CONF}"
sed -i "s|cms.keystore_pswd =.*$|cms.keystore_pswd = #{SERENGETI_KEYSTORE_PWD}|g" "#{SERENGETI_VC_CONF}"
sed -i "s|vim.cms_moref =.*$|vim.cms_moref = VirtualMachine:#{h["evs_SelfMoRef"]}|g" "#{SERENGETI_VC_CONF}"

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
chmod a+w #{SERENGETI_WEBAPP_CONF}
/etc/init.d/tomcat start

# generate random password for root/serengeti user when lockdown
if [ -e /opt/serengeti/etc/lock_down ]; then
  /opt/serengeti/sbin/set-password -a
fi

# Node VM use default password if serengeti is beta build
if [ ! -e /opt/serengeti/etc/lock_down ]; then
  echo "knife[:vm_use_default_password] = true" >> /opt/serengeti/.chef/knife.rb
fi

# remove ovf env file
rm -f "#{SERENGETI_VC_PROPERTIES}"

# generate ssh key pair
rm -rf "/home/#{SERENGETI_USER}/.ssh"
su - "#{SERENGETI_USER}" -c "ssh-keygen -t rsa -N '' -f /home/#{SERENGETI_USER}/.ssh/id_rsa"

# init vhm property file
init_uuid=`grep "^serengeti.initialize.uuid" #{SERENGETI_WEBAPP_CONF} | awk '{print $3}'`
while [ "${init_uuid}" = "" -o "${init_uuid}" = "true" ]
do
   sleep 1
   echo "waiting serengeti uuid is intialized"
   init_uuid=`grep "^serengeti.initialize.uuid" #{SERENGETI_WEBAPP_CONF} | awk '{print $3}'`
done
serengeti_vapp_name=`grep "^serengeti.uuid" #{SERENGETI_WEBAPP_CONF} | awk '{print $3}'`
serengeti_root_folder_prefix=`grep "^serengeti.root_folder_prefix" #{SERENGETI_WEBAPP_CONF} | awk '{print $3}'`
serengeti_uuid="${serengeti_root_folder_prefix}-${serengeti_vapp_name}"
echo serengeti_uuid=${serengeti_uuid} 
chmod a-w #{SERENGETI_WEBAPP_CONF}

if [ -e "#{VHM_CONF}" ]; then
  sed -i "s|uuid=.*$|uuid=${serengeti_uuid}|g" "#{VHM_CONF}"
  sed -i "s|vCenterId=.*$|vCenterId=#{h["evs_IP"]}|g" "#{VHM_CONF}"
  sed -i "s|keyStorePath=.*$|keyStorePath=#{SERENGETI_KEYSTORE_PATH}|g" "#{VHM_CONF}"
  sed -i "s|keyStorePwd=.*$|keyStorePwd=#{SERENGETI_KEYSTORE_PWD}|g" "#{VHM_CONF}"
  sed -i "s|extensionKey=.*$|extensionKey=#{SERENGETI_VCEXT_ID}|g" "#{VHM_CONF}"
  sed -i "s|vHadoopUser=.*$|vHadoopUser=serengeti|g" "#{VHM_CONF}"
  sed -i "s|vHadoopHome=.*$|vHadoopHome=/usr/lib/hadoop|g" "#{VHM_CONF}"
  sed -i "s|vHadoopExcludeTTFile=.*$|vHadoopExcludeTTFile=/usr/lib/hadoop/conf/mapred.hosts.exclude|g" "#{VHM_CONF}"
  sed -i "s|vHadoopPrvkeyFile=.*$|vHadoopPrvkeyFile=/home/serengeti/.ssh/id_rsa|g" "#{VHM_CONF}"
  grep -q "vCenterThumbprint" "#{VHM_CONF}" || echo "vCenterThumbprint=" >> "#{VHM_CONF}"
  sed -i "s|vCenterThumbprint=.*$|vCenterThumbprint=#{h["evs_thumbprint"]}|g" "#{VHM_CONF}"
  chmod 400 "#{VHM_CONF}"
  chown serengeti:serengeti "#{VHM_CONF}"
fi

# start vhm service on everyboot
if [ -e "#{VHM_START}" ]; then
  echo "su serengeti -c \\"bash #{VHM_START}\\"" >> /etc/rc.local
fi

# remove the path in Serengeti UI URL
if [ -e "#{HTTPD_CONF}" ]; then
  sed -i "s|# Redirect permanent.*$|Redirect permanent /datadirector http://#{ethip}:8080/serengeti|g" "#{HTTPD_CONF}"
  service httpd restart
fi

# remove vc token in ovf env
bash #{CLEAR_OVF_ENV_SCRIPT}
EOF
