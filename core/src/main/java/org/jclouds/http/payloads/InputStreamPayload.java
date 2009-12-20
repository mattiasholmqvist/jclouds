/**
 *
 * Copyright (C) 2009 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
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
 * ====================================================================
 */
package org.jclouds.http.payloads;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jclouds.http.Payload;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

/**
 * @author Adrian Cole
 */
public class InputStreamPayload implements Payload {
   private final InputStream content;

   public InputStreamPayload(InputStream content) {
      this.content = checkNotNull(content, "content");
   }

   public InputStream getRawContent() {
      return content;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream getContent() {
      return content;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isRepeatable() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeTo(OutputStream outstream) throws IOException {
      InputStream in = getContent();
      try {
         ByteStreams.copy(getContent(), outstream);
      } finally {
         Closeables.closeQuietly(in);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Long calculateSize() {
      return null;
   }
}