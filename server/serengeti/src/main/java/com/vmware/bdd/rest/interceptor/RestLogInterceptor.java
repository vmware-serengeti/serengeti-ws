/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.rest.interceptor;


import com.vmware.bdd.utils.CommonUtil;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class RestLogInterceptor implements HandlerInterceptor {

   private static final Logger logger = Logger.getLogger(RestLogInterceptor.class);
   private static String auditFileName = "serengeti.audit";
   private static File auditFile;
   long millisecond = 0L;
   boolean isSuceed;

   static {
      auditFile = createAuditFile();
   }

   private static File createAuditFile() {
      File auditFile = new File(getLogDir() + auditFileName);
      if (!auditFile.exists()) {
         try {
            auditFile.createNewFile();
         } catch (IOException e) {
            logger.warn(auditFileName + " created failed: " + e.getMessage());
         }
      }
      return auditFile;
   }

   private static String getLogDir() {
      String homeDir = System.getProperty("serengeti.home.dir");
      if (homeDir == null) {
         homeDir = "/tmp/serengeti/";
      }
      return homeDir + "/logs/";
   }

   @Override
   public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
      isSuceed = false;
      millisecond = System.currentTimeMillis();
      return true;
   }

   @Override
   public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) {
      isSuceed = true;
   }

   @Override
   public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
         throws Exception {
      millisecond = System.currentTimeMillis() - millisecond;
      String location = request.getHeader("X-FORWARDED-FOR");
      if (CommonUtil.isBlank(location)) {
         String address = request.getRemoteAddr();
         String host = request.getRemoteHost();
         if (!CommonUtil.isBlank(address)) {
            location = address;
         } else if (!CommonUtil.isBlank(host)) {
            location = host;
         }
      }
      String url = request.getRequestURI();
      if (request.getQueryString() != null) {
         url += "?" + request.getQueryString();
      }
      String info = location + ", " + request.getMethod() + " " + url + ", " + response.getStatus() + ", " + millisecond + "ms";
      logger.info(info);
      String currentTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH).format(System.currentTimeMillis());
      CommonUtil.writeFile(auditFile, currentTime + " AUDIT " + info + "\n", true);
   }
}
