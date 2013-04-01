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

package com.vmware.aurora.interfaces.build;

import java.util.Date;

public interface IBuildInfo {
   public String getBuildType();
   
   public boolean isDevel();
   public boolean isDebug();

   public enum BuildMetadata {
      BRANCH_NAME, BUILD_NUMBER, BUILD_BUILDTYPE, BUILD_RELTYPE, BUILD_TIME, CHANGE_NUMBER, PRODUCT_VERSION
   }

   public String getBuildMetadata(BuildMetadata name);

   public Date getBuildTime();
}
