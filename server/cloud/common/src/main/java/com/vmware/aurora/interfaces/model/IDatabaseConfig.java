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
package com.vmware.aurora.interfaces.model;

/**
 * <code>IDatabaseConfig</code> interface exposes a subset of database
 * configurations to UI and Web Services clients. <br>
 *
 * This interface provides a read/write view into backend CMS objects.
 *
 */
public interface IDatabaseConfig  {

   /**
    * The priority to consume excess resources like CPU and Memory.
    */
   public enum Priority { Automatic, Low, Normal, High }

   //
   // Resource configuration
   //
   /**
    * Return the number of vCPU.
    */
   public short getvCpuNumber();

   /**
    * Return the number of vCPU.
    */
   public void setvCpuNumber(short vCpuNumber);

   /**
    * Return the memory size of the virtual machine that hosts the database, in MB.
    */
   public int getMemoryMb();

   /**
    * Set the memory size of the virtual machine that hosts the database, in MB.
    */
   public void setMemoryMb(int memoryMb);

   /**
    * Return the CPU reservation of the virtual machine that hosts the database, in MHz.
    */
   public int getCpuReservationMHz();

   /**
    * Set the CPU reservation of the virtual machine that hosts the database, in MHz.
    */
   public void setCpuReservationMHz(int cpuReservationMHz);

   /**
    * Return the Memory reservation of the virtual machine that hosts the database, in MB.
    */
   public int getMemoryReservationMb();

   /**
    * Set the Memory reservation of the virtual machine that hosts the database, in MB.
    */
   public void setMemoryReservationMb(int memoryReservationMb);

   /**
    * Return the database data storage, in GB.
    */
   public int getStorageGb();

   /**
    * Set the database data storage, in GB.
    */
   public void setStorageGb(int storageGb);

   /**
    * Return the database swap storage, in MB.
    */
   public int getSwapStorageMb();

   /**
    * Set the database swap storage, in MB.
    */
   public void setSwapStorageMb(int swapStorageMb);

   /**
    * Return the priority to use excess resource for the virtual machine
    * that hosts the database. Though vSphere supports CPU and memory to use different
    * priority, here we just used the same priority for all.
    */
   public Priority getPriority();

   /**
    * Set the priority to use excess CPU and memory resource for the virtual machine
    * that hosts the database. Though vSphere supports CPU and memory to use different
    * priority, here we just used the same priority for all.
    */
   public void setPriority(Priority priority);

   //
   // High availability configuration
   //
   /**
    * Return true if the high availability is enabled for the virtual machine
    * that hosts the database; otherwise false.
    */
   public boolean isHighAvailabilityEnabled();

   /**
    * Set whether the high availability is enabled for the virtual machine that
    * hosts the database.
    */
   public void setHighAvailabilityEnabled(boolean highAvailabilityEnabled);
}
