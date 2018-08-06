package com.siemens.ra.ts.nettysocketlibrary.messages;

import com.siemens.ra.ts.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelRegistered extends ChannelMessage {
  public ChannelRegistered(ChannelHandlerContext ctx) {
    super(ctx);
  }
}
