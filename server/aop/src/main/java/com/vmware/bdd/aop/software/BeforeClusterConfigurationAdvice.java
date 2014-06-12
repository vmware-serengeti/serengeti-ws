package com.vmware.bdd.aop.software;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class BeforeClusterConfigurationAdvice {
   @Before("args(clusterName)")
   public void preClusterConfiguration(String clusterName) {
      //TODO: implement pre cluster configuration, for instance, detect if disk format is finished
   }
}
