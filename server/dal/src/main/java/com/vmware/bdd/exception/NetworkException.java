/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.exception;

import java.util.List;

import com.vmware.bdd.entity.IpBlockEntity;

public class NetworkException extends BddException {
   private static final long serialVersionUID = 1L;

   public NetworkException() {
   }

   public NetworkException(Throwable cause, String errorId, Object... detail) {
      super(cause, "NETWORK", errorId, detail);
   }

   public static NetworkException NETWORK_IN_USE(List<String> clusterNames) {
      return new NetworkException(null, "NETWORK_IN_USE", clusterNames);
   }

   public static NetworkException IP_ADDR_IN_USE() {
      return new NetworkException(null, "IP_ADDR_IN_USE");
   }

   public static NetworkException OUT_OF_IP_ADDR() {
      return new NetworkException(null, "OUT_OF_IP_ADDR");
   }

   public static NetworkException OVERLAPPED_IP_BLOCKS(IpBlockEntity prev,
         IpBlockEntity next) {
      return new NetworkException(null, "OVERLAPPED_IP_BLOCKS", prev.toString(),
            next.toString());
   }

   public static NetworkException IP_OUT_OF_RANGE(String addr) {
      return new NetworkException(null, "IP_OUT_OF_RANGE", addr);
   }

   public static NetworkException IP_BLOCK_CONTAINS_GATEWAY(IpBlockEntity blk) {
      return new NetworkException(null, "IP_BLOCK_CONTAINS_GATEWAY",
            blk.getBeginAddress(), blk.getEndAddress());
   }
}
