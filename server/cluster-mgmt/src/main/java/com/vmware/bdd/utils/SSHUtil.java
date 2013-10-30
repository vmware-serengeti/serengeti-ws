package com.vmware.bdd.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import com.vmware.bdd.utils.AuAssert;


public class SSHUtil {
   private static JSch jsch;
   private static Session session;
   private static final Logger logger = Logger.getLogger(JobUtils.class);

   private static void connect(String user, String privateKeyFile,
         String hostIP, int sshPort) {
      jsch = new JSch();

      try {
         session = jsch.getSession(user, hostIP, sshPort);
         jsch.addIdentity(privateKeyFile);
         java.util.Properties config = new java.util.Properties();
         config.put("StrictHostKeyChecking", "no");
         session.setConfig(config);
         session.setTimeout(Constants.SSH_SESSION_TIMEOUT);
         session.connect();
         logger.debug("SSH session is connected!");
      } catch (JSchException e) {
         e.printStackTrace();
      }
   }

   public static boolean execCmd(String user, String privateKeyFile,
         String hostIP, int sshPort, String command) {
      AuAssert.check(command != null);

      connect(user, privateKeyFile, hostIP, sshPort);

      ChannelExec channel = null;
      logger.info("going to exec command");
      try {
         channel = (ChannelExec) session.openChannel("exec");

         if (channel != null) {
            logger.debug("SSH channel is opened!");
            channel.setPty(true); // to enable sudo
            channel.setCommand(command);

            BufferedReader in =
                  new BufferedReader(new InputStreamReader(
                        channel.getInputStream()));
            channel.connect();
            if (!channel.isConnected()) {
               logger.error("Cannot setup SSH channel connection.");
            }

            StringBuilder buff = new StringBuilder();
            while (true) {
               String line = in.readLine();
               buff.append(line);

               if (channel.isClosed()) {
                  int exitStatus = channel.getExitStatus();
                  logger.debug("Exit status from exec is: " + exitStatus);
                  logger.debug("command result: " + buff.toString());

                  in.close();
                  if (exitStatus == 0) {
                     return true;
                  } else {
                     return false;
                  }
               }

               //to avoid CPU busy
               try {
                  Thread.sleep(200);
               } catch (InterruptedException e) {
               }
            }
         } else {
            logger.error("Cannot open SSH channel to " + hostIP + ".");
            return false;
         }
      } catch (IOException e) {
         e.printStackTrace();
      } catch (JSchException e) {
         e.printStackTrace();
      } finally {
         channel.disconnect();
         session.disconnect();
      }
      return false;
   }
}