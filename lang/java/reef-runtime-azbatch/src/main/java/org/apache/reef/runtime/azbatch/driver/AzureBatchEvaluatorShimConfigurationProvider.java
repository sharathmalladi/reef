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
import org.apache.reef.runtime.azbatch.evaluator.EvaluatorShimConfiguration;
import org.apache.reef.runtime.common.utils.RemoteManager;
import org.apache.reef.tang.Configuration;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.wake.remote.address.LocalAddressProvider;
import org.apache.reef.wake.remote.ports.TcpPortProvider;
import org.apache.reef.wake.remote.ports.parameters.TcpPortList;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration provider for the Azure Batch evaluator shim.
 */
@Private
public class AzureBatchEvaluatorShimConfigurationProvider {

  private final RemoteManager remoteManager;
  private final TcpPortProvider portProvider;
  private final LocalAddressProvider localAddressProvider;
  private final Set<String> tcpPortList;

  @Inject
  AzureBatchEvaluatorShimConfigurationProvider(
      @Parameter(TcpPortList.class) final Set<Integer> tcpPortList,
      final RemoteManager remoteManager,
      final LocalAddressProvider localAddressProvider,
      final TcpPortProvider portProvider) {
    this.remoteManager = remoteManager;
    this.portProvider = portProvider;
    this.localAddressProvider = localAddressProvider;
    this.tcpPortList = new HashSet<String>(tcpPortList.size());
    for (int port: tcpPortList) {
      this.tcpPortList.add(Integer.toString(port));
    }
  }

  /**
   * Constructs a {@link Configuration} object which will be serialized and written to shim.config and
   * used to launch the evaluator shim.
   *
   * @param containerId id of the container for which the shim is being launched.
   * @return A {@link Configuration} object needed to launch the evaluator shim.
   */
  public Configuration getConfiguration(final String containerId) {

    return EvaluatorShimConfiguration.CONF.getBuilder()
        .bindImplementation(LocalAddressProvider.class, this.localAddressProvider.getClass())
        .bindImplementation(TcpPortProvider.class, this.portProvider.getClass())
        .build()
        .set(EvaluatorShimConfiguration.DRIVER_REMOTE_IDENTIFIER, this.remoteManager.getMyIdentifier())
        .set(EvaluatorShimConfiguration.CONTAINER_IDENTIFIER, containerId)
        .setMultiple(EvaluatorShimConfiguration.TCP_PORT_LIST, this.tcpPortList)
        .build();
  }
}

