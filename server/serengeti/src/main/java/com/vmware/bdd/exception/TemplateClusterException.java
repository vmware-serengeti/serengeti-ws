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
package com.vmware.bdd.exception;

public class TemplateClusterException extends BddException {
   private static final long serialVersionUID = 1L;
   public TemplateClusterException() {
   }

   public TemplateClusterException(Throwable cause, String errorId, Object... detail) {
      super(cause, "TEMPLATE_CLUSTER", errorId, detail);
   }

   public static TemplateClusterException TEMPLATE_NODEGROUPS_UNDEFINED() {
      return new TemplateClusterException(null, "TEMPLATE_NODEGROUPS_UNDEFINED");
   }

   public static TemplateClusterException TEMPLATE_ROLES_EMPTY(String gName) {
      return new TemplateClusterException(null, "TEMPLATE_ROLES_EMPTY", gName);
   }

   public static TemplateClusterException INCOMPLETE_TEMPLATE_GROUPS() {
      return new TemplateClusterException(null, "INCOMPLETE_TEMPLATE_GROUPS");
   }
}
