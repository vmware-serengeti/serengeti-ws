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
package com.vmware.bdd.specpolicy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterType;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.Version;

public class ClusterSpecFactory {
   private enum MAPREDUCE_VERSION {
      V1, V2
   }

   private enum HDP_VERSION {
      V1, V2_0, V2_1, V2_2, UNKNOWN
   }
   private static final Logger logger = Logger
         .getLogger(ClusterSpecFactory.class);

   private static final String HDFS_TEMPLATE_SPEC = "hdfs-template-spec.json";
   private static final String HDFS_MAPRED_TEMPLATE_SPEC =
         "hdfs-mapred-template-spec.json";
   private static final String HDFS_YARN_TEMPLATE_SPEC =
         "hdfs-yarn-template-spec.json";
   private static final String HDFS_HBASE_TEMPLATE_SPEC =
         "hdfs-hbase-template-spec.json";
   private static final String HDFS_MAPRED_MAPR_TEMPLATE_SPEC =
         "hdfs-mapred-mapr-template-spec.json";
   private static final String HDFS_HBASE_MAPR_TEMPLATE_SPEC =
         "hdfs-hbase-mapr-template-spec.json";
   private static final String HDFS_GPHD_TEMPLATE_SPEC =
         "hdfs-gphd-template-spec.json";
   private static final String HDFS_MAPRED_GPHD_TEMPLATE_SPEC =
         "hdfs-mapred-gphd-template-spec.json";
   private static final String HDFS_HBASE_GPHD_TEMPLATE_SPEC =
         "hdfs-hbase-gphd-template-spec.json";
   private static final String CM_HDFS_MAPRED_TEMPLATE_SPEC =
         "cm-hdfs-mapred-template-spec.json";
   private static final String CM_HDFS_YARN_TEMPLATE_SPEC =
         "cm-hdfs-yarn-template-spec.json";
   private static final String CM_HBASE_TEMPLATE_SPEC =
         "cm-hbase-template-spec.json";
   private static final String AM_HDFS_V1_TEMPLATE_SPEC =
         "am-hdfs-v1-template-spec.json";
   private static final String AM_HDFS_V2_TEMPLATE_SPEC =
         "am-hdfs-v2-template-spec.json";
   private static final String AM_HDFS_MAPRED_TEMPLATE_SPEC =
         "am-hdfs-mapred-template-spec.json";
   private static final String AM_HDP_2_0_HDFS_YARN_TEMPLATE_SPEC =
         "am-hdp-2-0-hdfs-yarn-template-spec.json";
   private static final String AM_HDP_2_1_HDFS_YARN_TEMPLATE_SPEC =
         "am-hdp-2-1-hdfs-yarn-template-spec.json";
   private static final String AM_HDP_2_2_HDFS_YARN_TEMPLATE_SPEC =
         "am-hdp-2-2-hdfs-yarn-template-spec.json";
   private static final String AM_HDP_CUSTOMIZED_HDFS_YARN_TEMPLATE_SPEC =
         "am-hdp-customized-hdfs-yarn-template-spec.json";
   private static final String AM_HDP_V1_HBASE_TEMPLATE_SPEC =
         "am-hdp-1-hbase-template-spec.json";
   private static final String AM_HDP_2_0_HBASE_TEMPLATE_SPEC =
         "am-hdp-2-0-hbase-template-spec.json";
   private static final String AM_HDP_2_1_HBASE_TEMPLATE_SPEC =
         "am-hdp-2-1-hbase-template-spec.json";
   private static final String AM_HDP_2_2_HBASE_TEMPLATE_SPEC =
         "am-hdp-2-2-hbase-template-spec.json";
   private static final String AM_HDP_CUSTOMIZED_HBASE_TEMPLATE_SPEC =
         "am-hdp-customized-hbase-template-spec.json";
   private static final String AM_HDFS_PURE_HBASE_TEMPLATE_SPEC =
         "am-hdfs-pure-hbase-template-spec.json";

   private static File locateSpecFile(String filename, String appManagerType) {
      // try to locate file directly
      File specFile = new File(filename);
      if (specFile.exists()) {
         return specFile;
      }

      // search ${serengeti.home.dir}/conf directory
      String homeDir = System.getProperties().getProperty("serengeti.home.dir");
      if (homeDir != null && !homeDir.trim().isEmpty()) {
         StringBuilder builder = new StringBuilder();
         builder.append(homeDir).append(File.separator).append("conf")
               .append(File.separator).append(appManagerType).append(File.separator).append("spec-templates").append(File.separator).append(filename);
         specFile = new File(builder.toString());

         if (!specFile.exists()) {
            logger.warn("template cluster file does not exist: " + builder);
         } else {
            return specFile;
         }
      }

      // search in class paths
      URL filePath = ConfigurationUtils.locate(filename);
      if (filePath != null) {
         specFile = ConfigurationUtils.fileFromURL(filePath);
      }

      AuAssert.check(specFile.exists(), "spec file not found: " + specFile.getAbsolutePath());

      return specFile;
   }

   /**
    * Load and create cluster from spec file
    * 
    * @return cluster spec
    */
   public static ClusterCreate loadFromFile(File file)
         throws FileNotFoundException {
      Reader fileReader = null;
      try {
         fileReader = new FileReader(file);
         Gson gson = new Gson();
         return gson.fromJson(fileReader, ClusterCreate.class);
      } finally {
         if (fileReader != null) {
            try {
               fileReader.close();
            } catch (IOException e) {
               logger.error("Failed to release buffer: " + e.getMessage());
            }
         }
      }
   }

   /**
    * Create default cluster spec.
    * 
    * @param type
    *           cluster type
    * @return default cluster spec
    * @throws FileNotFoundException
    */
   public static ClusterCreate createDefaultSpec(ClusterType type,
         String vendor, String distroVersion, String appManagerType) throws FileNotFoundException {
      // loading from file each time is slow but fine
      if (vendor.trim().equalsIgnoreCase(Constants.MAPR_VENDOR)) {
         switch (type) {
         case HDFS_MAPRED:
            return loadFromFile(locateSpecFile(HDFS_MAPRED_MAPR_TEMPLATE_SPEC, appManagerType));
         case HDFS_HBASE:
            return loadFromFile(locateSpecFile(HDFS_HBASE_MAPR_TEMPLATE_SPEC, appManagerType));
         default:
            throw BddException.INVALID_PARAMETER("cluster type", type);
         }
      } else if (vendor.trim().equalsIgnoreCase(Constants.GPHD_VENDOR)) {
         switch (type) {
         case HDFS:
            return loadFromFile(locateSpecFile(HDFS_GPHD_TEMPLATE_SPEC, appManagerType));
         case HDFS_MAPRED:
            return loadFromFile(locateSpecFile(HDFS_MAPRED_GPHD_TEMPLATE_SPEC, appManagerType));
         case HDFS_HBASE:
            return loadFromFile(locateSpecFile(HDFS_HBASE_GPHD_TEMPLATE_SPEC, appManagerType));
         default:
            throw BddException.INVALID_PARAMETER("cluster type", type);
         }
      } else if (Constants.AMBARI_PLUGIN_TYPE.equals(appManagerType)
            && vendor.trim().equalsIgnoreCase(Constants.HDP_VENDOR)) {
         MAPREDUCE_VERSION mr = getDefaultMapReduceVersion(vendor, distroVersion);
         HDP_VERSION hdpVersion = getDefaultHdfsVersion(vendor, distroVersion);
         if (type == null) {
            if (mr == MAPREDUCE_VERSION.V1) {
               return loadFromFile(locateSpecFile(AM_HDFS_MAPRED_TEMPLATE_SPEC, appManagerType));
            } else {
               if (hdpVersion == HDP_VERSION.V2_0) {
                  return loadFromFile(locateSpecFile(AM_HDP_2_0_HDFS_YARN_TEMPLATE_SPEC, appManagerType));
               } else if (hdpVersion == HDP_VERSION.V2_1) {
                  return loadFromFile(locateSpecFile(AM_HDP_2_1_HDFS_YARN_TEMPLATE_SPEC, appManagerType));
               } else if (hdpVersion == HDP_VERSION.V2_2) {
                  return loadFromFile(locateSpecFile(AM_HDP_2_2_HDFS_YARN_TEMPLATE_SPEC, appManagerType));
               } else {
                  return loadFromFile(locateSpecFile(AM_HDP_CUSTOMIZED_HDFS_YARN_TEMPLATE_SPEC, appManagerType));
               }
            }
         }
         switch (type) {
         case HDFS:
            if (hdpVersion == HDP_VERSION.V1) {
               return loadFromFile(locateSpecFile(AM_HDFS_V1_TEMPLATE_SPEC, appManagerType));
            } else {
               return loadFromFile(locateSpecFile(AM_HDFS_V2_TEMPLATE_SPEC, appManagerType));
            }
         case HDFS_MAPRED:
            if (mr == MAPREDUCE_VERSION.V1) {
               return loadFromFile(locateSpecFile(AM_HDFS_MAPRED_TEMPLATE_SPEC, appManagerType));
            } else {
               if (hdpVersion == HDP_VERSION.V2_0) {
                  return loadFromFile(locateSpecFile(AM_HDP_2_0_HDFS_YARN_TEMPLATE_SPEC, appManagerType));
               } else if (hdpVersion == HDP_VERSION.V2_1) {
                  return loadFromFile(locateSpecFile(AM_HDP_2_1_HDFS_YARN_TEMPLATE_SPEC, appManagerType));
               } else if (hdpVersion == HDP_VERSION.V2_2) {
                  return loadFromFile(locateSpecFile(AM_HDP_2_2_HDFS_YARN_TEMPLATE_SPEC, appManagerType));
               } else {
                  return loadFromFile(locateSpecFile(AM_HDP_CUSTOMIZED_HDFS_YARN_TEMPLATE_SPEC, appManagerType));
               }
            }
         case HDFS_HBASE:
            if (Configuration.getBoolean(Constants.AMBARI_HBASE_DEPEND_ON_MAPREDUCE)) {
               if (hdpVersion == HDP_VERSION.V1) {
                  return loadFromFile(locateSpecFile(AM_HDP_V1_HBASE_TEMPLATE_SPEC, appManagerType));
               } else {
                  if (hdpVersion == HDP_VERSION.V2_0) {
                     return loadFromFile(locateSpecFile(AM_HDP_2_0_HBASE_TEMPLATE_SPEC, appManagerType));
                  } else if (hdpVersion == HDP_VERSION.V2_1) {
                     return loadFromFile(locateSpecFile(AM_HDP_2_1_HBASE_TEMPLATE_SPEC, appManagerType));
                  } else if (hdpVersion == HDP_VERSION.V2_2){
                     return loadFromFile(locateSpecFile(AM_HDP_2_2_HBASE_TEMPLATE_SPEC, appManagerType));
                  } else {
                     return loadFromFile(locateSpecFile(AM_HDP_CUSTOMIZED_HBASE_TEMPLATE_SPEC, appManagerType));
                  }
               }
            } else {
               return loadFromFile(locateSpecFile(AM_HDFS_PURE_HBASE_TEMPLATE_SPEC, appManagerType));
            }
         default:
            throw BddException.INVALID_PARAMETER("cluster type", type);
         }
      } else {
         MAPREDUCE_VERSION mr =
               getDefaultMapReduceVersion(vendor, distroVersion);
         if (Constants.CLOUDERA_MANAGER_PLUGIN_TYPE.equals(appManagerType)) {
            if (type.equals(ClusterType.HDFS_HBASE)) {
               return loadFromFile(locateSpecFile(CM_HBASE_TEMPLATE_SPEC, appManagerType));
            }
            if (mr == MAPREDUCE_VERSION.V1) {
               return loadFromFile(locateSpecFile(CM_HDFS_MAPRED_TEMPLATE_SPEC, appManagerType));
            } else {
               return loadFromFile(locateSpecFile(CM_HDFS_YARN_TEMPLATE_SPEC, appManagerType));
            }
         }
         switch (type) {
         case HDFS:
            return loadFromFile(locateSpecFile(HDFS_TEMPLATE_SPEC, appManagerType));
         case HDFS_MAPRED:
            if (mr == MAPREDUCE_VERSION.V1) {
               return loadFromFile(locateSpecFile(HDFS_MAPRED_TEMPLATE_SPEC, appManagerType));
            } else {
               return loadFromFile(locateSpecFile(HDFS_YARN_TEMPLATE_SPEC, appManagerType));
            }
         case HDFS_HBASE:
            return loadFromFile(locateSpecFile(HDFS_HBASE_TEMPLATE_SPEC, appManagerType));
         default:
            throw BddException.INVALID_PARAMETER("cluster type", type);
         }
      }
   }

   private static MAPREDUCE_VERSION getDefaultMapReduceVersion(String vendor,
         String distroVersion) {
      if (vendor.trim().equalsIgnoreCase(Constants.DEFAULT_VENDOR)) {
         return MAPREDUCE_VERSION.V2;
      }

      if (vendor.trim().equalsIgnoreCase(Constants.APACHE_VENDOR)) {
         return MAPREDUCE_VERSION.V1;
      }

      if (vendor.trim().equalsIgnoreCase(Constants.BIGTOP_VENDOR)) {
         return MAPREDUCE_VERSION.V2;
      }

      if (vendor.trim().equalsIgnoreCase(Constants.CDH_VENDOR)) {
         if (distroVersion.startsWith("5")) {
            return MAPREDUCE_VERSION.V2;
         } else {
            return MAPREDUCE_VERSION.V1;
         }
      }

      if (vendor.trim().equalsIgnoreCase(Constants.HDP_VENDOR)) {
         if (distroVersion.startsWith("2")) {
            return MAPREDUCE_VERSION.V2;
         } else {
            return MAPREDUCE_VERSION.V1;
         }
      }

      if (vendor.trim().equalsIgnoreCase(Constants.INTEL_VENDOR)) {
         if (distroVersion.startsWith("3")) {
            return MAPREDUCE_VERSION.V2;
         } else {
            return MAPREDUCE_VERSION.V1;
         }
      }

      if (vendor.trim().equalsIgnoreCase(Constants.PHD_VENDOR)) {
         return MAPREDUCE_VERSION.V2;
      }

      if (vendor.trim().equalsIgnoreCase(Constants.MAPR_VENDOR)) {
         return MAPREDUCE_VERSION.V1;
      }

      if (vendor.trim().equalsIgnoreCase(Constants.GPHD_VENDOR)) {
         return MAPREDUCE_VERSION.V1;
      }

      logger.error("Unknown distro vendor, return default mapreduce version 2");
      return MAPREDUCE_VERSION.V2;
   }

   private static HDP_VERSION getDefaultHdfsVersion(String vendor,
         String distroVersion) {

      if (vendor.trim().equalsIgnoreCase(Constants.HDP_VENDOR)) {
         if (Version.compare(distroVersion, "1.0") >= 0 && Version.compare(distroVersion, "2.0") < 0) {
            return HDP_VERSION.V1;
         } else if (Version.compare(distroVersion, "2.0") >= 0 && Version.compare(distroVersion, "2.1") < 0) {
            return HDP_VERSION.V2_0;
         } else if (Version.compare(distroVersion, "2.1") >= 0 && Version.compare(distroVersion, "2.2") < 0) {
            return HDP_VERSION.V2_1;
         } else if (Version.compare(distroVersion, "2.2") >= 0 && Version.compare(distroVersion, "2.3") < 0) {
            return HDP_VERSION.V2_2;
         } else {
            return HDP_VERSION.UNKNOWN;
         }
      }

      logger.error("Unknown distro HDP version");
      return HDP_VERSION.UNKNOWN;
   }

   /**
    * There are two approach to create a cluster: 1) specify a cluster type and
    * optionally overwriting the parameters 2) specify a customized spec with
    * cluster type not specified
    * 
    * @param spec
    *           spec with customized field
    * @return customized cluster spec
    * @throws FileNotFoundException
    */
   public static ClusterCreate getCustomizedSpec(ClusterCreate spec, String appManagerType)
         throws FileNotFoundException {
      if ((spec.getType() == null)
            || (spec.getType() != null && spec.isSpecFile())) {
         return spec;
      }

      ClusterCreate newSpec =
            createDefaultSpec(spec.getType(), spec.getDistroVendor(), spec.getDistroVersion(), appManagerType);

      // --name
      if (spec.getName() != null) {
         newSpec.setName(spec.getName());
      }

      //--password
      newSpec.setPassword(spec.getPassword());

      // --appManager
      if(!CommonUtil.isBlank(spec.getAppManager())) {
         newSpec.setAppManager(spec.getAppManager());
      }

      // --locaRepoURL
      if(!CommonUtil.isBlank(spec.getLocalRepoURL())) {
         newSpec.setLocalRepoURL(spec.getLocalRepoURL());
      }

      // --distro
      if (spec.getDistro() != null) {
         newSpec.setDistro(spec.getDistro());
      }

      //vendor
      if (spec.getDistroVendor() != null) {
         newSpec.setDistroVendor(spec.getDistroVendor());
      }

      //version
      if (spec.getDistroVersion() != null) {
         newSpec.setDistroVersion(spec.getDistroVersion());
      }

      // --dsNames
      if (spec.getDsNames() != null) {
         newSpec.setDsNames(spec.getDsNames());
      }

      // --rpNames
      if (spec.getRpNames() != null) {
         newSpec.setRpNames(spec.getRpNames());
      }

      // --networkConfig
      if (spec.getNetworkConfig() != null) {
         newSpec.setNetworkConfig(spec.getNetworkConfig());
      }

      // --topology
      if (spec.getTopologyPolicy() != null) {
         newSpec.setTopologyPolicy(spec.getTopologyPolicy());
      }

      if(MapUtils.isNotEmpty(spec.getInfrastructure_config())) {
         newSpec.setInfrastructure_config(spec.getInfrastructure_config());
      }

      return newSpec;
   }
}
