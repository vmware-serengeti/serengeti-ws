/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.software.mgmt.plugin.model;

import com.vmware.bdd.software.mgmt.plugin.utils.ReflectionUtils;
import org.testng.annotations.Test;

/**
 * Author: Xiaoding Bian
 * Date: 7/29/14
 * Time: 11:55 AM
 */
public class TestReflectionUtils {

   @Test
   public void testGetClass() {
      Class<? extends Object> clazz = ReflectionUtils.getClass(
            "com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint", Object.class);
      ReflectionUtils.newInstance(clazz);
   }
}
