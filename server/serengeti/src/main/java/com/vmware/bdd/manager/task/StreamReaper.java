/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
package com.vmware.bdd.manager.task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

class StreamReaper extends TracedRunnable {
   private static final Logger logger = Logger.getLogger(StreamReaper.class);
   private InputStream inStream;
   private File outputFile;

   public StreamReaper(InputStream inStream, File outputFile) {
      this.inStream = inStream;
      this.outputFile = outputFile;
   }

   @Override
   public void doWork() throws Exception {
      BufferedReader bufInStream = null;
      BufferedWriter bufOutStream = null;
      try {
         bufInStream = new BufferedReader(new InputStreamReader(inStream));
         FileWriter fileStream = new FileWriter(outputFile);
         bufOutStream = new BufferedWriter(fileStream);

         String line;
         while (true) {
            try {
               line = bufInStream.readLine();
            } catch (IOException ex) {
               logger.warn("stream break, assume pipe is broken", ex);
               break;
            }
            if (line == null) {
               break;
            }
            bufOutStream.write(line);
            bufOutStream.newLine();
            /*
             * some overhead here, fork a thread to flush periodically if it's a
             * problem.
             */
            bufOutStream.flush();
         }
         bufOutStream.flush();
      } finally {
         if (bufInStream != null) {
            try {
               bufInStream.close();
            } catch (IOException e) {
               logger.error("falied to close input stream", e);
            }
         }

         if (bufOutStream != null) {
            try {
               bufOutStream.close();
            } catch (IOException e) {
               logger.error("falied to close output stream: " + outputFile, e);
            }
         }
      }
   }

   @Override
   public void onStart() {
      logger.info("start dumping: " + outputFile);
   }

   @Override
   public void onException(Throwable t) {
      logger.error("falied to dump the stream: " + outputFile);
   }

   @Override
   public void onFinish() {
      logger.info("finish dumping: " + outputFile);
   }
}