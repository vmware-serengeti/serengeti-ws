/******************************************************************************
 *       Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *      Licensed under the Apache License, Version 2.0 (the "License");
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

public class RestErrorHandler implements ResponseErrorHandler {
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
      if (contentType == MediaType.APPLICATION_JSON) {
         throw new CliRestException(delegate.extractData(response).getMessage());
      } else {
         HttpStatus statusCode = response.getStatusCode();
         String errorMsg="";
         if (statusCode == HttpStatus.UNAUTHORIZED) {
            errorMsg = Constants.CONNECT_SESSION_TIME_OUT;
         } else {
            errorMsg = statusCode.getReasonPhrase();
         }
         throw new CliRestException(statusCode, errorMsg);
      }
   }
}
