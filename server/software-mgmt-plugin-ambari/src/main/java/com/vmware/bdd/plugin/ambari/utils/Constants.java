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
package com.vmware.bdd.plugin.ambari.utils;

public interface Constants {

   public static final String AMBARI_PLUGIN_NAME = "Ambari";
   public static final String HEALTH_STATUS = "RUNNING";
   public static final String AMBARI_SSH_USER = "serengeti";
   public static final String AMBARI_SERVER_VERSION_1_6_0 = "1.6.0";
   public static final String VERSION_UNBOUNDED = "-1";

   public static final String CONFIG_HDFS_SITE = "hdfs-site";
   public static final String CONFIG_YARN_SITE = "yarn-site";
   public static final String CONFIG_MAPRED_SITE = "mapred-site";

   public static final String CONFIG_DFS_NAMENODE_NAME_DIR = "dfs.namenode.name.dir";
   public static final String CONFIG_DFS_NAME_DIR = "dfs.name.dir";
   public static final String CONFIG_DFS_DATANODE_DATA_DIR = "dfs.datanode.data.dir";
   public static final String CONFIG_DFS_DATA_DIR = "dfs.data.dir";
   public static final String CONFIG_DFS_NAMENODE_CHECKPOINT_DIR = "dfs.namenode.checkpoint.dir";
   public static final String CONFIG_DFS_CHECKPOINT_DIR = "dfs.checkpoint.dir";
   public static final String CONFIG_YARN_NODEMANAGER_LOCAL_DIRS = "yarn.nodemanager.local-dirs";
   public static final String CONFIG_JOURNALNODE_EDITS_DIR = "dfs.journalnode.edits.dir";
   public static final String CONFIG_MAPRED_LOCAL_DIR = "mapred.local.dir";
   public static final String CONFIG_LEVELDB_TIMELINE_STORE_PATH = "yarn.timeline-service.leveldb-timeline-store.path";

   public static final String HOST_BOOTSTRAP_FAILED = "FAILED";
   public static final String HOST_BOOTSTRAP_MSG = "Failed to bootstrap host";
}
