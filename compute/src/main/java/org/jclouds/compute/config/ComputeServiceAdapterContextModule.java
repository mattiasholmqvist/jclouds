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
package org.jclouds.compute.config;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.inject.util.Types.newParameterizedType;
import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.collect.Memoized;
import org.jclouds.collect.TransformingSetSupplier;
import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.ImageBuilder;
import org.jclouds.compute.internal.ComputeServiceContextImpl;
import org.jclouds.compute.strategy.CreateNodeWithGroupEncodedIntoName;
import org.jclouds.compute.strategy.DestroyNodeStrategy;
import org.jclouds.compute.strategy.GetNodeMetadataStrategy;
import org.jclouds.compute.strategy.ListNodesStrategy;
import org.jclouds.compute.strategy.PopulateDefaultLoginCredentialsForImageStrategy;
import org.jclouds.compute.strategy.RebootNodeStrategy;
import org.jclouds.compute.strategy.ResumeNodeStrategy;
import org.jclouds.compute.strategy.SuspendNodeStrategy;
import org.jclouds.compute.strategy.impl.AdaptingComputeServiceStrategies;
import org.jclouds.domain.Location;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.rest.suppliers.MemoizedRetryOnTimeOutButNotOnAuthorizationExceptionSupplier;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

/**
 * 
 * @author Adrian Cole
 */
public class ComputeServiceAdapterContextModule<S, A, N, H, I, L> extends BaseComputeServiceContextModule {

   private Class<A> asyncClientType;
   private Class<S> syncClientType;

   public ComputeServiceAdapterContextModule(Class<S> syncClientType, Class<A> asyncClientType) {
      this.syncClientType = syncClientType;
      this.asyncClientType = asyncClientType;
   }

   @SuppressWarnings( { "unchecked", "rawtypes" })
   @Override
   protected void configure() {
      super.configure();
      bind(new TypeLiteral<ComputeServiceContext>() {
      }).to(
               (TypeLiteral) TypeLiteral.get(newParameterizedType(ComputeServiceContextImpl.class, syncClientType,
                        asyncClientType))).in(Scopes.SINGLETON);
   }

   @Override
   protected void configureLocationModule() {
      // configuring below
   }

   @Provides
   @Singleton
   @Memoized
   protected Supplier<Set<? extends Location>> supplyLocationCache(@Named(PROPERTY_SESSION_INTERVAL) long seconds,
            final ComputeServiceAdapter<N, H, I, L> adapter, final Function<L, Location> transformer) {
      return new MemoizedRetryOnTimeOutButNotOnAuthorizationExceptionSupplier<Set<? extends Location>>(authException,
               seconds, new Supplier<Set<? extends Location>>() {
                  @Override
                  public Set<? extends Location> get() {
                     return ImmutableSet.<Location> copyOf(transform(filter(adapter.listLocations(), notNull()),
                              transformer));
                  }
               });
   }

   @Provides
   @Singleton
   protected Supplier<Set<? extends Hardware>> provideHardware(final ComputeServiceAdapter<N, H, I, L> adapter,
            Function<H, Hardware> transformer) {
      return new TransformingSetSupplier<H, Hardware>(new Supplier<Iterable<H>>() {

         @Override
         public Iterable<H> get() {
            return filter(adapter.listHardwareProfiles(), notNull());
         }

      }, transformer);
   }

   @Provides
   @Singleton
   protected Supplier<Set<? extends Image>> provideImages(final ComputeServiceAdapter<N, H, I, L> adapter,
            Function<I, Image> transformer, AddDefaultCredentialsToImage addDefaultCredentialsToImage) {
      return new TransformingSetSupplier<I, Image>(new Supplier<Iterable<I>>() {

         @Override
         public Iterable<I> get() {
            return filter(adapter.listImages(), notNull());
         }

      }, compose(addDefaultCredentialsToImage, transformer));
   }

   @Singleton
   public static class AddDefaultCredentialsToImage implements Function<Image, Image> {
      private final PopulateDefaultLoginCredentialsForImageStrategy credsForImage;

      @Inject
      public AddDefaultCredentialsToImage(PopulateDefaultLoginCredentialsForImageStrategy credsForImage) {
         this.credsForImage = credsForImage;
      }

      @Override
      public Image apply(Image arg0) {
         if (arg0 == null)
            return null;
         LoginCredentials credentials = credsForImage.apply(arg0);
         return credentials != null ? ImageBuilder.fromImage(arg0).defaultCredentials(credentials).build() : arg0;
      }

      @Override
      public String toString() {
         return "addDefaultCredentialsToImage()";
      }
   }

   @Provides
   @Singleton
   protected CreateNodeWithGroupEncodedIntoName defineAddNodeWithTagStrategy(
            AdaptingComputeServiceStrategies<N, H, I, L> in) {
      return in;
   }

   @Provides
   @Singleton
   protected DestroyNodeStrategy defineDestroyNodeStrategy(AdaptingComputeServiceStrategies<N, H, I, L> in) {
      return in;
   }

   @Provides
   @Singleton
   protected GetNodeMetadataStrategy defineGetNodeMetadataStrategy(AdaptingComputeServiceStrategies<N, H, I, L> in) {
      return in;
   }

   @Provides
   @Singleton
   protected ListNodesStrategy defineListNodesStrategy(AdaptingComputeServiceStrategies<N, H, I, L> in) {
      return in;
   }

   @Provides
   @Singleton
   protected RebootNodeStrategy defineRebootNodeStrategy(AdaptingComputeServiceStrategies<N, H, I, L> in) {
      return in;
   }

   @Provides
   @Singleton
   protected ResumeNodeStrategy defineStartNodeStrategy(AdaptingComputeServiceStrategies<N, H, I, L> in) {
      return in;
   }

   @Provides
   @Singleton
   protected SuspendNodeStrategy defineStopNodeStrategy(AdaptingComputeServiceStrategies<N, H, I, L> in) {
      return in;
   }
}