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
package org.apache.reef.runtime.common.client;

import org.apache.reef.annotations.Provided;
import org.apache.reef.annotations.audience.ClientSide;
import org.apache.reef.annotations.audience.Private;
import org.apache.reef.client.REEF;
import org.apache.reef.client.SubmittedJob;
import org.apache.reef.client.parameters.DriverConfigurationProviders;
import org.apache.reef.client.parameters.JobSubmittedHandler;
import org.apache.reef.runtime.common.client.api.JobSubmissionEvent;
import org.apache.reef.runtime.common.client.api.JobSubmissionHandler;
import org.apache.reef.runtime.common.launch.parameters.ErrorHandlerRID;
import org.apache.reef.tang.*;
import org.apache.reef.tang.annotations.Name;
import org.apache.reef.tang.annotations.NamedParameter;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.util.REEFVersion;
import org.apache.reef.util.logging.LoggingScope;
import org.apache.reef.util.logging.LoggingScopeFactory;
import org.apache.reef.wake.EventHandler;

import javax.inject.Inject;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default REEF implementation.
 */
@ClientSide
@Provided
@Private
public final class REEFImplementation implements REEF {

  private static final Logger LOG = Logger.getLogger(REEFImplementation.class.getName());

  private final JobSubmissionHandler jobSubmissionHandler;
  private final JobSubmissionHelper jobSubmissionHelper;
  private final InjectionFuture<EventHandler<SubmittedJob>> jobSubmittedHandler;
  private final RunningJobs runningJobs;
  private final ClientWireUp clientWireUp;
  private final LoggingScopeFactory loggingScopeFactory;
  private final Set<ConfigurationProvider> configurationProviders;

  /**
   * @param jobSubmissionHandler
   * @param jobSubmissionHelper
   * @param jobStatusMessageHandler is passed only to make sure it is instantiated
   * @param runningJobs
   * @param clientWireUp
   * @param reefVersion provides the current version of REEF.
   * @param configurationProviders
   */
  @Inject
  private REEFImplementation(
        final JobSubmissionHandler jobSubmissionHandler,
        final JobSubmissionHelper jobSubmissionHelper,
        final JobStatusMessageHandler jobStatusMessageHandler,
        final RunningJobs runningJobs,
        final ClientWireUp clientWireUp,
        final LoggingScopeFactory loggingScopeFactory,
        final REEFVersion reefVersion,
        @Parameter(JobSubmittedHandler.class) final InjectionFuture<EventHandler<SubmittedJob>> jobSubmittedHandler,
        @Parameter(DriverConfigurationProviders.class) final Set<ConfigurationProvider> configurationProviders) {

    this.jobSubmissionHandler = jobSubmissionHandler;
    this.jobSubmittedHandler = jobSubmittedHandler;
    this.jobSubmissionHelper = jobSubmissionHelper;
    this.runningJobs = runningJobs;
    this.clientWireUp = clientWireUp;
    this.configurationProviders = configurationProviders;
    this.loggingScopeFactory = loggingScopeFactory;

    clientWireUp.performWireUp();
    reefVersion.logVersion();
  }

  @Override
  public void close() {

    LOG.log(Level.FINE, "Close REEF: shutdown jobs");
    this.runningJobs.closeAllJobs();

    LOG.log(Level.FINE, "Close REEF: shutdown client");
    this.clientWireUp.close();

    LOG.log(Level.FINE, "Close REEF: shutdown job submitter");
    try {
      this.jobSubmissionHandler.close();
    } catch (final Exception ex) {
      LOG.log(Level.WARNING, "Could not shutdown job submitter", ex);
    }

    LOG.log(Level.FINE, "Close REEF: done");
  }

  @Override
  public void submit(final Configuration driverConf) {
    try (LoggingScope ls = this.loggingScopeFactory.reefSubmit()) {
      final Configuration driverConfiguration = createDriverConfiguration(driverConf);
      final JobSubmissionEvent submissionMessage;
      try {
        if (this.clientWireUp.isClientPresent()) {
          submissionMessage = this.jobSubmissionHelper.getJobSubmissionBuilder(driverConfiguration)
              .setRemoteId(this.clientWireUp.getRemoteManagerIdentifier())
              .build();
        } else {
          submissionMessage = this.jobSubmissionHelper.getJobSubmissionBuilder(driverConfiguration)
              .setRemoteId(ErrorHandlerRID.NONE)
              .build();
        }
      } catch (final Exception e) {
        throw new RuntimeException("Exception while processing driver configuration.", e);
      }

      this.jobSubmissionHandler.onNext(submissionMessage);

      this.jobSubmittedHandler.get().onNext(
          new SubmittedJobImpl(this.jobSubmissionHandler.getApplicationId()));
    }
  }

  /**
   * Assembles the final Driver Configuration by merging in all the Configurations provided by ConfigurationProviders.
   *
   * @param driverConfiguration
   * @return
   */
  private Configuration createDriverConfiguration(final Configuration driverConfiguration) {
    final ConfigurationBuilder configurationBuilder = Tang.Factory.getTang()
        .newConfigurationBuilder(driverConfiguration);
    for (final ConfigurationProvider configurationProvider : this.configurationProviders) {
      configurationBuilder.addConfiguration(configurationProvider.getConfiguration());
    }
    return configurationBuilder.build();
  }

  /**
   * The driver remote identifier.
   */
  @NamedParameter(doc = "The driver remote identifier.")
  public static final class DriverRemoteIdentifier implements Name<String> {
  }
}
