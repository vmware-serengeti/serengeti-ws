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
package com.vmware.bdd.plugin.clouderamgr.poller.host.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Author: Xiaoding Bian
 * Date: 6/26/14
 * Time: 6:36 PM
 */
public class HostInstallDetailsParser implements IDetailsParser{

   @Override
   public ParseResult parse(String pageContent) {

      Document doc = Jsoup.parse(pageContent);
      ParseResult result = new ParseResult();

      for (Element item : doc.getElementsByClass("wrap")) {
         if (item.hasAttr("style")) {
            //result.put(PROGRESS_KEY, Integer.valueOf(item.attr("style").split("\\s+")[1].replace("%", "")));
         }
         for (Element subItem : item.getElementsByClass("message")) {
            result.setMessage(subItem.text());
         }
      }

      for (Element item : doc.getElementsByClass("progress")) {
         for (Element subItem : item.getElementsByClass("bar")) {
            if (subItem.hasAttr("style")) {
               result.setPercent(Integer.valueOf(subItem.attr("style").split("\\s+")[1].replace("%", "")));
            }
         }
      }
      return result;
   }
}
