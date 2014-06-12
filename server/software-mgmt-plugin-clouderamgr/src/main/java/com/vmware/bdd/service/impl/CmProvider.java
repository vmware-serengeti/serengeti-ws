package com.vmware.bdd.service.impl;

import com.vmware.bdd.exception.CmException;
import com.vmware.bdd.service.ICmProviderService;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Author: Xiaoding Bian
 * Date: 5/22/14
 * Time: 3:55 PM
 */
public class CmProvider {

   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface CmProviderExportMethod {
      String name();
   }

   private String version;
   private String versionApi;
   private String versionCdh;

   private File path;
   private String operate;

   private String ip;
   private String ipInternal;
   private int port = 7180;
   private String user = "admin";
   private String password = "admin";

   private ICmProviderService cmService;

   private static final Map<String, Method> OPERATIONS = new HashMap<String, Method>();
   static {
      for (Method method : CmProviderServiceImpl.class.getMethods()) {
         if (method.isAnnotationPresent(CmProviderExportMethod.class)) {
            OPERATIONS.put(method.getAnnotation(CmProviderExportMethod.class).name(), method);
         }
      }
   }

   public static Set<String> getOperations() {
      return new HashSet<String>(OPERATIONS.keySet());
   }

   public CmProvider() throws CmException {
   }

     @CmProviderExportMethod(name = "version")
  public CmProvider version(String version) throws CmException {
    this.version = version;
    this.cmService = null;
    return this;
  }

  @CmProviderExportMethod(name = "versionApi")
  public CmProvider versionApi(String versionApi) throws CmException {
    this.versionApi = versionApi;
    this.cmService = null;
    return this;
  }

  @CmProviderExportMethod(name = "versionCdh")
  public CmProvider versionCdh(String versionCdh) throws CmException {
    this.versionCdh = versionCdh;
    this.cmService = null;
    return this;
  }

  @CmProviderExportMethod(name = "ip")
  public CmProvider ip(String ip) throws CmException {
    if (ip == null || ip.equals("")) {
      throw new CmException();
    }
    this.ip = ip;
    this.cmService = null;
    return this;
  }

  @CmProviderExportMethod(name = "ipInternal")
  public CmProvider ipInternal(String ipInternal) throws CmException {
    if (ipInternal == null || ipInternal.equals("")) {
      throw new CmException();
    }
    this.ipInternal = ipInternal;
    return this;
  }

  @CmProviderExportMethod(name = "port")
  public CmProvider port(String port) throws CmException {
    if (port == null || port.equals("") || !StringUtils.isNumeric(port)) {
      throw new CmException();
    }
    this.port = Integer.parseInt(port);
    this.cmService = null;
    return this;
  }

  @CmProviderExportMethod(name = "user")
  public CmProvider user(String user) throws CmException {
    if (user == null || user.equals("")) {
      throw new CmException();
    }
    this.user = user;
    this.cmService = null;
    return this;
  }

  @CmProviderExportMethod(name = "password")
  public CmProvider password(String password) throws CmException {
    if (password == null || password.equals("")) {
      throw new CmException();
    }
    this.password = password;
    this.cmService = null;
    return this;
  }

  @CmProviderExportMethod(name = "path")
  public CmProvider path(String path) throws CmException {
    if (path == null) {
      throw new CmException();
    }
    this.path = new File(path);
    return this;
  }

  @CmProviderExportMethod(name = "operate")
  public CmProvider operate(String operate) throws CmException {
    if (operate == null || !OPERATIONS.containsKey(operate)) {
      throw new CmException();
    }
    this.operate = operate;
    return this;
  }

}
