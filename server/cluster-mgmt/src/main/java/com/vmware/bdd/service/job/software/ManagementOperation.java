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
package com.vmware.bdd.service.job.software;

/**
 * @author Jarred Li
 * @version 0.8
 * @since 0.8
 * 
 */
public enum ManagementOperation {

   QUERY,
   CREATE,
   UPDATE,
   START,
   STOP,
   DESTROY,
   CONFIGURE,
   CONFIGURE_HARDWARE,
   ENABLE_OPERATION_FLAG
   
}
