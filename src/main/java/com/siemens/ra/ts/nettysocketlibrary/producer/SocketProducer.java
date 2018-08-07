package com.siemens.ra.ts.nettysocketlibrary.producer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.siemens.ra.ts.nettysocketlibrary.channel.ChannelMessage;
import com.siemens.ra.ts.nettysocketlibrary.channel.DefaultHandler;
import com.siemens.ra.ts.nettysocketlibrary.channel.DefaultInitializer;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelActivated;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelInactivated;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelRead;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelRegistered;
import com.siemens.ra.ts.nettysocketlibrary.utils.CliUtils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class SocketProducer implements Flow.Subscriber<ChannelMessage>, Runnable {
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  private ExecutorService executorService = Executors.newFixedThreadPool(4);
  private CompletableFuture<Void> listening = new CompletableFuture<>();
  private CompletableFuture<Void> connected = new CompletableFuture<>();
  private CompletableFuture<Void> disconnected = new CompletableFuture<>();
  private final CompletableFuture<Void> terminated = new CompletableFuture<>();

  private EventLoopGroup bossGroup = new NioEventLoopGroup();
  private EventLoopGroup workerGroup = new NioEventLoopGroup();
  private ServerBootstrap bootstrap = new ServerBootstrap();
  private ChannelFuture channelFuture;
  
  private Flow.Subscription subscription;
  private ChannelHandlerContext ctx;
  private final BlockingQueue<String> receivedMessages = new LinkedBlockingDeque<>();
  private final SubmissionPublisher<String> messagePublisher = new SubmissionPublisher<>();

  private int port;
  private DefaultInitializer channelInitializer;

  public SocketProducer(int port) {
    super();
    this.port = port;
  }

  public void addSubscriber(final Subscriber<String> messageSubscriber) {
    if (messageSubscriber != null) {
      this.messagePublisher.subscribe(messageSubscriber);
    }
  }

  public void removeSubscriber(final Subscriber<String> messageSubscriber) {
    if (messageSubscriber != null) {
      this.messagePublisher.getSubscribers().remove(messageSubscriber);
    }
  }

  public void setChannelInitializer(final DefaultInitializer channelInitializer) {
    this.channelInitializer = channelInitializer;
  }

  @Override
  public void run() {
    LOGGER.info("Running socket producer:");

    try {
      bootstrap.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .localAddress(new InetSocketAddress("localhost", port))
          .option(ChannelOption.SO_BACKLOG, 128)
          .childOption(ChannelOption.SO_KEEPALIVE, true)
          .childHandler(channelInitializer);

      // Bind and start to accept incoming connections.
      channelFuture = bootstrap.bind().sync();

      listening();

      // Wait until the server socket is closed via close() method
      channelFuture.channel().closeFuture().sync();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    }

    shutdown();

    LOGGER.info("Socker producer stopped.");
  }

  public void close() {
    if (channelFuture != null) {
      channelFuture.channel().close();
    }
  }

  public void send(String msg) throws IOException {
    if ((channelFuture.channel() != null) && channelFuture.channel().isActive()) {
      ctx.writeAndFlush(msg);
    } else {
      throw new IOException("Can't send message to inactive connection");
    }
  }

  public BlockingQueue<String> receive() {
    return receivedMessages;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    this.subscription = subscription;
    this.subscription.request(1);
  }

  @Override
  public void onNext(ChannelMessage msg) {
    if (msg instanceof ChannelRegistered) {
      this.ctx = msg.getCtx();
    } else if (msg instanceof ChannelActivated) {
      connected();
    } else if (msg instanceof ChannelInactivated) {
      disconnected();
    } else if (msg instanceof ChannelRead) {
      if (messagePublisher.getSubscribers().size() > 0) {
        messagePublisher.submit(((ChannelRead) msg).getMessage());
      } else {
        receivedMessages.add(((ChannelRead) msg).getMessage());
      }
    }

    subscription.request(1);
  }

  @Override
  public void onError(Throwable throwable) {
    LOGGER.error("Error from publisher: {}", throwable.getMessage());
    subscription.cancel();
  }

  @Override
  public void onComplete() {
    LOGGER.info("Subscription completed");
  }

  public synchronized void waitUntilListening() throws InterruptedException {
    try {
      listening.get();
    } catch (ExecutionException ee) {
      LOGGER.error(ee.toString());
    }
    listening = new CompletableFuture<>();
  }

  public synchronized void waitUntilConnected() throws InterruptedException {
    try {
      connected.get();
    } catch (ExecutionException ee) {
      LOGGER.error(ee.toString());
    }
    connected = new CompletableFuture<>();
  }

  public synchronized void waitUntilDisconnected() throws InterruptedException {
    try {
      disconnected.get();
    } catch (ExecutionException ee) {
      LOGGER.error(ee.toString());
    }
    disconnected = new CompletableFuture<>();
  }

  public synchronized void waitUntilTerminated() throws InterruptedException {
    try {
      terminated.get();
    } catch (ExecutionException ee) {
      LOGGER.error(ee.toString());
    }
  }

  private void listening() {
    newSingleThreadExecutor().submit(() -> {
      listening.complete(null);
    });
  }

  private void connected() {
    newSingleThreadExecutor().submit(() -> {
      connected.complete(null);
    });
  }

  private void disconnected() {
    newSingleThreadExecutor().submit(() -> {
      disconnected.complete(null);
    });
  }

  private void terminated() {
    newSingleThreadExecutor().submit(() -> {
      terminated.complete(null);
    });
  }

  private void shutdown() {
    executorService.shutdown();
    connected();
    disconnected();
    terminated();
  }

  public static void main(String[] args) {
    // Default value
    int port = 9999;
    
    Options options = CliUtils.createCliOption(); 
    HelpFormatter formatter = new HelpFormatter();
    try {
        CommandLine cmd = CliUtils.createCommandLine(args, options);
        port = Integer.parseInt(cmd.getOptionValue("port")); // e.g.: 9092
    } catch (NumberFormatException | ParseException e) {
        System.err.println(e.getMessage());
        formatter.printHelp("SocketProducer", options);
        System.out.println("Using default value: " + port);
    }

    SocketProducer socketProducer = new SocketProducer(port);
    socketProducer.setChannelInitializer(new DefaultInitializer(new DefaultHandler(socketProducer)));
    socketProducer.run();
  }
}
