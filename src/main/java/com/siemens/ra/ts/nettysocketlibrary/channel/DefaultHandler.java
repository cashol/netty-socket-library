package com.siemens.ra.ts.nettysocketlibrary.channel;

import java.util.concurrent.Flow.Subscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelActivated;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelExceptionCaught;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelInactivated;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelRead;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelReadCompleted;
import com.siemens.ra.ts.nettysocketlibrary.messages.ChannelRegistered;

import java.util.concurrent.SubmissionPublisher;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

@Sharable
public class DefaultHandler extends SimpleChannelInboundHandler<String> {
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  final private SubmissionPublisher<ChannelMessage> publisher = new SubmissionPublisher<>();

  public DefaultHandler(final Subscriber<ChannelMessage> channelSubscriber) {
    publisher.subscribe(channelSubscriber);
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("channelRegistered(): {}", ctx);
    publisher.submit(new ChannelRegistered(ctx));
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("channelActive(): {}", ctx);
    publisher.submit(new ChannelActivated(ctx));
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("channelInactive(): {}", ctx);
    publisher.submit(new ChannelInactivated(ctx));
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    try {
      //LOGGER.info("channelRead0(): {}", ctx);
      publisher.submit(new ChannelRead(ctx, msg));
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    //LOGGER.info("channelReadComplete(): {}", ctx);
    publisher.submit(new ChannelReadCompleted(ctx));
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("***** channelWritabilityChanged(): {}", ctx);

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    LOGGER.error("exceptionCaught(): {} {}", ctx, cause.getMessage());
    publisher.submit(new ChannelExceptionCaught(ctx, cause));
    // Close the connection when an exception is raised.
    ctx.close();
  }
}
