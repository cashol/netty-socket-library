package com.siemens.ra.ts.nettysocketlibrary.messages;

import com.siemens.ra.ts.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelInactivated extends ChannelMessage {
  public ChannelInactivated(ChannelHandlerContext ctx) {
    super(ctx);
  }
}
