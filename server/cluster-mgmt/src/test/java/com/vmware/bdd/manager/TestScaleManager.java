package com.vmware.bdd.manager;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.ResourceScale;

public class TestScaleManager {
   private ScaleManager scaleMgr;

   //@Test
   public void buildJobParameters() {
      ApplicationContext context =
            new ClassPathXmlApplicationContext("spring/*.xml");
      scaleMgr = context.getBean("scaleManager", ScaleManager.class);
      ResourceScale scale = new ResourceScale("apache2", "worker", 3, 4000);
      scaleMgr.buildJobParameters(scale);
   }

   @Test
   public void scaleNodeGroupResource() {

   }
}
