package com.siemens.ra.ts.nettysocketlibrary.channel;

import io.netty.channel.ChannelHandlerContext;

public class ChannelMessage {
  private ChannelHandlerContext ctx;

  public ChannelMessage(ChannelHandlerContext ctx) {
    super();
    this.ctx = ctx;
  }

  public ChannelHandlerContext getCtx() {
    return ctx;
  }
}
