/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.usermgmt.SssdConfigurationGenerator;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.usermgmt.UserMgmtServerService;


/**
 * Created By xiaoliangl on 12/30/14.
 */
@Component
public class ClusterLdapUserMgmtCfgService {
   private final static Logger LOGGER = Logger.getLogger(ClusterLdapUserMgmtCfgService.class);

   @Autowired
   private IClusterEntityManager clusterEntityManager;

   @Autowired
   private UserMgmtServerService userMgmtServerService;

   @Autowired
   private SssdConfigurationGenerator sssdConfigurationGenerator;

   @Autowired
   private ClusterUserMgmtValidService clusterUserMgmtValidService;

   @Autowired
   private NodeLdapUserMgmtConfService nodeLdapUserMgmtConfService;

   @Autowired
   private ServiceUserConfigService serviceUserConfigService;

   public Map<String, String> getUserMgmtCfg(String clusterName) {
      Map<String, String> userMgmtCfg = clusterEntityManager.findUserMgmtCfg(clusterName);

      if (MapUtils.isEmpty(userMgmtCfg)) {
         LOGGER.info("no need to configure user management for cluster, no usermgmt cfg found in database.");
         return null;
      }

      return userMgmtCfg;
   }

   public void configureUserMgmt(String clusterName) {
      List<NodeEntity> nodeEntityList = clusterEntityManager.findAllNodes(clusterName);

      ArrayList<String> nodeMgmtIpList = new ArrayList<>();
      for (NodeEntity nodeEntity : nodeEntityList) {
         nodeMgmtIpList.add(nodeEntity.getPrimaryMgtIpV4());
      }

      String[] nodeMgmtIps = new String[nodeEntityList.size()];
      nodeMgmtIpList.toArray(nodeMgmtIps);

      configureUserMgmt(clusterName, nodeEntityList);
   }

   public void configureUserMgmt(String clusterName, List<NodeEntity> nodeEntityList) {
      Map<String, String> userMgmtCfg = getUserMgmtCfg(clusterName);
      Map<String, Map<String, String>> serviceUserCfg = serviceUserConfigService.getServiceUserConfigs(clusterName);

      if (userMgmtCfg == null && serviceUserCfg == null) {
         LOGGER.info("no need to do usermgmt configuration.");
         return;
      }

      if (CollectionUtils.isEmpty(nodeEntityList)) {
         LOGGER.info("the target node list is empty, skip usermgmt configuration.");
         return;
      }

      ArrayList<String> nodeMgmtIpList = new ArrayList<>();
      for (NodeEntity nodeEntity : nodeEntityList) {
         nodeMgmtIpList.add(nodeEntity.getPrimaryMgtIpV4());
      }
      String[] nodeMgmtIps = new String[nodeEntityList.size()];
      nodeMgmtIpList.toArray(nodeMgmtIps);

      Set<String> groupNameSet = new HashSet<>();

      String[] userMgmtGroups = clusterUserMgmtValidService.getGroupNames(userMgmtCfg);
      if (!ArrayUtils.isEmpty(userMgmtGroups)) {
         groupNameSet.addAll(Arrays.asList(userMgmtGroups));
      }

      Set<String> serviceUserGroupSet = serviceUserConfigService.getServiceUserGroups(serviceUserCfg);
      if (!CollectionUtils.isEmpty(serviceUserGroupSet)) {
         groupNameSet.addAll(serviceUserGroupSet);
      }

      String[] groupNames = new String[groupNameSet.size()];
      groupNameSet.toArray(groupNames);

      String sssdConfContent = sssdConfigurationGenerator.getConfigurationContent(
            userMgmtServerService.getByName(UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME, false),
            groupNames
      );

      File taskDir = CommandUtil.createWorkDir(System.currentTimeMillis());

      File localSssdConfFile = new File(taskDir, "sssd.conf");

      try (FileWriter fileWriter = new FileWriter(localSssdConfFile);) {
         //write sssd.conf for all nodes
         fileWriter.write(sssdConfContent);

      } catch (IOException ioe) {
         throw new RuntimeException("failed to write sssd.conf for usermgmt configuration.", ioe);
      }

      try {
         // scp to one node's tmp folder
         // cp to /etc/sssd
         // sudo authconfig --enablesssd --enablesssdauth --enablemkhomedir --updateall
         nodeLdapUserMgmtConfService.configureSssd(nodeMgmtIps, localSssdConfFile.getAbsolutePath());
      } finally {
         try {
            localSssdConfFile.delete();
         } catch (Exception ex) {
            LOGGER.warn("the local sssd file can not be deleted! please delete it manually!: " + localSssdConfFile.getAbsolutePath(), ex);
         }
      }

      String disableLocalUserFlag = userMgmtCfg.get(UserMgmtConstants.DISABLE_LOCAL_USER_FLAG);

      if (disableLocalUserFlag != null) {
         boolean disableLocalUser = Boolean.parseBoolean(disableLocalUserFlag);
         if (disableLocalUser) {
            nodeLdapUserMgmtConfService.disableLocalUsers(nodeMgmtIps);
         }
      }
   }

   public void configureUserMgmt(String clusterName, NodeEntity node) {
      configureUserMgmt(clusterName, Arrays.asList(node));
   }
}
