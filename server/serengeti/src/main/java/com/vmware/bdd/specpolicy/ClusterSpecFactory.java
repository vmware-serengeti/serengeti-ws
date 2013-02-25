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
package com.vmware.bdd.specpolicy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;

import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterType;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;

public class ClusterSpecFactory {
   private static final Logger logger = Logger.getLogger(ClusterSpecFactory.class);

   private static final String HDFS_TEMPLATE_SPEC = "hdfs-template-spec.json";
   private static final String HDFS_MAPRED_TEMPLATE_SPEC = "hdfs-mapred-template-spec.json";
   private static final String HDFS_HBASE_TEMPLATE_SPEC = "hdfs-hbase-template-spec.json";
   private static final String HDFS_MAPRED_MAPR_TEMPLATE_SPEC = "hdfs-mapred-mapr-template-spec.json";

   private static File locateSpecFile(String filename) {
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
               .append(File.separator).append(filename);
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

      AuAssert.check(specFile.exists());

      return specFile;
   }

   /**
    * Load and create cluster from spec file
    * 
    * @return cluster spec
    */
   public static ClusterCreate loadFromFile(File file) throws FileNotFoundException {
      Reader fileReader = new FileReader(file);
      Gson gson = new Gson();
      return gson.fromJson(fileReader, ClusterCreate.class);
   }

   /**
    * Create default cluster spec.
    * 
    * @param type
    *           cluster type
    * @return default cluster spec
    * @throws FileNotFoundException
    */
   public static ClusterCreate createDefaultSpec(ClusterType type, final String vendor)
         throws FileNotFoundException {
      // loading from file each time is slow but fine
      if (vendor.trim().equalsIgnoreCase(Constants.MAPR_VENDOR)) {
         switch (type) {
         case HDFS_MAPRED:
            return loadFromFile(locateSpecFile(HDFS_MAPRED_MAPR_TEMPLATE_SPEC));
         default:
            throw BddException.INVALID_PARAMETER("cluster type", type);
         }
      } else {
         switch (type) {
         case HDFS:
            return loadFromFile(locateSpecFile(HDFS_TEMPLATE_SPEC));
         case HDFS_MAPRED:
            return loadFromFile(locateSpecFile(HDFS_MAPRED_TEMPLATE_SPEC));
         case HDFS_HBASE:
            return loadFromFile(locateSpecFile(HDFS_HBASE_TEMPLATE_SPEC));
         default:
            throw BddException.INVALID_PARAMETER("cluster type", type);
         }
      }
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
   public static ClusterCreate getCustomizedSpec(ClusterCreate spec)
         throws FileNotFoundException {
      if (spec.getType() == null) {
         return spec;
      }

      ClusterCreate newSpec = createDefaultSpec(spec.getType(), spec.getDistroVendor());

      // --name
      if (spec.getName() != null) {
         newSpec.setName(spec.getName());
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
      if(spec.getDistroVersion() != null) {
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

      // --networkName
      if (spec.getNetworkName() != null) {
         newSpec.setNetworkName(spec.getNetworkName());
      }

      // --topology
      if (spec.getTopologyPolicy() != null) {
         newSpec.setTopologyPolicy(spec.getTopologyPolicy());
      }

      return newSpec;
   }
}
