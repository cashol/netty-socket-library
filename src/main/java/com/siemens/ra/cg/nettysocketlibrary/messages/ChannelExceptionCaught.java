package com.siemens.ra.cg.nettysocketlibrary.messages;

import com.siemens.ra.cg.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelExceptionCaught extends ChannelMessage {
  private Throwable cause;
  
  public ChannelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    super(ctx);
    this.cause = cause;
  }
  public Throwable getCause() {
    return cause;
  }
}
