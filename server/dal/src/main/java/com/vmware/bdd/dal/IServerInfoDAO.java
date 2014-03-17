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

import com.vmware.bdd.entity.ServerInfoEntity;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
public interface IServerInfoDAO extends IBaseDAO<ServerInfoEntity> {

   List<ServerInfoEntity> findAll();

}
