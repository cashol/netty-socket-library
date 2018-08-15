package com.siemens.ra.ts.nettysocketlibrary.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.siemens.ra.ts.nettysocketlibrary.channel.DefaultHandler;
import com.siemens.ra.ts.nettysocketlibrary.channel.DefaultInitializer;
import com.siemens.ra.ts.nettysocketlibrary.consumer.SocketConsumer;
import com.siemens.ra.ts.nettysocketlibrary.producer.SocketProducer;

class NettySocketLibraryTest {
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  private static final int SERVER_PORT = 9999;
  private static final String PRODUCER_MESSAGE = "12345";
  private static final String CONSUMER_MESSAGE = "abcde";

  private ExecutorService executorService = Executors.newFixedThreadPool(4);
  private SocketProducer socketProducer;
  private SocketConsumer socketConsumer;
  private boolean connected = false;

  @BeforeEach
  void createSocketProducerAndConsumer() throws InterruptedException {
    socketProducer = new SocketProducer(9999);
    socketProducer.setChannelInitializer(new DefaultInitializer(new DefaultHandler(socketProducer)));
    Runnable producerThread = () -> {
      socketProducer.run();
    };
    executorService.execute(producerThread);
    socketProducer.waitUntilListening();

    socketConsumer = new SocketConsumer("127.0.0.1", SERVER_PORT);
    socketConsumer.setChannelInitializer(new DefaultInitializer(new DefaultHandler(socketConsumer)));
    Runnable consumerThread = () -> {
      socketConsumer.run();
    };
    executorService.execute(consumerThread);
    socketConsumer.waitUntilConnecting();
  }

  @AfterEach
  void closeSocketProducerAndConsumer() throws InterruptedException {
    // clean-up:      
    socketConsumer.close();
    socketConsumer.waitUntilTerminated();
    socketProducer.close();
    socketProducer.waitUntilTerminated();

    Thread.sleep(100);
  }

  private void isConsumerConnected() throws InterruptedException {
    Runnable connectionThread = () -> {
      try {
        socketConsumer.waitUntilConnected();
        socketProducer.waitUntilConnected();
        connected = true;
      } catch (InterruptedException e) {
        throw new AssertionError(e.getMessage());
      }
    };
    executorService.execute(connectionThread);
    
    await()
      .atLeast(100, TimeUnit.MILLISECONDS).atMost(200, TimeUnit.MILLISECONDS)
      .until(() -> connected);
  }

  @Nested
  @DisplayName("connecting producer and consumer")
  class ConnectingProducerAndConsumer {
    @Test
    @DisplayName("are producer and consumer connected")
    void areProducerAndConsumerConnected() throws InterruptedException {
      // when

      // then
      isConsumerConnected();
    }

    @Test
    @DisplayName("are messages sent and received")
    void areMessagesSentAndReceived() throws InterruptedException, IOException {
      // prepare
      isConsumerConnected();

      // when
      socketProducer.send(PRODUCER_MESSAGE);
      socketConsumer.send(CONSUMER_MESSAGE);

      await()
        .atLeast(100, TimeUnit.MILLISECONDS).atMost(200, TimeUnit.MILLISECONDS)
        .until(() -> !socketConsumer.receive().isEmpty());
      assertThat(socketConsumer.receive().take()).isEqualTo(PRODUCER_MESSAGE);
      assertThat(socketConsumer.receive()).isEmpty();

      assertThat(socketProducer.receive()).isNotEmpty();
      assertThat(socketProducer.receive().take()).isEqualTo(CONSUMER_MESSAGE);
      assertThat(socketProducer.receive()).isEmpty();
    }
  }

  @Nested
  @DisplayName("using publisher and subscriber")
  class UsingPublisherSubscriber implements Flow.Subscriber<String> {
    private Flow.Subscription subscription;
    private List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void connectProducerAndConsumer() throws InterruptedException {
      subscription = null;
      receivedMessages.clear();
      isConsumerConnected();
    }

    @Test
    @DisplayName("is consumer subscribed")
    void isConsumerSubscribed() throws InterruptedException {
      // when
      socketConsumer.addSubscriber(this);

      // then
      await()
        .atLeast(100, TimeUnit.MILLISECONDS).atMost(200, TimeUnit.MILLISECONDS)
        .until(() -> subscription != null);
    }

    @Test
    @DisplayName("was message received")
    void wasMessageReceived() throws InterruptedException, IOException {
      // prepare
      socketConsumer.addSubscriber(this);
      await()
        .atLeast(100, TimeUnit.MILLISECONDS).atMost(200, TimeUnit.MILLISECONDS)
        .until(() -> subscription != null);

      // when
      socketProducer.send(PRODUCER_MESSAGE);

      // then
      await()
        .atLeast(100, TimeUnit.MILLISECONDS).atMost(200, TimeUnit.MILLISECONDS)
        .until(() -> !receivedMessages.isEmpty());
      assertThat(receivedMessages.size()).isEqualTo(1);
      assertThat(receivedMessages.get(0)).isEqualTo(PRODUCER_MESSAGE);
    }
    
    @Override
    public void onSubscribe(Subscription subscription) {
      LOGGER.info("Consumer subscribed");
      this.subscription = subscription;
      this.subscription.request(1);
    }

    @Override
    public void onNext(String message) {
      LOGGER.info("Message receiced");
      receivedMessages.add(message);
    }

    @Override
    public void onError(Throwable throwable) {
      LOGGER.error("Error from consumer: {}", throwable.getMessage());
      subscription.cancel();
    }

    @Override
    public void onComplete() {
      LOGGER.info("Subscription completed");
    }
  }
}
