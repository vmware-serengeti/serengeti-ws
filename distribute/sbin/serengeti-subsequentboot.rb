#!/usr/bin/ruby
require "rexml/document"
include REXML
require "socket"
require "fileutils"

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

ENTERPRISE_EDITION_FLAG="/opt/serengeti/etc/enterprise"

SERENGETI_CERT_FILE="/opt/serengeti/.certs/serengeti.pem"
SERENGETI_PRIVATE_KEY="/opt/serengeti/.certs/private.pem"

def is_enterprise_edition?
  File.exist? ENTERPRISE_EDITION_FLAG
end

HTTPD_CONF="/etc/httpd/conf/httpd.conf"

CLEAR_OVF_ENV_SCRIPT="/opt/serengeti/sbin/clear-ovf-env.sh"

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
# update Chef Server url
sed -i "s/chef_server_url.*/#{fqdn_url}/" "#{CHEF_CONF}"
# update yum server url
sed -i "s|http://.*/yum|http://#{ethip}/yum|" "#{SERENGETI_HOME}/www/yum/repos/base/serengeti-base.repo"
sed -i "s|http://.*/yum|http://#{ethip}/yum|" "#{SERENGETI_HOME}/.chef/knife.rb"
EOF

system <<EOF

#stop tomcat for update serengeti.properties
/etc/init.d/tomcat stop

#update serengeti.properties for web service
sed -i "s/distro_root =.*/#{distroip}/" "#{SERENGETI_WEBAPP_CONF}"

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

# update serengeti server ip address in httpd conf
if [ -e "#{HTTPD_CONF}" ]; then
  sed -i "s|Redirect permanent.*$|Redirect permanent /datadirector http://#{ethip}:8080/serengeti|g" "#{HTTPD_CONF}"
  service httpd restart
fi

# remove ovf env file
rm -f "#{SERENGETI_VC_PROPERTIES}"

# remove vc token in ovf env
bash #{CLEAR_OVF_ENV_SCRIPT}
EOF
