/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.reef.wake.remote.ports;


import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.wake.remote.ports.parameters.TcpPortRangeBegin;
import org.apache.reef.wake.remote.ports.parameters.TcpPortRangeCount;
import org.apache.reef.wake.remote.ports.parameters.TcpPortRangeEnd;
import org.apache.reef.wake.remote.ports.parameters.TcpPortRangeTryCount;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A TcpPortProvider which gives out ports in serial order.
 */
public final class SerialTcpPortProvider implements TcpPortProvider {
  private final int portRangeBegin;
  private final int portRangeEnd;
  private static final Logger LOG = Logger.getLogger(RangeTcpPortProvider.class.getName());
  List<Integer> ports;

  @Inject
  public SerialTcpPortProvider(@Parameter(TcpPortRangeBegin.class) final int portRangeBegin,
                              @Parameter(TcpPortRangeEnd.class) final int portRangeEnd) {
    this.portRangeBegin = portRangeBegin;
    this.portRangeEnd = portRangeEnd;
    LOG.log(Level.INFO, "Instantiating " + this);
    this.ports = new ArrayList<>();
    for (Integer i = portRangeBegin; i <= portRangeEnd; i++) {
      this.ports.add(i);
    }
  }

  /**
   * Returns an iterator over a set of tcp ports.
   *
   * @return an Iterator.
   */
  @Override
  public Iterator<Integer> iterator() {
    return ports.iterator();
  }

  @Override
  public String toString() {
    return "RangeTcpPortProvider{" +
        "portRangeBegin=" + portRangeBegin +
        ", portRangeEnd=" + portRangeEnd +
        '}';
  }
}
