/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

package com.vmware.aurora.composition;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Utility Class for the all Schemas
 *
 * @author sridharr
 *
 */
public class SchemaUtil {

   @SuppressWarnings("unchecked")
   public static <T> T getSchema(String xmlSchema, Class<T> clazz)
         throws JAXBException {
      JAXBContext context = JAXBContext.newInstance(clazz);
      Unmarshaller um = context.createUnmarshaller();
      StringReader input = new StringReader(xmlSchema);
      return (T) um.unmarshal(input);
   };

   @SuppressWarnings("unchecked")
   public static <T> T getSchema(File file, Class<T> clazz)
         throws JAXBException {
      JAXBContext context = JAXBContext.newInstance(clazz);
      Unmarshaller um = context.createUnmarshaller();
      return (T) um.unmarshal(file);
   }

   public static <T> String getXmlString(T jaxbObject) throws JAXBException {
      StringWriter sw = new StringWriter();
      JAXBContext context = JAXBContext.newInstance(jaxbObject.getClass());
      Marshaller m = context.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.marshal(jaxbObject, sw);
      return sw.toString();
   }

   public static <T> void putXml(T jaxbObject, File f) {
      JAXB.marshal(jaxbObject, f);
   }

}
