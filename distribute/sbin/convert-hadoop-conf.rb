#!/usr/bin/ruby

require 'rexml/document'
require 'json'

if ARGV.size == 0
  puts "Usage: convert-hadoop-conf.rb hadoop_conf_dir [indent_size]"
  puts "Convert the hadoop xml configuration files into json format."
  exit
end

# configuration
$blacklist = [
  'fs.default.name',
  'dfs.http.address', 'dfs.name.dir', 'dfs.data.dir',
  'mapred.job.tracker', 'mapred.local.dir',
  'HADOOP_HOME', 'HADOOP_COMMON_HOME', 'HADOOP_MAPRED_HOME', 'HADOOP_HDFS_HOME', 'HADOOP_CONF_DIR', 'HADOOP_PID_DIR'
]
xml_files = [ 'core-site.xml', 'hdfs-site.xml', 'mapred-site.xml' ]
prop_files = [ 'hadoop-env.sh' ]
files = [ xml_files, prop_files ].flatten

conf_dir = ARGV[0]
indent = ARGV[1] || 2

$finalized = {}
hadoop_conf = {}
cluster_conf = { 'configuration' => { 'hadoop' => hadoop_conf } }

def add_to_conf(conf, name, value, final)
  return if name.to_s.empty? || value.to_s.empty?
  return if $blacklist.include?(name) || $finalized[name]
  $finalized[name] = true if final
  conf[name] = value
end

# convert
files.each do |f|
  conf = {}
  path = File.join(conf_dir, f)
  if File.exist?(path)
    body = File.read(path)
    if xml_files.include?(f)
      doc = REXML::Document.new(body)
      doc.elements.each('configuration/property') do |e|
        name = e.elements['name'].text
        value = e.elements['value'].text
        final = e.elements['final']
        final = final ? final.text == 'true' : false
        add_to_conf(conf, name, value, final)
      end
    else
      body.each_line do |line|
        line.strip!
        next if line.start_with?('#') || !line.index('=')
        prefix = 'export '
        line = line[prefix.length..-1].strip if line.index(prefix) == 0
        name, value = line.split('=')
        add_to_conf(conf, name, value, false)
      end
    end
  end
  hadoop_conf[f] = conf
end

# print
opts = { :indent => ' ' * indent.to_i }
json = JSON.pretty_generate(cluster_conf, opts)
puts json[1...-1]
puts

