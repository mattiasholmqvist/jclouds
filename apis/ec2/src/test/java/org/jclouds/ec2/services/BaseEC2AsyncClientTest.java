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
package org.jclouds.ec2.services;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.inject.Singleton;

import org.jclouds.aws.domain.Region;
import org.jclouds.aws.filters.FormSigner;
import org.jclouds.compute.domain.Image;
import org.jclouds.date.DateService;
import org.jclouds.ec2.EC2AsyncClient;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.EC2ContextBuilder;
import org.jclouds.ec2.EC2PropertiesBuilder;
import org.jclouds.ec2.compute.domain.RegionAndName;
import org.jclouds.ec2.config.EC2RestClientModule;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.RequiresHttp;
import org.jclouds.rest.ConfiguresRestClient;
import org.jclouds.rest.RestClientTest;
import org.jclouds.rest.RestContextFactory;
import org.jclouds.rest.RestContextSpec;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.inject.Provides;

/**
 * @author Adrian Cole
 */
@Test(groups = "unit")
public abstract class BaseEC2AsyncClientTest<T> extends RestClientTest<T> {
   @RequiresHttp
   @ConfiguresRestClient
   protected static class StubEC2RestClientModule extends EC2RestClientModule<EC2Client, EC2AsyncClient> {

      public StubEC2RestClientModule() {
         super(EC2Client.class, EC2AsyncClient.class, DELEGATE_MAP);
      }

      @Provides
      @Singleton
      LoadingCache<RegionAndName, Image> provide(){
         return CacheBuilder.newBuilder().build(new CacheLoader<RegionAndName, Image>() {

            @Override
            public Image load(RegionAndName key) throws Exception {
               return null;
            }

         });
      }
      
      @Override
      protected String provideTimeStamp(DateService dateService, int expiration) {
         return "2009-11-08T15:54:08.897Z";
      }

      protected void bindRegionsToProvider() {
         bindRegionsToProvider(Regions.class);
      }

      static class Regions implements javax.inject.Provider<Map<String, URI>> {
         @Override
         public Map<String, URI> get() {
            return ImmutableMap.<String, URI> of(Region.EU_WEST_1, URI.create("https://ec2.eu-west-1.amazonaws.com"),
                  Region.US_EAST_1, URI.create("https://ec2.us-east-1.amazonaws.com"), Region.US_WEST_1,
                  URI.create("https://ec2.us-west-1.amazonaws.com"));
         }
      }

      protected void bindZonesToProvider() {
         bindZonesToProvider(Zones.class);
      }

      static class Zones implements javax.inject.Provider<Map<String, String>> {
         @Override
         public Map<String, String> get() {
            return ImmutableMap.<String, String> of("us-east-1a", "us-east-1");
         }
      }
   }

   protected FormSigner filter;

   @Override
   protected void checkFilters(HttpRequest request) {
      assertEquals(request.getFilters().size(), 1);
      assertEquals(request.getFilters().get(0).getClass(), FormSigner.class);
   }

   public BaseEC2AsyncClientTest() {
      super();
   }

   @Override
   @BeforeTest
   protected void setupFactory() throws IOException {
      super.setupFactory();
      this.filter = injector.getInstance(FormSigner.class);
   }

   @Override
   protected Module createModule() {
      return new StubEC2RestClientModule();
   }

   protected String provider = "ec2";

   /**
    * this is only here as "ec2" is not in rest.properties
    */
   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public RestContextSpec<?, ?> createContextSpec() {
      return RestContextFactory.<EC2Client, EC2AsyncClient> contextSpec(provider, "https://ec2.us-east-1.amazonaws.com",
            EC2AsyncClient.VERSION, "", "", "identity", "credential", EC2Client.class, EC2AsyncClient.class,
            (Class) EC2PropertiesBuilder.class, (Class) EC2ContextBuilder.class, ImmutableSet.<Module> of());
   }
}
