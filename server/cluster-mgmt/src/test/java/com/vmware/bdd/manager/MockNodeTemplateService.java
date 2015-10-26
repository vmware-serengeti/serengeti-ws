package com.vmware.bdd.manager;

import java.util.Calendar;
import java.util.Date;

import mockit.Mock;
import mockit.MockClass;

import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.service.resmgmt.impl.NodeTemplateService;

@MockClass(realClass = NodeTemplateService.class)
public class MockNodeTemplateService {

   @Mock
   public Date getLastModifiedTime(VcVirtualMachine vm) {
      return Calendar.getInstance().getTime();
   }
}
