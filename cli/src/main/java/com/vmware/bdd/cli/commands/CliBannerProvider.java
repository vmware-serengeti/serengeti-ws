/*
 * Copyright 2011-2012 the original author or authors.
 * Portions copyright(c) 2012-2013 VMware, Inc. All rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vmware.bdd.cli.commands;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.plugin.support.DefaultBannerProvider;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

/**
 * The class to set cli banners through spring shell
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CliBannerProvider extends DefaultBannerProvider implements CommandMarker {

	/* (non-Javadoc)
	 * @see org.springframework.shell.plugin.BannerProvider#getBanner()
	 */
	@CliCommand(value = { "version" }, help = "Displays current CLI version")
	public String getBanner() {
		StringBuffer buf = new StringBuffer();
		buf.append("=================================================" + StringUtils.LINE_SEPARATOR);
		buf.append("*  _____                                 _   _  *" + StringUtils.LINE_SEPARATOR);
		buf.append("* / ____|  ___ _ __ ___ _ __   __ _  ___| |_(_) *" + StringUtils.LINE_SEPARATOR);
		buf.append("* \\____ \\ / _ \\ '__/ _ \\ '_ \\ / _` |/ _ \\ __| | *" + StringUtils.LINE_SEPARATOR);
		buf.append("*  ____) |  __/ | |  __/ | | | (_| |  __/ |_| | *" + StringUtils.LINE_SEPARATOR);
		buf.append("* |_____/ \\___|_|  \\___|_| |_|\\__, |\\___|\\__|_| *" + StringUtils.LINE_SEPARATOR);
		buf.append("*                             |___/             *" + StringUtils.LINE_SEPARATOR);
		buf.append("*                                               *" + StringUtils.LINE_SEPARATOR);
		buf.append("=================================================" + StringUtils.LINE_SEPARATOR);
		buf.append("Version: " + this.getVersion());
		return buf.toString();

	}

	/* (non-Javadoc)
	 * @see org.springframework.shell.plugin.BannerProvider#getVersion()
	 */
	public String getVersion() {
		return "0.8.0";
	}

	/* (non-Javadoc)
	 * @see org.springframework.shell.plugin.BannerProvider#getWelcomMessage()
	 */
	public String getWelcomeMessage() {
		return "Welcome to Serengeti CLI";
	}
	
	public String name() {
	   return "Serengeti CLI";
	}
}
