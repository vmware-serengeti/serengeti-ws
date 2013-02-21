/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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
package com.vmware.bdd.utils;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * This is RabbitMQ consumer, which use 'direct' mode.
 * 
 */
public class RabbitMQConsumer {
   private static final Logger logger = Logger.getLogger(RabbitMQConsumer.class);
   /*
    * The max wait time before the last message arrival after the command
    * finished.
    */
   private static long mqRecvTimeoutMs = 1000 * 10;
   private static long mqKeepAliveTimeMs = 1000 * 60;

   static {
      mqRecvTimeoutMs = Configuration.getLong("task.rabbitmq.recv_timeout_ms",
            mqRecvTimeoutMs);
      mqKeepAliveTimeMs = Configuration.getLong("task.rabbitmq.keepalive_time_ms",
            mqKeepAliveTimeMs);
   }

   public interface MessageListener {
      /**
       * Process the message and decide whether to continue receiving messages.
       * 
       * @return flag indicates whether to continue listening
       * @throws Exception
       *            any runtime exception
       */
      boolean onMessage(String message) throws Exception;
   }

   private String host;
   private int port;
   private String username;
   private String password;
   private String exchangeName;
   private String queueName;
   private String routingKey;
   private boolean getQueue;
   
   // volatile is a must to insert memory barrier because read has no lock
   private volatile boolean stopping = false;
   private volatile boolean graceStopping = false;
   private Date mqExpireTime;

   public RabbitMQConsumer(String host, int port, String username, String password,
         String exchangeName, String queueName, String routingKey, boolean getQueue) throws IOException {
      this.host = host;
      this.port = port;
      this.username = username;
      this.password = password;
      this.exchangeName = exchangeName;
      this.queueName = queueName;
      this.routingKey = routingKey;
      this.getQueue = getQueue;
   }

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getExchangeName() {
      return exchangeName;
   }

   public void setExchangeName(String exchangeName) {
      this.exchangeName = exchangeName;
   }

   public String getQueueName() {
      return queueName;
   }

   public void setQueueName(String queueName) {
      this.queueName = queueName;
   }

   public String getRoutingKey() {
      return routingKey;
   }

   public void setRoutingKey(String routingKey) {
      this.routingKey = routingKey;
   }

   public boolean isGetQueue() {
      return getQueue;
   }

   public void setGetQueue(boolean getQueue) {
      this.getQueue = getQueue;
   }

   public void forceStopNow() {
      synchronized (this) {
         mqExpireTime = new Date();
         stopping = true;
         logger.info("force to stop receiving messages now");
      }
   }

   public void forceStop() {
      synchronized (this) {
         extendExpirationTime();
         stopping = true;
         logger.info("force to stop receiving messages after " + mqKeepAliveTimeMs + " ms");
      }
   }

   synchronized private void extendExpirationTime() {
      Date now = new Date();
      Date deadline = new Date(now.getTime() + mqKeepAliveTimeMs);
      if (mqExpireTime == null || mqExpireTime.after(deadline)) {
         mqExpireTime = deadline;
      }
   }
   
   public void graceStop() {
      synchronized (this) {
         extendExpirationTime();
         stopping = true;
         graceStopping = true;
         logger.info("gracefully stop receiving messages if no message received after " + mqKeepAliveTimeMs + " ms");
      }
   }

   /**
    * Receive and process each message until the listener indicating. A new
    * queue will be created when start and will be deleted when stopping
    * receiving message.
    * 
    * FIXME Is it a best practice to create one queue for one task? Or we should
    * create one thread to handle all messages?
    * 
    * @param listener
    *           message processor callback
    * @throws IOException
    */
   public void processMessage(MessageListener listener) throws IOException {
      ConnectionFactory factory = new ConnectionFactory();
      if (username != null && !username.equals("")) {
         factory.setUsername(username);
         factory.setPassword(password);
      }
      factory.setVirtualHost("/");
      factory.setHost(host);
      factory.setPort(port);

      Connection conn = factory.newConnection();
      Channel channel = conn.createChannel();

      /**
       * make exchange and queue non-durable
       */
      channel.exchangeDeclare(exchangeName, "direct", true);
      if(!getQueue) {
         channel.queueDeclare(queueName, false, true, true, null);         
      } else {
         queueName = channel.queueDeclare().getQueue();
      }
      channel.queueBind(queueName, exchangeName, routingKey);

      boolean noAck = false;
      QueueingConsumer consumer = new QueueingConsumer(channel);
      channel.basicConsume(queueName, noAck, consumer);

      while (true) {
         QueueingConsumer.Delivery delivery;
         try {
            delivery = consumer.nextDelivery(mqRecvTimeoutMs);
         } catch (InterruptedException e) {
            logger.warn("message consumer interrupted", e);
            continue;
         }

         if (delivery == null) {
            logger.debug("timeout, no message received");
            if (stopping && new Date().after(mqExpireTime)) {
               logger.error("stop receiving messages without normal termination");
               break;
            }
            continue;
         }

         String message = new String(delivery.getBody());
         if (graceStopping) {
            extendExpirationTime();
         }

         logger.debug("message received: " + message);
         try {
            if (!listener.onMessage(message)) {
               logger.info("stop receiving messages normally");
               break;
            }
         } catch (Throwable t) {
            logger.error("calling message listener failed", t);
            // discard and continue in non-debug mode
            AuAssert.unreachable();
         }
         channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      }

      try {
         channel.queueDelete(queueName);
      } catch (AlreadyClosedException e) {
         logger.error("failed to delete queue: " + queueName, e);
      }

      try {
         channel.close();
      } catch (AlreadyClosedException e) {
         logger.error("failed to close channel, queue: " + queueName, e);
      }

      try {
         conn.close();
      } catch (AlreadyClosedException e) {
         logger.error("failed to close connection, queue: " + queueName, e);
      }
   }
}
