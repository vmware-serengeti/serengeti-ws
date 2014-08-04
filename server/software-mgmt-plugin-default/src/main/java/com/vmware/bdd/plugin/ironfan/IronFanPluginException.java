package com.vmware.bdd.plugin.ironfan;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;

/**
 * Exceptions from IronFan plugin
 */
public class IronFanPluginException extends SoftwareManagementPluginException {

   /**
    * The error code will be translated to pre-defined error message according to a message bundle.
    *
    * @param errCode predefined error code
    * @param cause   cause exception
    * @param details additional details
    */
   public IronFanPluginException(String errCode, Throwable cause, Object... details) {
      super(errCode, cause, details);
   }

   public final static IronFanPluginException GET_ROLES_ERR_EXIT_CODE(int exitCode) {
      throw new IronFanPluginException("IRONFAN.GET_ROLES_ERR_EXIT_CODE", null, exitCode);
   }

   public final static IronFanPluginException GET_ROLES_EXCEPTION(Exception cause) {
      return new IronFanPluginException("IRONFAN.GET_ROLES_EXCEPTION", cause);
   }
}
