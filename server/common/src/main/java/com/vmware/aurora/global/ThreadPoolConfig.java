package com.vmware.aurora.global;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.util.Arrays;

/**
 * Created by Chaolong on 9/30/2015.
 */
public class ThreadPoolConfig {

	private int corePoolSize;
	private int maxPoolSize;
	private int workQueue;
	private String namePrefix;

	public ThreadPoolConfig(String[] config) {
		corePoolSize = Integer.parseInt(config[0]);
		maxPoolSize = Integer.parseInt(config[1]);
		workQueue = Integer.parseInt(config[2]);
		namePrefix = config[3];
	}

	public int getCorePoolSize() {
		return corePoolSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public int getWorkQueue() {
		return workQueue;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public String toString() {
		ToStringBuilder ts = new ToStringBuilder(this).append("corePoolSize",getCorePoolSize()).append("maxPoolSize", getMaxPoolSize()).append("workQueue",getWorkQueue()).append("namePrefix",getNamePrefix());
		return ts.toString();
	}
}
