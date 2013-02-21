/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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
package com.vmware.bdd.apitypes;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

public class DistroReadTest {

   @Test
   public void testDistroSort(){
      DistroRead distro1 = new DistroRead();
      distro1.setName("distroB");
      DistroRead distro2 = new DistroRead();
      distro2.setName("distroA");
      DistroRead distro3 = new DistroRead();
      distro3.setName("1Distro");
      DistroRead[] distros = new DistroRead[] {distro1, distro2, distro3};
      Arrays.sort(distros);
      assertEquals(distros[0].getName(), "1Distro");
      assertEquals(distros[1].getName(), "distroA");
      assertEquals(distros[2].getName(), "distroB");
   }

}
