package com.siemens.ra.ts.nettysocketlibrary.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
  
  private ExecutorService executorService = Executors.newFixedThreadPool(4);
  private SocketProducer socketProducer;
  private SocketConsumer socketConsumer;

  @Nested
  @DisplayName("connecting producer and consumer")
  class ConnectingProducerAndConsumer {
    private static final int SERVER_PORT = 9999;
    private static final String PRODUCER_MESSAGE = "12345";
    private static final String CONSUMER_MESSAGE = "abcde";
    private boolean connected = false;
    
    @BeforeEach
    void instantiateSocketProducerAndConsumer() throws InterruptedException {
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
    void closeSockets() throws InterruptedException {
      // clean-up:      
      socketConsumer.close();
      socketConsumer.waitUntilTerminated();
      socketProducer.close();
      socketProducer.waitUntilTerminated();      
    }

    @Test
    @DisplayName("are producer and consumer connected")
    void areProducerAndConsumerConnected() throws InterruptedException {
      // when
      
      // then
      isConsumerConnected();      
    }
    
    @Test
    @DisplayName("are messages sent and received")
    void areMessagesSentAndReceived() throws InterruptedException {
      // prepare
      isConsumerConnected();
      
      // when
      try {
        socketProducer.send(PRODUCER_MESSAGE);
        socketConsumer.send(CONSUMER_MESSAGE);
      } catch (IOException e) {
        LOGGER.error(e.getMessage());
      }
      
      Thread.sleep(100);
      
      assertThat(socketConsumer.receive()).isNotEmpty();
      assertThat(socketConsumer.receive().take()).isEqualTo(PRODUCER_MESSAGE);
      assertThat(socketConsumer.receive()).isEmpty();

      assertThat(socketProducer.receive()).isNotEmpty();
      assertThat(socketProducer.receive().take()).isEqualTo(CONSUMER_MESSAGE);
      assertThat(socketProducer.receive()).isEmpty();
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
      Thread.sleep(100);
      assertThat(connected).isTrue();
    }
  }
}
