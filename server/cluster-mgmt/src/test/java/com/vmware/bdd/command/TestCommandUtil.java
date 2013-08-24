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
package com.vmware.bdd.command;

import static org.testng.AssertJUnit.assertTrue;
import java.io.File;
import java.io.IOException;

import org.testng.annotations.Test;

public class TestCommandUtil {
   @Test
   public void testCreateWorkDir() throws IOException {
      long executionId = 1L;
      long lastCmdId = 0L;
      CommandUtil.createWorkDir(executionId);
      File path = new File(CommandUtil.taskRootDir, Long.toString(executionId));
      assertTrue(path.exists());
      File childPath = new File(path, Long.toString(++lastCmdId));
      assertTrue(childPath.exists());
      CommandUtil.createWorkDir(executionId);
      childPath = new File(path, Long.toString(++lastCmdId));
      assertTrue(childPath.exists());
      delete(CommandUtil.taskRootDir.getAbsolutePath());
   }

   private void delete(String filepath) throws IOException {
      File f = new File(filepath);
      if (f.exists() && f.isDirectory()) {
         if (f.listFiles().length == 0) {
            f.delete();
         } else {
            File delFile[] = f.listFiles();
            int i = f.listFiles().length;
            for (int j = 0; j < i; j++) {
               if (delFile[j].isDirectory()) {
                  delete(delFile[j].getAbsolutePath());
               }
               delFile[j].delete();
            }
         }
      }
   }

}
