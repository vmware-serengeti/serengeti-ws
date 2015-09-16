/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.aurora.util;

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class HbaseRegionServerOptsUtil {

   static final Logger logger = Logger.getLogger(HbaseRegionServerOptsUtil.class);

   //Get Hbase_RegionServer_OPTS static string parameter
   private static String getHbaseRegionServerStringParameter() {
      return "-XX:CMSInitiatingOccupancyFraction=80 -XX:+UseConcMarkSweepGC "
            + "-XX:-CMSIncrementalMode -XX:+UseCMSInitiatingOccupancyOnly -XX:+CMSClassUnloadingEnabled "
            + "-XX:+OptimizeStringConcat -XX:+UseStringCache -XX:+AlwaysPreTouch ";
   }

   //Get Hbase_RegionServer_OPTS string parameter for Ironfan cluster
   public static String getIronfanHbaseRegionServerStringParameter(long nodeMemMhz, int groupRoleNums) {
      long hbaseHeapMhz = getHeapSizeMhz(nodeMemMhz, groupRoleNums);
      String parameter = "-Xmx" + hbaseHeapMhz + "m"
            + " -Xms" + hbaseHeapMhz + "m"
            + " -Xmn" + (long)(0.33 * hbaseHeapMhz) + "m "
            + getHbaseRegionServerStringParameter();
      return parameter;
   }

   //Get Hbase_RegionServer_OPTS string parameter for CM cluster
   public static String getCMHbaseRegionServerStringParameter(long nodeMemMhz, int groupRoleNums) {
      long hbaseHeapByte = getHeapSizeByte(nodeMemMhz, groupRoleNums);
      String parameter = "-Xmx" + hbaseHeapByte + " -Xms" + hbaseHeapByte
            + " -Xmn" + (long)(0.33 * hbaseHeapByte) + " "
            + getHbaseRegionServerStringParameter();
      return parameter;
   }

   //Get Hbase_RegionServer_OPTS string parameter for Ambari cluster
   public static String getAmbariHbaseRegionServerStringParameter() {
      String parameter = "export HBASE_REGIONSERVER_OPTS=\"$HBASE_REGIONSERVER_OPTS "
            + getHbaseRegionServerStringParameter()
            + "\"\n";
      return parameter;
   }

   //Caculate the hbase region server java heap size in Mhz
   public static long getHeapSizeMhz(long nodeMemMhz, int groupRoleNums) {
      logger.info("in getHeapSizeMhz");
      long osMemMhz = 1024;
      long hbaseHeapMhz = nodeMemMhz - 1024*(groupRoleNums - 1) - osMemMhz;
      hbaseHeapMhz = (hbaseHeapMhz > 1024) ? hbaseHeapMhz : 1024;;
      logger.info("in getHeapSizeMhz"+hbaseHeapMhz);
      return  hbaseHeapMhz;
   }

   //Caculate the hbase region server java heap size in byte
   public static long getHeapSizeByte(long nodeMemMhz, int groupRoleNums) {
      return getHeapSizeMhz(nodeMemMhz, groupRoleNums)*1024*1024;
   }

}
