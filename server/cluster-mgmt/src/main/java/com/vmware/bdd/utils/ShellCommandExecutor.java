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
package com.vmware.bdd.utils;

import com.vmware.bdd.exception.BddException;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: Xiaoding Bian
 * Date: 3/28/14
 * Time: 5:26 PM
 */
public class ShellCommandExecutor {

   private final static Logger logger = Logger.getLogger(ShellCommandExecutor.class);

   protected long timeoutInterval = 0L;
   private int exitCode;
   private volatile AtomicBoolean completed;
   private AtomicBoolean timedOut;
   private File dir;
   private Map<String, String> environment;
   private String[] command;
   private StringBuffer output;
   private Process process;

   public static void execCmd(String command, File dir, Map<String, String> env, long timeoutInSec, String description) {
      ShellCommandExecutor executor = new ShellCommandExecutor(new String[]{"bash", "-c", command}, dir, env, timeoutInSec);
      try {
         logger.info(executor.toString());
         executor.execute();
      } catch (ExitCodeException ec) {
         logger.error(ec.getMessage());
      } catch (IOException e) {
         logger.error(e.getMessage());
      } finally {
         if (executor.getOutput() != null) {
            logger.info(executor.getOutput());
         }
         if (executor.isTimedOut()) {
            throw BddException.ExecCommand(null, description + Constants.EXEC_COMMAND_TIMEOUT);
         }
         if (!executor.isCompleted() || executor.getExitCode() != 0) {
            throw BddException.ExecCommand(null, description + Constants.EXEC_COMMAND_FAILED);
         }
      }
   }

   public ShellCommandExecutor(String[] execString) {
      this(execString, null);
   }

   public ShellCommandExecutor(String[] execString, File dir) {
      this(execString, dir, null);

   }

   public ShellCommandExecutor(String[] execString, File dir, Map<String, String> env) {
      this(execString, dir, env, 0L);
   }

   public ShellCommandExecutor(String[] execString, File dir, Map<String, String> env, long timeoutInSec) {
      command = execString.clone();
      if (dir != null) {
         this.setWorkDir(dir);
      }
      if (env != null) {
         this.setEnvironment(env);
      }
      timeoutInterval = timeoutInSec * 1000;
   }

   public String[] getExecString() {
      return command;
   }

   public int getExitCode() {
      return exitCode;
   }

   private void parseExecResult(BufferedReader br) throws IOException {
      output = new StringBuffer();
      char[] buf = new char[512];
      int nRead;
      while ( (nRead = br.read(buf, 0, buf.length)) > 0 ) {
         output.append(buf, 0, nRead);
      }
   }

   public String getOutput() {
      return (output == null) ? "" : output.toString();
   }

   public String toString() {
      StringBuilder builder = new StringBuilder();
      String[] args = getExecString();
      for (String s : args) {
         if (s.indexOf(' ') >= 0) {
            builder.append('"').append(s).append('"');
         } else {
            builder.append(s);
         }
         builder.append(' ');
      }
      return builder.toString();
   }

   public Process getProcess() {
      return process;
   }

   public boolean isCompleted() {
      return completed.get();
   }

   public boolean isTimedOut() {
      return timedOut.get();
   }

   private void setTimedOut() {
      this.timedOut.set(true);
   }

   public void setWorkDir(File dir) {
      this.dir = dir;
   }

   public void setEnvironment(Map<String, String> environment) {
      this.environment = environment;
   }

   /**
    * Might throw IOException if timeout
    */
   public void execute() throws IOException {
      exitCode = 0;
      ProcessBuilder builder = new ProcessBuilder(getExecString());
      Timer timeoutTimer = null;
      ShellTimeoutTimerTask timeoutTimerTask = null;
      timedOut = new AtomicBoolean(false);
      completed = new AtomicBoolean(false);

      if (environment != null) {
         builder.environment().putAll(this.environment);
      }
      if (dir != null) {
         builder.directory(this.dir);
      }

      process = builder.start();
      if (timeoutInterval > 0) {
         timeoutTimer = new Timer();
         timeoutTimerTask = new ShellTimeoutTimerTask(this);
         //One time scheduling.
         timeoutTimer.schedule(timeoutTimerTask, timeoutInterval);
      }
      final BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      BufferedReader inReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      final StringBuffer errMsg = new StringBuffer();

      // read error and input streams as this would free up the buffers
      // free the error stream buffer
      Thread errThread = new Thread() {
         @Override
         public void run() {
            try {
               String line = errReader.readLine();
               while((line != null) && !isInterrupted()) {
                  errMsg.append(line);
                  errMsg.append(System.getProperty("line.separator"));
                  line = errReader.readLine();
               }
            } catch(IOException ioe) {
               logger.warn("Error reading the error stream", ioe);
            }
         }
      };

      try {
         errThread.start();
      } catch (IllegalStateException ise) { }

      try {
         parseExecResult(inReader); // parse the output
         // clear the input stream buffer
         String line = inReader.readLine();
         while(line != null) {
            line = inReader.readLine();
         }
         exitCode  = process.waitFor();
         try {
            // make sure that the error thread exits
            errThread.join();
         } catch (InterruptedException ie) {
            logger.warn("Interrupted while reading the error stream", ie);
         }
         completed.set(true);
         if (exitCode != 0) {
            throw new ExitCodeException(exitCode, errMsg.toString());
         }
      } catch (InterruptedException ie) {
         throw new IOException(ie.toString());
      } finally {
         if ((timeoutTimer != null) && !timedOut.get()) {
            timeoutTimer.cancel();
         }
         // close the input stream
         try {
            inReader.close();
         } catch (IOException ioe) {
            logger.warn("Error while closing the input stream", ioe);
         }
         if (!completed.get()) {
            errThread.interrupt();
         }
         try {
            errReader.close();
         } catch (IOException ioe) {
            logger.warn("Error while closing the error stream", ioe);
         }
         process.destroy();
      }
   }

   public static class ExitCodeException extends IOException {
      int exitCode;

      public ExitCodeException(int exitCode, String message) {
         super(message);
         this.exitCode = exitCode;
      }

      public int getExitCode() {
         return exitCode;
      }
   }

   private static class ShellTimeoutTimerTask extends TimerTask {
      private ShellCommandExecutor executor;

      public ShellTimeoutTimerTask(ShellCommandExecutor executor) {
         this.executor = executor;
      }

      @Override
      public void run() {
         Process p = executor.getProcess();
         try {
            p.exitValue();
         } catch (Exception e) {
            //Process has not terminated.
            //So check if it has completed
            //if not just destroy it.
            if (p != null && !executor.completed.get()) {
               executor.setTimedOut();
               p.destroy();
            }
         }
      }
   }
}
