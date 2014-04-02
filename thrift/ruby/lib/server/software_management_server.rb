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

require 'server/software_management_handler'
require 'simple_logger'

module Software
  module Mgmt
    module Thrift
      class SoftwareManagementServer
        def log
          @log ||= SimpleLogger.new_logger(STDOUT)
        end

        def run(serengetiHome,configFile,listenHost,listenPort)
          handler = Software::Mgmt::Thrift::SoftwareManagementHandler.new(serengetiHome, configFile)
          processor = Software::Mgmt::Thrift::SoftwareManagement::Processor.new(handler)
          transport = ::Thrift::ServerSocket.new(listenHost,listenPort)
          transportFactory = ::Thrift::BufferedTransportFactory.new()
          server = ::Thrift::ThreadPoolServer.new(processor,transport,transportFactory)

          log.info("Starting software management server on #{listenPort}...")
          server.serve()
        end
      end
    end
  end
end
