/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Author: Xiaoding Bian
 * Date: 3/31/14
 * Time: 5:08 PM
 */
public class TestShellCommandExecutor {
   @BeforeMethod
   public void setup() {

   }

   @BeforeMethod
   public void tearDown() {

   }

   @Test
   public void testBasicShellCommand() throws IOException{
      String[] command = {"bash", "-c", "date"};
      ShellCommandExecutor executor = new ShellCommandExecutor(command);
      try {
         executor.execute();
      } catch (ShellCommandExecutor.ExitCodeException e) {
      } finally {
         assertEquals(executor.getExitCode(), 0);
         assertFalse(executor.isTimedOut());
      }
   }

   @Test
   public void testShellCommandWithDirAndEnv() throws IOException{
      File workDir = new File("/tmp");
      Map<String, String> env = new HashMap<String, String>();
      env.put("DATA_DIR", "/mnt/data");
      String[] command = {"bash", "-c", "date; pwd; echo ${DATA_DIR}"};
      ShellCommandExecutor executor = new ShellCommandExecutor(command, workDir, env);
      try {
         executor.execute();
      } catch (ShellCommandExecutor.ExitCodeException e) {
      } finally {
         assertTrue(executor.isCompleted());
         assertEquals(executor.getExitCode(), 0);
         assertTrue(executor.getOutput().contains("/tmp"));
         assertTrue(executor.getOutput().contains("/mnt/data"));
         assertFalse(executor.isTimedOut());
      }
   }

   // ExitCodeException should be thrown
   @Test(expectedExceptions = ShellCommandExecutor.ExitCodeException.class)
   public void testShellCommandFail() throws Exception {
            String[] command = {"bash", "-c", " echo failure >> /dev/stderr; exit 1"};
      ShellCommandExecutor executor = new ShellCommandExecutor(command);
      try {
         executor.execute();
      } catch(Exception e) {
         System.out.println(e.getMessage());
         throw e;
      } finally {
         assertEquals(executor.getExitCode(), 1);
         assertTrue(executor.isCompleted());
      }
   }

   @Test(expectedExceptions = IOException.class)
   public void testShellCommandTimeout() throws IOException {
      // This command will timeout, should throw IOException since process is destroyed.
      // The final state is: compltedte=false, timedout=true, exitcode=0(a fake return
      // value since we have no chance to update it)
      String[] command = {"bash", "-c", "sleep 3; echo hello" };
      ShellCommandExecutor executor = new ShellCommandExecutor(command, null, null, 1);
      try {
         executor.execute();
      } catch (ShellCommandExecutor.ExitCodeException e) {
      } finally {
         assertTrue(executor.isTimedOut());
         assertFalse(executor.isCompleted());
         assertEquals(executor.getExitCode(), 0);
      }
   }

   @Test
   public void testExecCmdTimeout() {
      String command = "sleep 3; echo hello" ;
      try {
         ShellCommandExecutor.execCmd(command, null, null, 1, "timeout test");
      } catch (Exception e) {
         System.out.println(e.getMessage());
      }
   }

   @Test
   public void testExecCmdFail() {
      String command = "echo failure >> /dev/stderr; exit 1";
      try {
         ShellCommandExecutor.execCmd(command, null, null, 1, "timeout test");
      } catch (Exception e) {
         System.out.println(e.getMessage());
      }
   }
}
