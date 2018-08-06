package com.siemens.ra.ts.nettysocketlibrary.messages;

import com.siemens.ra.ts.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelReadCompleted extends ChannelMessage {
  public ChannelReadCompleted(ChannelHandlerContext ctx) {
    super(ctx);
  }
}
