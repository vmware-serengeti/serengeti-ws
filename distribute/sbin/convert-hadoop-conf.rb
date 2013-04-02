#!/usr/bin/ruby

require 'rubygems'
require 'rexml/document'
require 'json'
require 'optparse'
require 'ostruct'

def main
  options = parse_arg(ARGV)
  conf_dirs = ARGV
  init_blacklist(options.blacklist)
  init_conf_files()
  $cluster_conf = {'configuration' => {}}
  $finalized = {}
  prop_files = [ 'hadoop-env.sh', 'hbase-env.sh', 'java.env', 'log4j.properties', 'hbase-env-regionserver.sh' ]
  txt_files = [ 'fair-scheduler.xml' ]
  convert(conf_dirs, prop_files, txt_files)
  print(options)
end

def print(options)
  opts = { :indent => ' ' * options.indent.to_i }
  json = JSON.pretty_generate($cluster_conf, opts)
  puts json[1...-1]
  puts
end

def add_to_conf(conf, name, value, final, entry, file)
  return if name.to_s.empty? || value.to_s.empty?
  return if blacklist_contains?(entry, file, name) || $finalized[name]
  $finalized[name] = true if final
  conf[name] = value
end

def blacklist_contains?(entry, file, key)
  $blacklist.each do |item|
    if item.has_key?(entry) && item[entry].has_key?(file) && item[entry][file].include?(key)
      return true
    end
  end
  return false
end

def convert(conf_dirs, prop_files, txt_files)
  conf_dirs.each do |dir|
    if File.exist?(File.join(dir, 'core-site.xml'))
      entry = 'hadoop'
    elsif File.exist?(File.join(dir, 'hbase-site.xml'))
      entry = 'hbase'
    elsif File.exist?(File.join(dir, 'zoo.cfg'))
      entry = 'zookeeper'
    else
      next
    end
    $configurable_files[entry].each do |f|
      conf = {}
      path = File.join(dir, f)
      if File.exist?(path)
        body = File.read(path)
        if prop_files.include?(f)
          body.each_line do |line|
            line.strip!
            next if line.start_with?('#') || !line.index('=')
            prefix = 'export '
            line = line[prefix.length..-1].strip if line.index(prefix) == 0
            name, value = line.split('=')
            add_to_conf(conf, name, value, false, entry, f)
          end
        elsif txt_files.include?(f)
          add_to_conf(conf, 'text', body.to_s, false, entry, f)
        else
          doc = REXML::Document.new(body)
          doc.elements.each('configuration/property') do |e|
            name = e.elements['name'].text
            value = e.elements['value'].text
            final = e.elements['final']
            final = final ? final.text == 'true' : false
            add_to_conf(conf, name, value, final, entry, f)
          end
      end
      if !$cluster_conf['configuration'].has_key?(entry)
        $cluster_conf['configuration'][entry] = {}
      end
      $cluster_conf['configuration'][entry][f] = conf
      end
    end
  end
end

def parse_arg(args)
  options = OpenStruct.new
  options.blacklist = ""
  options.indent = 2
  options.verbose = false
  opts = OptionParser.new do |opts|
    opts.banner = "Convert hadoop xml configuration files into json format.\nUsage: convert-hadoop-conf.rb [options] dir1 dir2 ..."
    opts.separator ""
    opts.separator "Specific Options:"
    opts.on("-i", "--indent [IDENT_NUM]", Integer, "the indent number") do |indent|
      options.indent = indent
    end
    opts.on("-f", "--file [BLACK_LIST]", "the blacklist file") do |filename|
      options.blacklist = filename || ''
    end
    opts.on('-h', "--help", 'Display this screen') do
      puts opts
      exit
    end
  end
  opts.parse!(args)
  options
end

def init_blacklist(filename)
  if filename != '' && File.exist?(filename)
    file = File.new(filename, "r")
    content = ""
    file.each {|line| content << line}
    $blacklist = JSON.parse(content)
  else 
    $blacklist = JSON.parse(%{
    [
      {
        "hadoop": {
          "core-site.xml": [
            "net.topology.impl",
            "net.topology.nodegroup.aware",
            "dfs.block.replicator.classname"
          ],
          "hdfs-site.xml": [
            "dfs.http.address",
            "dfs.name.dir",
            "dfs.data.dir",
            "topology.script.file.name"
          ],
          "mapred-site.xml": [
            "mapred.job.tracker",
            "mapred.local.dir",
            "mapred.task.cache.levels",
            "mapred.jobtracker.jobSchedulable",
            "mapred.jobtracker.nodegroup.awareness"
          ],
          "hadoop-env.sh": [
            "HADOOP_HOME",
            "HADOOP_COMMON_HOME",
            "HADOOP_MAPRED_HOME",
            "HADOOP_HDFS_HOME",
            "HADOOP_CONF_DIR",
            "HADOOP_PID_DIR"
          ],
          "log4j.properties": []
        }
      },
      {
        "zookeeper": {
          "java.env": [],
          "log4j.properties": []
        }
      },
      {
        "hbase": {
          "hbase-site.xml": [
            "hbase.cluster.distributed",
            "hbase.zookeeper.quorum"
          ],
          "hbase-env.sh": [
            "HBASE_CONF_DIR",
            "HBASE_LOG_DIR",
            "HBASE_PID_DIR",
            "HBASE_MANAGES_ZK",
            "HBASE_IDENT_STRING"
          ],
          "log4j.properties": []
        }
      }
    ]
    })
  end
end

def init_conf_files
  $configurable_files = JSON.parse(%{
  {
    "hadoop": [
      "core-site.xml",
      "hdfs-site.xml",
      "mapred-site.xml",
      "yarn-site.xml",
      "fair-scheduler.xml",
      "capacity-scheduler.xml",
      "mapred-queue-acls.xml",
      "hadoop-env.sh",
      "log4j.properties"
    ],
    "zookeeper": [
      "java.env",
      "log4j.properties"
    ],
    "hbase": [
      "hbase-site.xml",
      "hbase-env.sh",
      "hbase-env-regionserver.sh",
      "hbase-policy.xml",
      "log4j.properties"
    ]
  }
  })
end

main()
