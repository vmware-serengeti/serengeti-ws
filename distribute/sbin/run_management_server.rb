#!/usr/bin/env ruby

require 'server/software_management_server'


if ARGV.length != 4
  puts "usage: software_management_server {serengeti_home} {chef_client_config_file} {listen_host} {listen_port}"
end

serengetiHome = ARGV[0]
configFile = ARGV[1]
listenHost = ARGV[2]
listenPort = ARGV[3]

managementServer = Software::Mgmt::Thrift::SoftwareManagementServer.new
managementServer.run(serengetiHome, configFile,listenHost,listenPort.to_i)


