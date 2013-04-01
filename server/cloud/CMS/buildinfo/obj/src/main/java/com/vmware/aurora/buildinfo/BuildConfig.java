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

package com.vmware.aurora.buildinfo;

/*
 * This class defines constants specific to the build configuration which will
 * be rolled into a BuildInfo object.
 *
 * There are separate implementations, at the source level, of BuildConfig.java
 * for each build type; only one will be compiled into the larger project.
 * Because of the way Java classes work, they all need to have the same filename,
 * which means they need to live in separate directories; we use a combination of
 * Maven's module and profile features to select exactly one of them, which works
 * if (and only if) each BuildConfig implementation lives in its own Maven module,
 * and all these Maven modules also have the same artifact name as each other.
 */
public class BuildConfig {
   public enum BuildType {
      BUILDTYPE_OBJ, BUILDTYPE_OPT, BUILDTYPE_BETA, BUILDTYPE_RELEASE
   }

   public static final BuildType buildType = BuildType.BUILDTYPE_OBJ;
   public static final boolean devel = true;
   public static final boolean debug = true;

}
