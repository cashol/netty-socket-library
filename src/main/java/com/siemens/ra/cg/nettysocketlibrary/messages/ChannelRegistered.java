package com.siemens.ra.cg.nettysocketlibrary.messages;

import com.siemens.ra.cg.nettysocketlibrary.channel.ChannelMessage;

import io.netty.channel.ChannelHandlerContext;

public class ChannelRegistered extends ChannelMessage {
  public ChannelRegistered(ChannelHandlerContext ctx) {
    super(ctx);
  }
}
