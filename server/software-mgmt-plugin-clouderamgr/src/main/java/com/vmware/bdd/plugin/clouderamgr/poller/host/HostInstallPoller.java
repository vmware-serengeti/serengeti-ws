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
package com.vmware.bdd.plugin.clouderamgr.poller.host;

import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.v6.RootResourceV6;
import com.google.gson.Gson;
import com.vmware.bdd.plugin.clouderamgr.poller.host.parser.HostInstallDetailsParser;
import com.vmware.bdd.plugin.clouderamgr.poller.host.parser.IDetailsParser;
import com.vmware.bdd.plugin.clouderamgr.poller.host.parser.ParseResult;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author: Xiaoding Bian
 * Date: 6/26/14
 * Time: 6:25 PM
 */
public class HostInstallPoller extends StatusPoller{
   private static final Logger logger = Logger.getLogger(HostInstallPoller.class);
   private final String POST_ADDR = "/cmf/j_spring_security_check";
   private final String POST_USER_KEY = "j_username";
   private final String POST_PASSWORD_KEY = "j_password";

   private String domain;
   private String username;
   private String password;
   private BasicCookieStore cookieStore;
   private CloseableHttpClient httpClient;
   private PoolingHttpClientConnectionManager httpClientConnectionManager = null;
   private IDetailsParser parser = new HostInstallDetailsParser();

   private int leftStepsNum = 10;

   private RootResourceV6 rootResource;
   private Long parentCmdId;
   private ClusterReportQueue reportQueue;
   private ClusterReport currentReport;
   private int endProgress;
   private ExecutorService executor;
   private Set<String> msgSet = new HashSet<String>();
   volatile private boolean reported;
   volatile private boolean running =  true;

   public HostInstallPoller(final RootResourceV6 rootResource, final Long parentCmdId,
         final ClusterReport currentReport,
         final ClusterReportQueue reportQueue, int endProgress,
         String domain, String username, String password) throws Exception {
      this.rootResource = rootResource;
      this.parentCmdId = parentCmdId;
      this.reportQueue = reportQueue;
      this.currentReport = currentReport;
      this.endProgress = endProgress;
      this.domain = domain;
      this.username = username;
      this.password = password;
   }

   @Override
   public void setup() {
      try {
         login();
         httpClientConnectionManager = new PoolingHttpClientConnectionManager();
         httpClientConnectionManager.setMaxTotal(20);
         this.httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore)
               .setConnectionManager(httpClientConnectionManager).build();
         int maxSessionNum = 10;
         int i = 0;
         reported = true;
         executor = Executors.newCachedThreadPool();
         for (final ApiCommand command : rootResource.getCommandsResource().readCommand(parentCmdId).getChildren()) {
            /* Each crawler will launch a http session with CM server and keep on requesting.
             * So far, we only report cluster level status, so it's no need to monitor each subcommand,
             * especially for large scale cluster.
             */
            if (i == maxSessionNum) {
               break;
            }
            executor.submit(new Crawler(command.getId()));
            i += 1;
         }
      } catch (Exception e) {
         // As this implementation does not follow official APIs, may not work
         // in future version, just ignore any exception
      }
   }

   @Override
   public boolean poll() {
      ClusterReport toReport = null;
      synchronized (currentReport) {
         if (!reported) {
            int currentProgress = currentReport.getProgress();
            int toProgress = currentProgress + (endProgress - currentProgress) / leftStepsNum;
            currentReport.setProgress(toProgress > endProgress ? endProgress : toProgress );
            leftStepsNum -= 1;
            toReport = currentReport.clone();
            reported = true;
         }
      }
      if (toReport != null) {
         // free the "currentReport" lock before adding to reportQueue to avoid blocking
         logger.info("report status progress=" + toReport.getProgress() + ", action=" + toReport.getAction());
         reportQueue.addClusterReport(toReport);
      }
      if (rootResource.getCommandsResource().readCommand(parentCmdId).getEndTime() != null) {
         running = false;
         executor.shutdown();
         return true;
      }
      return false;
   }

   @Override
   public void tearDown() {
      try {
         this.httpClient.close();
      } catch (IOException e) {
      }

      if (httpClientConnectionManager != null) {
         httpClientConnectionManager.shutdown();
      }
   }

   private void login() throws Exception {
      cookieStore = new BasicCookieStore();
      CloseableHttpClient loginClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
      URI uri = new URI(domain + POST_ADDR);
      HttpUriRequest login = RequestBuilder.post()
            .setUri(uri)
            .addParameter(POST_USER_KEY, username)
            .addParameter(POST_PASSWORD_KEY, password)
            .build();
      logger.info("Login " + uri.toString());

      CloseableHttpResponse response = loginClient.execute(login);
      try {
         HttpEntity entity = response.getEntity();

         logger.info("Login form get: " + response.getStatusLine());
         EntityUtils.consume(entity);

         List<Cookie> cookies = cookieStore.getCookies();
         if (cookies.isEmpty()) {
            logger.info("Get no cookies");
         } else {
            logger.info("All cookies: " + (new Gson()).toJson(cookies));
         }
      } finally {
         response.close();
         loginClient.close();
      }
   }

   private class Crawler implements Runnable {
      private Long commandId;
      private String currentMsg = null;

      public Crawler(Long commandId) {
         this.commandId = commandId;
      }

      @Override
      public void run() {
         String detailsUrl = domain + "/cmf/command/" + commandId + "/detailsContent";
         HttpGet httpget = new HttpGet(detailsUrl);
         try {
            while (running) {
               CloseableHttpResponse response = null;
               try {
                  if (rootResource.getCommandsResource().readCommand(commandId).getEndTime() != null
                        && !rootResource.getCommandsResource().readCommand(commandId).getSuccess()) {
                     break;
                  }

                  response = httpClient.execute(httpget);
                  ParseResult result = parser.parse(EntityUtils.toString(response.getEntity()));
                  if (result.getPercent() == 100) {
                     break;
                  }
                  if (result.getMessage() != null && !result.getMessage().equals(currentMsg)) {
                     currentMsg = result.getMessage();
                     synchronized (currentReport) {
                        if (!msgSet.contains(currentMsg)) {
                           msgSet.add(currentMsg);
                           currentReport.setAction(currentMsg);
                           reported = false;
                        }
                     }
                  }
                  Thread.sleep(5 * 1000);
               } catch (Exception e) {
                  // ignore
                  break;
               } finally {
                  try {
                     response.close();
                  } catch (IOException e) {
                  }
               }
            }
         } finally {
            httpget.releaseConnection();
         }
      }
   }
}
