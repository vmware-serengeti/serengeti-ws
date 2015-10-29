/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.manager;

import com.vmware.bdd.exception.BddException;

/**
 * Created by qjin on 11/15/14.
 */
public class ShrinkException extends BddException {
   private static final long serialVersionUID = 1l;

   public ShrinkException() {
   }

   public ShrinkException(Throwable cause, String errorId, Object... detail) {
      super(cause,"CLUSTER_SHRINK", errorId, detail);
   }

   public static ShrinkException DECOMISSION_FAILED(Throwable cause, String errMsg) {
      return new ShrinkException(cause, "DECOMISSION_FAILED", errMsg);
   }

   public static ShrinkException NO_NEED_TO_SHRINK() {
      return new ShrinkException(null, "NO_NEED_TO_SHRINK");
   }

   public static ShrinkException SHRINK_NODE_GROUP_FAILED(Throwable t, String clusterName, String errMsg) {
      return new ShrinkException(t, "SHRINK_NODE_GROUP_FAILED", clusterName, errMsg);
   }

   public static ShrinkException DELETE_VM_FAILED(Throwable cause, String clusterName,
                                                  String nodeName) {
      return new ShrinkException(cause, "DELETE_VM_FAILED", clusterName, nodeName, cause.getMessage());
   }
}
