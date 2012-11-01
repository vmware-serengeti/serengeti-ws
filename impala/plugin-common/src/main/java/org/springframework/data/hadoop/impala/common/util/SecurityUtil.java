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
package org.springframework.data.hadoop.impala.common.util;

import java.security.Permission;



/**
 * Utility class to prevent System.exit() call. To use this class, write your code as following:
 * <code>
		forbidSystemExitCall();

		try {
			// your reference code.
			 
		} catch (ExitTrappedException e) {
			System.out.println("catch exit trap");
		finally {
			enableSystemExitCall();
		}
 * </code>
 * 
 * @author Jarred Li
 *
 */

public class SecurityUtil {

	public static class ExitTrappedException extends SecurityException {
		private static final long serialVersionUID = 8542706657719758115L;
		
	}

	public void forbidSystemExitCall() {
		final SecurityManager securityManager = new SecurityManager() {
			@Override
			public void checkPermission(Permission permission) {
				if (permission.getName().startsWith("exitVM")) {
					throw new ExitTrappedException();
				}
			}
		};
		System.setSecurityManager(securityManager);
	}

	public void enableSystemExitCall() {
		System.setSecurityManager(null);
	}
	
}
