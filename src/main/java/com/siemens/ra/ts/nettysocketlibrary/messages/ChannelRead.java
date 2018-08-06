package com.siemens.ra.ts.nettysocketlibrary.messages;

import com.siemens.ra.ts.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelRead extends ChannelMessage {
  private String message;

  public ChannelRead(ChannelHandlerContext ctx, String message) {
    super(ctx);
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
