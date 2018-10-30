package com.siemens.ra.cg.nettysocketlibrary.channel;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class DefaultInitializer extends ChannelInitializer<SocketChannel> {
  private DefaultHandler handler;

  public DefaultInitializer(DefaultHandler handler) {
    super();
    this.handler = handler;
  }

  @Override
  protected void initChannel(SocketChannel channel) throws Exception {
    ChannelPipeline pipeline = channel.pipeline();

    pipeline.addLast("decoder", new StringDecoder());
    pipeline.addLast("encoder", new StringEncoder());

    pipeline.addLast("handler", handler);
  }

  public DefaultHandler getHandler() {
    return handler;
  }
}
