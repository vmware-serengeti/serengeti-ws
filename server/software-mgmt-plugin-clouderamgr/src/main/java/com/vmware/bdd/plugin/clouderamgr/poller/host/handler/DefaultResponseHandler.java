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
package com.vmware.bdd.plugin.clouderamgr.poller.host.handler;

import com.vmware.bdd.plugin.clouderamgr.poller.host.parser.IDetailsParser;
import com.vmware.bdd.plugin.clouderamgr.poller.host.parser.ParseResult;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Author: Xiaoding Bian
 * Date: 6/26/14
 * Time: 7:13 PM
 */
public class DefaultResponseHandler implements ResponseHandler<ParseResult> {
   private IDetailsParser contentParser;

   public DefaultResponseHandler() {}

   public DefaultResponseHandler(IDetailsParser parser) {
      this.contentParser = parser;
   }

   @Override
   public ParseResult handleResponse(HttpResponse response) throws IOException {

      StatusLine statusLine = response.getStatusLine();
      HttpEntity entity = response.getEntity();
      if (statusLine.getStatusCode() >= 300) {
         throw new HttpResponseException(
               statusLine.getStatusCode(),
               statusLine.getReasonPhrase());
      }
      if (entity == null) {
         throw new ClientProtocolException("Response contains no content");
      }

      return contentParser.parse(EntityUtils.toString(entity));
   }
}
