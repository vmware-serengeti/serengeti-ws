package com.vmware.bdd.service.job.software;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SoftwareManagementStepTest {
   private ApplicationContext ctx;

   @BeforeClass
   public void init() {
      ctx = new ClassPathXmlApplicationContext("spring/*-context.xml");
   }

   @Test
   public void createClusterTasklet() {
      SoftwareManagementStep softwareCreateClusterTasklet =
            ctx.getBean("softwareCreateClusterTasklet",
                  SoftwareManagementStep.class);
      ManagementOperation operation =
            softwareCreateClusterTasklet.getManagementOperation();
      Assert.assertEquals(ManagementOperation.CREATE, operation);
   }
}
