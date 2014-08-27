/*****************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
 ****************************************************************************/
package com.vmware.bdd.cli.commands;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import jline.console.ConsoleReader;
import jline.internal.Configuration;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.impl.Indenter;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.codehaus.jackson.util.DefaultPrettyPrinter.Lf2SpacesIndenter;
import org.fusesource.jansi.internal.Kernel32;
import org.springframework.shell.core.JLineShell;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.util.ClassUtils;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.utils.CommonUtil;


public class CommandsUtils {

   static final Logger logger = Logger.getLogger(CommandsUtils.class);

   public static List<String> inputsConvert(String inputs) {

      return CommonUtil.inputsConvert(inputs);
   }

   public static String prettyRoleOutput(List<String> roles,
         final String delimiter) {
      StringBuilder roleStr = new StringBuilder();
      if (roles != null) {
         for (String role : roles) {
            roleStr.append(role + delimiter);
         }
      }

      if (roleStr.length() > 0) {
         roleStr.deleteCharAt(roleStr.length() - 1);
      }
      return roleStr.toString();
   }

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

   public static <T> T getObjectByJsonString(Class<T> entityType, String jsonString) throws JsonParseException,
         JsonMappingException, IOException {
      ObjectMapper mapper = getMapper();
      T mappedObject = null;
      mappedObject = mapper.readValue(jsonString, entityType);
      return mappedObject;
   }

   public static void prettyJsonOutput(Object object, String fileName)
         throws Exception {
      OutputStream out = null;
      try {
         if (fileName != null) {
            out = new FileOutputStream(fileName);
         } else {
            out = System.out;
         }
         JsonFactory factory = new JsonFactory();
         JsonGenerator generator = factory.createJsonGenerator(out);
         ObjectMapper mapper = getMapper();
         mapper.setSerializationInclusion(Inclusion.NON_NULL);
         generator.setCodec(mapper);
         DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
         Indenter indenter = new Lf2SpacesIndenter();
         prettyPrinter.indentArraysWith(indenter);
         generator.setPrettyPrinter(prettyPrinter);
         generator.writeObject(object);
         writeEndingMsgToScreen(fileName);
      } finally {
         if (out != null && !(out instanceof PrintStream)) {
            out.close();
         }
      }
   }

   /*
    * Determine if it is the window OS
    */
   public static boolean isJansiAvailable() {
      return ClassUtils.isPresent("org.fusesource.jansi.AnsiConsole",
            JLineShell.class.getClassLoader())
            && OsUtils.isWindows()
            && System.getProperty("jline.terminal") == null;
   }

   /**
    * Check whether the given String has actual text. More specifically, returns
    * <code>false</code> if the string not <code>null</code>, its length is
    * greater than 0, and it contains at least one non-whitespace character.
    * <p>
    *
    * <pre>
    * CommandsUtils.isBlank(null) = true
    * CommandsUtils.isBlank("") = true
    * CommandsUtils.isBlank(" ") = true
    * CommandsUtils.isBlank("12345") = false
    * CommandsUtils.isBlank(" 12345 ") = false
    * </pre>
    *
    * @param str
    *           the String to check(may be null).
    * @return the opposite of
    */
   public static boolean isBlank(final String str) {
      return CommonUtil.isBlank(str);
   }

   public static String notNull(final String str, final String desStr) {
      return CommonUtil.notNull(str, desStr);
   }

   /**
    * Show a table(include table column names and table contents) by left
    * justifying. More specifically, the {@code columnNamesWithGetMethodNames}
    * argument is a map struct, the key is table column name and value is method
    * name list which it will be invoked by reflection. The {@code entities}
    * argument is traversed entity array.It is source of table data. In
    * addition,the method name must be each of the {@code entities} argument 's
    * member. The {@code spacesBeforeStart} argument is whitespace in the front
    * of the row.
    * <p>
    *
    * @param columnNamesWithGetMethodNames
    *           the container of table column name and invoked method name.
    * @param entities
    *           the traversed entity array.
    * @param spacesBeforeStart
    *           the whitespace in the front of the row.
    * @throws Exception
    */
   public static void printInTableFormat(
         LinkedHashMap<String, List<String>> columnNamesWithGetMethodNames,
         Object[] entities, String spacesBeforeStart) throws Exception {
      if (entities != null && entities.length > 0) {
         // get number of columns
         int columnNum = columnNamesWithGetMethodNames.size();

         String[][] table = new String[entities.length + 1][columnNum];

         //build table header: column names
         String[] tableHeader = new String[columnNum];
         Set<String> columnNames = columnNamesWithGetMethodNames.keySet();
         columnNames.toArray(tableHeader);

         //put table column names into the first row
         table[0] = tableHeader;

         //build table contents
         Collection<List<String>> getMethodNamesCollect =
               columnNamesWithGetMethodNames.values();
         int i = 1;
         for (Object entity : entities) {
            int j = 0;
            for (List<String> getMethodNames : getMethodNamesCollect) {
               Object tempValue = null;
               int k = 0;
               for (String methodName : getMethodNames) {
                  if (tempValue == null)
                     tempValue = entity;
                  Object value =
                        tempValue.getClass().getMethod(methodName)
                              .invoke(tempValue);
                  if (k == getMethodNames.size() - 1) {
                     table[i][j] =
                           value == null ? ""
                                 : ((value instanceof Double) ? String
                                       .valueOf(round(
                                             ((Double) value).doubleValue(), 2,
                                             BigDecimal.ROUND_FLOOR)) : value
                                       .toString());
                     if (isJansiAvailable() && !isBlank(table[i][j])) {
                        table[i][j] = transferEncoding(table[i][j]);
                     }
                     j++;
                  } else {
                     tempValue = value;
                     k++;
                  }
               }
            }
            i++;
         }

         printTable(table, spacesBeforeStart);
      }
   }

   private static void printTable(String[][] table, String spacesBeforeStart) {
      // find the maximum length of a string in each column
      int numOfColumns = table[0].length;
      int[] lengths = new int[numOfColumns];

      for (int i = 0; i < table.length; i++) {
         for (int j = 0; j < numOfColumns; j++) {
            if (table[i][j] != null) {
               lengths[j] = Math.max(table[i][j].length(), lengths[j]);
            }
         }
      }

      // generate a format string for each column
      String[] formats = new String[numOfColumns];

      for (int i = 0; i < lengths.length; i++) {
         lengths[i] +=
               (i + 1 == numOfColumns) ? 0 : Constants.FORMAT_COLUMN_DISTANCE;
         formats[i] =
               "%1$-" + lengths[i] + "s"
                     + (i + 1 == lengths.length ? "\n" : "");
      }

      // print out
      for (int i = 0; i < table.length; i++) {
         System.out.print(spacesBeforeStart);
         //print '------'
         if (i == 1) {
            StringBuilder outputBuffer = new StringBuilder();
            for (int l : lengths) {
               for (int k = 0; k < l; k++)
                  outputBuffer.append("-");
            }
            outputBuffer.append("\n").append(spacesBeforeStart);
            System.out.print(outputBuffer.toString());
         }
         for (int j = 0; j < table[i].length; j++) {
            System.out.printf(formats[j], table[i][j]);
         }
      }

      System.out.println();
   }

   public static void printCmdSuccess(String objectType, String name,
         String result) {
      if (isJansiAvailable() && !isBlank(name)) {
         try {
            name = transferEncoding(name);
         } catch (UnsupportedEncodingException|CliException e) {
            logger.warn("failed to transferEncoding: " + e.getMessage());
         }
      }
      if (!isBlank(name)) {
         System.out.println(objectType + " " + name + " " + result);
      } else {
         System.out.println(objectType + " " + result);
      }
   }

   public static void printCmdFailure(String objectType, String name,
         String opName, String result, String message) {
      if (isJansiAvailable() && !isBlank(name)) {
         try {
            name = transferEncoding(name);
         } catch (UnsupportedEncodingException|CliException e) {
            logger.warn("failed to transferEncoding: " + e.getMessage());
         }
      }
      if (!isBlank(name)) {
         System.out.println(objectType + " " + name + " " + opName + " "
               + result + ": " + message);
      } else if (!isBlank(opName)) {
         System.out.println(objectType + " " + opName + " " + result + ": "
               + message);
      } else {
         System.out.println(objectType + " " + result + ": " + message);
      }
   }

   public static void prettyOutputErrorNode(List<NodeGroupRead> nodegroups)
         throws Exception {
      List<NodeRead> failedNodes = new ArrayList<NodeRead>();
      for (NodeGroupRead nodegroup : nodegroups) {
         List<NodeRead> nodes = nodegroup.getInstances();
         if (nodes != null) {
            for (NodeRead node : nodes) {
               if (node.isActionFailed()) {
                  failedNodes.add(node);
               }
            }
         }
      }

      if (!failedNodes.isEmpty()) {
         System.out.println();
         System.out
         .println(Constants.FAILED_NODES_MESSAGE + failedNodes.size());
         CommandsUtils.printSeperator();
         for (NodeRead failedNode : failedNodes) {
            System.out.println(" [" + Constants.FORMAT_TABLE_COLUMN_NAME + "] " + failedNode.getName());
            System.out.println(" [" + Constants.FORMAT_TABLE_COLUMN_STATUS + "] " + failedNode.getStatus());
            System.out.println(" [" + Constants.FORMAT_TABLE_COLUMN_ERROR + "] " + failedNode.getErrMessage());
            CommandsUtils.printSeperator();;
         }
         System.out.println();
      }
   }

   private static void printSeperator() {
      StringBuffer seperator =
            new StringBuffer().append(Constants.OUTPUT_INDENT);
      for (int i = 0; i < Constants.SEPERATOR_LEN; i++) {
         seperator.append("-");
      }
      System.out.println(seperator.toString());
   }

   /**
    * Take the accuracy of double data.
    * <p>
    * For example: <br>
    * A double value = 100.345678; <br>
    * The Double ret = round (value, 4, BigDecimal.ROUND_HALF_UP); <br>
    * "Ret 100.3457 <br>
    *
    * @param value
    *           Double data value. @param scale Precision digits (reserve of
    *           decimal digits).
    * @param roundingMode
    *           Precision value way.
    * @return Precision calculation of data.
    */
   private static double round(double value, int scale, int roundingMode) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(scale, roundingMode);
      double d = bd.doubleValue();
      return d;
   }

   private static ObjectMapper getMapper() {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
      return mapper;
   }

   public static Properties readProperties(String propertiesFilePath) {
      Properties properties = new Properties();
      FileInputStream fis = null;
      try {
         File file = new File(propertiesFilePath);
         if (!file.exists()) {
            return null;
         }
         fis = new FileInputStream(propertiesFilePath);
         properties.load(fis);
         return properties;
      } catch (IOException e) {
         System.out.println(e.getMessage());
         return null;
      } finally {
         if (fis != null) {
            try {
               fis.close();
            } catch (IOException e) {
               System.out.println(e.getMessage());
            }
         }
      }
   }

   public static void writeProperties(Properties properties,
         String propertiesFilePath) {
      FileOutputStream fos = null;
      try {
         Properties prop = null;
         File file = new File(propertiesFilePath);
         if (file.exists()) {
            prop = new Properties();
            prop.load(new FileInputStream(propertiesFilePath));
            prop.putAll(properties);
         } else {
            prop = properties;
         }
         fos = new FileOutputStream(propertiesFilePath);
         prop.store(fos, "");
      } catch (IOException e) {
         System.out.println(e.getMessage());
      } finally {
         if (fos != null) {
            try {
               fos.close();
            } catch (IOException e1) {
               System.out.println(e1.getMessage());
            }
         }
      }
   }

   public static boolean showWarningMsg(final String name,
         final String targetObject, final String operateType,
         final List<String> warningMsgList, final boolean alwaysAnswerYes) {
      if (warningMsgList != null && !warningMsgList.isEmpty()) {
         for (String message : warningMsgList) {
            System.out.println(message);
         }
         if (!isContinue(name, targetObject, operateType, Constants.PARAM_PROMPT_CONTINUE_MESSAGE, alwaysAnswerYes)) {
            return false;
         }
      }
      return true;
   }

   private static boolean isContinue(final String name,
         final String targetObject, final String operateType,
         final String promptMsg, final boolean alwaysAnswerYes) {
      if (alwaysAnswerYes) {
         return true;
      }

      boolean continueCreate = true;
      boolean continueLoop = true;
      String readMsg = "";
      try {
         ConsoleReader reader = new ConsoleReader();
         // Set prompt message
         reader.setPrompt(promptMsg);
         int k = 0;
         while (continueLoop) {
            if (k >= 3) {
               continueCreate = false;
               break;
            }
            // Read user input
            readMsg = reader.readLine();
            if (readMsg.trim().equalsIgnoreCase("yes")
                  || readMsg.trim().equalsIgnoreCase("y")) {
               continueLoop = false;
            } else if (readMsg.trim().equalsIgnoreCase("no")
                  || readMsg.trim().equalsIgnoreCase("n")) {
               continueLoop = false;
               continueCreate = false;
            } else {
               k++;
            }
         }
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(targetObject,
               name, operateType,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         continueCreate = false;
      }
      return continueCreate;
   }

   public static String getExceptionMessage(Exception e) {
      if (e.getCause() == null) {
         return e.getMessage();
      } else {
         return getRootCause(e).getMessage();
      }
   }

   private static Throwable getRootCause(Exception e) {
      Throwable cause = e.getCause();
      Throwable rootCause = null;
      while (cause != null) {
         rootCause = cause;
         cause = cause.getCause();
      }
      return rootCause;
   }

   /*
    * Transfer terminal output encoding to system encoding.
    * It only take effect on windows OS.
    */
   public static String transferEncoding(final String src)
         throws UnsupportedEncodingException, CliException {
      //      Return CMD output code page.
      int codePage = Kernel32.GetConsoleOutputCP();
      String outputEncoding = "ms" + codePage;
      if (!java.nio.charset.Charset.isSupported(outputEncoding)) {
         outputEncoding = "cp" + codePage;
         if (!java.nio.charset.Charset.isSupported(outputEncoding)) {
            String errorMsg =
                  "Cannot figure out the Java Charset of this code page ("
                        + codePage + ")...";
            logger.error("CommandsUtils::transferEncoding: " + errorMsg);
            throw new CliException(errorMsg);
         }
      }
      return new String(src.getBytes(outputEncoding),
            Configuration.getEncoding());
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

   public static void prettyOutputStrings(List<Object> objs, String fileName, String delimeter) throws Exception {
      StringBuffer buff = new StringBuffer();
      if (CommonUtil.isBlank(delimeter)) {
         delimeter = "\n";
      }

      for (Object obj : objs) {
         if (obj != null) {
            String str = obj.toString();
            if (!CommandsUtils.isBlank(str)) {
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
         bw.flush();
         writeEndingMsgToScreen(fileName);
      } finally {
         if (bw != null && out != null && !(out instanceof PrintStream)) {
            bw.close();
            out.close();
         }
      }
   }

   private static void writeEndingMsgToScreen(String fileName) throws Exception {
      if (fileName == null) {
         System.out.println();
      } else {
         File file = new File(fileName);
         String filePath = file.getAbsolutePath();
         if (isJansiAvailable() && !isBlank(filePath)) {
            filePath = transferEncoding(filePath);
         }
         System.out.println("Exported to file " + filePath);
      }
   }

   public enum PromptType {
      USER_NAME, PASSWORD
   }

   public static boolean prompt(String msg, PromptType promptType,
         Map<String, String> loginInfo) throws Exception {
      int k = 0;
      String enter = "";
      while (k < 3) {
         enter = readEnter(msg, promptType);
         if (!CommandsUtils.isBlank(enter)) {
            if (promptType == PromptType.USER_NAME) {
               loginInfo.put(Constants.LOGIN_USERNAME, enter);
            } else {
               loginInfo.put(Constants.LOGIN_PASSWORD, enter);
            }
            break;
         } else {
            StringBuilder warningMsg = new StringBuilder();
            if (promptType == PromptType.USER_NAME) {
               warningMsg.append(Constants.CONNECT_USER_NAME);
            } else {
               warningMsg.append(Constants.CONNECT_PASSWORD);
            }
            warningMsg.append(Constants.CONNECT_CAN_NOT_BE_NULL);
            System.out.println(warningMsg.toString());
         }
         k++;
      }
      return k < 3;
   }

   private static String readEnter(String msg, PromptType promptType) throws Exception {
      String enter = "";
      ConsoleReader reader = new ConsoleReader();
      reader.setPrompt(msg);
      if (promptType == PromptType.USER_NAME) {
         enter = reader.readLine();
      } else if (promptType == PromptType.PASSWORD) {
         enter = reader.readLine(Character.valueOf('*'));
      }
      return enter;
   }
}
