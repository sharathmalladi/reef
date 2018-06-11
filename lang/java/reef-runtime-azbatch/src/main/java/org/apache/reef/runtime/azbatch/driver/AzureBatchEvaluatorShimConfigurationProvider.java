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
package org.apache.reef.runtime.azbatch.driver;

import org.apache.reef.annotations.audience.Private;
import org.apache.reef.driver.parameters.DriverMemory;
import org.apache.reef.runtime.azbatch.client.AzureBatchDriverConfigurationProviderImpl;
import org.apache.reef.runtime.azbatch.evaluator.EvaluatorShimConfiguration;
import org.apache.reef.runtime.common.utils.RemoteManager;
import org.apache.reef.tang.Configuration;
import org.apache.reef.tang.Injector;
import org.apache.reef.tang.Tang;
import org.apache.reef.tang.exceptions.InjectionException;
import org.apache.reef.wake.remote.RemoteConfiguration;
import org.apache.reef.wake.remote.address.ContainerBasedLocalAddressProvider;
import org.apache.reef.wake.remote.address.HostnameBasedLocalAddressProvider;
import org.apache.reef.wake.remote.address.LocalAddressProvider;
import org.apache.reef.wake.remote.address.LoopbackLocalAddressProvider;
import org.apache.reef.wake.remote.impl.SocketRemoteIdentifier;
import org.apache.reef.wake.remote.ports.TcpPortProvider;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration provider for the Azure Batch evaluator shim.
 */
@Private
public class AzureBatchEvaluatorShimConfigurationProvider {

  private static final Logger LOG = Logger.getLogger(AzureBatchEvaluatorShimConfigurationProvider.class.getName());
  private final RemoteManager remoteManager;

  @Inject
  AzureBatchEvaluatorShimConfigurationProvider(final RemoteManager remoteManager) {
    this.remoteManager = remoteManager;
  }

  /**
   * Constructs a {@link Configuration} object which will be serialized and written to shim.config and
   * used to launch the evaluator shim.
   *
   * @param containerId id of the container for which the shim is being launched.
   * @param driverIdentifier identifier of the driver that is launching the container.
   * @return A {@link Configuration} object needed to launch the evaluator shim.
   */
  public Configuration getConfiguration(final String containerId, final String driverIdentifier) throws InjectionException {
    TcpPortProvider portProvider;
    try {
      portProvider = Tang.Factory.getTang().newInjector().getInstance(TcpPortProvider.class);
    } catch (InjectionException ex) {
      LOG.log(Level.SEVERE, "Unable to get instance of TcpPortProvider", ex);
      throw new RuntimeException("Unable to get instance of TcpPortProvider");
    }

    LOG.log(Level.INFO, "driverIdentifier is ", driverIdentifier);
    LOG.log(Level.INFO, "this.remoteManager.getMyIdentifier() is ", this.remoteManager.getMyIdentifier());
	LOG.log(Level.INFO, "PortProvider is " + portProvider.getClass().getName());
    Integer evaluatorPort = portProvider.iterator().next();

    return EvaluatorShimConfiguration.CONF
        .set(EvaluatorShimConfiguration.DRIVER_REMOTE_IDENTIFIER, driverIdentifier)
        .set(EvaluatorShimConfiguration.CONTAINER_IDENTIFIER, containerId)
        .set(EvaluatorShimConfiguration.CONTAINER_PORT, evaluatorPort)
        .build();
  }
}
