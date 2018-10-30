package com.siemens.ra.cg.nettysocketlibrary.messages;

import com.siemens.ra.cg.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelInactivated extends ChannelMessage {
  public ChannelInactivated(ChannelHandlerContext ctx) {
    super(ctx);
  }
}
