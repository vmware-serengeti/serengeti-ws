/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.aurora.exception;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;

public class CertificateMgmtException extends AuroraException {
   // Default public constructor. This should only be used by AMF client.
   public CertificateMgmtException() {}

   private CertificateMgmtException(Throwable t, String errorid, Object... args) {
      super(t, "CERTIFICATEMGMT", errorid, args);
   }

   public static final CertificateMgmtException KEY_ENTITY_NOT_FOUND(Long keyId) {
      return new CertificateMgmtException(null, "KEY_ENTITY_NOT_FOUND", keyId);
   }

   public static final CertificateMgmtException INVALID_ENTITY_TYPE(String type) {
      return new CertificateMgmtException(null, "INVALID_ENTITY_TYPE", type);
   }

   public static final CertificateMgmtException KEY_IS_ASSIGNED(Long keyId) {
      return new CertificateMgmtException(null, "KEY_IS_ASSIGNED", keyId);
   }

   public static final CertificateMgmtException CERTIFICATE_ENTITY_NOT_FOUND(
         Long keyId, Long certId) {
      return new CertificateMgmtException(null, "CERTIFICATE_ENTITY_NOT_FOUND",
            certId, keyId);
   }

   public static final CertificateMgmtException INVALID_PARAMETER(
         Object... args) {
      return new CertificateMgmtException(null, "INVALID_PARAMETER", args);
   }

   public static final CertificateMgmtException DUPLICATED_KEY_NAME(
         String keyName) {
      return new CertificateMgmtException(null, "DUPLICATED_KEY_NAME", keyName);
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_CHAIN(
         Long keyId, Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_CHAIN", keyId);
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_ENCODING(Long keyId, Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_ENCODING_FOR_KEY", keyId);
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_EXPIRED(Long keyId, Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_EXPIRED_FOR_KEY", keyId);
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_NOT_YET_VALID(Long keyId, Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_NOT_YET_VALID_FOR_KEY", keyId);
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_PARSING_ERROR(Long keyId, Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_PARSING_ERROR_FOR_KEY", keyId);
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_CHAIN_NO_ALGORITHM(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_CHAIN_NO_ALGORITHM");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_INVALID_PATH(Long keyId, Throwable e) {
      if (keyId == null) {
         if (e.getCause() instanceof CertificateEncodingException) {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_ENCODING_FOR_UPLOADED_KEY");
         } else if (e.getCause() instanceof CertificateExpiredException) {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_EXPIRED_FOR_UPLOADED_KEY");
         } else if (e.getCause() instanceof CertificateNotYetValidException) {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_NOT_YET_VALID_FOR_UPLOADED_KEY");
         } else if (e.getCause() instanceof CertificateParsingException) {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_PARSING_ERROR_FOR_UPLOADED_KEY");
         } else {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_INVALID_PATH_FOR_UPLOADED_KEY");
         }
      } else {
         if (e.getCause() instanceof CertificateEncodingException) {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_ENCODING_FOR_KEY", keyId);
         } else if (e.getCause() instanceof CertificateExpiredException) {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_EXPIRED_FOR_KEY", keyId);
         } else if (e.getCause() instanceof CertificateNotYetValidException) {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_NOT_YET_VALID_FOR_KEY", keyId);
         } else if (e.getCause() instanceof CertificateParsingException) {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_PARSING_ERROR_FOR_KEY", keyId);
         } else {
            return new CertificateMgmtException(e, "INVALID_CERTIFICATE_INVALID_PATH_FOR_KEY", keyId);
         }
      }
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_CHAIN_FOR_UPLOADED_KEY(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_CHAIN_FOR_UPLOADED_KEY");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_ENCODING_FOR_UPLOADED_KEY(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_ENCODING_FOR_UPLOADED_KEY");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_EXPIRED_FOR_UPLOADED_KEY(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_EXPIRED_FOR_UPLOADED_KEY");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_NOT_YET_VALID_FOR_UPLOADED_KEY(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_NOT_YET_VALID_FOR_UPLOADED_KEY");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_PARSING_ERROR_FOR_UPLOADED_KEY(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_PARSING_ERROR_FOR_UPLOADED_KEY");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_INVALID_PATH_FOR_UPLOADED_KEY(Throwable e) {
      return INVALID_CERTIFICATE_INVALID_PATH(null, e);
   }

   public static final CertificateMgmtException INVALID_KEY(Long keyId,
         Throwable e) {
      return new CertificateMgmtException(e, "INVALID_KEY", keyId);
   }

   public static final CertificateMgmtException INVALID_KEY_PASSWORD(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_KEY_PASSWORD");
   }

   public static final CertificateMgmtException INVALID_KEYSTORE_PASSWORD(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_KEYSTORE_PASSWORD");
   }

   public static final CertificateMgmtException INVALID_DER_CERTIFICATE(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_DER_CERTIFICATE");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_ENCODING(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_ENCODING");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_EXPIRED(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_EXPIRED");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_NOT_YET_VALID(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_NOT_YET_VALID");
   }

   public static final CertificateMgmtException INVALID_CERTIFICATE_PARSING_ERROR(Throwable e) {
      return new CertificateMgmtException(e, "INVALID_CERTIFICATE_PARSING_ERROR");
   }

   public static final CertificateMgmtException INVALID_KEYSTORE(String keystoreType,
         Throwable e) {
      return new CertificateMgmtException(e, "INVALID_KEYSTORE", keystoreType);
   }

   public static final CertificateMgmtException UNSUPPORTED_KEYSTORE_TYPE(String keystoreType,
         Throwable e) {
      return new CertificateMgmtException(e, "UNSUPPORTED_KEYSTORE_TYPE", keystoreType);
   }

   public static final CertificateMgmtException UNSUPPORTED_ALGORITHM(Throwable e) {
      return new CertificateMgmtException(e, "UNSUPPORTED_ALGORITHM");
   }

   public static final CertificateMgmtException UNSUPPORTED_ALGORITHM(Long keyId, Throwable e) {
      return new CertificateMgmtException(e, "KEY_UNSUPPORTED_ALGORITHM", keyId);
   }

   public static final CertificateMgmtException VM_ASSIGNMENT_IN_PROGRESS(
         Long vmId) {
      return new CertificateMgmtException(null, "VM_ASSIGNMENT_IN_PROGRESS",
            vmId);
   }

   public static final CertificateMgmtException KEY_IN_ASSIGNMENT_STATUS(
         Long keyId) {
      return new CertificateMgmtException(null, "KEY_IN_ASSIGNMENT_STATUS",
            keyId);
   }

   public static final CertificateMgmtException KEY_ASSIGNMENT_FAIL() {
	      return new CertificateMgmtException(null, "KEY_ASSIGNMENT_FAIL");
   }

   public static final CertificateMgmtException CMS_AUTO_KEY_NOT_FOUND() {
      return new CertificateMgmtException(null, "CMS_AUTO_KEY_NOT_FOUND");
   }

   public static final CertificateMgmtException CERTIFICATE_EXPIRES_IN_TEN_DAYS() {
      return new CertificateMgmtException(null,
            "CERTIFICATE_EXPIRES_IN_TEN_DAYS");
   }

   public static final CertificateMgmtException ASSIGNED_KEY_IS_NOT_FOUND() {
      return new CertificateMgmtException(null, "ASSIGNED_KEY_IS_NOT_FOUND");
   }

   public static final CertificateMgmtException CUSTOM_CERTIFICATE_IS_CONFIGURED() {
      return new CertificateMgmtException(null, "CUSTOM_CERTIFICATE_IS_CONFIGURED");
   }

   public static final CertificateMgmtException CERTIFICATE_CHANGE_TIMEOUT() {
      return new CertificateMgmtException(null, "CERTIFICATE_CHANGE_TIMEOUT");
   }

   public static final CertificateMgmtException NOT_KEY_ENTRY(String alias) {
      return new CertificateMgmtException(null, "NOT_KEY_ENTRY", alias);
   }

   public static final CertificateMgmtException DSA_KEY_ALGORITHM_IS_NOT_SUPPORTED() {
      return new CertificateMgmtException(null, "DSA_KEY_ALGORITHM_IS_NOT_SUPPORTED");
   }

   public static final CertificateMgmtException ACQUIRE_TRUST_LOCK_TIMEOUT() {
      return new CertificateMgmtException(null, "ACQUIRE_TRUST_LOCK_TIMEOUT");
   }

   public static final CertificateMgmtException EMPTY_CERTIFICATE() {
      return new CertificateMgmtException(null, "EMPTY_CERTIFICATE");
   }

   public static final CertificateMgmtException KEY_VALUES_MISMATCH() {
      return new CertificateMgmtException(null, "KEY_VALUES_MISMATCH");
   }

   public static final CertificateMgmtException DB_IS_LOCKED(String dbName) {
      return new CertificateMgmtException(null, "DB_IS_LOCKED", dbName);
   }

   public static final CertificateMgmtException SYSTEM_VM_ASSIGNMENT_IS_RUNNING() {
      return new CertificateMgmtException(null, "SYSTEM_VM_ASSIGNMENT_IS_RUNNING");
   }
}
