/******************************************************************************
 *   Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.cli.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jline.console.ConsoleReader;

import org.apache.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.vmware.bdd.apitypes.Connect;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.apitypes.TaskRead.Type;
import com.vmware.bdd.cli.commands.CommandsUtils;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.cli.commands.CookieCache;
import com.vmware.bdd.cli.config.RunWayConfig;
import com.vmware.bdd.cli.config.RunWayConfig.RunType;
import com.vmware.bdd.utils.CommonUtil;

/**
 * RestClient provides common rest apis required by resource operations.
 * 
 */
@Component
public class RestClient {

   static final Logger logger = Logger.getLogger(RestClient.class);

   private String hostUri;

   @Autowired
   private RestTemplate client;

   static {
      trustSSLCertificate();
   }

   private RestClient() {
      hostUri = getHostUriProperty();
   }

   /*
    *  Get Serengeti host from cli property file
    */
   private String getHostUriProperty() {
      String hostUri = null;
      FileReader hostFileReader = null;

      try {
         hostFileReader = new FileReader(Constants.PROPERTY_FILE);
         Properties hostProperties = new Properties();
         hostProperties.load(hostFileReader);

         if (hostProperties != null
               && hostProperties.get(Constants.PROPERTY_HOST) != null) {
            hostUri =
                  Constants.HTTPS_CONNECTION_PREFIX
                        + (String) hostProperties.get(Constants.PROPERTY_HOST)
                        + Constants.HTTPS_CONNECTION_LOGIN_SUFFIX;
         }
      } catch (Exception e) {//not set yet; or read io error
      } finally {
         if (hostFileReader != null) {
            try {
               hostFileReader.close();
            } catch (IOException e) {
               //nothing to do
            }
         }
      }

      return hostUri;
   }

   /**
    * connect to a Serengeti server
    * 
    * @param host
    *           host url with optional port
    * @param username
    *           serengeti login user name
    * @param password
    *           serengeti password
    */
   public Connect.ConnectType connect(final String host, final String username,
         final String password) {
      String oldHostUri = hostUri;

      hostUri =
            Constants.HTTPS_CONNECTION_PREFIX + host
                  + Constants.HTTPS_CONNECTION_LOGIN_SUFFIX;

      try {
         ResponseEntity<String> response =
               login(Constants.REST_PATH_LOGIN, String.class, username,
                     password);

         if (response.getStatusCode() == HttpStatus.OK) {
            //normal response
            updateHostproperty(host);
            String cookieValue = response.getHeaders().getFirst("Set-Cookie");
            if (cookieValue.contains(";")) {
               cookieValue = cookieValue.split(";")[0];
            }
            writeCookieInfo(cookieValue);
            System.out.println(Constants.CONNECT_SUCCESS);
         } else {
            //error
            System.out.println(Constants.CONNECT_FAILURE);
            //recover old hostUri
            hostUri = oldHostUri;
            return Connect.ConnectType.ERROR;
         }
      } catch (CliRestException cliRestException) {
         if (cliRestException.getStatus() == HttpStatus.UNAUTHORIZED) {
            System.out.println(Constants.CONNECT_UNAUTHORIZATION_CONNECT);
            //recover old hostUri
            hostUri = oldHostUri;
            return Connect.ConnectType.UNAUTHORIZATION;
         } else if (cliRestException.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
            System.out.println(cliRestException.getMessage());
            return Connect.ConnectType.ERROR;
         } else {
            System.out.println(Constants.CONNECT_FAILURE + ": "
                  + cliRestException.getStatus() + " "
                  + cliRestException.getMessage().toLowerCase());
            return Connect.ConnectType.ERROR;
         }
      } catch (Exception e) {
         System.out.println(Constants.CONNECT_FAILURE + ": "
               + (CommandsUtils.getExceptionMessage(e)));
         return Connect.ConnectType.ERROR;
      }
      return Connect.ConnectType.SUCCESS;
   }

   /**
    * Disconnect the session
    */
   public void disconnect() {
      try {
         checkConnection();
         logout(Constants.REST_PATH_LOGOUT, String.class);
      } catch (CliRestException cliRestException) {
         if (cliRestException.getStatus() == HttpStatus.UNAUTHORIZED) {
            writeCookieInfo("");
         }
      } catch (Exception e) {
         System.out.println(Constants.DISCONNECT_FAILURE + ": "
               + CommandsUtils.getExceptionMessage(e));
      }
   }

   private void writeCookieInfo(String cookie) {
      CookieCache.put("Cookie", cookie);
      Properties properties = new Properties();
      properties.put("Cookie", cookie);
      CommandsUtils.writeProperties(properties, Constants.PROPERTY_FILE);
   }

   private String readCookieInfo() {
      String cookieValue = "";
      cookieValue = CookieCache.get("Cookie");
      if (CommandsUtils.isBlank(cookieValue)) {
         Properties properties = null;
         properties = CommandsUtils.readProperties(Constants.PROPERTY_FILE);
         if (properties != null) {
            return properties.getProperty("Cookie");
         } else {
            return null;
         }
      }
      return cookieValue;
   }

   private <T> ResponseEntity<T> restGetById(final String path,
         final String id, final Class<T> respEntityType,
         final boolean hasDetailQueryString) {
      String targetUri =
            hostUri + Constants.HTTPS_CONNECTION_API + path + "/" + id;
      if (hasDetailQueryString) {
         targetUri += Constants.QUERY_DETAIL;
      }
      return restGetByUri(targetUri, respEntityType);
   }

   private <T> ResponseEntity<T> restGet(final String path,
         final Class<T> respEntityType, final boolean hasDetailQueryString) {
      String targetUri = hostUri + Constants.HTTPS_CONNECTION_API + path;
      if (hasDetailQueryString) {
         targetUri += Constants.QUERY_DETAIL;
      }
      return restGetByUri(targetUri, respEntityType);
   }

   private <T> ResponseEntity<T> login(final String path,
         final Class<T> respEntityType, final String username,
         final String password) {
      StringBuilder uriBuff = new StringBuilder();
      uriBuff.append(hostUri).append(path);
      if (!CommandsUtils.isBlank(username) && !CommandsUtils.isBlank(password)) {
         uriBuff.append("?").append("j_username=").append(username)
               .append("&j_password=").append(password);
      }
      return restPostByUri(uriBuff.toString(), respEntityType, false);
   }

   private <T> ResponseEntity<T> logout(final String path,
         final Class<T> respEntityType) {
      StringBuilder uriBuff = new StringBuilder();
      uriBuff.append(hostUri).append(path);
      return restGetByUri(uriBuff.toString(), respEntityType);
   }

   private <T> ResponseEntity<T> restGetByUri(String uri,
         Class<T> respEntityType) {
      HttpHeaders headers = buildHeaders();
      HttpEntity<String> entity = new HttpEntity<String>(headers);

      return client.exchange(uri, HttpMethod.GET, entity, respEntityType);
   }

   private <T> ResponseEntity<T> restPostByUri(String uri,
         Class<T> respEntityType, boolean withCookie) {
      HttpHeaders headers = buildHeaders(withCookie);
      HttpEntity<String> entity = new HttpEntity<String>(headers);

      return client.exchange(uri, HttpMethod.POST, entity, respEntityType);
   }

   private HttpHeaders buildHeaders(boolean withCookie) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      List<MediaType> acceptedTypes = new ArrayList<MediaType>();
      acceptedTypes.add(MediaType.APPLICATION_JSON);
      acceptedTypes.add(MediaType.TEXT_HTML);
      headers.setAccept(acceptedTypes);

      if (withCookie) {
         String cookieInfo = readCookieInfo();
         headers.add("Cookie", cookieInfo == null ? "" : cookieInfo);
      }
      return headers;
   }

   private HttpHeaders buildHeaders() {
      return buildHeaders(true);
   }

   /*
    * Update host property file
    */
   private void updateHostproperty(final String host) throws IOException {
      FileOutputStream hostFile = null;
      try {
         Properties hostProperty = new Properties();
         hostProperty.setProperty(Constants.PROPERTY_HOST, host);
         hostFile = new FileOutputStream(Constants.PROPERTY_FILE);
         hostProperty.store(hostFile, Constants.PROPERTY_FILE_HOST_COMMENT);
      } catch (IOException e) {
         StringBuilder exceptionMsg = new StringBuilder();
         exceptionMsg.append(Constants.PROPERTY_FILE_HOST_FAILURE);
         if (!CommonUtil.isBlank(e.getMessage())) {
            exceptionMsg.append(",").append(e.getMessage());
         }
         throw new IOException(exceptionMsg.toString());
      } finally {
         if (hostFile != null) {
            try {
               hostFile.close();
            } catch (IOException e) {
               //nothing to do
            }
         }
      }
   }

   /**
    * Create an object through rest apis
    * 
    * @param entity
    *           the creation content
    * @param path
    *           the rest url
    * @param verb
    *           the http method
    * @param prettyOutput
    *           output callback
    */
   public void createObject(Object entity, final String path,
         final HttpMethod verb, PrettyOutput... prettyOutput) {
      checkConnection();
      try {
         if (verb == HttpMethod.POST) {
            ResponseEntity<String> response = restPost(path, entity);
            if (!validateAuthorization(response)) {
               return;
            }
            processResponse(response, HttpMethod.POST, prettyOutput);
         } else {
            throw new Exception(Constants.HTTP_VERB_ERROR);
         }

      } catch (Exception e) {
         throw new CliRestException(CommandsUtils.getExceptionMessage(e));
      }
   }

   private ResponseEntity<String> restPost(String path, Object entity) {
      String targetUri = hostUri + Constants.HTTPS_CONNECTION_API + path;

      HttpHeaders headers = buildHeaders();
      HttpEntity<Object> postEntity = new HttpEntity<Object>(entity, headers);

      return client.exchange(targetUri, HttpMethod.POST, postEntity,
            String.class);
   }

   /*
    * Will process normal response with/without a task location header
    */
   private TaskRead processResponse(ResponseEntity<String> response,
         HttpMethod verb, PrettyOutput... prettyOutput) throws Exception {

      HttpStatus responseStatus = response.getStatusCode();
      if (responseStatus == HttpStatus.ACCEPTED) {//Accepted with task in the location header
         //get task uri from response to trace progress
         HttpHeaders headers = response.getHeaders();
         URI taskURI = headers.getLocation();
         String[] taskURIs = taskURI.toString().split("/");
         String taskId = taskURIs[taskURIs.length - 1];

         TaskRead taskRead;
         int oldProgress = 0;
         Status oldTaskStatus = null;
         Status taskStatus = null;
         int progress = 0;
         do {
            ResponseEntity<TaskRead> taskResponse =
                  restGetById(Constants.REST_PATH_TASK, taskId, TaskRead.class,
                        false);

            //task will not return exception as it has status
            taskRead = taskResponse.getBody();

            progress = (int) (taskRead.getProgress() * 100);
            taskStatus = taskRead.getStatus();

            //fix cluster deletion exception
            Type taskType = taskRead.getType();
            if ((taskType == Type.DELETE) && (taskStatus == TaskRead.Status.COMPLETED)) {
               clearScreen();
               System.out.println(taskStatus + " " + progress + "%\n");
               break;
            }

            if ((prettyOutput != null && prettyOutput.length > 0 && (taskRead.getType() == Type.VHM ? prettyOutput[0]
                  .isRefresh(true) : prettyOutput[0].isRefresh(false)))
                  || oldTaskStatus != taskStatus
                  || oldProgress != progress) {
               //clear screen and show progress every few seconds 
               clearScreen();
               //output completed task summary first in the case there are several related tasks
               if (prettyOutput != null && prettyOutput.length > 0
                     && prettyOutput[0].getCompletedTaskSummary() != null) {
                  for (String summary : prettyOutput[0]
                        .getCompletedTaskSummary()) {
                     System.out.println(summary + "\n");
                  }
               }
               System.out.println(taskStatus + " " + progress + "%\n");

               if (prettyOutput != null && prettyOutput.length > 0) {
                  // print call back customize the detailed output case by case
                  prettyOutput[0].prettyOutput();
               }

               if (oldTaskStatus != taskStatus || oldProgress != progress) {
                  oldTaskStatus = taskStatus;
                  oldProgress = progress;
                  if (taskRead.getProgressMessage() != null) {
                     System.out.println(taskRead.getProgressMessage());
                  }
               }
            }
            try {
               Thread.sleep(3 * 1000);
            } catch (InterruptedException ex) {
               //ignore
            }
         } while (taskStatus != TaskRead.Status.COMPLETED
               && taskStatus != TaskRead.Status.FAILED
               && taskStatus != TaskRead.Status.ABANDONED
               && taskStatus != TaskRead.Status.STOPPED);

         String errorMsg = taskRead.getErrorMessage();
         if (!taskRead.getStatus().equals(TaskRead.Status.COMPLETED)) {
            throw new CliRestException(errorMsg);
         } else { //completed
            if (taskRead.getType().equals(Type.VHM)) {
               logger.info("task type is vhm");
               Thread.sleep(5*1000);
               if (prettyOutput != null && prettyOutput.length > 0
                     && prettyOutput[0].isRefresh(true)) {
                  //clear screen and show progress every few seconds
                  clearScreen();
                  System.out.println(taskStatus + " " + progress + "%\n");

                  // print call back customize the detailed output case by case
                  if (prettyOutput != null && prettyOutput.length > 0) {
                     prettyOutput[0].prettyOutput();
                  }
               }
            } else {
               return taskRead;
            }
         }
      }
      return null;
   }

   private void clearScreen() {
      AnsiConsole.systemInstall();
      String separator = "[";
      char ESC = 27;
      String clearScreen = "2J";
      System.out.print(ESC + separator + clearScreen);
      AnsiConsole.systemUninstall();
   }

   /**
    * Generic method to get an object by id
    * 
    * @param id
    * @param entityType
    *           the object type
    * @param path
    *           the rest url
    * @param verb
    *           the http method
    * @param detail
    *           flag to retrieve detailed information or not
    * @return the object
    */
   public <T> T getObject(final String id, Class<T> entityType,
         final String path, final HttpMethod verb, final boolean detail) {
      checkConnection();
      try {
         if (verb == HttpMethod.GET) {
            ResponseEntity<T> response =
                  restGetById(path, id, entityType, detail);
            if (!validateAuthorization(response)) {
               return null;
            }
            T objectRead = response.getBody();

            return objectRead;
         } else {
            throw new Exception(Constants.HTTP_VERB_ERROR);
         }
      } catch (Exception e) {
         throw new CliRestException(CommandsUtils.getExceptionMessage(e));
      }
   }

   /**
    * Method to get by path
    * 
    * @param entityType
    * @param path
    * @param verb
    * @param detail
    * @return
    */
   public <T> T getObjectByPath(Class<T> entityType, final String path,
         final HttpMethod verb, final boolean detail) {
      checkConnection();

      try {
         if (verb == HttpMethod.GET) {
            ResponseEntity<T> response = restGet(path, entityType, detail);

            T objectRead = response.getBody();

            return objectRead;
         } else {
            throw new Exception(Constants.HTTP_VERB_ERROR);
         }
      } catch (Exception e) {
         throw new CliRestException(CommandsUtils.getExceptionMessage(e));
      }
   }

   /**
    * Generic method to get all objects of a type
    * 
    * @param entityType
    *           object type
    * @param path
    *           the rest url
    * @param verb
    *           the http method
    * @param detail
    *           flag to retrieve detailed information or not
    * @return the objects
    */
   public <T> T getAllObjects(final Class<T> entityType, final String path,
         final HttpMethod verb, final boolean detail) {
      checkConnection();
      try {
         if (verb == HttpMethod.GET) {
            ResponseEntity<T> response = restGet(path, entityType, detail);
            if (!validateAuthorization(response)) {
               return null;
            }
            T objectsRead = response.getBody();

            return objectsRead;
         } else {
            throw new Exception(Constants.HTTP_VERB_ERROR);
         }
      } catch (Exception e) {
         throw new CliRestException(CommandsUtils.getExceptionMessage(e));
      }
   }

   /**
    * Delete an object by id
    * 
    * @param id
    * @param path
    *           the rest url
    * @param verb
    *           the http method
    * @param prettyOutput
    *           utput callback
    */
   public void deleteObject(final String id, final String path,
         final HttpMethod verb, PrettyOutput... prettyOutput) {
      checkConnection();
      try {
         if (verb == HttpMethod.DELETE) {
            ResponseEntity<String> response = restDelete(path, id);
            if (!validateAuthorization(response)) {
               return;
            }
            processResponse(response, HttpMethod.DELETE, prettyOutput);
         } else {
            throw new Exception(Constants.HTTP_VERB_ERROR);
         }

      } catch (Exception e) {
         throw new CliRestException(CommandsUtils.getExceptionMessage(e));
      }
   }

   private ResponseEntity<String> restDelete(String path, String id) {
      String targetUri =
            hostUri + Constants.HTTPS_CONNECTION_API + path + "/" + id;

      HttpHeaders headers = buildHeaders();
      HttpEntity<String> entity = new HttpEntity<String>(headers);

      return client
            .exchange(targetUri, HttpMethod.DELETE, entity, String.class);
   }

   private void checkConnection() {
      if (hostUri == null) {
         throw new CliRestException(Constants.NEED_CONNECTION);
      } else if (CommandsUtils.isBlank(readCookieInfo())) {
         throw new CliRestException(Constants.CONNECT_CHECK_LOGIN);
      }
   }

   /**
    * process requests with query parameters
    * 
    * @param id
    * @param path
    *           the rest url
    * @param verb
    *           the http method
    * @param queryStrings
    *           required query strings
    * @param prettyOutput
    *           output callback
    */
   public void actionOps(final String id, final String path,
         final HttpMethod verb, final Map<String, String> queryStrings,
         PrettyOutput... prettyOutput) {
      checkConnection();
      try {
         if (verb == HttpMethod.PUT) {
            ResponseEntity<String> response =
                  restActionOps(path, id, queryStrings);
            if (!validateAuthorization(response)) {
               return;
            }
            processResponse(response, HttpMethod.PUT, prettyOutput);
         } else {
            throw new Exception(Constants.HTTP_VERB_ERROR);
         }

      } catch (Exception e) {
         throw new CliRestException(CommandsUtils.getExceptionMessage(e));
      }
   }

   private ResponseEntity<String> restActionOps(String path, String id,
         Map<String, String> queryStrings) {
      String targetUri =
            hostUri + Constants.HTTPS_CONNECTION_API + path + "/" + id;
      if (queryStrings != null) {
         targetUri = targetUri + buildQueryStrings(queryStrings);
      }
      HttpHeaders headers = buildHeaders();
      HttpEntity<String> entity = new HttpEntity<String>(headers);

      return client.exchange(targetUri, HttpMethod.PUT, entity, String.class);
   }

   private String buildQueryStrings(Map<String, String> queryStrings) {
      StringBuilder stringBuilder = new StringBuilder("?");

      Set<Entry<String, String>> entryset = queryStrings.entrySet();
      for (Entry<String, String> entry : entryset) {
         stringBuilder.append(entry.getKey() + "=" + entry.getValue());
      }

      return stringBuilder.toString();
   }

   /**
    * Update an object
    * 
    * @param entity
    *           the updated content
    * @param path
    *           the rest url
    * @param verb
    *           the http method
    * @param prettyOutput
    *           output callback
    */
   public void update(Object entity, final String path, final HttpMethod verb,
         PrettyOutput... prettyOutput) {
      checkConnection();
      try {
         if (verb == HttpMethod.PUT) {
            ResponseEntity<String> response = restUpdate(path, entity);
            if (!validateAuthorization(response)) {
               return;
            }
            processResponse(response, HttpMethod.PUT, prettyOutput);
         } else {
            throw new Exception(Constants.HTTP_VERB_ERROR);
         }

      } catch (Exception e) {
         throw new CliRestException(CommandsUtils.getExceptionMessage(e));
      }
   }

   public TaskRead updateWithReturn(Object entity, final String path, final HttpMethod verb, PrettyOutput... prettyOutput) {
      checkConnection();
      try {
         if (verb == HttpMethod.PUT) {
            ResponseEntity<String> response = restUpdate(path, entity);
            if (!validateAuthorization(response)) {
               return null;
            }
            return processResponse(response, HttpMethod.PUT, prettyOutput);
         } else {
            throw new Exception(Constants.HTTP_VERB_ERROR);
         }
      } catch (Exception e) {
         throw new CliRestException(CommandsUtils.getExceptionMessage(e));
      }
   }

   private ResponseEntity<String> restUpdate(String path, Object entityName) {
      String targetUri = hostUri + Constants.HTTPS_CONNECTION_API + path;

      HttpHeaders headers = buildHeaders();
      HttpEntity<Object> entity = new HttpEntity<Object>(entityName, headers);

      return client.exchange(targetUri, HttpMethod.PUT, entity, String.class);
   }

   @SuppressWarnings("rawtypes")
   private boolean validateAuthorization(ResponseEntity response) {
      if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
         System.out.println(Constants.CONNECT_UNAUTHORIZATION_OPT);
         return false;
      }
      return true;
   }

   /*
    * It will be trusted if users type 'yes' after CLI is aware of new SSL certificate. 
    */
   private static void trustSSLCertificate() {
      String errorMsg = "";
      try {
         KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
         TrustManagerFactory tmf =
               TrustManagerFactory.getInstance(TrustManagerFactory
                     .getDefaultAlgorithm());
         tmf.init(keyStore);
         SSLContext ctx = SSLContext.getInstance("SSL");
         ctx.init(new KeyManager[0],
               new TrustManager[] { new DefaultTrustManager(keyStore) },
               new SecureRandom());
         SSLContext.setDefault(ctx);
         HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String string, SSLSession ssls) {
               return true;
            }
         });
      } catch (KeyStoreException e) {
         errorMsg = "Key Store error: " + e.getMessage();
      } catch (KeyManagementException e) {
         errorMsg = "SSL Certificate error: " + e.getMessage();
      } catch (NoSuchAlgorithmException e) {
         errorMsg = "SSL Algorithm error: " + e.getMessage();
      } finally {
         if (!CommandsUtils.isBlank(errorMsg)) {
            System.out.println(errorMsg);
            logger.error(errorMsg);
         }
      }
   }

   private static class DefaultTrustManager implements X509TrustManager {

      private KeyStore keyStore;

      public DefaultTrustManager (KeyStore keyStore) {
         this.keyStore = keyStore;
      }

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
         String errorMsg = "";
         /*
          * load key store file 
          */
         char[] pwd = "changeit".toCharArray();
         InputStream in = null;
         OutputStream out = null;
         try {
            File file = new File("serengeti.keystore");
            if (file.isFile() == false) {
               char SEP = File.separatorChar;
               File dir =
                     new File(System.getProperty("java.home") + SEP + "lib"
                           + SEP + "security");
               file = new File(dir, "serengeti.keystore");
               if (file.isFile() == false) {
                  file = new File(dir, "cacerts");
               }
            }
            in = new FileInputStream(file);
            keyStore.load(in, pwd);
            /*
             * show certificate informations
             */
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            String md5Fingerprint = "";
            String sha1Fingerprint = "";
            SimpleDateFormat dateFormate = new SimpleDateFormat("yyyy/MM/dd");
            for (int i = 0; i < chain.length; i++) {
               X509Certificate cert = chain[i];
               sha1.update(cert.getEncoded());
               md5.update(cert.getEncoded());
               md5Fingerprint = toHexString(md5.digest());
               sha1Fingerprint = toHexString(sha1.digest());
               if (keyStore.getCertificate(md5Fingerprint) != null) {
                  if (i == chain.length - 1) {
                     return;
                  } else {
                     continue;
                  }
               }
               System.out.println("Certificate");
               System.out
                     .println("================================================================");
               System.out.println("Subject:  " + cert.getSubjectDN());
               System.out.println("Issuer:  " + cert.getIssuerDN());
               System.out.println("SHA Fingerprint:  " + sha1Fingerprint);
               System.out.println("MD5 Fingerprint:  " + md5Fingerprint);
               System.out.println("Issued on:  "
                     + dateFormate.format(cert.getNotBefore()));
               System.out.println("Expires on:  "
                     + dateFormate.format(cert.getNotAfter()));
               System.out.println("Signature:  " + cert.getSignature());
               System.out.println();

               ConsoleReader reader = new ConsoleReader();
               // Set prompt message
               reader.setPrompt(Constants.PARAM_PROMPT_ADD_CERTIFICATE_MESSAGE);
               // Read user input
               String readMsg = "";
               if (RunWayConfig.getRunType().equals(RunType.MANUAL)) {
                  readMsg = reader.readLine();
               } else {
                  readMsg = "yes";
               }
               if (!readMsg.trim().equalsIgnoreCase("yes")
                     && !readMsg.trim().equalsIgnoreCase("y")) {
                  if (i == chain.length - 1) {
                     throw new CertificateException(
                           "Not find a valid certificate.");
                  } else {
                     continue;
                  }
               }
               /*
                *  add new certificate into key store file.
                */
               keyStore.setCertificateEntry(md5Fingerprint, cert);
               out = new FileOutputStream("serengeti.keystore");
               keyStore.store(out, pwd);
            }

         } catch (FileNotFoundException e) {
            errorMsg = "Cannot find file warning: " + e.getMessage();
         } catch (NoSuchAlgorithmException e) {
            errorMsg = "SSL Algorithm error: " + e.getMessage();
         } catch (IOException e) {
            errorMsg = "SSL Algorithm error: " + e.getMessage();
         } catch (KeyStoreException e) {
            errorMsg = "Key store error: " + e.getMessage();
         } finally {
            if (!CommandsUtils.isBlank(errorMsg)) {
               System.out.println(errorMsg);
               logger.error(errorMsg);
            }
            if (in != null) {
               try {
                  in.close();
               } catch (IOException e) {
                  logger.warn("Input stream of serengeti.keystore close failed.");
               }
            }
            if (out != null) {
               try {
                  out.close();
               } catch (IOException e) {
                  logger.warn("Output stream of serengeti.keystore close failed.");
               }
            }
         }
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
         return null;
      }
   }

   private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

   /*
    * transfer a byte array to a hexadecimal string 
    */
   private static String toHexString(byte[] bytes) {
      StringBuilder sb = new StringBuilder(bytes.length * 3);
      for (int b : bytes) {
         b &= 0xff;
         sb.append(HEXDIGITS[b >> 4]);
         sb.append(HEXDIGITS[b & 15]);
         sb.append(':');
      }
      if (sb.length() > 0) {
         sb.delete(sb.length() - 1, sb.length());
      }
      return sb.toString().toUpperCase();
   }

}
