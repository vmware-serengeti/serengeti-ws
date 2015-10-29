#
#   Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
require 'ironfan'
require 'ironfan/monitor'
require 'software_management'
require 'server/progress_monitor'
require 'simple_logger'

module Software
  module Mgmt
    module Thrift
      class SoftwareManagementHandler
        include Ironfan::Monitor
        include ProgressMonitor

        attr_reader :serengetiHome, :configFile
       
        def log
          @log ||= SimpleLogger.new_logger(STDOUT)
        end

        def initialize(serengetiHome, configFile)
          @serengetiHome = serengetiHome
          @configFile = configFile
          Chef::Config.from_file(@configFile)
        end

        def runClusterOperation(clusterOperation)
          option = ""
          name = clusterOperation.targetName

          case clusterOperation.action
          when ClusterAction::QUERY
            action = "show"
            option = "-f #{clusterOperation.specFileName}" 
          when ClusterAction::CREATE, ClusterAction::UPDATE
            action = "create"
            option = "-f #{clusterOperation.specFileName} --yes --bootstrap"
          when ClusterAction::START
            action = "bootstrap"
            option = "-f #{clusterOperation.specFileName} --yes"
          when ClusterAction::STOP
            action = "stop"
            option = "-f #{clusterOperation.specFileName} --yes"
          when ClusterAction::DESTROY
            clusterName = name.split('-')[0]
            clusterFile = "#{@serengetiHome}/tmp/.ironfan-clusters/#{clusterName}.rb"
            if File::exist?(clusterFile)
              action = "kill"
              option = "--no-cloud --yes"
            else
              log.info("cluster #{clusterName} does not exist")
              return 0
            end
          when ClusterAction::CONFIGURE
            action = "bootstrap"
            option = "-f #{clusterOperation.specFileName} --yes"
          else
            log.info("Invalid cluster operation")
            return 1
          end

          Dir.chdir("#{@serengetiHome}")
          knifeCmd = "knife cluster #{action} #{name} #{option} #{clusterOperation.logLevel}"
          log.info("============= Invoking Ironfan Knife CLI =============")
          log.info(knifeCmd)
          system(knifeCmd + " 2>&1 1>>#{@serengetiHome}/logs/ironfan.log")
          exitCode = $?.exitstatus
          # system returns true if the command gives zero exit status, false for
          # non zero exit status. Returns nil if command execution fails.
          log.info("============= Ironfan Knife CLI exited with status code #{exitCode} =============")
          exitCode
        end

        def getOperationStatusWithDetail(targetName)
          log.debug("get operation progress for cluster #{targetName} ...")
          begin
            status = getClusterOperationStatus(targetName, true)
          rescue Exception => e
            log.error("Exception was thrown during querying cluster status from Chef. Error message: #{e.message}")
            stackTrace = e.backtrace.join("\n")
            log.error(" Stack trace: #{stackTrace}")
            error = ClusterOperationException.new("Exception was thrown during querying cluster status from Chef. Error message: #{e.message}")
            raise error
          end
          log.debug("progress: #{status.operationStatus.inspect}")
          status
        end

        def resetNodeProvisionAttribute(targetName)
          begin
            clusterName = fetchClusterName(targetName)
            nodes = []
            query = Chef::Search::Query.new
            query.search(:node, "cluster_name:#{clusterName}") do |n|
              nodes.push(n) if n.name.start_with?(targetName)
            end
            nodes = nodes.sort_by! { |n| n.name }
            nodes.each do |node|
              attrs = get_provision_attrs(node)
              attrs[:finished] = false
              attrs[:succeed] = nil
              attrs[:progress] = 0
              attrs[:action] = ''
              attrs[:error_msg] = ''
              set_provision_attrs(node, attrs)
              node.save
            end
          rescue Exception => e
            log.error("Exception was thrown during reseting Chef node's attribute, error message: #{e.message}")
            stackTrace = e.backtrace.join("\n")
            log.error(" Stack trace: #{stackTrace}")
            error = ClusterOperationException.new("Exception was thrown during reseting Chef node's attribute, error message: #{e.message}")
            raise error
          end
        end
      end
    end
  end
end
