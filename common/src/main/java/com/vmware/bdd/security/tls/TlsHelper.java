package com.vmware.bdd.security.tls;
/*
 * **********************************************************************
 * Copyright 2014 VMware, Inc. All rights reserved. VMware Confidential
 * **********************************************************************
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Scanner;

import com.vmware.bdd.utils.ByteArrayUtils;

/**
 * Helper class for application specific functionality
 */

public class TlsHelper {

   /**
    * Present the user with the certificate thumbprint through a UI. Accepts
    * user input as whether the certificate is to be trusted or not
    *
    * @param certInfo
    * @param os       an output stream for
    */
   public static void presentUserWithCert(CertificateInfo certInfo, OutputStream os) {
      /**
       * This is where the user explicitly trusts the certificate.
       * Re-implement as per application needs
       */
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

      PrintStream ps = new PrintStream(os);

      ps.println("Certificate");
      ps.println("================================================================");
      ps.println("Subject:  " + certInfo.getSubjectDn());
      ps.println("Issuer:  " + certInfo.getIssuerDn());
      ps.println("Serial Number: " + certInfo.getSerialNumber());
      ps.println("Issued on:  " + dateFormat.format(certInfo.getNotBefore()));
      ps.println("Expires on:  " + dateFormat.format(certInfo.getNotAfter()));
      ps.println("SHA Fingerprint:  " + certInfo.getSha1Fingerprint());
      ps.println();
   }

   public static String getUserInput() {
      Scanner in = new Scanner(System.in);
      String response = in.nextLine();
      in.close();
      return response;
   }

   /**
    * @param certificate An X509 certificate. See RFC 5280
    */
   public static CertificateInfo getCertificateInfo(X509Certificate certificate) {
      char[] sha1Hash = new char[0];
      try {
         sha1Hash = ByteArrayUtils.byteArrayToHexChars(TlsHelper.getSha1Digest(certificate.getEncoded()));
      } catch (CertificateEncodingException e) {
         e.printStackTrace();
      }
      CertificateInfo certInfo = new CertificateInfo();

      certInfo.setSubjectDn(certificate.getSubjectDN().toString());
      certInfo.setIssuerDn(certificate.getIssuerDN().toString());
      certInfo.setSha1Fingerprint(String.valueOf(sha1Hash));
      certInfo.setNotBefore(certificate.getNotBefore());
      certInfo.setNotAfter(certificate.getNotAfter());

//      certInfo.setSignature(String.valueOf(Helper.byteArrayToHex(certificate.getSignature())));

      certInfo.setSerialNumber(String.valueOf(ByteArrayUtils.byteArrayToHexString(certificate.getSerialNumber().toByteArray())));

      return certInfo;
   }

   private final static int FIND_ROOT_CAUSE_LEVEL = 4;

   public static <T> T findRootCause(IOException ioEx, Class<T> klass) {
      if (ioEx == null) {
         return null;
      }

      T uce = null;
      int j = 0;
      Throwable throwable = ioEx.getCause();


      while (throwable != null && j < FIND_ROOT_CAUSE_LEVEL) {
         if (klass.isInstance(throwable)) {
            uce = klass.cast(throwable);
            break;
         } else {
            throwable = throwable.getCause();
         }
      }

      return uce;
   }


   public static byte[] getSha1Digest(byte[] data) {
      try {
         MessageDigest sha1 = MessageDigest.getInstance("SHA1");

         sha1.update(data);
         return sha1.digest();
      } catch (NoSuchAlgorithmException e) {
         //should not happen
      }

      return null;
   }
}
