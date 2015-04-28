package com.vmware.bdd.plugin.ironfan.utils;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

/**
 * Created by qjin on 3/12/15.
 */
public class DefaultUtils {
   private static final Logger logger = Logger.getLogger(DefaultUtils.class);

   public static String getConfDir() {
      return com.vmware.bdd.utils.CommonUtil.getConfDir() + File.separator + Constants.DEFAULT_PLUGIN_NAME;
   }

   public static Process launchProcess(String cmd) {
       if (cmd == null || cmd.isEmpty()) {
           return null;
       }

       Process p = null;
       try {
           ProcessBuilder pb = new ProcessBuilder(Arrays.asList(cmd.split(" ")));
           pb.redirectErrorStream();
           p = pb.start();
           p.waitFor();
       } catch (Exception e) {
           p = null;
           logger.error("Failed to execute command " + cmd + " : " + e.getMessage());
       }

       return p;
   }

   public static boolean exec(String cmd) {
      Process p = launchProcess(cmd);
      return p != null && p.exitValue() == 0;
  }
}
