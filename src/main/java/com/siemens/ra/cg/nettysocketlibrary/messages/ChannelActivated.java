package com.siemens.ra.cg.nettysocketlibrary.messages;

import com.siemens.ra.cg.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelActivated extends ChannelMessage {
  public ChannelActivated(ChannelHandlerContext ctx) {
    super(ctx);
  }
}
