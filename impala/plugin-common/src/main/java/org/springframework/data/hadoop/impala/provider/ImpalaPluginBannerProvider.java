/*
 * Copyright 2011-2012 the original author or authors.
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
package org.springframework.data.hadoop.impala.provider;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.plugin.BannerProvider;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Banner Provider to customize Spring Shell Banner
 * 
 * @author Jarred Li
 *
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ImpalaPluginBannerProvider implements BannerProvider, CommandMarker {

	@CliCommand(value = { "version" }, help = "Displays current CLI version")
	public String getBanner() {
		StringBuilder buf = new StringBuilder();
		buf.append("_________ _______  ________  _______  _        _______ " + StringUtils.LINE_SEPARATOR);
		buf.append("\\__   __/(       )(  ____  )(  ___  )( \\      (  ___  )" + StringUtils.LINE_SEPARATOR);
		buf.append("   ) (   | () () || (    ) || (   ) || (      | (   ) |" + StringUtils.LINE_SEPARATOR);
		buf.append("   | |   | || || || (____) || (___) || |      | (___) |" + StringUtils.LINE_SEPARATOR);
		buf.append("   | |   | |(_)| ||  ______)|  ___  || |      |  ___  |" + StringUtils.LINE_SEPARATOR);
		buf.append("   | |   | |   | || (       | (   ) || |      | (   ) |" + StringUtils.LINE_SEPARATOR);
		buf.append("___) (___| )   ( || )       | )   ( || (____/\\| )   ( |" + StringUtils.LINE_SEPARATOR);
		buf.append("\\_______/|/     \\||/        |/     \\|(_______/|/     \\|" + StringUtils.LINE_SEPARATOR);

		buf.append("Version:" + this.getVersion());
		return buf.toString();
	}

	public String getVersion() {
		Package pkg = ImpalaPluginBannerProvider.class.getPackage();
		String version = (pkg != null ? pkg.getImplementationVersion() : "");
		return (StringUtils.hasText(version) ? version : "Unknown Version");
	}

	public String getWelcomeMessage() {
		return "Welcome to Impala CLI";
	}

	public String name() {
		return "Impala CLI Banner Provider";
	}
}
