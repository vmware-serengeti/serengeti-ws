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
require 'ironfan'
require 'ironfan/monitor'
require 'simple_logger'

module Software
  module Mgmt
    module Thrift
      module ProgressMonitor
        include Ironfan::Monitor

        ERROR_BOOTSTRAP_FAILURE = "Bootstrapping cluster %s failed"

        def log
          @log ||= SimpleLogger.new_logger(STDOUT)
        end

        def getClusterOperationStatus(targetName, withDetail = false)
          if(withDetail)
            clusterDetail = OperationStatusWithDetail.new
          end

          clusterName = fetchClusterName(targetName)
          clusterMain = OperationStatus.new
          clusterMain.total = 0
          clusterMain.success = 0
          clusterMain.failure = 0
          clusterMain.running = 0
          clusterMain.finished = false
          clusterMain.succeed = false
          clusterMain.progress = 0
          clusterMain.error_msg = ''
          if(withDetail)
            clusterData = ClusterData.new
            clusterData.clusterName = clusterName
            groups = Hash.new

            nodes = fetchClusterNodes(targetName)
            nodes.each do |node|
              server = node['provision'] || {}
              # create groups
              facet_name = node['facet_name']
              group = groups[facet_name] || GroupData.new
              group.groupName ||= facet_name
              group.instances ||= []
              serverData = convert2Data(server)
              group.instances << serverData
              groups[facet_name] = group
              # create cluster summary
              clusterMain.success += 1 if serverData.finished and serverData.succeed
              clusterMain.failure += 1 if serverData.finished and !serverData.succeed
              clusterMain.running += 1 if !serverData.finished
              clusterMain.progress += serverData.progress || 0
            end
            clusterMain.total = nodes.length
            clusterMain.progress /= clusterMain.total if clusterMain.total != 0
            clusterMain.finished = (clusterMain.running == 0)
            clusterMain.succeed = (clusterMain.success == clusterMain.total)
            clusterMain.error_msg = ERROR_BOOTSTRAP_FAILURE % clusterName if clusterMain.finished and !clusterMain.succeed

            clusterData.groups = groups
            clusterDetail.clusterData = clusterData
            clusterDetail.operationStatus = clusterMain
          end
          cluster = clusterMain
          if(withDetail)
            cluster = clusterDetail
          end
          cluster
        end

        def convert2Data(server)
          serverData = ServerData.new
          serverData.status = server['status']
          serverData.action = server['action']
          serverData.bootstrapped = server['bootstrapped']
          serverData.created = server['created']
          serverData.deleted = server['deleted']
          serverData.error_code = server['error_code']
          serverData.error_msg = server['error_msg']
          serverData.finished = server['finished']
          serverData.ha = server['ha']
          serverData.hostName = server['hostname']
          serverData.ipAddress = server['ip_address']
          serverData.name = server['name']
          serverData.physicalHost = server['physical_host']
          serverData.progress = server['progress']
          serverData.rack = server['rack']
          serverData.succeed = server['succeed']
          serverData
        end

        def fetchClusterName(targetName)
          return targetName.split('-')[0]
        end

        def fetchClusterNodes(targetName)
          nodes = []
          clusterName = fetchClusterName(targetName)
          # This method will be called about every 6 seconds and it will retrieve all Chef Nodes of this cluster.
          # So use Chef Partial Search API to get the needed Chef Node attributes only. This can reduce server response time.
          # See http://www.opscode.com/blog/2012/10/31/introducing-partial-search-for-opscode-hosted-chef/
          query = Chef::Search::Query.new
          query.partial_search(:node, "cluster_name:#{clusterName}", keys: { name: ['name'], facet_name: ['facet_name'], provision: ['provision'] }) do |n|
            nodes.push(n) if n['name'].start_with?(targetName)
          end
          nodes.sort_by! { |n| n['name'] }
        end
      end
    end
  end
end
