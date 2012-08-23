#!/usr/bin/ruby
#Author: Jun wang
#
#Generate hadoop rack awareness mapping file
# usage: topology-generator.rb cluster_name rack_description_file
require 'json'
require 'net/http'
require 'highline/import'

def check_rack_host_desc_format( file_name )
  f = File.open(file_name, "r")

  line_num = 1
  f.each_line do |line|
    stripped_line = line.strip
    if stripped_line == ""
      line_num += 1
      next
    end
    valid = '[^:]+'
    if (/^\/#{valid}:#{valid}/ =~ stripped_line) != 0
      puts "incorrect rack/host description format from #{file_name} at line #{line_num}"
      return 0
    end
    line_num += 1
  end

  return 1
end

unless ARGV.length == 2
  puts "Usage: topology-generator.rb cluster_name description_file"
  exit
end
cluster_name = ARGV[0]
rack_descriptiion_file = ARGV[1]

#login serengeti first
puts "login serengeti now."
username = ask("username: ") { |q| q.echo = true }
password = ask("password: ") { |q| q.echo = "*" }
path = "/serengeti/j_spring_security_check?j_username=#{username}&j_password=#{password}"
http = Net::HTTP.new('localhost', 8080)
headers = {
  'Content-Type' => 'text/html'
}
conn_resp = http.post(path, nil, headers)
unless conn_resp.code == '200'
  puts "Cannot authenticate, please check if username/password is valid."
  exit
end

#get cluster information
cookie = conn_resp["set-cookie"]
path = "/serengeti/api/cluster/#{cluster_name}"
headers = {
  'Cookie' => "#{cookie}",
  'Content-Type' => 'application/json'
}
get_cluster_resp = http.get(path, headers)

#logout serengeti as a good practice
path = "/serengeti/j_spring_security_logout"
http.get(path, headers)

unless get_cluster_resp.code == '200'
  puts "Cannot get cluster information, please check if the cluster name is correct."
  exit
end

cluster = JSON.parse(get_cluster_resp.body)

unless cluster['status'] == 'RUNNING'
  puts "Please make sure the cluster is running before you can run this script."
  exit
end
unless File.file? ARGV[1]
  puts "The rack/host description file does not exit."
  exit
end

unless (check_rack_host_desc_format ARGV[1]) == 1
  exit
end

# generate two topology files: rack awareness, and host_as_rack awareness.
rack_file = File.open(ARGV[1], "r")
host_vm_file = File.open("./host_as_rack.topology", "w")
rack_vm_file = File.open("./rack_vm.topology", "w")

host_as_rack_flag = true
rack_file.each_line do |line|
  stripped_line = line.strip
  if stripped_line == "" 
    next
   end
  rack_hosts = stripped_line.split ':' #format: rack_id: host1, host2...
  rack_id = rack_hosts[0]
  hosts = rack_hosts[1]
  hosts_id = hosts.split ','
  hosts_id.each do |host_id|
    nodegroups = cluster['nodeGroups']
    nodegroups.each do |nodegroup|
      instances = nodegroup['instances']
      instances.each do |instance|
        esx_host = instance['hostName']
        instance_ip = instance['ip']
        if esx_host != nil && esx_host != "" && instance_ip != nil && instance_ip != "" && host_id.strip != ""
          if host_as_rack_flag
            host_vm_file.puts "#{instance_ip}\t/#{esx_host}"
          end
          if host_id.strip == esx_host 
            rack_vm_file.puts "#{instance_ip}\t#{rack_id}"
          end
        end
      end
    end
    host_as_rack_flag = false #we only generate host_as_rack mapping file one time
  end
end
