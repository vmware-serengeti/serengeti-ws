package com.vmware.bdd.plugin.clouderamgr.exception;

/**
 * Created by admin on 8/6/14.
 */
public class CommandExecFailException extends ClouderaManagerException{

   private String hostId;

   /**
    * The error code will be translated to pre-defined error message according to a message bundle.
    *
    * @param errCode predefined error code
    * @param cause   cause exception
    * @param details additional details
    */
   public CommandExecFailException(String errCode, Throwable cause, Object... details) {
      super(errCode, cause, details);
   }

   public String getRefHostId() {
      return hostId;
   }

   public static CommandExecFailException EXECUTE_COMMAND_FAIL(String hostId, String refMsg) {
      CommandExecFailException e = new CommandExecFailException("CLOUDERA_MANAGER.EXECUTE_COMMAND_FAIL", null, hostId, refMsg);
      e.hostId = hostId;
      return e;
   }
}
