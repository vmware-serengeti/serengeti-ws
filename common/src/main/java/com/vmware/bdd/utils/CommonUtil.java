/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.StringUtil;
import com.vmware.bdd.apitypes.StorageRead;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.NetworkDnsType;
import com.vmware.bdd.exception.BddException;
import org.springframework.batch.core.scope.context.ChunkContext;

public class CommonUtil {

   static final Logger logger = Logger.getLogger(CommonUtil.class);

   public static String getConfDir() {
      String homeDir = System.getProperties().getProperty("serengeti.home.dir");
      if (StringUtil.isNullOrWhitespace(homeDir)) {
         homeDir = Constants.DEFAULT_SERENGETI_ROOT_DIR;
      }
      StringBuilder builder = new StringBuilder();
      builder.append(homeDir).append(File.separator).append("conf");
      return builder.toString();
   }

   public static File getConfigurationFile(final String filename, final String typeName) {
      // try to locate file directly
      File specFile = new File(filename);
      if (specFile.exists()) {
         return specFile;
      }

      // search ${serengeti.home.dir}/conf directory
      String homeDir = System.getProperties().getProperty("serengeti.home.dir");
      if (homeDir != null && !homeDir.trim().isEmpty()) {
         StringBuilder builder = new StringBuilder();
         builder.append(getConfDir()).append(File.separator).append(filename);
         specFile = new File(builder.toString());

         if (!specFile.exists()) {
            logger.warn(typeName + " file does not exist: " + builder);
         } else {
            return specFile;
         }
      }

      // search in class paths
      URL filePath = ConfigurationUtils.locate(filename);
      if (filePath != null) {
         specFile = ConfigurationUtils.fileFromURL(filePath);
      }

      if (!specFile.exists()) {
         String errorMsg = "Can not find file " + filename;
         logger.fatal(errorMsg);
         throw new RuntimeException(errorMsg);
      }

      return specFile;
   }

   public static String readJsonFile(URL fileURL) {
      StringBuilder jsonBuff = new StringBuilder();
      if (fileURL != null) {
         String fileName = fileURL.getFile();
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

   public static String readJsonFile(final String fileName) {
      URL fileURL = CommonUtil.class.getClassLoader().getResource(fileName);
      return readJsonFile(fileURL);
   }

   //TODO this is copied from CLI CommandsUtils
   public static String dataFromFile(String filePath) throws IOException,
         FileNotFoundException {
      StringBuffer dataStringBuffer = new StringBuffer();
      FileInputStream fis = null;
      InputStreamReader inputStreamReader = null;
      BufferedReader bufferedReader = null;
      try {
         fis = new FileInputStream(filePath);
         inputStreamReader = new InputStreamReader(fis, "UTF-8");
         bufferedReader = new BufferedReader(inputStreamReader);
         String line = "";
         while ((line = bufferedReader.readLine()) != null) {
            dataStringBuffer.append(line);
            dataStringBuffer.append("\n");
         }
      } finally {
         if (fis != null) {
            fis.close();
         }
         if (inputStreamReader != null) {
            inputStreamReader.close();
         }
         if (bufferedReader != null) {
            bufferedReader.close();
         }
      }
      return dataStringBuffer.toString();
   }


   public static void gracefulRackTopologyOutput(
         Map<String, String> racksTopology, String filename, String delimeter)
         throws Exception {
      List<Object> list = new ArrayList<Object>();

      if (racksTopology != null && racksTopology.size() > 0) {
         Iterator<Entry<String, String>> it =
               racksTopology.entrySet().iterator();
         Map.Entry<String, String> entry = null;
         String vmIP = "";
         String rackPath = "";
         while (it.hasNext()) {
            entry = (Map.Entry<String, String>) it.next();
            vmIP = entry.getKey();
            rackPath = entry.getValue();
            StringBuffer buff = new StringBuffer();
            list.add(buff.append(vmIP).append(" ").append(rackPath).toString());
         }
      }

      prettyOutputStrings(list, filename, delimeter);
   }

   /*
    * throws IO Exception
    */
   public static void prettyOutputStrings(List<Object> objs, String fileName, String delimeter) throws Exception {
      StringBuffer buff = new StringBuffer();
      if (isBlank(delimeter)) {
         delimeter = System.lineSeparator();
      }

      for (Object obj : objs) {
         if (obj != null) {
            String str = obj.toString();
            if (!isBlank(str)) {
               buff.append(str).append(delimeter);
            }
         }
      }
      if (buff.length() > 0) {
         buff.delete(buff.length() - delimeter.length(), buff.length());
      }

      OutputStream out = null;
      BufferedWriter bw = null;
      try {
         if (!isBlank(fileName)) {
            out = new FileOutputStream(fileName);
         } else {
            out = System.out;
         }
         bw = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
         bw.write(buff.toString());
         if (!isBlank(fileName)) {
            // [Bug 1406542] always append a newline at the end of the file
            bw.newLine();
         }
         bw.flush();
      } finally {
         if (bw != null && out != null && !(out instanceof PrintStream)) {
            bw.close();
            out.close();
         }
      }
   }

   public static List<String> inputsConvert(String inputs) {
      List<String> names = new ArrayList<String>();
      for (String s : inputs.split(",")) {
         if (!s.trim().isEmpty()) {
            names.add(s.trim());
         }
      }
      return names;
   }

   public static String inputsConvert(Set<String> words) {
      String newWordStr = "";
      if (words != null && !words.isEmpty()) {
         StringBuffer wordsBuff = new StringBuffer();
         for (String word : words) {
            wordsBuff.append(word).append(",");
         }
         wordsBuff.delete(wordsBuff.length()-1, wordsBuff.length());
         newWordStr = wordsBuff.toString();
      }
      return newWordStr;
   }

   public static <K, V> String inputsConvert(Map<K, V> wordsMap) {
      StringBuffer wordsBuff = new StringBuffer();
      if (wordsMap != null && !wordsMap.isEmpty()) {
         for (Entry<K, V> entry : wordsMap.entrySet()) {
            wordsBuff.append(entry.getKey()).append(":")
                  .append(entry.getValue()).append(",");
         }
         wordsBuff.delete(wordsBuff.length() - 1, wordsBuff.length());
      }
      return wordsBuff.toString();
   }

   public static boolean isBlank(final String str) {
      return str == null || str.trim().isEmpty();
   }

   public static String notNull(final String str, final String desStr) {
      return str == null ? desStr : str;
   }

   public static boolean validateVcResourceName(final String input) {
      return match(input, Constants.VC_RESOURCE_NAME_PATTERN);
   }

   public static boolean validateResourceName(final String input) {
      return match(input, Constants.RESOURCE_NAME_PATTERN);
   }

   public static boolean validateDistroName(final String input) {
      return match(input, Constants.DISTRO_NAME_PATTERN);
   }

   public static boolean validateClusterName(final String input) {
      return match(input, Constants.CLUSTER_NAME_PATTERN);
   }

   public static boolean validateNodeGroupName(final String input) {
      return match(input, Constants.NODE_GROUP_NAME_PATTERN);
   }

   public static boolean validataPathInfo(final String input) {
      return match(input, Constants.REST_REQUEST_PATH_INFO_PATTERN);
   }

   public static boolean validateVcDataStoreNames(List<String> names) {
      if (names == null || names.isEmpty()) {
         return false;
      }
      for (String name : names) {
         if (!validateVcDataStoreName(name)) {
            return false;
         }
      }
      return true;
   }

   private static boolean validateVcDataStoreName(final String input) {
      return match(input, Constants.VC_DATASTORE_NAME_PATTERN);
   }

   private static boolean match(final String input, final String regex) {
      Pattern pattern = Pattern.compile(regex);
      return pattern.matcher(input).matches();
   }

   public static boolean matchDatastorePattern(Set<String> patterns, Set<String> datastores) {
      for (String pattern : patterns) {
         // the datastore pattern should be converted to Java Regular Expression already
         for (String datastore : datastores) {
            try {
               if (datastore.matches(pattern)) {
                  return true;
               }
            } catch (Exception e) {
               logger.error(e.getMessage());
               continue;
            }
         }
      }
      return false;
   }

   public static String escapePattern(String pattern) {
      return pattern.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
   }

   public static String getDatastoreJavaPattern(String pattern) {
      return escapePattern(pattern).replace("?", ".").replace("*", ".*");
   }

   public static String getUUID() {
      UUID uuid = UUID.randomUUID();
      return uuid.toString();
   }

   public static long makeVmMemoryDivisibleBy4(long memory) {
      if ((memory % 4) == 0) {
         return memory;
      } else {
         long temp = memory / 4;
         return temp * 4;
      }
   }

   public static boolean validateClusterPassword(String password) {
      Pattern pattern = Pattern.compile("((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[_@#$%^&*]).{8,20})");
      if (pattern.matcher(password).matches()) {
         return true;
      }
      return false;
   }

   public static boolean passwordContainInvalidCharacter(String password) {
      Pattern pattern = Pattern.compile("[a-zA-Z0-9_@#$%^&*]+");
      if (!pattern.matcher(password).matches()) {
         return true;
      }
      return false;
   }

   public static char[] allChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*_=+-/".toCharArray();
   public static String randomString(int count) {
      return RandomStringUtils.random(count, allChars);
   }

   public static String getClusterName(String vmName) throws BddException {
      String[] split = splitVmName(vmName);
      return split[0];
   }

   public static long getVmIndex(String vmName) throws BddException {
      String[] split = splitVmName(vmName);
      try {
         return Long.valueOf(split[2]);
      } catch (Exception e) {
         logger.error("vm name " + vmName
               + " violate serengeti vm name definition.");
         throw BddException.VM_NAME_VIOLATE_NAME_PATTERN(vmName);
      }
   }

   private static String[] splitVmName(String vmName) {
      String[] split = vmName.split("-");
      if (split.length < 3) {
         throw BddException.VM_NAME_VIOLATE_NAME_PATTERN(vmName);
      }
      return split;
   }

   public static Process execCommand(String cmd) {
      if (cmd == null || cmd.isEmpty()) {
         return null;
      }

      Process p = null;
      try {
         p = new ProcessBuilder(Arrays.asList(cmd.split(" "))).start();
         p.waitFor();
      } catch (Exception e) {
         p = null;
         logger.error("Failed to execute command " + cmd + " : " + e.getMessage());
      }

      return p;
   }

   public static String decode(final String paramName) {
      try {
         return URLDecoder.decode(paramName, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         logger.error(e.getMessage(), e);
         return paramName;
      }
   }

   public static String encode(final String paramName) {
      try {
         return URLEncoder.encode(paramName, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         logger.error(e.getMessage(), e);
         return paramName;
      }
   }

   public static String getCurrentTimestamp() {
      return "[" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Calendar.getInstance().getTime()) + "]";
   }

   private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

   /*
    * transfer a byte array to a hexadecimal string
    */
   public static String toHexString(byte[] bytes) {
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

   public static KeyStore loadAppMgrKeyStore(String keystorePath) {
      File file =
            new File(keystorePath
                  + Constants.APPMANAGER_KEYSTORE_FILE);
      if (file.isFile() == false) {
         char SEP = File.separatorChar;
         File dir =
               new File(System.getProperty("java.home") + SEP + "lib" + SEP
                     + "security");
         file = new File(dir, Constants.APPMANAGER_KEYSTORE_FILE);
         if (file.isFile() == false) {
            file = new File(dir, "cacerts");
         }
      }

      KeyStore keyStore = null;
      try {
         keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      } catch (KeyStoreException e) {
         logger.error("Can't get KeyStore instance. ", e);
         return null;
      }
      InputStream in = null;
      try {
         in = new FileInputStream(file);
         keyStore.load(in, Constants.APPMANAGER_KEYSTORE_PASSWORD);
      } catch (FileNotFoundException e) {
         logger.error("Can't find file " + file.getAbsolutePath(), e);
         return null;
      } catch (NoSuchAlgorithmException e) {
         logger.error("No such algorithm error during loading keystore.", e);
         return null;
      } catch (CertificateException e) {
         logger.error("Certificate exception during loading keystore.", e);
         return null;
      } catch (IOException e) {
         logger.error("Caught IO Exception.", e);
         return null;
      } finally {
         if (in != null) {
            try {
               in.close();
            } catch (IOException e) {
               logger.warn("Input stream of appmanagers.jks close failed.");
            }
         }
      }
      return keyStore;
   }

   public static boolean validateUrl(String url, List<String> errorMsgs) {
      if (errorMsgs == null) {
         errorMsgs = new ArrayList<String>();
      }
      boolean result = true;
      try {
         URI uri = new URI(url);
         String schema = uri.getScheme();
         if (!"https".equalsIgnoreCase(schema) && !"http".equalsIgnoreCase(schema)) {
            errorMsgs.add("URL should starts with http or https");
            result = false;
         }
         if ("https".equalsIgnoreCase(schema) && uri.getHost().matches(Constants.IP_PATTERN)) {
            errorMsgs.add("You should use FQDN instead of ip address when using https protocol");
            result = false;
         }
      } catch (URISyntaxException e) {
         logger.error("invalid URL syntax ", e);
         errorMsgs.add("invalid URL syntax");
         return false;
      }

      return result;
   }

   public static String mergeErrorMsgList(List<String> errorMsgs) {
      StringBuilder errorMsg = new StringBuilder();
      for (String msg : errorMsgs) {
         errorMsg.append(msg);
         errorMsg.append(", ");
      }
      return errorMsg.substring(0, errorMsg.length() - 2);
   }

   /**
   *
   * @param host
   * @param port
   * @param waitTime
   */
  public static boolean checkServerConnection(final String host, final int port, int waitTime) {
     boolean connectResult = false;

     // validate the server is reachable
     ExecutorService exec = Executors.newFixedThreadPool(1);
     try {
        // if the target ip does not exist or the host is shutdown, it will take about 2 minutes
        // for the socket connection to time out.
        // here we fork a child thread to do the actual connecting action, if it does not succeed
        // within given waitTime, we will consider it to be failure.
        Future<Boolean> futureResult = exec.submit(new Callable<Boolean>(){
           @Override
           public Boolean call() throws Exception {
              try {
                 new Socket(host, port);
                 return true;
              } catch (Exception e) {
              }
              return false;
           }
        });

        // wait for the thread to finish within given waitTime
        Boolean result = (Boolean)waitForThreadResult(futureResult, waitTime);

        // result==null means the time out occurs
        if ( null != result ) {
           connectResult = result;
        }
     } catch (Exception e) {
        logger.error("Unexpected error occurred with threading.");
     } finally {
        if ( null != exec ) {
           exec.shutdown();
        }
     }

     return connectResult;
  }

  public static Object waitForThreadResult(Future<?> result, int waitTime) {
     for ( int i=0; i<waitTime; i++ ) {
        try {
           if ( result.isDone() ) {
              return result.get();
           }

           // sleep 1 second here
           Thread.sleep(1000);
        } catch (Exception e) {
           logger.error("Unexpected error occurred with threading.");
        }
     }

     // time out now
     return null;
  }

  public static boolean validateDnsType(NetworkDnsType dnsType){
     if(NetworkDnsType.isDynamic(dnsType) || NetworkDnsType.isNormal(dnsType) || NetworkDnsType.isOthers(dnsType)) {
        return true;
     } else {
        return false;
     }
  }

   public static void writeFile(File file, String content) {
      writeFile(file, content, false);
   }

   public static void writeFile(File file, String content, boolean append) {
      OutputStream out = null;
      BufferedWriter bw = null;
      try {
         out = new FileOutputStream(file, append);
         bw = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
         bw.write(content);
         bw.flush();
      } catch (IOException e) {
         logger.warn("Write file failed : " + e.getMessage());
      } finally {
         if (bw != null && out != null) {
            try {
               bw.close();
               out.close();
            } catch (IOException e) {
               logger.warn("Close file failed : " + e.getMessage());
            }
         }
      }
   }
  public static boolean getBooleanFromString(String booleanString, boolean defaultValue) {
     boolean ret = defaultValue;
     if (!StringUtils.isBlank(booleanString)) {
        ret = Boolean.valueOf(booleanString);
     }
     return ret;
  }

   public static String getCustomizedSudoCmd() {
      return Configuration.getString(Constants.SUDO_COMMAND, Constants.DEFAULT_SUDO_COMMAND);
   }

   public static StorageRead.DiskScsiControllerType getSystemAndSwapControllerType() {
      String controllerType;
      try {
         controllerType = Configuration.getString(Constants.SYSTEM_AND_SWAP_DISK_CONTROLLER_TYPE);
      } catch (NoSuchElementException e) {
         controllerType = null;
      }
      if (controllerType == null || controllerType.equalsIgnoreCase(StorageRead.DiskScsiControllerType.LSI_CONTROLLER.getDisplayName())) {
         return StorageRead.DiskScsiControllerType.LSI_CONTROLLER;
      } else if (controllerType.equalsIgnoreCase(StorageRead.DiskScsiControllerType.PARA_VIRTUAL_CONTROLLER.getDisplayName())) {
         return StorageRead.DiskScsiControllerType.PARA_VIRTUAL_CONTROLLER;
      } else {
         throw BddException.INVALID_PARAMETER(Constants.SYSTEM_AND_SWAP_DISK_CONTROLLER_TYPE, controllerType);
      }
   }

}
