/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

package com.vmware.aurora.vc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

import com.vmware.aurora.exception.BaseVMException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcTask.TaskType;
import com.vmware.aurora.vc.VcTaskMgr.IVcTaskBody;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcService;
import com.vmware.vim.binding.impl.vim.host.DatastoreBrowser_Impl;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.FileManager;
import com.vmware.vim.binding.vim.HttpNfcLease;
import com.vmware.vim.binding.vim.VirtualDiskManager;
import com.vmware.vim.binding.vim.VirtualDiskManager.VirtualDiskSpec;
import com.vmware.vim.binding.vim.fault.FileNotFound;
import com.vmware.vim.binding.vim.fault.Timedout;
import com.vmware.vim.binding.vim.host.DatastoreBrowser;
import com.vmware.vim.binding.vim.host.DatastoreBrowser.FileInfo;
import com.vmware.vim.binding.vim.host.DatastoreBrowser.SearchResults;
import com.vmware.vim.binding.vim.host.DatastoreBrowser.SearchSpec;

/**
 * This is a collection of utility functions to do
 * file operations in VC datastores.
 */
public class VcFileManager {
   private static final Logger logger = Logger.getLogger(VcFileManager.class);

   /**
    * Maximum size of ovf description we'll take.
    */
   static final long MAX_OVF_SIZE = 64 * 1024;
   static final String DS_PATH_PATTERN = "^\\[([^\\]]+)\\](.+)$";

   /*
    * load OVF contents from a file.
    */
   static private String
   loadOvfContents(String ovfPath) throws IOException {
      BufferedReader reader = null;
      char [] ovfBuf = null;
      try {
         File ovfFile = new File(ovfPath);
         reader = new BufferedReader(new FileReader(ovfFile));
         AuAssert.check(ovfFile.length() < MAX_OVF_SIZE);
         int totalLen = (int)(ovfFile.length() < MAX_OVF_SIZE ?
                              ovfFile.length() : MAX_OVF_SIZE);
         ovfBuf = new char[totalLen];
         int len, offset = 0;
         while (offset < totalLen &&
                (len = reader.read(ovfBuf, offset, totalLen - offset)) != -1) {
            offset += len;
         }
      } finally {
         if (reader != null) {
            reader.close();
         }
      }

      return new String(ovfBuf);
   }

   /**
    * {@link ProgressListener}  keeps track of percentage progress of a job composed
    * of multiple tasks and update the {@link HttpNfcLease} periodically.
    * The total percentage of all tasks is no more than 100.
    * When a task is done, the listener's {@link lenDone} of finished task is
    * incremented by the task's workload of the entire job.
    * When a task is in progress, incremental work is added temporarily
    * to {@link lenDone} for reporting.
    */
   private static class ProgressListener {
      private long lenDone = 0;
      private long pctLen; // length for finishing 1 percent
      private final HttpNfcLease nfcLease;
      private int updatePeriod;
      private long lastTime;
      private volatile int lastProgress = 0;

      public ProgressListener(HttpNfcLease nfcLease, long totalLen) {
         this.nfcLease = nfcLease;
         if (totalLen <= 0) {
            this.pctLen = Long.MAX_VALUE;
         } else {
            this.pctLen = totalLen / 100 + 1;
         }
         if (nfcLease != null) {
            updatePeriod = nfcLease.getInfo().getLeaseTimeout() * 1000 / 2;
            if (updatePeriod > 5000) {
               updatePeriod = 5000; // update progress at least every 5 seconds
            }
         } else {
            updatePeriod = 5000;
         }
         lastTime = System.currentTimeMillis();
      }

      private void updateProgress(int progress) {
         if (progress >= lastProgress) {
            lastProgress = progress;
            if (nfcLease != null) {
               nfcLease.progress(lastProgress, null);
            }
         }
      }

      /**
       * Update intermediate progress for the current task.
       * @throws Timedout
       */
      public void updateTask(long len) throws Timedout {
         if (System.currentTimeMillis() - lastTime > updatePeriod) {
            updateProgress((int)((lenDone + len) / pctLen));
         }
      }

      /**
       * Done with the current task and update progress.
       */
      public void taskDone(long taskLen) throws Timedout {
         lenDone += taskLen;
         updateProgress((int)(lenDone / pctLen));
      }

      public long getLenDone() {
         return lenDone;
      }

      public void setLenDone(long lenDone) throws Timedout {
         this.lenDone = lenDone;
      }
   }

   /*
    * A RequestEntity implementation for file with calls to progress listener.
    */
   private static class ProgressListenerRequestEntity implements RequestEntity {
      private File file;
      private String contentType;
      private ProgressListener listener;
      // Change from 16k to 512k to speed up deploy process
      private final int BUF_SIZE = 1024 * 512;

      public ProgressListenerRequestEntity(File file, String contentType,
                                               ProgressListener listener) {
         this.file = file;
         this.contentType = contentType;
         this.listener = listener;
      }

      @Override
      public long getContentLength() {
         return file.length();
      }

      @Override
      public String getContentType() {
         return contentType;
      }

      @Override
      public boolean isRepeatable() {
         return true;
      }

      @Override
      public void writeRequest(final OutputStream out) throws IOException {
         byte[] buf = new byte[BUF_SIZE];
         int i = 0;
         long offset = 0;
         InputStream instream = new FileInputStream(file);
         try {
            while ((i = instream.read(buf)) >= 0) {
               out.write(buf, 0, i);
               offset += i;
               listener.updateTask(offset);
            }
            listener.taskDone(offset);
         } catch (Timedout e) {
            throw new IOException("Progress listener failed.", e);
         } finally {
             instream.close();
         }
      }
   }

   /*
    * Upload file to a given URL.
    */
   static private void
   uploadFileWork(String url, boolean isPost, File file, String contentType,
                  String cookie, ProgressListener listener)
   throws Exception {
      EntityEnclosingMethod method;
      final RequestEntity entity =
         new ProgressListenerRequestEntity(file, contentType, listener);
      if (isPost) {
         method = new PostMethod(url);
         method.setContentChunked(true);
      } else {
         method = new PutMethod(url);
         method.addRequestHeader("Cookie", cookie);
         method.setContentChunked(false);
         HttpMethodParams params = new HttpMethodParams();
         params.setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true);
         method.setParams(params);
      }
      method.setRequestEntity(entity);

      logger.info("upload " + file + " to " + url);
      long t1 = System.currentTimeMillis();
      boolean ok = false;
      try {
         HttpClient httpClient = new HttpClient();
         int statusCode = httpClient.executeMethod(method);
         String response = method.getResponseBodyAsString(100);
         logger.debug("status: " + statusCode + " response: " + response);
         if (statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_OK) {
            throw new Exception("Http post failed");
         }
         method.releaseConnection();
         ok = true;
      } finally {
         if (!ok) {
            method.abort();
         }
      }
      long t2 = System.currentTimeMillis();
      logger.info("upload " + file + " done in " + (t2 - t1) + " ms");
   }

   /*
    * Get VC File URL.
    */
   private static String getVcFileUrl(VcDatastore datastore, String dsFilePath)
   throws Exception {
      VcService vcs = VcContext.getService();
      String rootFolderUrl = vcs.getServiceUrl().replaceFirst("sdk", "folder");
      AuAssert.check(rootFolderUrl.endsWith("folder"));
      //construct upload file destination URL
      String relativePath = URLEncoder.encode(String.format("%s?dcPath=%s&dsName=%s", dsFilePath,
            datastore.getDatacenter().getURLName(), datastore.getURLName()), "UTF-8");
      return String.format("%s/%s", rootFolderUrl, relativePath);
   }

   static private void
   uploadFileLoop(String hostUrl, File file, VcDatastore ds,
                  String dsFilePath, ProgressListener listener) throws Exception {
      Exception e1 = null;
      String vcUrl = getVcFileUrl(ds, dsFilePath);
      // Using vmware-streamVmdk is 2x as fast as octet-stream
      final String hostContentType = "application/x-vnd.vmware-streamVmdk";
      final String vcContentType = "application/octet-stream";
      // Retry 3 times using host URL
      // XXX VC folder uploading is not completely implemented.
      Boolean reqsUseVc[] = new Boolean[]{false, false, false};
      long lenDone = listener.getLenDone();
      VcService vcs = VcContext.getService();
      for (Boolean useVc: reqsUseVc) {
         try {
            if (useVc) {
               /*
                * If we fail to upload to host directly, try to use
                * "VC folder put API", which is orders of magnitude slower.
                */
               String sessionString = "vmware_soap_session=" + vcs.getClientSessionId();
               uploadFileWork(vcUrl, false, file, vcContentType,
                              sessionString, listener);
            } else {
               uploadFileWork(hostUrl, true, file, hostContentType, null, listener);
            }
            return;
         } catch (Exception e) {
            e1 = e;
            logger.info("failed to upload file " +
                        file + " to " + hostUrl + ":" + e.getMessage());
            listener.setLenDone(lenDone);
         }
      }
      if (e1 != null) {
         throw e1;
      }
   }

   /**
    * Import an OVF as a virtual machine to a datastore.
    * @param name name of the VM
    * @param rp a resource pool connected to the datastore
    * @param ds the destination datastore
    * @param network default network setting
    * @param ipPolicy default ip allocation policy
    * @param ovfPath OVF file path to be imported.
    * @return the imported VM
    * @throws Exception
    */
//   static public VcVirtualMachine
//   importVm(String name, VcResourcePool rp, VcDatastore ds,
//            VcNetwork network, String ovfPath)
//   throws Exception {
//      ManagedObjectReference vmRef;
//      AuAssert.check(VcContext.isInTaskSession());
//      VcService vcs = VcContext.getService();
//
//      CreateImportSpecParams importParams = new CreateImportSpecParamsImpl();
//      importParams.setDeploymentOption("");
//      importParams.setLocale("");
//      importParams.setEntityName(name);
//      NetworkMapping[] nets = {
//            new NetworkMappingImpl("Network 1", network.getMoRef()),
//            new NetworkMappingImpl("Network 2", network.getMoRef())
//      };
//      importParams.setNetworkMapping(nets);
//      importParams.setIpAllocationPolicy(IpAllocationPolicy.transientPolicy.toString());
//      importParams.setDiskProvisioning("thin");
//
//      // create import spec from ovf
//      CreateImportSpecResult specResult = vcs.getOvfManager().createImportSpec(
//            loadOvfContents(ovfPath), rp.getMoRef(), ds.getMoRef(), importParams);
//      AuAssert.check(specResult.getError() == null && specResult.getWarning() == null);
//      VmImportSpec importSpec = (VmImportSpec)specResult.getImportSpec();
//      // start importing the vApp and get the lease to upload vmdks
//      HttpNfcLease nfcLease = rp.importVApp(importSpec);
//
//      // total bytes to be imported
//      long importTotal = 0;
//      // map: deviceId -> File
//      HashMap<String, File> fileMap = new HashMap<String, File>();
//      String basePath = new File(ovfPath).getParent();
//      for (FileItem item : specResult.getFileItem()) {
//         File f = new File(basePath + File.separator + item.getPath());
//         fileMap.put(item.getDeviceId(), f);
//         importTotal += f.length();
//      }
//
//      try {
//         // wait for nfc lease to become ready
//         State state = nfcLease.getState();
//         while (state != State.ready) {
//            Thread.sleep(1000);
//            state = nfcLease.getState();
//            if (state == State.error) {
//               Exception e = nfcLease.getError();
//               logger.error(e.getMessage(), e.getCause());
//               throw e;
//            }
//         }
//
//        nfcLease.progress(0);
//        ProgressListener listener = new ProgressListener(nfcLease, importTotal);
//        vmRef = nfcLease.getInfo().getEntity();
//         //@TODO 1. if this method will be used in future, the TrustManager need to be refactored.
//        //ThumbprintTrustManager tm = HttpsConnectionUtil.getThumbprintTrustManager();
//         // upload all files
//         for(DeviceUrl deviceUrl : nfcLease.getInfo().getDeviceUrl()) {
//            File f = fileMap.get(deviceUrl.getImportKey());
//            String thumbprint = deviceUrl.getSslThumbprint();
//            //@TODO 2. if this method will be used in future, the TrustManager need to be refactored.
//            //tm.add(thumbprint.toString(), Thread.currentThread());
//            try {
//               uploadFileLoop(deviceUrl.getUrl(), f, ds,
//                              name + "/" + f.getName(), listener);
//            } finally {
//               //@TODO 3. if this method will be used in future, the TrustManager need to be refactored.
//               //tm.remove(thumbprint.toString(), Thread.currentThread());
//            }
//         }
//         nfcLease.progress(100);
//         nfcLease.complete();
//      } catch (Exception e) {
//         logger.error(e.getCause());
//         try {
//            /*
//             * By aborting the lease, VC also deletes the VM.
//             */
//            nfcLease.abort(null);
//         } catch (Exception e1) {
//            logger.error("got exception trying to abort nfcLease", e1);
//         }
//         throw VcException.UPLOAD_ERROR(e);
//      }
//
//      return VcCache.get(vmRef);
//   }

   public static void uploadFile(String localPath, VcDatastore datastore, String datastorePath)
         throws Exception {
      AuAssert.check(VcContext.isInTaskSession());
      VcService vcs = VcContext.getService();

      final String url = VcFileManager.getVcFileUrl(datastore, datastorePath);
      final String vcContentType = "application/octet-stream";
      final String sessionString = "vmware_soap_session=" + vcs.getClientSessionId();
      final File file = new File(localPath);
      final ProgressListener progress = new ProgressListener(null, 0);

      uploadFileWork(url, false, file, vcContentType, sessionString, progress);
   }

   public static String getDsFromPath(String dsPath) throws Exception {
      Pattern pattern = Pattern.compile(DS_PATH_PATTERN);
      Matcher match = pattern.matcher(dsPath);
      AuAssert.check(match.matches());
      return match.group(1);
   }

   /**
    * Get the datastore path.
    * @param ds datastore
    * @param path pathname to the file inside a datastore
    * @return full datastore pathname
    */
   public static String getDsPath(VcDatastore ds, String path) {
      // shouldn't be formatted already
      AuAssert.check(!path.matches(DS_PATH_PATTERN));
      return String.format("[%s] %s", ds.getURLName(), path);
   }

   /**
    * Get the datastore path for a file under the VM directory.
    * @param vm virtual machine object
    * @param name file name
    * @return
    */
   public static String getDsPath(VcVirtualMachine vm, String name) {
      return String.format("%s/%s", vm.getPathName(), name);
   }

   /**
    * Get the datastore path for a file under the VM directory on a different datastore.
    * The file would be either at the root of the datastore name-prefixed with the VM
    * name or in a directory with same name as the VM directory.
    * @param vm virtual machine object
    * @param ds datastore (null means the default VM datastore)
    * @param name file name
    * @return
    */
   public static String getDsPath(VcVirtualMachine vm, VcDatastore ds, String name) {
      if (ds == null) {
         return getDsPath(vm, name);
      } else {
         try {
            return String.format("[%s]", ds.getName());
         }
         catch (Exception ex) {
            throw BaseVMException.INVALID_FILE_PATH(ex, vm.getPathName());
         }
      }
   }

   /**
    * Extract disk name of a given disk file name
    * @param diskFileName the given disk file name
    */
   public static String getDiskName(String diskFileName) {
      return diskFileName.substring(diskFileName.lastIndexOf('/') + 1);
   }

   /**
    * Copy virtual disk from source datastore path to destination datastore
    * path.
    *
    * @param srcDsPath
    *           source pathname to the file
    * @param srcDc
    *           source datacenter
    * @param dstDsPath
    *           destination pathname to the file
    * @param dstDc
    *           destination datacenter
    * @param diskSpec
    *           destination virtual disk specification
    * @throws Exception
    */
   protected static VcTask copyVirtualDisk(final String srcDsPath,
         final VcDatacenter srcDc, final String dstDsPath,
         final VcDatacenter dstDc, final VirtualDiskSpec diskSpec,
         final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         @Override
         public VcTask body() throws Exception {
            final VirtualDiskManager mgr = VcContext.getService().getVirtualDiskManager();
            return new VcTask(TaskType.CopyVmdk,
               mgr.copyVirtualDisk(srcDsPath, srcDc.getMoRef(),
                                   dstDsPath, dstDc.getMoRef(), diskSpec, true),
               callback);
         }
      });
      return task;
   }

   protected static void copyVirtualDisk(final String srcDsPath,
         final VcDatacenter srcDc, final String dstDsPath,
         final VcDatacenter dstDc, final VirtualDiskSpec diskSpec)
   throws Exception {
      VcTask task = copyVirtualDisk(srcDsPath, srcDc, dstDsPath, dstDc,
                                    diskSpec, null);
      task.waitForCompletion();
   }

   /**
    * Copy virtual disk from source datastore path to destination datastore
    * path.
    *
    * @param srcDsPath
    *           source pathname to the file
    * @param srcDc
    *           source datacenter
    * @param dstDsPath
    *           destination pathname to the file
    * @param dstDc
    *           destination datacenter
    * @throws Exception
    */
   protected static VcTask moveVirtualDisk(final String srcDsPath,
         final VcDatacenter srcDc, final String dstDsPath,
         final VcDatacenter dstDc,
         final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         @Override
         public VcTask body() throws Exception {
            final VirtualDiskManager mgr = VcContext.getService().getVirtualDiskManager();
            return new VcTask(TaskType.CopyVmdk,
               mgr.moveVirtualDisk(srcDsPath, srcDc.getMoRef(),
                                   dstDsPath, dstDc.getMoRef(), true, null),
               callback);
         }
      });
      return task;
   }

   public static void moveVirtualDisk(final String srcDsPath,
         final VcDatacenter srcDc, final String dstDsPath,
         final VcDatacenter dstDc)
   throws Exception {
      VcTask task = moveVirtualDisk(srcDsPath, srcDc, dstDsPath, dstDc, null);
      task.waitForCompletion();
   }

   /**
    * Delete the virtual disk using its data store path.
    *
    * @param dsPath
    *           pathname to the data-store.
    * @param dc
    *           datacenter
    * @throws Exception
    */
   protected static VcTask deleteVirtualDisk(final String dsPath,
         final VcDatacenter dc, final IVcTaskCallback callback)
         throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         @Override
         public VcTask body() throws Exception {
            VirtualDiskManager mgr = VcContext.getService().getVirtualDiskManager();
            return new VcTask(TaskType.DeleteVmdk, mgr.deleteVirtualDisk(
                  dsPath, dc.getMoRef()), callback);
         }
      });
      return task;
   }

   public static void deleteVirtualDisk(final String dsPath,
         final VcDatacenter dc) throws Exception {
      VcTask task = deleteVirtualDisk(dsPath, dc, null);
      task.waitForCompletion();
   }

   /**
    * Get the UUID of a virtual disk using its datastore path.
    * @param dsPath pathname in the datastore
    * @param dc datacenter
    * @return UUID of the disk
    * @throws Exception
    */
   public static String queryVirtualDiskUuid(final String dsPath,
         final VcDatacenter dc) throws Exception {
      final VirtualDiskManager mgr =
            VcContext.getService().getVirtualDiskManager();
      return mgr.queryVirtualDiskUuid(dsPath, dc.getMoRef());
   }

   /**
    * Copy file from source datastore path to destination datastore path.
    * @param srcDs source datastore
    * @param srcPath source pathname to the file
    * @param dstDs destination datastore
    * @param dstPath destination pathname to the file
    * @throws Exception
    */
   public static VcTask copyFile(final VcDatastore srcDs, final String srcPath,
         final VcDatastore dstDs, final String dstPath,
         final IVcTaskCallback callback) throws Exception {
      final String srcDsPath = getDsPath(srcDs, srcPath);
      final String dstDsPath = getDsPath(dstDs, dstPath);
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         @Override
         public VcTask body() throws Exception {
            FileManager mgr = VcContext.getService().getFileManager();
            return new VcTask(TaskType.CopyFile,
               mgr.copyFile(srcDsPath, srcDs.getDatacenterMoRef(),
                            dstDsPath, dstDs.getDatacenterMoRef(), true),
               callback);
         }
      });
      return task;
   }

   public static void copyFile(final VcDatastore srcDs, final String srcPath,
         final VcDatastore dstDs, final String dstPath) throws Exception {
      VcTask task = copyFile(srcDs, srcPath, dstDs, dstPath, null);
      task.waitForCompletion();
   }

   /**
    * Delete a file or directory in a datastore.
    * @param datastore
    * @param filePath
    * @throws Exception
    */
   public static VcTask deleteFile(final VcDatastore datastore, final String filePath,
         final IVcTaskCallback callback) throws Exception {
      final String dsPath = getDsPath(datastore, filePath);
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         @Override
         public VcTask body() throws Exception {
            FileManager mgr = VcContext.getService().getFileManager();
            return new VcTask(TaskType.DeleteFile,
                  mgr.deleteFile(dsPath, datastore.getDatacenterMoRef()), callback);
         }
      });
      return task;
   }

   public static void deleteFile(final VcDatastore datastore, final String filePath)
   throws Exception {
      VcTask task = deleteFile(datastore, filePath, null);
      task.waitForCompletion();
   }

   /**
    * Move file from source datastore path to destination datastore path.
    * @param srcDs source datastore
    * @param srcPath source pathname to the file
    * @param dstDs destination datastore
    * @param dstPath destination pathname to the file
    * @throws Exception
    */
   public static VcTask moveFile(final VcDatastore srcDs, final String srcPath,
         final VcDatastore dstDs, final String dstPath,
         final IVcTaskCallback callback)
   throws Exception {
      final String srcDsPath = getDsPath(srcDs, srcPath);
      final String dstDsPath = getDsPath(dstDs, dstPath);
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         @Override
         public VcTask body() throws Exception {
            FileManager mgr = VcContext.getService().getFileManager();
            return new VcTask(TaskType.MoveFile,
                  mgr.moveFile(srcDsPath, srcDs.getDatacenterMoRef(),
                               dstDsPath, dstDs.getDatacenterMoRef(), true), callback);
         }
      });
      return task;
   }

   public static void moveFile(final VcDatastore srcDs, final String srcPath,
         final VcDatastore dstDs, final String dstPath) throws Exception {
      VcTask task = moveFile(srcDs, srcPath, dstDs, dstPath, null);
      task.waitForCompletion();
   }

   private static VcTask searchFile(final VcDatastore ds, final String filePath,
         final IVcTaskCallback callback) throws Exception {
      // split file path into dir and fname
      int index = filePath.lastIndexOf('/');
      String fname = filePath.substring(index + 1, filePath.length());
      String fileDir = filePath.substring(0, index);

      final String dsPath = getDsPath(ds, fileDir);
      final SearchSpec spec = new DatastoreBrowser_Impl.SearchSpecImpl();
      spec.setMatchPattern(new String[]{fname});
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         @Override
         public VcTask body() throws Exception {
            Datastore mo = ds.getManagedObject();
            DatastoreBrowser browser = MoUtil.getManagedObject(mo.getBrowser());
            return new VcTask(TaskType.SearchFile,
                              browser.search(dsPath, spec), callback);
         }
      });
      return task;
   }

   /**
    * Search for a file in a datastore.
    * @param ds datastore
    * @param filePath file path in the datastore
    * @return file name if the file exists
    * @throws Exception
    */
   public static String searchFile(final VcDatastore ds, final String filePath)
   throws Exception {
      try {
         VcTask task = searchFile(ds, filePath, null);
         // XXX This task frequently raises a WARN in VcTask for dropped VC taskInfo.state.
         task.waitForCompletion();
         SearchResults results = (SearchResults)task.getTaskResult();
         FileInfo[] files = results.getFile();
         if (files != null && files.length > 0) {
            AuAssert.check(files.length == 1);
            // Only return the file name. It's directory is reported elsewhere.
            return files[0].getPath();
         }
         return null;
      } catch (FileNotFound e) {
         // return null if the file is not found
         return null;
      }
   }
}
