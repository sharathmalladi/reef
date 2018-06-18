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
package org.apache.reef.runtime.azbatch.client;

import org.apache.commons.lang.StringUtils;
import org.apache.reef.annotations.audience.Public;
import org.apache.reef.runtime.azbatch.parameters.*;
import org.apache.reef.tang.Configuration;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.wake.remote.ports.parameters.TcpPortRangeBegin;
import org.apache.reef.wake.remote.ports.parameters.TcpPortRangeCount;

import javax.inject.Inject;

/**
 * Class that provides the runtime configuration for Azure Batch.
 */
@Public
public final class AzureBatchRuntimeConfigurationProvider {
  private final String azureBatchAccountName;
  private final String azureBatchAccountKey;
  private final String azureBatchAccountUri;
  private final String azureBatchPoolId;
  private final String azureStorageAccountName;
  private final String azureStorageAccountKey;
  private final String azureStorageContainerName;
  private final String containerRegistryServer;
  private final String containerRegistryUsername;
  private final String containerRegistryPassword;
  private final Integer tcpPortRangeBegin;
  private final Integer tcpPortRangeCount;
  private final Boolean isWindows;
  private final Boolean isDockerContainer;

  /**
   * Private constructor.
   */
  @Inject
  private AzureBatchRuntimeConfigurationProvider(
      @Parameter(AzureBatchAccountName.class) final String azureBatchAccountName,
      @Parameter(AzureBatchAccountKey.class) final String azureBatchAccountKey,
      @Parameter(AzureBatchAccountUri.class) final String azureBatchAccountUri,
      @Parameter(AzureBatchPoolId.class) final String azureBatchPoolId,
      @Parameter(AzureStorageAccountName.class) final String azureStorageAccountName,
      @Parameter(AzureStorageAccountKey.class) final String azureStorageAccountKey,
      @Parameter(AzureStorageContainerName.class) final String azureStorageContainerName,
      @Parameter(ContainerRegistryServer.class) final String containerRegistryServer,
      @Parameter(ContainerRegistryUsername.class) final String containerRegistryUsername,
      @Parameter(ContainerRegistryPassword.class) final String containerRegistryPassword,
      @Parameter(TcpPortRangeBegin.class) final Integer tcpPortRangeBegin,
      @Parameter(TcpPortRangeCount.class) final Integer tcpPortRangeCount,
      @Parameter(IsWindows.class) final Boolean isWindows) {
    this.azureBatchAccountName = azureBatchAccountName;
    this.azureBatchAccountKey = azureBatchAccountKey;
    this.azureBatchAccountUri = azureBatchAccountUri;
    this.azureBatchPoolId = azureBatchPoolId;
    this.azureStorageAccountName = azureStorageAccountName;
    this.azureStorageAccountKey = azureStorageAccountKey;
    this.azureStorageContainerName = azureStorageContainerName;
    this.containerRegistryServer = containerRegistryServer;
    this.containerRegistryUsername = containerRegistryUsername;
    this.containerRegistryPassword = containerRegistryPassword;
    this.tcpPortRangeBegin = tcpPortRangeBegin;
    this.tcpPortRangeCount = tcpPortRangeCount;
    this.isWindows = isWindows;

    if (!StringUtils.isEmpty(this.containerRegistryServer)) {
      this.isDockerContainer = true;
    } else {
      this.isDockerContainer = false;
    }
  }

  public Configuration getAzureBatchRuntimeConfiguration() {
    return AzureBatchRuntimeConfigurationCreator
            .getOrCreateAzureBatchRuntimeConfiguration(this.isWindows, this.isDockerContainer)
            .set(AzureBatchRuntimeConfiguration.AZURE_BATCH_ACCOUNT_NAME, this.azureBatchAccountName)
            .set(AzureBatchRuntimeConfiguration.AZURE_BATCH_ACCOUNT_KEY, this.azureBatchAccountKey)
            .set(AzureBatchRuntimeConfiguration.AZURE_BATCH_ACCOUNT_URI, this.azureBatchAccountUri)
            .set(AzureBatchRuntimeConfiguration.AZURE_BATCH_POOL_ID, this.azureBatchPoolId)
            .set(AzureBatchRuntimeConfiguration.AZURE_STORAGE_ACCOUNT_NAME, this.azureStorageAccountName)
            .set(AzureBatchRuntimeConfiguration.AZURE_STORAGE_ACCOUNT_KEY, this.azureStorageAccountKey)
            .set(AzureBatchRuntimeConfiguration.AZURE_STORAGE_CONTAINER_NAME, this.azureStorageContainerName)
            .set(AzureBatchRuntimeConfiguration.CONTAINER_REGISTRY_SERVER, this.containerRegistryServer)
            .set(AzureBatchRuntimeConfiguration.CONTAINER_REGISTRY_USERNAME, this.containerRegistryUsername)
            .set(AzureBatchRuntimeConfiguration.CONTAINER_REGISTRY_PASSWORD, this.containerRegistryPassword)
            .set(AzureBatchRuntimeConfiguration.TCP_PORT_RANGE_BEGIN, this.tcpPortRangeBegin)
            .set(AzureBatchRuntimeConfiguration.TCP_PORT_RANGE_COUNT, this.tcpPortRangeCount)
            .build();
  }
}
