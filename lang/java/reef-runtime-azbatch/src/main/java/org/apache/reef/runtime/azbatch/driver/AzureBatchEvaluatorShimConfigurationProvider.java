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

import com.microsoft.azure.batch.protocol.models.CloudTask;
import com.microsoft.azure.batch.protocol.models.ComputeNode;
import com.microsoft.azure.batch.protocol.models.InboundEndpoint;
import org.apache.commons.lang.StringUtils;
import org.apache.reef.annotations.audience.Private;
import org.apache.reef.driver.parameters.DriverMemory;
import org.apache.reef.runtime.azbatch.client.AzureBatchDriverConfigurationProviderImpl;
import org.apache.reef.runtime.azbatch.evaluator.EvaluatorShimConfiguration;
import org.apache.reef.runtime.azbatch.util.batch.AzureBatchHelper;
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
import org.apache.reef.wake.remote.ports.ListTcpPortProvider;
import org.apache.reef.wake.remote.ports.TcpPortProvider;
import org.apache.reef.wake.remote.ports.parameters.TcpPortList;

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration provider for the Azure Batch evaluator shim.
 */
@Private
public class AzureBatchEvaluatorShimConfigurationProvider {

  private static final Logger LOG = Logger.getLogger(AzureBatchEvaluatorShimConfigurationProvider.class.getName());
  RemoteManager remoteManager;
  TcpPortProvider portProvider;
  LocalAddressProvider localAddressProvider;
  AzureBatchHelper azureBatchHelper;

  @Inject
  AzureBatchEvaluatorShimConfigurationProvider(
      final RemoteManager remoteManager,
      final LocalAddressProvider localAddressProvider,
      final AzureBatchHelper azureBatchHelper,
      final TcpPortProvider portProvider) {
    this.remoteManager = remoteManager;
    this.portProvider = portProvider;
    this.localAddressProvider = localAddressProvider;
    this.azureBatchHelper = azureBatchHelper;
  }

  /**
   * Constructs a {@link Configuration} object which will be serialized and written to shim.config and
   * used to launch the evaluator shim.
   *
   * @param containerId      id of the container for which the shim is being launched.
   * @param driverIdentifier identifier of the driver that is launching the container.
   * @return A {@link Configuration} object needed to launch the evaluator shim.
   */
  public Configuration getConfiguration(final String containerId, final String driverIdentifier) throws InjectionException {
    LOG.log(Level.INFO, "driverIdentifier is " + driverIdentifier);
    LOG.log(Level.INFO, "AzureBatchEvaluatorShimConfigurationProvider.localAddressProvider class is " + this.localAddressProvider.getClass().getName());
    LOG.log(Level.INFO, "AzureBatchEvaluatorShimConfigurationProvider.localAddressProvider is " + this.localAddressProvider.getLocalAddress());
    LOG.log(Level.INFO, "AzureBatchEvaluatorShimConfigurationProvider.remoteManager.getMyIdentifier() is " + this.remoteManager.getMyIdentifier());
    LOG.log(Level.INFO, "PortProvider is " + portProvider.getClass().getName());

    String[] ports = {"2000", "2001" };

    final String availablePortsList = StringUtils.join(ports, TcpPortList.SEPARATOR);

    return EvaluatorShimConfiguration.CONF.getBuilder()
        .bindImplementation(LocalAddressProvider.class, ContainerBasedLocalAddressProvider.class)
        .bindNamedParameter(TcpPortList.class, availablePortsList)
        .bindImplementation(TcpPortProvider.class, ListTcpPortProvider.class)
        .build()
        .set(EvaluatorShimConfiguration.DRIVER_REMOTE_IDENTIFIER, this.remoteManager.getMyIdentifier())
        .set(EvaluatorShimConfiguration.CONTAINER_IDENTIFIER, containerId)
        .build();
  }

  public String getRemoteIdentifier() {

    try {
      Integer port = Integer.parseInt(StringUtils.substringAfterLast(this.remoteManager.getMyIdentifier(), ":"));
      SocketRemoteIdentifier socketIdentifier = new SocketRemoteIdentifier(new InetSocketAddress(this.localAddressProvider.getLocalAddress(), port));
      ComputeNode driverNode = this.azureBatchHelper.getComputeNode();
      List<InboundEndpoint> inboundEndpoints = driverNode.endpointConfiguration().inboundEndpoints();
      for (InboundEndpoint endpoint : inboundEndpoints) {
        if (endpoint.backendPort() == socketIdentifier.getSocketAddress().getPort()) {
          InetSocketAddress socketIdentifier2 = new InetSocketAddress(endpoint.publicIPAddress(), endpoint.frontendPort());
          return new SocketRemoteIdentifier(socketIdentifier2).toString();
        }
      }
      throw new IllegalArgumentException(
          String.format("Input value of remoteIdentifier {0} does not map to a backend point.", this.remoteManager.getMyIdentifier()));

    } catch (IOException ex) {
      throw new RuntimeException("Encountered IOException", ex);
    }
  }
}
