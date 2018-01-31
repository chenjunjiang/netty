package com.chenjj.io.nio.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author: chenjj
 * @Date: 2018-01-30
 * @Description:
 */
public class TimeClientHandler extends ChannelInboundHandlerAdapter {

  private final ByteBuf message;

  public TimeClientHandler() {
    byte[] request = "QUERY TIME ORDER".getBytes();
    message = Unpooled.buffer(request.length);
    message.writeBytes(request);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    // 发送消息给服务端
    ctx.writeAndFlush(message);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ByteBuf byteBuf = (ByteBuf) msg;
    byte[] response = new byte[byteBuf.readableBytes()];
    byteBuf.readBytes(response);
    String body = new String(response, "UTF-8");
    System.out.println("Now is : " + body);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}
