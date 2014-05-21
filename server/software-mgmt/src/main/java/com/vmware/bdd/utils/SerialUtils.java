package com.vmware.bdd.utils;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 3:50 PM
 */
public class SerialUtils {
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

   private static ObjectMapper getMapper() {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
      return mapper;
   }
}
