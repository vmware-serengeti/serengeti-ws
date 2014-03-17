package com.vmware.bdd.utils;

import java.io.*;

import org.apache.log4j.Logger;

import com.vmware.bdd.exception.BddException;

public class ExecCommandUtil {

   private final static Logger logger = Logger.getLogger(ExecCommandUtil.class);

   public static void execCmd(String command, String description) throws IOException, InterruptedException {

      String msg = null;

      Process process = null;
      BufferedReader stdOut = null;
      BufferedReader stdError = null;
      try {

         // using the Runtime exec method
         String[] commandArray = new String[] { "/bin/bash", "-c", command };
         process = Runtime.getRuntime().exec(commandArray);

         stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));

         stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

         int returnCode = process.waitFor();

         // read the output from the command
         if (stdOut != null) {
            while ((msg = stdOut.readLine()) != null) {
               logger.info(msg);
            }
         }

         // read any errors from the attempted command
         if (stdError != null) {
            while ((msg = stdError.readLine()) != null) {
               if (returnCode > 0) {
                  logger.error(msg);
               } else {
                  logger.warn(msg);
               }
            }
         }

         if (returnCode > 0) {
            throw BddException.ExecCommand(null, description + " failed.");
         }

      } catch (IOException e) {
         logger.error("Failed to run command '" + command + "'");
         throw BddException.ExecCommand(e, e.getMessage());
      } finally {
         if (process != null) {
            process.destroy();
         }
         process = null;

         if (stdOut != null) stdOut.close();
         if (stdError != null) stdError.close();
      }
   }
}
