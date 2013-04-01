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
package com.vmware.bdd.dal;

import java.util.List;

import com.vmware.bdd.entity.IpBlockEntity;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 * 
 */
public interface IIpBlockDAO extends IBaseDAO<IpBlockEntity> {
   /**
    * Merge adjacent (overlapping) IP blocks if they have the same type.
    * 
    * Side effects:
    * 
    * 1) this call will modify all the objects in the input list, call
    * <code>dup</code> if this is not desirable.
    * 
    * 2) the objects in the returned list still reference the original objects,
    * and the one which are merged will be deleted if this is called in a active
    * Hibernate session.
    * 
    * @param ipBlocks
    *           source blocks
    * @param ignoreOwner
    *           whether ignore owner
    * @param ignoreType
    *           whether ignore block type
    * @param silentWhenOverlap
    *           whether to keep silent or raise error if blocks are overlapped
    * 
    * @return merged list.
    */
   public abstract List<IpBlockEntity> merge(List<IpBlockEntity> ipBlocks,
         boolean ignoreOwner, boolean ignoreType, boolean silentWhenOverlap);

}
