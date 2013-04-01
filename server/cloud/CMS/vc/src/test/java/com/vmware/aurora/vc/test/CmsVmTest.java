/************************************************************
 * Copyright (c) 2011 VMware, Inc.  All rights reserved.
 ************************************************************/
package com.vmware.aurora.vc.test;

import org.testng.annotations.Test;

import com.vmware.aurora.vc.CmsVApp;

public class CmsVmTest extends AbstractVcTest {

   @Test
   public void cmsVmTest() throws Exception {
      CmsVApp cmsVApp = CmsVApp.getInstance();
      System.out.println(cmsVApp.getCMS());
      System.out.println(cmsVApp.getLDAP());
   }
}
