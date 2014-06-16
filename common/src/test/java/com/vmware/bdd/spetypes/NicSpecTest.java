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
package com.vmware.bdd.spetypes;

import com.vmware.bdd.apitypes.NetConfigInfo;
import com.vmware.bdd.spectypes.NicSpec;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Author: Xiaoding Bian
 * Date: 12/23/13
 * Time: 12:07 PM
 */
public class NicSpecTest {

   @Test
   public void testNetTrafficDef() {
      NicSpec.NetTrafficDefinition def1 = new NicSpec.NetTrafficDefinition(NetConfigInfo.NetTrafficType.MGT_NETWORK, 1);
      NicSpec.NetTrafficDefinition def2 = new NicSpec.NetTrafficDefinition(NetConfigInfo.NetTrafficType.MGT_NETWORK, 1);
      assertTrue(def1.equals(def1));
      assertFalse(def1.equals(null));
      assertTrue(def1.equals(def2));
      assertTrue(def1.hashCode() == def2.hashCode());

      NicSpec nicSpec = new NicSpec();
      nicSpec.addToNetDefs(NetConfigInfo.NetTrafficType.MGT_NETWORK, 2);
      nicSpec.addToNetDefs(NetConfigInfo.NetTrafficType.MGT_NETWORK, 2);
      assertTrue(nicSpec.getNetTrafficDefinitionSet().size() == 1);
   }
}
