#
#   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0

#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

require 'thrift'
require 'chef'
require 'chef/knife'
require 'chef/knife/cluster_show.rb'
require 'chef/knife/cluster_create.rb'
require 'chef/knife/cluster_start.rb'
require 'chef/knife/cluster_stop.rb'
require 'chef/knife/cluster_kill.rb'
require 'chef/knife/cluster_bootstrap.rb'
require 'ironfan'
require 'ironfan/monitor'
require 'software_management'
require 'server/progress_monitor'
require 'logger'

module Software
  module Mgmt
    module Thrift
      class SoftwareManagementHandler
        include Ironfan::Monitor
	include Ironfan::KnifeCommon
        include ProgressMonitor

        attr_reader :serengetiHome, :configFile
       
        def log
          @log ||= Logger.new(STDOUT)
        end

        def initialize(serengetiHome, configFile)
          @serengetiHome = serengetiHome
          @configFile = configFile
          Chef::Config.from_file(@configFile)
        end

        def runClusterOperation(clusterOperation)
          log.info("start:==============================================================================")
          log.info("cluster action: #{clusterOperation.action}")
          log.info("target name: #{clusterOperation.targetName}")

          Chef::Config[:distro] = 'centos5-vmware'
          optionStr = "-f #{clusterOperation.specFileName} --yes --bootstrap"

          case clusterOperation.action
          when ClusterAction::QUERY
            operation = createQueryOperation
            optionStr = "-f #{clusterOperation.specFileName}"
          when ClusterAction::CREATE
            operation = createCreateOperation
          when ClusterAction::UPDATE
            operation = createUpdateOperation
          when ClusterAction::START
            operation = createStartOperation
          when ClusterAction::STOP
            operation = createStopOperation
            optionStr = "-f #{clusterOperation.specFileName} --yes"
          when ClusterAction::DESTROY
            clusterName = targetName.split('-')[0]
            clusterFile = "#{@serengetiHome}/tmp/.ironfan-clusters/#{clusterName}.rb"
            if File::exist?(clusterFile)
              operation = createDestroyOperation
              optionStr = "-f #{clusterOperation.specFileName} --yes"
            else
              log.info("cluster #{clusterName} does not exist")
              return 0
            end
          when ClusterAction::CONFIGURE
            operation = createConfigureOperation
            optionStr = "-f #{clusterOperation.specFileName} --yes"
          when ClusterAction::CONFIGURE_HARDWARE
            operation = createConfigureHardwareOperation
            optionStr = "-f #{clusterOperation.specFileName} --yes"
          when ClusterAction::ENABLE_CHEF_CLIENT
            operation = createStartOperation
            optionStr = "-f #{clusterOperation.specFileName} --set-chef-client-flag true --yes"
          else
            log.info("invalid operation")
          end

          log.debug("option : #{optionStr}")

          Dir.chdir("#{@serengetiHome}")

          #clear cache in the Ironfan.          
          Ironfan.clear_clusters
          
          operation.class.load_deps
          operation.class.chef_config_dir
          operation.config[:config_file]=@configFile
          operation.config[:verbosity] = 0
          if clusterOperation.logLevel
            if clusterOperation.logLevel == "V"
              operation.config[:verbosity] = 1
            elsif clusterOperation.logLevel == "VV"
              operation.config[:verbosity] = 2
            end
          end
          Chef::Log.level = :warn
          operation.configure_chef
          options = optionStr.split
          operation.parse_options(options)
          operation.name_args = [clusterOperation.targetName]
          exitCode = 0
          begin
            operation.run
          rescue SystemExit => e
            log.info("rescued a exit call")
            exitCode = e.status
          end
          log.info("end:================================================================================")
          exitCode
        end

        def getOperationStatusWithDetail(targetName)
          log.debug("get operation status for target name: #{targetName}")
          status = getClusterOperationStatus(targetName, true)
          log.debug("status: #{status.inspect}")
          status
        end

        def createQueryOperation
          clusterShowObject = Chef::Knife::ClusterShow.new
        end

        def createCreateOperation
          clusterCreateObject = Chef::Knife::ClusterCreate.new
        end

        def createUpdateOperation
       	  clusterCreateObject = Chef::Knife::ClusterCreate.new
        end

        def createStartOperation
          clusterStartObject = Chef::Knife::ClusterStart.new
        end

        def createStopOperation
          clusterStopObject = Chef::Knife::ClusterStop.new
        end

        def createDestroyOperation
          clusterDestroyObject = Chef::Knife::ClusterKill.new
        end

        def createConfigureOperation
          clusterConfigureObject = Chef::Knife::ClusterBootstrap.new
        end

        def createConfigureHardwareOperation
          clusterConfigureObject = Chef::Knife::ClusterConfig.new
        end

      end
    end
  end
end
