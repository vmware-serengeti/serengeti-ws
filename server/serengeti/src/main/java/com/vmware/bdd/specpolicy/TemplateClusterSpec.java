/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroup.InstanceType;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.exception.TemplateClusterException;
import com.vmware.bdd.spectypes.GroupType;
import com.vmware.bdd.spectypes.HadoopRole;

public class TemplateClusterSpec {
   private static final Logger logger = Logger.getLogger(TemplateClusterSpec.class);
   private static final String TEMPLATE_CLUSTER_SPEC_JSON = "template-cluster-spec.json";
   private static ClusterCreate templateClusterConfig;
   private static Map<GroupType, NodeGroupCreate> templateGroups =
      new HashMap<GroupType, NodeGroupCreate>();

   static {
      init();
   }

   private static void loadDefaultValue() {
      templateClusterConfig = new ClusterCreate();
      templateClusterConfig.setDistro("apache");

      // master group
      List<String> roles = new ArrayList<String>();
      roles.add(HadoopRole.HADOOP_NAMENODE_ROLE.toString());
      roles.add(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString());
      NodeGroupCreate master = createDefaultNodeGroup(GroupType.MASTER_GROUP.toString(), roles,
            1, InstanceType.MEDIUM, GroupType.MASTER_GROUP, "on");

      // job tracker group template
      templateGroups.put(GroupType.MASTER_JOBTRACKER_GROUP, master);

      // worker group
      roles = new ArrayList<String>();
      roles.add(HadoopRole.HADOOP_DATANODE.toString());
      roles.add(HadoopRole.HADOOP_TASKTRACKER.toString());
      createDefaultNodeGroup(GroupType.WORKER_GROUP.toString(), roles,
            3, InstanceType.SMALL, GroupType.WORKER_GROUP, "off");

      // client group
      roles = new ArrayList<String>();
      roles.add(HadoopRole.HADOOP_CLIENT_ROLE.toString());
      roles.add(HadoopRole.HIVE_ROLE.toString());
      roles.add(HadoopRole.HIVE_SERVER_ROLE.toString());
      roles.add(HadoopRole.PIG_ROLE.toString());
      createDefaultNodeGroup(GroupType.CLIENT_GROUP.toString(), roles,
            1, InstanceType.SMALL, GroupType.CLIENT_GROUP, "off");

      templateClusterConfig.setNodeGroups(templateGroups.values().toArray(new NodeGroupCreate[]{}));
   }

   private static NodeGroupCreate createDefaultNodeGroup(String name, List<String> roles, 
         int instanceNum, InstanceType instanceType, GroupType groupType, String ha) {
      NodeGroupCreate group = new NodeGroupCreate();
      group.setName(name);
      group.setRoles(roles);
      group.setInstanceNum(instanceNum);
      group.setInstanceType(instanceType);
      group.setHaFlag(ha);
      templateGroups.put(groupType, group);
      return group;
   }

   private static void init() {
      String homeDir = System.getProperties().getProperty("serengeti.home.dir");
      File templateFile = null;
      if (homeDir != null && homeDir.length() > 0) {
         StringBuilder builder = new StringBuilder();
         builder.append(homeDir).append(File.separator).append("conf").append(File.separator).append(TEMPLATE_CLUSTER_SPEC_JSON);
         templateFile = new File(builder.toString());
      } else {
         URL filePath = ConfigurationUtils.locate(TEMPLATE_CLUSTER_SPEC_JSON);
         if (filePath != null) {
            templateFile = ConfigurationUtils.fileFromURL(filePath);
         }
      }

      if (templateFile == null) {
         logger.error("cluster template spec is not found, using the default cluster value.");
         loadDefaultValue();
      }
      try {
         Reader fileReader = new FileReader(templateFile);
         createTemplate(fileReader);
      } catch (FileNotFoundException e) {
         logger.error("cluster template spec is not found, using the default cluster value.");
         loadDefaultValue();
      }
   }

   private static void createTemplate(Reader fileReader) {
      Gson gson = new Gson();
      templateClusterConfig = gson.fromJson(fileReader, ClusterCreate.class);
      NodeGroupCreate[] groups = templateClusterConfig.getNodeGroups();
      if (groups == null || groups.length == 0) {
         throw TemplateClusterException.TEMPLATE_NODEGROUPS_UNDEFINED();
      }
      EnumSet<HadoopRole> allRoles = EnumSet.noneOf(HadoopRole.class);  
      for (NodeGroupCreate group : groups) {
         List<String> roles = group.getRoles();
         if (roles == null || roles.isEmpty()) {
            throw TemplateClusterException.TEMPLATE_ROLES_EMPTY(group.getName());
         }
         EnumSet<HadoopRole> enumRoles = EnumSet.noneOf(HadoopRole.class);  
         for (String role : roles) {
            HadoopRole configuredRole = HadoopRole.fromString(role);
            if (configuredRole == null) {
               throw ClusterConfigException.UNSUPPORTED_HADOOP_ROLE(role, templateClusterConfig.getDistro());
            }
            enumRoles.add(configuredRole);
         }
         GroupType type = GroupType.fromHadoopRole(enumRoles);
         group.setGroupType(type);
         templateGroups.put(type, group);
         allRoles.addAll(enumRoles);
      }
      if (!templateGroups.containsKey(GroupType.MASTER_JOBTRACKER_GROUP)) {
         templateGroups.put(GroupType.MASTER_JOBTRACKER_GROUP, templateGroups.get(GroupType.MASTER_GROUP));
      }
      if (allRoles.size() < HadoopRole.values().length) {
         throw TemplateClusterException.INCOMPLETE_TEMPLATE_GROUPS();
      }
   }

   public static String getTemplateClusterManifest(String clusterName) {
      Gson gson =
         new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
      ClusterCreate result = new ClusterCreate(templateClusterConfig);
      result.setName(clusterName);
      return gson.toJson(result);
   }

   public static ClusterCreate getTemplateClusterAttributes() {
      return templateClusterConfig;
   }

   public static Map<GroupType, NodeGroupCreate> getTemplateGroupAttributes() {
      return templateGroups;
   }
}
