package com.siemens.ra.cg.nettysocketlibrary.messages;

import com.siemens.ra.cg.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelReadCompleted extends ChannelMessage {
  public ChannelReadCompleted(ChannelHandlerContext ctx) {
    super(ctx);
  }
}
