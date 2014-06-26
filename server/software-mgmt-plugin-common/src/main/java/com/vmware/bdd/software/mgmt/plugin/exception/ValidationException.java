package com.vmware.bdd.software.mgmt.plugin.exception;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends SoftwareManagementPluginException {
   private static final long serialVersionUID = 1L;

   private List<String> failedMsgList = new ArrayList<String>();
   private List<String> warningMsgList = new ArrayList<String>();

   public ValidationException() {
      super();
   }

   public ValidationException(String errCode, String message) {
      super(errCode, message, null);
   }

   public List<String> getFailedMsgList() {
       return failedMsgList;
   }

   public List<String> getWarningMsgList() {
       return warningMsgList;
   }
}
