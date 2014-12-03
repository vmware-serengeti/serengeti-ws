package com.vmware.bdd.security.tls;

import java.security.cert.X509Certificate;

/**
 * Created By xiaoliangl on 12/11/14.
 */
public interface TrustCertCallBack {
   public boolean doTrustOnFirstUse(X509Certificate x509certificate) ;
}
