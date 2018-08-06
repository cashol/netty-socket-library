package com.siemens.ra.ts.nettysocketlibrary.consumer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.siemens.ra.ts.nettysocketlibrary.channel.ChannelMessage;
import com.siemens.ra.ts.nettysocketlibrary.channel.DefaultHandler;
import com.siemens.ra.ts.nettysocketlibrary.channel.DefaultInitializer;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelActivated;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelInactivated;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelRead;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelRegistered;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class SocketConsumer implements Flow.Subscriber<ChannelMessage>, Runnable {
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  private ExecutorService executorService = Executors.newFixedThreadPool(4);
  private CompletableFuture<Void> connecting = new CompletableFuture<>();
  private CompletableFuture<Void> connected = new CompletableFuture<>();
  private CompletableFuture<Void> disconnected = new CompletableFuture<>();
  private final CompletableFuture<Void> terminated = new CompletableFuture<>();

  private Timer timer = new Timer();
  private EventLoopGroup group = new NioEventLoopGroup();
  private Bootstrap bootstrap = new Bootstrap();
  private ChannelFuture channelFuture;
  private Channel channel;

  private Flow.Subscription subscription;
  private ChannelHandlerContext ctx;
  private final BlockingQueue<String> receivedMessages = new LinkedBlockingDeque<>();
  private final SubmissionPublisher<String> messagePublisher = new SubmissionPublisher<>();
  private boolean running = true;

  private SocketAddress socketAddress;
  private DefaultInitializer channelInitializer;

  public SocketConsumer(String host, int port) {
    this(new InetSocketAddress(host, port));
  }

  public SocketConsumer(SocketAddress socketAddress) {
    this.socketAddress = socketAddress;
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
    LOGGER.info("Running socket consumer:");

    bootstrap.group(group)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(channelInitializer);

    scheduleConnect(10); // Runs until close() method invoked
  }

  public void close() {
    try {
      if (channel != null) {
        channel.close().sync();
      }
      running = false;
      timer.cancel();
      if (channelFuture != null) {
        channelFuture.channel().closeFuture().sync();
      }
    } catch (InterruptedException e) {
      LOGGER.error(e.getMessage());
    } finally {
      group.shutdownGracefully();
    }

    shutdown();

    LOGGER.info("Socker consumer stopped.");
  }

  public void send(String msg) throws IOException {
    if ((channel != null) && channel.isActive()) {
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

  private void scheduleConnect(long millis) {
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        doConnect();
      }
    }, millis);
  }

  private void doConnect() {
    try {
      channelFuture = bootstrap.connect(socketAddress);

      connecting();

      channelFuture.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (!future.isSuccess()) {//if is not successful, reconnect
            LOGGER.info("Try to connect ...");
            future.channel().close();
            future.cancel(true);
            if (running) {
              bootstrap.connect(socketAddress).addListener(this);
            }
          } else {//good, the connection is ok
            channel = future.channel();
            //add a listener to detect the connection lost
            addCloseDetectListener(channel);
          }
        }

        private void addCloseDetectListener(Channel channel) {
          //if the channel connection is lost, the ChannelFutureListener.operationComplete() will be called
          channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
              connectionLost();
              scheduleConnect(5);
            }
          });
        }
      });
    } catch (Exception ex) {
      scheduleConnect(1000);
    }
  }

  protected void connectionLost() {
    LOGGER.info("connectionLost()");
    disconnected();
  }

  public synchronized void waitUntilConnecting() throws InterruptedException {
    try {
      connecting.get();
    } catch (ExecutionException ee) {
      LOGGER.error(ee.toString());
    }
    connecting = new CompletableFuture<>();
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

  private void connecting() {
    newSingleThreadExecutor().submit(() -> {
      connecting.complete(null);
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
    int port = 9999;
    if (args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        System.err.println("Argument" + args[0] + " must be an integer! Use default value 9999");
      }
    }
    
    SocketConsumer socketConsumer = new SocketConsumer("127.0.0.1", port);
    socketConsumer.setChannelInitializer(new DefaultInitializer(new DefaultHandler(socketConsumer)));
    socketConsumer.run();
  }
}
