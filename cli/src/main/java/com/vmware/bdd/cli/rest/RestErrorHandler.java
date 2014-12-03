/******************************************************************************
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
 ******************************************************************************/
package com.vmware.bdd.cli.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseErrorHandler;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.utils.CommonUtil;

public class RestErrorHandler implements ResponseErrorHandler {

   static final Logger logger = Logger.getLogger(RestErrorHandler.class);
   private List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
   private final HttpMessageConverterExtractor<BddErrorMessage> delegate;

   RestErrorHandler() {
      messageConverters.add(new MappingJacksonHttpMessageConverter());
      delegate = new HttpMessageConverterExtractor<BddErrorMessage>(BddErrorMessage.class, messageConverters);
   }

   public boolean hasError(ClientHttpResponse response) throws IOException {
      HttpStatus statusCode = response.getStatusCode();
      if (statusCode.series() == Series.CLIENT_ERROR || statusCode.series() == Series.SERVER_ERROR) {
         return true;
      } else {
         return false;
      }
   }

   public void handleError(ClientHttpResponse response) throws IOException {
      MediaType contentType = response.getHeaders().getContentType();
      if (MediaType.APPLICATION_JSON.includes(contentType)) {
         throw new CliRestException(delegate.extractData(response).getMessage());
      } else {
         HttpStatus statusCode = response.getStatusCode();
         String errorMsg = "";
         if (statusCode == HttpStatus.UNAUTHORIZED) {
            errorMsg = Constants.CONNECT_SESSION_TIME_OUT;
         } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
            errorMsg =
                  "vCenter Server connect command failed: "
                        + getVCConnectErrorMsg(response.getBody());
         } else {
            errorMsg = statusCode.getReasonPhrase();
         }

         throw new CliRestException(statusCode, errorMsg);
      }
   }

   private String getVCConnectErrorMsg(InputStream is) {
      StringBuilder buffer = new StringBuilder();
      InputStreamReader inputStreamReader = null;
      BufferedReader bufferedReader = null;
      try {
         inputStreamReader = new InputStreamReader(is);
         bufferedReader = new BufferedReader(inputStreamReader);
         String line = "";
         while ((line = bufferedReader.readLine()) != null) {
            buffer.append(line);
            buffer.append("\n");
         }

      } catch (IOException e) {
         logger.error("Read response i/o stream failed: " + e.getMessage());
      } finally {
         if (is != null) {
            try {
               is.close();
            } catch (IOException e) {
               logger.error("Close response i/o stream failed: "
                     + e.getMessage());
            }
         }
         if (inputStreamReader != null) {
            try {
               inputStreamReader.close();
            } catch (IOException e) {
               logger.error("Close response i/o stream reader failed: "
                     + e.getMessage());
            }
         }
         if (bufferedReader != null) {
            try {
               bufferedReader.close();
            } catch (IOException e) {
               logger.error("Close response i/o buffered reader failed: "
                     + e.getMessage());
            }
         }
      }
      return findErrorMsg(buffer.toString(), "(Connection(.)*</h1>)+");
   }

   private static String findErrorMsg(final String input, final String regex) {
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(input);
      String str = "";
      if (matcher.find()) {
         str = matcher.group();
      }
      if (!CommonUtil.isBlank(str) && str.contains("</h1>")) {
         str = str.replace("</h1>", ".");
      }
      return str;
   }

}
