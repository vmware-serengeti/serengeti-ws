/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;

import org.apache.log4j.Logger;

public class CommonUtil {

   static final Logger logger = Logger.getLogger(CommonUtil.class);
   
   public static String readJsonFile(final String fileName) {
      StringBuilder jsonBuff = new StringBuilder();
      URL fileURL = CommonUtil.class.getClassLoader().getResource(fileName);
      if (fileURL != null) {
          InputStream in = null;
          try {
              in = new BufferedInputStream(fileURL.openStream());
              Reader rd = new InputStreamReader(in, "UTF-8");
              int c = 0;
              while ((c = rd.read()) != -1) {
                  jsonBuff.append((char) c);
              }
          } catch (IOException e) {
              logger.error(e.getMessage() + "\n Can not find " + fileName + " or IO read error.");
          } finally {
              try {
            	  if (in != null) {
            		  in.close();
            	  }
              } catch (IOException e) {
                  logger.error(e.getMessage() + "\n Can not close " + fileName + ".");
              }
          }
      }
      return jsonBuff.toString();
  }
}
