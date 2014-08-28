/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.manager;

/**
 * internal exception for Software management collector
 * Created By xiaoliangl on 8/29/14.
 */
public class SWMgrCollectorInternalException extends RuntimeException {
   public SWMgrCollectorInternalException(String msg) {
      super(msg);
   }

   public SWMgrCollectorInternalException(Throwable throwable, String msg) {
      super(msg, throwable);
   }
}
