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
package com.vmware.bdd.vmclone.service.intf;

import java.util.List;

import com.vmware.aurora.composition.concurrent.Scheduler.ProgressCallback;
import com.vmware.bdd.clone.spec.VmCreateResult;
import com.vmware.bdd.clone.spec.VmCreateSpec;

/**
 * vm clone service interface
 * 
 * @author tli
 * 
 */
public interface IClusterCloneService {

   /**
    * given the initial source "resource", create copies in "consumers". The max
    * concurrent copy a resource can support is defined by "maxConcurrentCopy".
    * The caller can get the progress through injected progress call back. This
    * will be a block call, return the results until all copy operations are
    * done
    * 
    * @param resource
    * @param maxConcurrentCopy
    * @param consumer
    * @param callback
    * @return
    */
   public List<VmCreateResult<?>> createCopies(VmCreateSpec resource, int maxConcurrentCopy,
         List<VmCreateSpec> consumer, ProgressCallback callback);

}