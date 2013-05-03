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
require 'chef/knife/cluster_bootstrap.rb'
require 'ironfan/monitor'
require 'logger'

module Software
  module Mgmt
    module Thrift
      module ProgressMonitor
        include Ironfan::Monitor
	include Ironfan::KnifeCommon
        
        def log
          @log ||= Logger.new(STDOUT)
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

            nodes = []
            query = Chef::Search::Query.new
            query.search(:node, "cluster_name:#{clusterName}") do |n|
              nodes.push(n) if n.name.start_with?(targetName)
            end
            nodes = nodes.sort_by! { |n| n.name }
            nodes.each do |node|
              server = get_provision_attrs(node).to_mash
              # create groups
              group = groups[node.facet_name] || GroupData.new
              group.groupName ||= node.facet_name
              group.instances ||= []
              serverData = convert2Data(server)
              group.instances << serverData
              groups[node.facet_name] = group
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
            clusterMain.error_msg = ERROR_BOOTSTAP_FAIL if clusterMain.finished and !clusterMain.succeed

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
          serverData.status = server[:status]
          serverData.action = server[:action]
          serverData.bootstrapped = server[:bootstrapped]
          serverData.created = server[:created]
          serverData.deleted = server[:deleted]
          serverData.error_code = server[:error_code]
          serverData.error_msg = server[:error_msg]
          serverData.finished = server[:finished]
          serverData.ha = server[:ha]
          serverData.hostName = server[:hostname]
          serverData.ipAddress = server[:ip_address]
          serverData.name = server[:name]
          serverData.physicalHost = server[:physical_host]
          serverData.progress = server[:progress]
          serverData.rack = server[:rack]
          serverData.succeed = server[:succeed]
          serverData
        end

        def getClusterData
          cluster = Mash.new
          cluster[:total] = 0
          cluster[:success] = 0
          cluster[:failure] = 0
          cluster[:running] = 0
          cluster[:finished] = false
          cluster[:succeed] = false
          cluster[:progress] = 0
          cluster[:error_msg] = ''
          cluster[:cluster_data] = Mash.new
          groups = cluster[:cluster_data][:groups] = Mash.new
          nodes = []
          query = Chef::Search::Query.new
          query.search(:node, "cluster_name:#{clusterName}") do |n|
            nodes.push(n) if n.name.start_with?(clusterName)
          end
          nodes = nodes.sort_by! { |n| n.name }
          nodes.each do |node|
            server = get_provision_attrs(node).to_mash
            # create groups
            group = groups[node.facet_name] || Mash.new
            group[:name] ||= node.facet_name
            group[:instances] ||= []
            group[:instances] << server
            groups[node.facet_name] = group
            # create cluster summary
            cluster[:success] += 1 if server[:finished] and server[:succeed]
            cluster[:failure] += 1 if server[:finished] and !server[:succeed]
            cluster[:running] += 1 if !server[:finished]
            cluster[:progress] += server[:progress] || 0
          end
          cluster[:total] = nodes.length
          cluster[:progress] /= cluster[:total] if cluster[:total] != 0
          cluster[:finished] = (cluster[:running] == 0)
          cluster[:succeed] = (cluster[:success] == cluster[:total])
          cluster[:error_msg] = ERROR_BOOTSTAP_FAIL if cluster[:finished] and !cluster[:succeed]
          data = JSON.parse(cluster.to_json) # convert keys from symbol to string
        end

        def fetchClusterName(targetName)
          return targetName.split('-')[0]
        end

      end
    end
  end
end
