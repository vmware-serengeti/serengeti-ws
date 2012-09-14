/*****************************************************************************
 *   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.impl.Indenter;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.codehaus.jackson.util.DefaultPrettyPrinter.Lf2SpacesIndenter;

import com.vmware.bdd.utils.CommonUtil;


public class CommandsUtils {

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

      StringBuffer dataStringsb = new StringBuffer();
      FileReader fileReader = null;
      BufferedReader reader = null;
      try {
         File f = new File(filePath);
         fileReader = new FileReader(f);
         reader = new BufferedReader(fileReader);
         String line = "";
         while ((line = reader.readLine()) != null) {
            dataStringsb.append(line);
            dataStringsb.append("\n");
         }
      } finally {
         if (reader != null) {
            reader.close();
         }
         if (fileReader != null) {
            fileReader.close();
         }
      }
      return dataStringsb.toString();
   }

   public static <T> T getObjectByJsonString(Class<T> entityType, String jsonString) throws JsonParseException,
         JsonMappingException, IOException {
      ObjectMapper mapper = getMapper();
      T NodeGroupsCreate = null;
      NodeGroupsCreate = mapper.readValue(jsonString, entityType);
      return NodeGroupsCreate;
   }

   public static void prettyJsonOutput(Object object, String fileName) 
   throws JsonParseException, JsonMappingException, IOException {
      OutputStream out = null;
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
      if (!isBlank(name)) {
         System.out.println(objectType + " " + name + " " + result);
      } else {
         System.out.println(objectType + " " + result);
      }
   }

   public static void printCmdFailure(String objectType, String name,
         String opName, String result, String message) {
      if (!isBlank(name)) {
         System.out.println(objectType + " " + name + " " + opName + " "
               + result + ": " + message);
      } else if(!isBlank(opName)) {
         System.out.println(objectType + " " + opName + " " + result + ": "
               + message);
      }else{
         System.out.println(objectType + " " + result + ": "
               + message);
      }
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
         if (!file.exists()){
            return null;
         }
         fis = new FileInputStream(propertiesFilePath);
         properties.load(fis);
         fis.close();
         return properties;
      } catch (IOException e) {
         System.out.println(e.getMessage());
         if (fis != null) {
        	 try {
        		 fis.close();
        	 } catch (IOException e1) {
        		 System.out.println(e1.getMessage());
        	 }
         }
         return null;
      } 
   }

   public static void writeProperties(Properties properties,
         String propertiesFilePath) {
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(propertiesFilePath);
         properties.store(fos, "");
         fos.close();
      } catch (IOException e) {
         System.out.println(e.getMessage());
         if (fos != null) {
            try {
               fos.close();
            } catch (IOException e1) {
               System.out.println(e1.getMessage());
            }
         }
      }
   }

}
