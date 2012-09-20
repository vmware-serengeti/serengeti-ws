/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package com.vmware.bdd.manager;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.vmware.bdd.utils.ConfigInfo;

public class RuntimeConnectionManager {
   private static final Logger logger = Logger.getLogger(RuntimeConnectionManager.class);
   private Channel runtimeChannel;
   private Connection conn = null;
   private String exchangeName =  ConfigInfo.getRuntimeMqExchangeName();

   /**
    * Setup rabbitMQ exchange and channel to notify VHMs
    * @throws IOException 
    */
   public void init() throws IOException {
      String serverHost = ConfigInfo.getMqServerHost();
      int serverPort = ConfigInfo.getMqServerPort();
      String serverUsername = ConfigInfo.getMqServerUsername();
      String serverPassword = ConfigInfo.getMqServerPassword();

      logger.info("init runtime exchange");

      ConnectionFactory factory = new ConnectionFactory();
      if (serverUsername != null && !serverUsername.equals("")) {
         factory.setUsername(serverUsername);
         factory.setPassword(serverPassword);
      }
      factory.setVirtualHost("/");
      factory.setHost(serverHost);
      factory.setPort(serverPort);
      try {
         conn = factory.newConnection();
         runtimeChannel = conn.createChannel();

         logger.info("creating exchange: " + exchangeName);
         runtimeChannel.exchangeDeclare(exchangeName, "direct",
               true, // durable
               false, // auto-delete
               null); // arguments map
      } catch (IOException e) {
         logger.error(e.getMessage());
         if(runtimeChannel != null) {
            runtimeChannel.close();
         }
         if(conn != null){
            conn.close();
         }
         throw e;
      }
   }

   /**
    * Send a message to VHMs (on runtime rabbit exchange)
    * 
    * @throws IOException
    */
   public void sendMessage(String routeKey,String message) throws IOException {
      logger.info("sending message \"" + message + "\" on exchange " + exchangeName + ".");
      runtimeChannel.basicPublish(exchangeName, routeKey, null, message.getBytes());
   }

   public Channel getRuntimeChannel() {
      return runtimeChannel;
   }

   public void destrory() throws IOException {
      if (runtimeChannel != null) {
         runtimeChannel.close();
      }
      if (conn != null) {
         conn.close();
      }
   }

}