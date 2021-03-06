/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.glesys.options;

import org.jclouds.http.options.BaseHttpRequestOptions;

/**
 * @author Adam Lowe
 */
public class AddRecordOptions extends BaseHttpRequestOptions {

   public static class Builder {
      /**
       * @see AddRecordOptions#ttl
       */
      public static AddRecordOptions ttl(int ttl) {
         AddRecordOptions options = new AddRecordOptions();
         return options.ttl(ttl);
      }

      /**
       * @see AddRecordOptions#mxPriority
       */
      public static AddRecordOptions mxPriority(int mxPriority) {
         AddRecordOptions options = new AddRecordOptions();
         return options.mxPriority(mxPriority);
      }
   }

   /** Configure TTL/Time-to-live for record */
   public AddRecordOptions ttl(int ttl) {
      formParameters.put("ttl", Integer.toString(ttl));
      return this;
   }

   /** Configure the priority of an MX record */
   public AddRecordOptions mxPriority(int mxPriority) {
      formParameters.put("mx_priority", Integer.toString(mxPriority));
      return this;
   }

}