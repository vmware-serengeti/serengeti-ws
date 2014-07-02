package com.vmware.bdd.service.job.software;

import mockit.Mock;

import com.vmware.bdd.service.job.StatusUpdater;

public class MockStatusUpdator implements StatusUpdater {

   @Mock
   public void setProgress(double progress) {
   }
}
