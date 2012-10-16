/******************************************************************************
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
 ******************************************************************************/
package com.vmware.bdd.cli.command.tests;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;

import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.apitypes.DatastoreReadDetail;
import com.vmware.bdd.cli.commands.CookieCache;
import com.vmware.bdd.cli.commands.DatastoreCommands;

/**
 * This class is the test of Datastore command.
 */
@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class DatastoreCommandsTest extends MockRestServer {
   @Autowired
   private DatastoreCommands datastoreCommands;

   @Test
   public void testCreateDatastore() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      buildReqRespWithoutReqBody(
            "http://127.0.0.1:8080/serengeti/api/datastores", HttpMethod.POST,
            HttpStatus.NO_CONTENT, "");

      //invalid type
      datastoreCommands.addDatastore("ds01", "ds*", "ANY");

      //normal case
      datastoreCommands.addDatastore("ds01", "ds*", "SHARED");
      CookieCache.clear();
   }
   
   @Test
   public void testCreateDatastoreFailure() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("already exists");
      ObjectMapper mapper = new ObjectMapper();

      buildReqRespWithoutReqBody(
            "http://127.0.0.1:8080/serengeti/api/datastores", HttpMethod.POST,
            HttpStatus.BAD_REQUEST, mapper.writeValueAsString(errorMsg));

      datastoreCommands.addDatastore("ds01", "ds*", "SHARED");
      CookieCache.clear();
   }

   @Test
   public void testDeleteDatastore() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      buildReqRespWithoutReqBody(
            "http://127.0.0.1:8080/serengeti/api/datastore/ds01", HttpMethod.DELETE,
            HttpStatus.NO_CONTENT, "");

      datastoreCommands.deleteDatastore("ds01");
      CookieCache.clear();
   }
   
   @Test
   public void testDeleteDatastoreFailure() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("not found");
      ObjectMapper mapper = new ObjectMapper();

      buildReqRespWithoutReqBody(
            "http://127.0.0.1:8080/serengeti/api/datastore/ds01", HttpMethod.DELETE,
            HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

      datastoreCommands.deleteDatastore("ds01");
      CookieCache.clear();
   }

   @Test
   public void testListDatastore() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      DatastoreRead rd1 = new DatastoreRead();
      rd1.setName("rd1Name");
      rd1.setType(DatastoreType.LOCAL);
      rd1.setTotalSpaceGB(100.32);
      rd1.setFreeSpaceGB(50.00);
      List<DatastoreReadDetail> datastoreReadDetails1 =
            new ArrayList<DatastoreReadDetail>();
      DatastoreReadDetail dr1Detail1 = new DatastoreReadDetail();
      dr1Detail1.setVcDatastoreName("vcds1");
      dr1Detail1.setHost("10.1.1.1");
      dr1Detail1.setTotalStorageSizeGB(90.32);
      dr1Detail1.setFreeSpaceGB(45.00);
      DatastoreReadDetail dr1Detail2 = new DatastoreReadDetail();
      dr1Detail2.setVcDatastoreName("vcds2");
      dr1Detail2.setHost("10.1.1.2");
      dr1Detail2.setTotalStorageSizeGB(10.00);
      dr1Detail2.setFreeSpaceGB(5.00);
      datastoreReadDetails1.add(dr1Detail1);
      datastoreReadDetails1.add(dr1Detail2);
      rd1.setDatastoreReadDetails(datastoreReadDetails1);

      DatastoreRead rd2 = new DatastoreRead();
      rd2.setName("rd2Name");
      rd2.setType(DatastoreType.SHARED);
      rd2.setTotalSpaceGB(20.00);
      rd2.setFreeSpaceGB(15.00);
      List<DatastoreReadDetail> datastoreReadDetails2 =
            new ArrayList<DatastoreReadDetail>();
      DatastoreReadDetail dr2Detail1 = new DatastoreReadDetail();
      dr2Detail1.setVcDatastoreName("vcds2");
      dr2Detail1.setHost("10.1.1.3");
      dr2Detail1.setTotalStorageSizeGB(20.00);
      dr2Detail1.setFreeSpaceGB(15.00);
      datastoreReadDetails2.add(dr2Detail1);
      rd2.setDatastoreReadDetails(datastoreReadDetails2);
      
      ObjectMapper mapper = new ObjectMapper();
      testListDatastore(mapper, new DatastoreRead[] { rd1, rd2 });
      CookieCache.clear();
   }

   private void testListDatastore(ObjectMapper mapper, DatastoreRead[] drs)
         throws Exception {
      buildReqRespWithoutReqBody(
            "http://127.0.0.1:8080/serengeti/api/datastores", HttpMethod.GET,
            HttpStatus.OK, mapper.writeValueAsString(drs));
      datastoreCommands.listDatastore(null, true);
   }

   @SuppressWarnings("unused")
   private void testListDatastoreByName(ObjectMapper mapper, DatastoreRead rd)
         throws Exception {
      buildReqRespWithoutReqBody(
            "http://127.0.0.1:8080/serengeti/api/datastore/rd1Name",
            HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(rd));
      datastoreCommands.listDatastore("rd1Name", true);
   }

   @Test
   public void testListDatastoreFailure() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      BddErrorMessage errorMsg = new BddErrorMessage();
      errorMsg.setMessage("not found");
      ObjectMapper mapper = new ObjectMapper();
      buildReqRespWithoutReqBody(
            "http://127.0.0.1:8080/serengeti/api/datastore/rd1Name",
            HttpMethod.GET, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));
      datastoreCommands.listDatastore("rd1Name", true);
      CookieCache.clear();
   }
}
