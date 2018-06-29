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
package org.apache.reef.wake.remote.transport.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.wake.EStage;
import org.apache.reef.wake.EventHandler;
import org.apache.reef.wake.impl.DefaultThreadFactory;
import org.apache.reef.wake.remote.Encoder;
import org.apache.reef.wake.remote.RemoteConfiguration;
import org.apache.reef.wake.remote.address.LocalAddressProvider;
import org.apache.reef.wake.remote.exception.RemoteRuntimeException;
import org.apache.reef.wake.remote.impl.TransportEvent;
import org.apache.reef.wake.remote.ports.TcpPortProvider;
import org.apache.reef.wake.remote.transport.Link;
import org.apache.reef.wake.remote.transport.LinkListener;
import org.apache.reef.wake.remote.transport.Transport;
import org.apache.reef.wake.remote.transport.exception.TransportRuntimeException;

import javax.inject.Inject;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Messaging transport implementation with Netty.
 */
public final class NettyMessagingTransport implements Transport {

  /**
   * Indicates a hostname that isn't set or known.
   */
  public static final String UNKNOWN_HOST_NAME = "##UNKNOWN##";

  private static final String CLASS_NAME = NettyMessagingTransport.class.getSimpleName();

  private static final Logger LOG = Logger.getLogger(CLASS_NAME);

  private static final int SERVER_BOSS_NUM_THREADS = 3;
  private static final int SERVER_WORKER_NUM_THREADS = 20;
  private static final int CLIENT_WORKER_NUM_THREADS = 10;

  private final ConcurrentMap<SocketAddress, LinkReference> addrToLinkRefMap = new ConcurrentHashMap<>();

  private final EventLoopGroup clientWorkerGroup;
  private final EventLoopGroup serverBossGroup;
  private final EventLoopGroup serverWorkerGroup;

  private final Bootstrap clientBootstrap;
  private final Channel acceptor;

  private final ChannelGroup clientChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
  private final ChannelGroup serverChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

  private final InetSocketAddress localAddress;

  private final NettyClientEventListener clientEventListener;
  private final NettyServerEventListener serverEventListener;

  private final int numberOfTries;
  private final int retryTimeout;

  /**
   * Constructs a messaging transport.
   *
   * @param hostAddress   the server host address
   * @param listenPort    the server listening port; when it is 0, randomly assign a port number
   * @param clientStage   the client-side stage that handles transport events
   * @param serverStage   the server-side stage that handles transport events
   * @param numberOfTries the number of tries of connection
   * @param retryTimeout  the timeout of reconnection
   * @param tcpPortProvider  gives an iterator that produces random tcp ports in a range
   */
  @Inject
  private NettyMessagingTransport(
      @Parameter(RemoteConfiguration.HostAddress.class) final String hostAddress,
      @Parameter(RemoteConfiguration.Port.class) final int listenPort,
      @Parameter(RemoteConfiguration.RemoteClientStage.class) final EStage<TransportEvent> clientStage,
      @Parameter(RemoteConfiguration.RemoteServerStage.class) final EStage<TransportEvent> serverStage,
      @Parameter(RemoteConfiguration.NumberOfTries.class) final int numberOfTries,
      @Parameter(RemoteConfiguration.RetryTimeout.class) final int retryTimeout,
      final TcpPortProvider tcpPortProvider,
      final LocalAddressProvider localAddressProvider) {

    if (listenPort < 0) {
      throw new RemoteRuntimeException("Invalid server port: " + listenPort);
    }

    final String host = UNKNOWN_HOST_NAME.equals(hostAddress) ? localAddressProvider.getLocalAddress() : hostAddress;

    this.numberOfTries = numberOfTries;
    this.retryTimeout = retryTimeout;
    this.clientEventListener = new NettyClientEventListener(this.addrToLinkRefMap, clientStage);
    this.serverEventListener = new NettyServerEventListener(this.addrToLinkRefMap, serverStage);

    this.serverBossGroup = new NioEventLoopGroup(SERVER_BOSS_NUM_THREADS,
        new DefaultThreadFactory(CLASS_NAME + ":ServerBoss"));
    this.serverWorkerGroup = new NioEventLoopGroup(SERVER_WORKER_NUM_THREADS,
        new DefaultThreadFactory(CLASS_NAME + ":ServerWorker"));
    this.clientWorkerGroup = new NioEventLoopGroup(CLIENT_WORKER_NUM_THREADS,
        new DefaultThreadFactory(CLASS_NAME + ":ClientWorker"));

    this.clientBootstrap = new Bootstrap()
        .group(this.clientWorkerGroup)
        .channel(NioSocketChannel.class)
        .handler(new NettyChannelInitializer(new NettyDefaultChannelHandlerFactory("client",
            this.clientChannelGroup, this.clientEventListener)))
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_KEEPALIVE, true);

    final ServerBootstrap serverBootstrap = new ServerBootstrap()
        .group(this.serverBossGroup, this.serverWorkerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new NettyChannelInitializer(new NettyDefaultChannelHandlerFactory("server",
            this.serverChannelGroup, this.serverEventListener)))
        .option(ChannelOption.SO_BACKLOG, 128)
        .option(ChannelOption.SO_REUSEADDR, true)
        .childOption(ChannelOption.SO_KEEPALIVE, true);

    LOG.log(Level.FINE, "Binding to {0}:{1}", new Object[] {host, listenPort});

    try {
      if (listenPort > 0) {
        this.localAddress = new InetSocketAddress(host, listenPort);
        this.acceptor = serverBootstrap.bind(this.localAddress).sync().channel();
      } else {
        InetSocketAddress socketAddr = null;
        Channel acceptorFound = null;
        for (int port : tcpPortProvider) {
          LOG.log(Level.FINEST, "Try port {0}", port);
          try {
            socketAddr = new InetSocketAddress(host, port);
            acceptorFound = serverBootstrap.bind(socketAddr).sync().channel();
            break;
          } catch (final Exception ex) {
            if (ex instanceof BindException) { // Not visible to catch :(
              LOG.log(Level.FINEST, "The port {0} is already bound. Try again", port);
            } else {
              throw ex;
            }
          }
        }
        if (acceptorFound == null) {
          throw new IllegalStateException("TcpPortProvider could not find a free port.");
        }
        this.localAddress = socketAddr;
        this.acceptor = acceptorFound;
      }
    } catch (final IllegalStateException | InterruptedException ex) {
      LOG.log(Level.SEVERE, "Cannot bind to port " + listenPort, ex);
      this.clientWorkerGroup.shutdownGracefully();
      this.serverBossGroup.shutdownGracefully();
      this.serverWorkerGroup.shutdownGracefully();
      throw new TransportRuntimeException("Cannot bind to port " + listenPort, ex);
    }

    LOG.log(Level.FINE, "Starting netty transport socket address: {0}", this.localAddress);
  }

  /**
   * Closes all channels and releases all resources.
   */
  @Override
  public void close() {

    LOG.log(Level.FINE, "Closing netty transport socket address: {0}", this.localAddress);

    final ChannelGroupFuture clientChannelGroupFuture = this.clientChannelGroup.close();
    final ChannelGroupFuture serverChannelGroupFuture = this.serverChannelGroup.close();
    final ChannelFuture acceptorFuture = this.acceptor.close();

    final ArrayList<Future> eventLoopGroupFutures = new ArrayList<>(3);
    eventLoopGroupFutures.add(this.clientWorkerGroup.shutdownGracefully());
    eventLoopGroupFutures.add(this.serverBossGroup.shutdownGracefully());
    eventLoopGroupFutures.add(this.serverWorkerGroup.shutdownGracefully());

    clientChannelGroupFuture.awaitUninterruptibly();
    serverChannelGroupFuture.awaitUninterruptibly();

    try {
      acceptorFuture.sync();
    } catch (final Exception ex) {
      LOG.log(Level.SEVERE, "Error closing the acceptor channel for " + this.localAddress, ex);
    }

    for (final Future eventLoopGroupFuture : eventLoopGroupFutures) {
      eventLoopGroupFuture.awaitUninterruptibly();
    }

    LOG.log(Level.FINE, "Closing netty transport socket address: {0} done", this.localAddress);
  }

  /**
   * Returns a link for the remote address if cached; otherwise opens, caches and returns.
   * When it opens a link for the remote address, only one attempt for the address is made at a given time
   *
   * @param remoteAddr the remote socket address
   * @param encoder    the encoder
   * @param listener   the link listener
   * @return a link associated with the address
   */
  @Override
  public <T> Link<T> open(final SocketAddress remoteAddr, final Encoder<? super T> encoder,
                          final LinkListener<? super T> listener) throws IOException {

    Link<T> link = null;

    for (int i = 0; i <= this.numberOfTries; ++i) {
      LinkReference linkRef = this.addrToLinkRefMap.get(remoteAddr);

      if (linkRef != null) {
        link = (Link<T>) linkRef.getLink();
        if (LOG.isLoggable(Level.FINE)) {
          LOG.log(Level.FINE, "Link {0} for {1} found", new Object[]{link, remoteAddr});
        }
        if (link != null) {
          return link;
        }
      }
      
      if (i == this.numberOfTries) {
        // Connection failure 
        throw new ConnectException("Connection to " + remoteAddr + " refused");
      }

      LOG.log(Level.FINE, "No cached link for {0} thread {1}",
          new Object[]{remoteAddr, Thread.currentThread()});

      // no linkRef
      final LinkReference newLinkRef = new LinkReference();
      final LinkReference prior = this.addrToLinkRefMap.putIfAbsent(remoteAddr, newLinkRef);
      final AtomicInteger flag = prior != null ?
          prior.getConnectInProgress() : newLinkRef.getConnectInProgress();

      synchronized (flag) {
        if (!flag.compareAndSet(0, 1)) {
          while (flag.get() == 1) {
            try {
              flag.wait();
            } catch (final InterruptedException ex) {
              LOG.log(Level.WARNING, "Wait interrupted", ex);
            }
          }
        }
      }

      linkRef = this.addrToLinkRefMap.get(remoteAddr);
      link = (Link<T>) linkRef.getLink();

      if (link != null) {
        return link;
      }

      ChannelFuture connectFuture = null;
      try {
        connectFuture = this.clientBootstrap.connect(remoteAddr);
        connectFuture.syncUninterruptibly();

        link = new NettyLink<>(connectFuture.channel(), encoder, listener);
        linkRef.setLink(link);

        synchronized (flag) {
          flag.compareAndSet(1, 2);
          flag.notifyAll();
        }
        break;
      } catch (final Exception e) {
        if (e.getClass().getSimpleName().compareTo("ConnectException") == 0) {
          LOG.log(Level.WARNING, "Connection refused. Retry {0} of {1}",
              new Object[]{i + 1, this.numberOfTries});
          synchronized (flag) {
            flag.compareAndSet(1, 0);
            flag.notifyAll();
          }

          if (i < this.numberOfTries) {
            try {
              Thread.sleep(retryTimeout);
            } catch (final InterruptedException interrupt) {
              LOG.log(Level.WARNING, "Thread {0} interrupted while sleeping", Thread.currentThread());
            }
          }
        } else {
          throw e;
        }
      }
    }
    
    return link;
  }

  /**
   * Returns a link for the remote address if already cached; otherwise, returns null.
   *
   * @param remoteAddr the remote address
   * @return a link if already cached; otherwise, null
   */
  public <T> Link<T> get(final SocketAddress remoteAddr) {
    final LinkReference linkRef = this.addrToLinkRefMap.get(remoteAddr);
    return linkRef != null ? (Link<T>) linkRef.getLink() : null;
  }

  /**
   * Gets a server local socket address of this transport.
   *
   * @return a server local socket address
   */
  @Override
  public SocketAddress getLocalAddress() {
    return this.localAddress;
  }

  /**
   * Gets a server listening port of this transport.
   *
   * @return a listening port number
   */
  @Override
  public int getListeningPort() {
    return this.localAddress.getPort();
  }

  /**
   * Registers the exception event handler.
   *
   * @param handler the exception event handler
   */
  @Override
  public void registerErrorHandler(final EventHandler<Exception> handler) {
    this.clientEventListener.registerErrorHandler(handler);
    this.serverEventListener.registerErrorHandler(handler);
  }

  @Override
  public String toString() {
    return String.format("NettyMessagingTransport: { address: %s }", this.localAddress);
  }
}
