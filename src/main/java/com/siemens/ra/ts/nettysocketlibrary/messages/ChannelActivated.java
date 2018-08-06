package com.siemens.ra.ts.nettysocketlibrary.messages;

import com.siemens.ra.ts.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelActivated extends ChannelMessage {
  public ChannelActivated(ChannelHandlerContext ctx) {
    super(ctx);
  }
}
