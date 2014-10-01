/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.cli.commands;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.plugin.BannerProvider;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;

import com.vmware.bdd.utils.Constants;

/**
 * The class to set cli banners through spring shell
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CliBannerProvider implements BannerProvider, CommandMarker {

	/* (non-Javadoc)
	 * @see org.springframework.shell.plugin.BannerProvider#getBanner()
	 */
	//@CliCommand(value = { "version" }, help = "Displays current CLI version")
	public String getBanner() {
		StringBuilder buf = new StringBuilder();
		buf.append("=================================================" + OsUtils.LINE_SEPARATOR);
		buf.append("*  _____                                 _   _  *" + OsUtils.LINE_SEPARATOR);
		buf.append("* / ____|  ___ _ __ ___ _ __   __ _  ___| |_(_) *" + OsUtils.LINE_SEPARATOR);
		buf.append("* \\____ \\ / _ \\ '__/ _ \\ '_ \\ / _` |/ _ \\ __| | *" + OsUtils.LINE_SEPARATOR);
		buf.append("*  ____) |  __/ | |  __/ | | | (_| |  __/ |_| | *" + OsUtils.LINE_SEPARATOR);
		buf.append("* |_____/ \\___|_|  \\___|_| |_|\\__, |\\___|\\__|_| *" + OsUtils.LINE_SEPARATOR);
		buf.append("*                             |___/             *" + OsUtils.LINE_SEPARATOR);
		buf.append("*                                               *" + OsUtils.LINE_SEPARATOR);
		buf.append("=================================================" + OsUtils.LINE_SEPARATOR);
		buf.append("Version: " + this.getVersion());
		return buf.toString();

	}

	/* (non-Javadoc)
	 * @see org.springframework.shell.plugin.BannerProvider#getVersion()
	 */
	public String getVersion() {
		return Constants.VERSION;
	}

	/* (non-Javadoc)
	 * @see org.springframework.shell.plugin.BannerProvider#getWelcomMessage()
	 */
	public String getWelcomeMessage() {
		return "Welcome to Serengeti CLI";
	}
	
	public String getProviderName() {
	   return "Serengeti CLI";
	}
}
