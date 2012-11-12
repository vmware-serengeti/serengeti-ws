#!/usr/bin/env ruby

require 'optparse'
require 'ostruct'
require 'date'
require 'json'

class ConfigDistro
  DISTRO_ROOT_DIR = '/opt/serengeti/www/distros'
  MANIFEST_PATH = "/opt/serengeti/www/distros/manifest"

  attr_reader :options

  def initialize(arguments)
    @arguments = arguments

    # Set defaults
    @options = OpenStruct.new
    @options.name = nil
    @options.hadoop_package_url = nil
    @options.pig_package_url = nil
    @options.hive_package_url = nil

    @has_error = false
    @distro = {}
  end

  # Parse options, check arguments, then process the command
  def run
    if arguments_valid? and parsed_options?

      process_command

    else
      output_usage
    end

  end

  protected

  def parsed_options?

    # Specify options
    opts = OptionParser.new

    opts.on('-n', '--name DISTRO_NAME') do |name|
      @options.name = name
    end

    opts.on('-a', '--hadoop HADOOP_PACKANGE_package_url') do |package_url|
      @options.hadoop_package_url = package_url
    end

    opts.on('-p', '--pig PIG_PACKANGE_package_url') do |package_url|
      @options.pig_package_url = package_url
    end

    opts.on('-i', '--hive HIVE_PACKANGE_package_url') do |package_url|
      @options.hive_package_url = package_url
    end

    opts.on('-h', '--help') do
      output_help
    end

    opts.parse!(@arguments) rescue return false

    true
  end

  def arguments_valid?
    true if @arguments.length > 0
  end

  def output_help
    output_usage
  end

  def output_usage
      puts "Usage: config-distro.rb [OPTIONS]"
      puts "commands:"
      puts "    config-distro.rb --name distro_name --hadoop hadoop_package_url --pig pig_package_url --hive hive_package_url"
      puts "    config-distro.rb --help"
  end

  # TO DO - do whatever this app does
  def process_command
    if @options.name.nil?
      error_message("Please enter the distro name of the parameter")
      return nil
    else
      @distro_path = File.join("#{DISTRO_ROOT_DIR}", @options.name)
      unless File.exists?(@distro_path)
        result = system("mkdir -p #{@distro_path}")
        error_message("Can not create the folder") and return nil unless result
      end
      @distro["name"] = @options.name
      @distro["version"] = "1.0.1"
    end

    download_tartall(@options.hadoop_package_url, "hadoop") if !@has_error

    download_tartall(@options.pig_package_url, "pig") if !@has_error

    download_tartall(@options.hive_package_url, "hive") if !@has_error

    if !@has_error
      generate_manifest
    end
  end

  def download_tartall(package_url, type)
    if package_url.nil?
      error_message("Please enter the #{type} package url parameter")
    else
      name = package_url.split("/").last
      if File.exists?(File.join(@distro_path, name))
        puts "The #{name} exists, do you overwrite?"
        while true
          puts "Please enter yes or no:"
          overwrite = gets.strip
          case overwrite
          when "yes"
            download(package_url, name, type)
            break
          when "no"
            break
          end
        end
      else
        download(package_url, name, type)
      end
      generate_package_info(name, type)
    end
  end

  def download(package_url, name, type)
    ret = system("wget #{package_url} -O #{File.join(@distro_path, name)}")
    unless ret
      system("rm -f #{File.join(@distro_path, name)}")
      error_message("Download the #{type} tarball has an error")
    else
      if File.size(File.join(@distro_path, name)) == 0
        system("rm -f #{File.join(@distro_path, name)}")
        error_message("Download the #{type} tarball has an error")
      end
    end
  end

  def generate_package_info(name, type)
    case type
    when "hadoop"
      @distro["packages"] = []
      @distro["packages"][0] = {"roles" => ["hadoop_namenode", "hadoop_jobtracker", "hadoop_tasktracker", "hadoop_datanode", "hadoop_client"], "tarball" => File.join(@options.name, name)}
    when "pig"
      @distro["packages"][1] = {"roles" => ["pig"], "tarball" => File.join(@options.name, name)}
    when "hive"
      @distro["packages"][2] = {"roles" => ["hive", "hive_server"], "tarball" => File.join(@options.name, name)}
    end
  end

  def error_message(message)
    puts "errors:"
    puts "    #{message}"
    puts ""
    @has_error = true
  end

  def generate_manifest
    distros = []
    begin
      distros = JSON.parse(File.read(MANIFEST_PATH))
    rescue
      distros << @distro
    end
    distros.each_with_index do |distro, index|
      if distro["name"] == @distro["name"]
        distros[index]["packages"] = @distro["packages"]
      else
        distros << @distro
      end
    end
    File.open(MANIFEST_PATH, "w") do |file|
      file.write(JSON.pretty_generate(distros))
    end
    puts "Generated manifest file"
  end

end

# Create and run the configDistro
configDistro = ConfigDistro.new(ARGV)
configDistro.run
