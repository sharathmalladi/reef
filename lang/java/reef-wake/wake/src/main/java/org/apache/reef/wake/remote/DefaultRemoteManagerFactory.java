/*
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
 */
package org.apache.reef.wake.remote;

import org.apache.reef.tang.Injector;
import org.apache.reef.tang.Tang;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.tang.exceptions.InjectionException;
import org.apache.reef.wake.EventHandler;
import org.apache.reef.wake.remote.address.LocalAddressProvider;
import org.apache.reef.wake.remote.ports.TcpPortProvider;
import org.apache.reef.wake.remote.transport.TransportFactory;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of RemoteManagerFactory.
 */
final class DefaultRemoteManagerFactory implements RemoteManagerFactory {

  private static final Logger LOG = Logger.getLogger(DefaultRemoteManagerFactory.class.getName());
  private final Injector injector = Tang.Factory.getTang().newInjector();

  private final Codec<?> codec;
  private final EventHandler<Throwable> errorHandler;
  private final boolean orderingGuarantee;
  private final int numberOfTries;
  private final int retryTimeout;
  private final LocalAddressProvider localAddressProvider;
  private final TransportFactory transportFactory;
  private final TcpPortProvider tcpPortProvider;

  @Inject
  private DefaultRemoteManagerFactory(
      @Parameter(RemoteConfiguration.MessageCodec.class) final Codec<?> codec,
      @Parameter(RemoteConfiguration.ErrorHandler.class) final EventHandler<Throwable> errorHandler,
      @Parameter(RemoteConfiguration.OrderingGuarantee.class) final boolean orderingGuarantee,
      @Parameter(RemoteConfiguration.NumberOfTries.class) final int numberOfTries,
      @Parameter(RemoteConfiguration.RetryTimeout.class) final int retryTimeout,
      final LocalAddressProvider localAddressProvider,
      final TransportFactory tpFactory,
      final TcpPortProvider tcpPortProvider) {

    LOG.log(Level.INFO, "DefaultRemoteManagerFactory is injected");
    LOG.log(Level.INFO, "localAddressProvider is " + localAddressProvider.getLocalAddress());
    LOG.log(Level.INFO, "localAddressProvider is " + localAddressProvider.getClass().getName());
    this.codec = codec;
    this.errorHandler = errorHandler;
    this.orderingGuarantee = orderingGuarantee;
    this.numberOfTries = numberOfTries;
    this.retryTimeout = retryTimeout;
    this.localAddressProvider = localAddressProvider;
    this.transportFactory = tpFactory;
    this.tcpPortProvider = tcpPortProvider;
  }

  @Override
  public RemoteManager getInstance(final String newRmName) {
    return getInstance(newRmName + "1", 0, this.codec, this.errorHandler);
  }

  @Override
  public <T> RemoteManager getInstance(final String newRmName,
                                       final String newHostAddress,
                                       final int newListeningPort,
                                       final Codec<T> newCodec) {
    return getInstance(newRmName + "2", newHostAddress, newListeningPort, newCodec,
        this.errorHandler, this.orderingGuarantee, this.numberOfTries, this.retryTimeout,
        this.localAddressProvider, this.tcpPortProvider);
  }

  @Override
  public <T> RemoteManager getInstance(final String newRmName,
                                       final String newHostAddress,
                                       final int newListeningPort,
                                       final Codec<T> newCodec,
                                       final EventHandler<Throwable> newErrorHandler,
                                       final boolean newOrderingGuarantee,
                                       final int newNumberOfTries,
                                       final int newRetryTimeout) {
    return getInstance(newRmName + "3", newHostAddress, newListeningPort, newCodec, newErrorHandler,
        newOrderingGuarantee, newNumberOfTries, newRetryTimeout, this.localAddressProvider, this.tcpPortProvider);
  }

  @Override
  public <T> RemoteManager getInstance(final String newRmName,
                                       final Codec<T> newCodec,
                                       final EventHandler<Throwable> newErrorHandler) {
    return getInstance(newRmName + "4", 0, newCodec, newErrorHandler);
  }

  @Override
  public <T> RemoteManager getInstance(final String newRmName,
                                       final int newListeningPort,
                                       final Codec<T> newCodec,
                                       final EventHandler<Throwable> newErrorHandler) {
    return getInstance(newRmName + "5", null, newListeningPort, newCodec, newErrorHandler, this.orderingGuarantee,
        this.numberOfTries, this.retryTimeout, this.localAddressProvider, this.tcpPortProvider);
  }

  @Override
  public <T> RemoteManager getInstance(final String newRmName,
                                       final String newHostAddress,
                                       final int newListeningPort,
                                       final Codec<T> newCodec,
                                       final EventHandler<Throwable> newErrorHandler,
                                       final boolean newOrderingGuarantee,
                                       final int newNumberOfTries,
                                       final int newRetryTimeout,
                                       final LocalAddressProvider newLocalAddressProvider,
                                       final TcpPortProvider newTcpPortProvider) {
    try {

      LOG.log(Level.INFO, "RMName is " + newRmName);
      LOG.log(Level.INFO, "newHostAddress is " + newHostAddress);
      LOG.log(Level.INFO, "newLocalAddressProvider is " + newLocalAddressProvider.getClass().getName());
      LOG.log(Level.INFO, "newLocalAddressProvider.LocalAddress is " + newLocalAddressProvider.getLocalAddress());
      final Injector newInjector = injector.forkInjector();

      if (newHostAddress != null) {
        newInjector.bindVolatileParameter(RemoteConfiguration.HostAddress.class, newHostAddress);
      }

      if (newListeningPort > 0) {
        newInjector.bindVolatileParameter(RemoteConfiguration.Port.class, newListeningPort);
      }

      newInjector.bindVolatileParameter(RemoteConfiguration.ManagerName.class, newRmName);
      newInjector.bindVolatileParameter(RemoteConfiguration.MessageCodec.class, newCodec);
      newInjector.bindVolatileParameter(RemoteConfiguration.ErrorHandler.class, newErrorHandler);
      newInjector.bindVolatileParameter(RemoteConfiguration.OrderingGuarantee.class, newOrderingGuarantee);
      newInjector.bindVolatileParameter(RemoteConfiguration.NumberOfTries.class, newNumberOfTries);
      newInjector.bindVolatileParameter(RemoteConfiguration.RetryTimeout.class, newRetryTimeout);
      newInjector.bindVolatileInstance(LocalAddressProvider.class, newLocalAddressProvider);
      newInjector.bindVolatileInstance(TransportFactory.class, this.transportFactory);
      newInjector.bindVolatileInstance(TcpPortProvider.class, newTcpPortProvider);

      return newInjector.getInstance(RemoteManager.class);

    } catch (final InjectionException e) {
      throw new RuntimeException(e);
    }
  }
}
