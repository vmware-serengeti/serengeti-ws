package com.vmware.bdd.service.resmgmt.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.aurora.util.CmsWorker;
import com.vmware.bdd.service.resmgmt.IVcInventorySyncService;
import com.vmware.bdd.service.utils.VcResourceUtils;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Created by xiaoliangl on 7/27/15.
 */
public class VcInventorySyncServiceTest extends BaseResourceTest {
   private final static Logger LOGGER = Logger.getLogger(VcInventorySyncServiceTest.class);

   private VcInventorySyncService syncService = new VcInventorySyncService();
   private VcInventorySyncCounters counters = new VcInventorySyncCounters();

   private boolean isVcInited = false;
   private boolean logStarted = false;

   @BeforeTest
   public void setupBeforeClass() throws InterruptedException {
      if (!isVcInited) {
         initVC();
      }

      Thread.sleep(60000);
   }

   private void logPerformance(final VcInventorySyncCounters counters) {
      if(logStarted) return;

      ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

      ses.scheduleAtFixedRate(new Runnable() {
         @Override
         public void run() {
            try {
               LOGGER.info("performance summary: " + convertObjectToJsonBytes(counters));
            } catch (IOException ex) {
               ex.printStackTrace();
               LOGGER.error(ex);
            }
         }
      }, 0, 5, TimeUnit.SECONDS);

      logStarted = true;
   }

   @Test
   public void testSync() throws InterruptedException, IOException {
      syncService.setCounters(counters);

      ExecutorService es = Executors.newFixedThreadPool(2);

      Runnable refresher = new Runnable() {
         @Override
         public void run() {
            try {
               syncService.refreshInventory();
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }
      };
      Future<?> future1 = es.submit(refresher);

      logPerformance(counters);

      while (!future1.isDone()) {
         try {
            Thread.sleep(500);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }

   @Test
   public void testSyncConcurrent() {
      syncService.setCounters(counters);

      ExecutorService es = Executors.newFixedThreadPool(2);

      Runnable refresher = new Runnable() {
         @Override
         public void run() {
            try {
               syncService.refreshInventory();
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }
      };
      Future<?> future1 = es.submit(refresher);
      Future<?> future2 = es.submit(refresher);

      logPerformance(counters);

      while (!future1.isDone() || !future2.isDone()) {
         try {
            Thread.sleep(500);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }

   public static String convertObjectToJsonBytes(Object object) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      return mapper.writeValueAsString(object);
   }
}
