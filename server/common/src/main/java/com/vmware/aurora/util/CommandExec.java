/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.aurora.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.vmware.aurora.exception.CmdLineException;

/**
 * A command line executor that accepts stdio input string,
 * logs stdout/stderr, and generates error message from stderr.
 */
public class CommandExec extends Thread {
   private static Logger logger = Logger.getLogger(CommandExec.class);

   Process cmd;
   BufferedReader stdout, stderr;
   BufferedWriter stdin;
   String input;
   String errMsg;
   String stdoutMsg;
   String userName;
   OutputHandler<?> outputHandler;
   long timeout;
   long timeTaken;
   boolean timedOut = false;

   boolean terminated = false;

   public static interface OutputHandler<T> {
      void processLine(String line, String userName);
      T getProcessResult();
   }

   private CommandExec(Process cmd, String input, long timeout, String userName,
                       OutputHandler<?>... outputHandler) {
      stdout = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
      stderr = new BufferedReader(new InputStreamReader(cmd.getErrorStream()));
      stdin = new BufferedWriter(new OutputStreamWriter(cmd.getOutputStream()));
      this.cmd = cmd;
      this.userName = userName;
      this.input = input;
      this.timeout = TimeUnit.MILLISECONDS.toNanos(timeout);
      if (outputHandler.length != 0) {
         this.outputHandler = outputHandler[0];
      }
   }

   // log available output and return the last line
   private String logOutput(BufferedReader reader) {
      String msg = null;

      try {
         while (reader.ready()) {
            String line = reader.readLine();
            if (line != null) {
               logger.info(line);
               msg = line;
               if (outputHandler != null) {
                  outputHandler.processLine(line, userName);
               }
            }
         }
      } catch (IOException e) {
         // The stream has been closed, do thing.
      }

      return msg;
   }

   /*
    * This thread handles input and output of the command process.
    * It also serves as a timer to kill the process.
    */
   @Override
   public void run() {
      long startTime = System.nanoTime();

      try {
         // Deliver input.
         if (input != null) {
            stdin.write(input);
         }
         stdin.close();

         // Wait for process to finish and get output/stderr.
         while (System.nanoTime() < startTime + timeout) {
            String line = logOutput(stdout);
            if (line != null) {
               stdoutMsg = line;
            }
            line = logOutput(stderr);
            if (line != null) {
               errMsg = line;
            }
            synchronized(this) {
               if (terminated) {
                  break;
               } else {
                  wait(1000);
               }
            }
         }

         timeTaken = System.nanoTime() - startTime;
         if (timeTaken <= 0) {
            timeTaken = 1;
         }

         // We come out of the loop because of timeout.
         synchronized(this) {
            if (!terminated) {
               timedOut = true;
            }
         }
      } catch (Exception e) {
         logger.error(e);
      }

      logOutput(stdout);
      // Last chance to log remaining output.
      String line = logOutput(stderr);
      if (line != null) {
         errMsg = line;
      }

      // In the case of a timeout, this would stop the process and
      // wake up the caller thread.
      cmd.destroy();
   }

   /*
    * Break out the running thread if the process has been terminated.
    */
   public synchronized void terminate() {
      terminated = true;
      notifyAll();
   }

   public static String exec(String cmdArgs[], String input,
         long timeout, OutputHandler<?>...handlers) {
      return exec(cmdArgs, null, input, timeout, null, handlers);
   }

   public static String exec(String cmdArgs[], String input,
         long timeout, String userName, OutputHandler<?>...handlers) {
      return exec(cmdArgs, null, input, timeout, userName, handlers);
   }

   public static String exec(String cmdArgs[], File workDir, String input,
         long timeout, OutputHandler<?>...handlers) {
      return exec(cmdArgs, workDir, input, timeout, null, handlers);
   }

   public static String exec(String cmdArgs[], File workDir, String input,
         long timeout, String userName, OutputHandler<?>...handlers) {
      Process cmd;
      CommandExec ioThread;
      int retVal = 0;
      InterruptedException intExc = null;

      try {
         cmd = Runtime.getRuntime().exec(cmdArgs, null, workDir);
      } catch (IOException e) {
         throw CmdLineException.EXEC_ERROR(e, cmdArgs[0]);
      }

      ioThread = new CommandExec(cmd, input, timeout, userName, handlers);
      ioThread.start();
      // Wait for the command to complete.
      try {
         retVal = cmd.waitFor();
      } catch (InterruptedException e) {
         intExc = e;
      }
      // Terminate the io thread if it is still in the loop.
      ioThread.terminate();
      try {
         ioThread.join();
      } catch (InterruptedException e) {
         // ignore interruption
      }

      if (intExc != null) {
         throw CmdLineException.INTERRUPTED(intExc, cmdArgs[0]);
      } else if (ioThread.timedOut) {
         throw CmdLineException.TIMEOUT(
               TimeUnit.NANOSECONDS.toSeconds(ioThread.timeTaken), cmdArgs[0]);
      } else if (retVal != 0) {
         logger.info("Got " + retVal + " while executing " + cmdArgs[0]);
         throw CmdLineException.COMMAND_ERROR(ioThread.errMsg);
      }

      return ioThread.stdoutMsg;
   }
}