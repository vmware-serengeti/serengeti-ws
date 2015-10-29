/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.clouderamgr.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 7/3/14
 * Time: 10:37 AM
 */
public interface Constants {
   public static final String CDH_REPO_PREFIX = "CDH";
   public static final String CDH_DISTRO_VENDOR = "CDH";
   public static final String CDH_PLUGIN_NAME = "ClouderaManager";
   public static final String CMS_NAME_TOKEN_DELIM = "_";
   public static final String VERSION_UNBOUNDED = "-1";

   public static final String CONFIG_DFS_NAME_DIR_LIST = "dfs_name_dir_list";
   public static final String CONFIG_DFS_DATA_DIR_LIST = "dfs_data_dir_list";
   public static final String CONFIG_FS_CHECKPOINT_DIR_LIST = "fs_checkpoint_dir_list";
   public static final String CONFIG_NM_LOCAL_DIRS = "yarn_nodemanager_local_dirs";
   public static final String CONFIG_MAPRED_JT_LOCAL_DIR_LIST = "jobtracker_mapred_local_dir_list";
   public static final String CONFIG_MAPRED_TT_LOCAL_DIR_LIST = "tasktracker_mapred_local_dir_list";
   public static final String CONFIG_HBASE_REGIONSERVER_JAVA_HEAPSIZE = "hbase_regionserver_java_heapsize";
   public static final String CONFIG_HBASE_REGIONSERVER_OPTS = "hbase_regionserver_java_opts";

   // HA configs
   public static final String CONFIG_DFS_JOURNALNODE_EDITS_DIR = "dfs_journalnode_edits_dir";
   public static final String CONFIG_AUTO_FAILOVER_ENABLED = "autofailover_enabled";
   public static final String CONFIG_DFS_FEDERATION_NAMESERVICE = "dfs_federation_namenode_nameservice";
   public static final String CONFIG_DFS_NAMENODE_QUORUM_JOURNAL_NAME = "dfs_namenode_quorum_journal_name";
   public static final String CONFIG_DFS_NAMESERVICE_MOUNTPOINTS = "nameservice_mountpoints";
   public static final String CONFIG_SQOOP_METASTORE_DATA_DIR = "sqoop_metastore_data_dir";
   public static final String CONFIG_ZOOKEEPER_SERVER_ID = "serverId";
   public static final String CONFIG_HUE__WEBHDFS = "hue_webhdfs";
   public static final String ROLE_CONFIG_GROUP_UPDATE_NOTES = "Update Base Role Config Group By VMware Big Data Extention";

   public static final Map<Integer, String> API_VERSION_SINCE_OF_CM_VERSION = new HashMap<Integer, String>(){
      {
         put(7, "5.1");
         put(6, "5.0");
         put(5, "4.7");
         put(4, "4.6");
         put(3, "4.5");
         put(2, "4.1");
         put(1, "4.0");
      }
   };
}
