package com.vmware.bdd.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.utils.CommonUtil;

public class ChefServerManager {

   private static final Logger logger = Logger.getLogger(ChefServerManager.class);
   private static HashSet<String> allRoles = null;
   private static String GET_ROLES_CMD = "/usr/bin/knife role list -c /opt/serengeti/.chef/knife.rb";

   public ChefServerManager() {
   }

   /**
    * Return true if the role exists in Chef Server.
    */
   public static boolean isValidRole(String role) {
      HashSet<String> roles = getAllRoles();
      return roles != null && roles.contains(role);
   }

   /**
    * Set all Chef Roles existing in Chef Server.
    */
   public static void setAllRoles(HashSet<String> allRoles) {
      ChefServerManager.allRoles = allRoles;
   }

   /**
    * Get all Chef Roles in Chef Server.
    */
   public static HashSet<String> getAllRoles() {
      if (allRoles == null) {
         synchronized(ChefServerManager.class) {
            if (allRoles == null) {
               HashSet<String> roles = new HashSet<String>();
               Process p = CommonUtil.execCommand(GET_ROLES_CMD);
               if (p == null || p.exitValue() != 0) {
                  throw ClusterConfigException.CANNOT_GET_ROLES_FROM_CHEF_SERVER();
               }

               BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
               try {
                  String role;
                  while ((role = buf.readLine()) != null) {
                     if (!role.isEmpty()) {
                        logger.info("Found role " + role + " in Chef Server.");
                        roles.add(role);
                     }
                  }
                  allRoles = roles;
               } catch (IOException e) {
                  logger.error("Failed to get all roles from Chef Server: " + e.getMessage());
                  throw ClusterConfigException.CANNOT_GET_ROLES_FROM_CHEF_SERVER();
               }
            }
         }
      }

      return allRoles;
   }

}
