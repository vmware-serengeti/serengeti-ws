/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.security.tls;

import java.util.Date;

/**
 * Created By xiaoliangl on 12/1/14.
 */
public class CertificateInfo {
   private String subjectDn;
   private String issuerDn;
   private String sha1Fingerprint;
   private Date notBefore;
   private Date notAfter;
//   private String signature;
   private String serialNumber;


   public void setSubjectDn(String subjectDn1) {
      subjectDn = subjectDn1;
   }

   public String getSubjectDn() {
      return subjectDn;
   }

   public void setIssuerDn(String issuerDn1) {
      issuerDn = issuerDn1;
   }

   public String getIssuerDn() {
      return issuerDn;
   }

   public void setSha1Fingerprint(String sha1Fingerprint1) {
      sha1Fingerprint = sha1Fingerprint1;
   }

   public String getSha1Fingerprint() {
      return sha1Fingerprint;
   }

   public void setNotBefore(Date notBefore) {
      this.notBefore = notBefore;
   }

   public Date getNotBefore() {
      return notBefore;
   }

   public void setNotAfter(Date notAfter) {
      this.notAfter = notAfter;
   }

   public Date getNotAfter() {
      return notAfter;
   }


   public void setSerialNumber(String serialNumber) {
      this.serialNumber = serialNumber;
   }

   public String getSerialNumber() {
      return serialNumber;
   }
}
