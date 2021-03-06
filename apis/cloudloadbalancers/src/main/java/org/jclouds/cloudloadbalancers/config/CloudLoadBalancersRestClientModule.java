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
package org.jclouds.cloudloadbalancers.config;

import static com.google.common.collect.Iterables.get;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

import org.jclouds.cloudloadbalancers.CloudLoadBalancersAsyncClient;
import org.jclouds.cloudloadbalancers.CloudLoadBalancersClient;
import org.jclouds.cloudloadbalancers.features.LoadBalancerAsyncClient;
import org.jclouds.cloudloadbalancers.features.LoadBalancerClient;
import org.jclouds.cloudloadbalancers.features.NodeAsyncClient;
import org.jclouds.cloudloadbalancers.features.NodeClient;
import org.jclouds.cloudloadbalancers.functions.ConvertLB;
import org.jclouds.cloudloadbalancers.handlers.ParseCloudLoadBalancersErrorFromHttpResponse;
import org.jclouds.cloudloadbalancers.reference.RackspaceConstants;
import org.jclouds.http.HttpErrorHandler;
import org.jclouds.http.HttpRetryHandler;
import org.jclouds.http.RequiresHttp;
import org.jclouds.http.annotation.ClientError;
import org.jclouds.http.annotation.Redirection;
import org.jclouds.http.annotation.ServerError;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.json.config.GsonModule.DateAdapter;
import org.jclouds.json.config.GsonModule.Iso8601DateAdapter;
import org.jclouds.location.Provider;
import org.jclouds.location.Region;
import org.jclouds.location.config.ProvideRegionToURIViaProperties;
import org.jclouds.logging.Logger.LoggerFactory;
import org.jclouds.openstack.keystone.v1_1.config.AuthenticationServiceModule;
import org.jclouds.openstack.keystone.v1_1.domain.Auth;
import org.jclouds.openstack.keystone.v1_1.handlers.RetryOnRenew;
import org.jclouds.rest.ConfiguresRestClient;
import org.jclouds.rest.config.RestClientModule;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Configures theRackspace Cloud Load Balancers connection.
 * 
 * @author Adrian Cole
 */
@RequiresHttp
@ConfiguresRestClient
public class CloudLoadBalancersRestClientModule extends
      RestClientModule<CloudLoadBalancersClient, CloudLoadBalancersAsyncClient> {

   public static final Map<Class<?>, Class<?>> DELEGATE_MAP = ImmutableMap.<Class<?>, Class<?>> builder()//
         .put(LoadBalancerClient.class, LoadBalancerAsyncClient.class)//
         .put(NodeClient.class, NodeAsyncClient.class)//
         .build();

   private final AuthenticationServiceModule module;

   public CloudLoadBalancersRestClientModule(AuthenticationServiceModule module) {
      super(CloudLoadBalancersClient.class, CloudLoadBalancersAsyncClient.class, DELEGATE_MAP);
      this.module = module;
   }

   public CloudLoadBalancersRestClientModule() {
      this(new AuthenticationServiceModule());
   }
   protected void bindRegionsToProvider() {
      bindRegionsToProvider(RegionUrisFromPropertiesAndAccountIDPathSuffix.class);
   }

   @Singleton
   public static class RegionUrisFromPropertiesAndAccountIDPathSuffix extends ProvideRegionToURIViaProperties {

      private final String accountID;
      private final javax.inject.Provider<UriBuilder> builders;

      @Inject
      protected RegionUrisFromPropertiesAndAccountIDPathSuffix(Injector injector,
            javax.inject.Provider<UriBuilder> builders, @Named(RackspaceConstants.PROPERTY_ACCOUNT_ID) String accountID) {
         super(injector);
         this.builders = builders;
         this.accountID = accountID;
      }
      
      @Override
      public Map<String, URI> get() {
         return Maps.transformValues(super.get(), new Function<URI, URI>(){

            @Override
            public URI apply(URI input) {
               return builders.get().uri(input).path(accountID).build();
            }
            
         });
      }
   }

   protected void bindRegionsToProvider(Class<? extends javax.inject.Provider<Map<String, URI>>> providerClass) {
      bind(new TypeLiteral<Map<String, URI>>() {
      }).annotatedWith(Region.class).toProvider(providerClass).in(Scopes.SINGLETON);
   }

   @Override
   protected void configure() {
      install(module);
      bind(DateAdapter.class).to(Iso8601DateAdapter.class);
      bindRegionsToProvider();
      install(new FactoryModuleBuilder().build(ConvertLB.Factory.class));
      super.configure();
   }

   @Provides
   @Singleton
   @Named(RackspaceConstants.PROPERTY_ACCOUNT_ID)
   protected String accountID(Auth response) {
      URI serverURL = Iterables.get(response.getServiceCatalog().get("cloudServers"), 0).getPublicURL();
      return serverURL.getPath().substring(serverURL.getPath().lastIndexOf('/') + 1);
   }

   @Provides
   @Singleton
   @Region
   public Set<String> regions(@Region Map<String, URI> endpoints) {
      return endpoints.keySet();
   }

   @Provides
   @Singleton
   @Nullable
   @Region
   protected String getDefaultRegion(@Provider URI uri, @Region Map<String, URI> map, LoggerFactory logFactory) {
      String region = ImmutableBiMap.copyOf(map).inverse().get(uri);
      if (region == null && map.size() > 0) {
         logFactory.getLogger(getClass().getName()).warn(
               "failed to find region for current endpoint %s in %s; choosing first: %s", uri, map, region);
         region = get(map.keySet(), 0);
      }
      return region;
   }

   @Override
   protected void bindErrorHandlers() {
      bind(HttpErrorHandler.class).annotatedWith(Redirection.class).to(
            ParseCloudLoadBalancersErrorFromHttpResponse.class);
      bind(HttpErrorHandler.class).annotatedWith(ClientError.class).to(
            ParseCloudLoadBalancersErrorFromHttpResponse.class);
      bind(HttpErrorHandler.class).annotatedWith(ServerError.class).to(
            ParseCloudLoadBalancersErrorFromHttpResponse.class);
   }

   @Override
   protected void bindRetryHandlers() {
      bind(HttpRetryHandler.class).annotatedWith(ClientError.class).to(RetryOnRenew.class);
   }
}
