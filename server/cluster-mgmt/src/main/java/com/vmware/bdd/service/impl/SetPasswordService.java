package com.vmware.bdd.service.impl;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.service.ISetPasswordService;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.SSHUtil;

public class SetPasswordService implements ISetPasswordService {

   private static final Logger logger = Logger.getLogger(ClusteringService.class);

   public ArrayList<String> setPasswordForNodes(String clusterName, ArrayList<String> ipsOfNodes, String password) {
      logger.info("Setting password for " + clusterName);
      logger.info("Nodes needed to be set password: " + ipsOfNodes.toString());

      ArrayList<String> failedIPs = null;
      try {
         for (String nodeIP : ipsOfNodes) {
            boolean succeed = setPasswordForNode(nodeIP, password);
            logger.info("set password for " + nodeIP + " result: " + succeed);
            if (!succeed) {
               if (failedIPs == null) {
                  failedIPs = new ArrayList<String>();
               }
               failedIPs.add(nodeIP);
            }
         }
      } catch (Exception e) {
         logger.error("error in setPassword for " + clusterName);
         throw BddException.INTERNAL(e, "Failed to set password for nodes in cluster " + clusterName);
      }
      return failedIPs;
   }

   public boolean setPasswordForNode(String nodeIP, String password) {
      logger.info("setting password of " + nodeIP);

      String privateKeyFile =
            Configuration.getString(Constants.SSH_PRIVATE_KEY_CONFIG_NAME, Constants.SSH_PRIVATE_KEY_FILE_NAME);
      String sshUser = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);
      int sshPort = Configuration.getInt(Constants.SSH_PORT_CONFIG_NAME, Constants.DEFAULT_SSH_PORT);

      String[] cmds = generateSetPasswdCommand(Constants.SET_PASSWORD_SCRIPT_CONFIG_NAME, password);
      boolean succeed = false;
      for (String cmd : cmds) {
         succeed = SSHUtil.execCmd(sshUser, privateKeyFile, nodeIP, sshPort, cmd);
         if (succeed) {
            logger.info("execute command" + " on " + nodeIP + " succeed.");
         } else {
            logger.info("execute command" + " on " + nodeIP + " failed.");
         }
      }
      if (succeed) {
         logger.info("set password for " + nodeIP + "succeed");
         return true;
      } else {
         logger.info("set password for " + nodeIP + "failed");
         return false;
      }
   }

   private String[] generateSetPasswdCommand(String setPasswdScriptConfig, String password) {
      String scriptFileName = Configuration.getString(setPasswdScriptConfig, Constants.DEFAULT_SET_PASSWORD_SCRIPT);

      String[] commands = new String[8];
      String tmpScript = "setPasswd.sh";
      commands[0] = "touch " + tmpScript;
      commands[1] = "echo \'" + scriptFileName + " -u <<EOF" + "\' >" + tmpScript;
      commands[2] = "echo " + password + " >> " + tmpScript;
      commands[3] = commands[2];
      commands[4] = "echo EOF >>" + tmpScript;
      commands[5] = "chmod +x " + tmpScript;
      commands[6] = "sudo ./" + tmpScript;
      commands[7] = "rm -f " + tmpScript;
      return commands;
   }
}