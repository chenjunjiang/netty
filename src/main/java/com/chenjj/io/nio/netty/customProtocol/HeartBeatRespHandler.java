package com.chenjj.io.nio.netty.customProtocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author: chenjj
 * @Date: 2018-02-13
 * @Description: 服务端心跳应答
 */
public class HeartBeatRespHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    NettyMessage message = (NettyMessage) msg;
    // 返回心跳应答消息
    if (message.getHeader() != null && message.getHeader().getType() == MessageType.HEARTBEAT_REQ
        .value()) {
      System.out.println("Receive client heart beat message : ---> " + message);
      NettyMessage respMessage = buildHeartBeat();
      System.out.println("Send heart beat response message to client : --->" + respMessage);
      ctx.writeAndFlush(respMessage);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    System.out.println("HeartBeatRespHandler出现了异常......");
    super.exceptionCaught(ctx, cause);
  }

  private NettyMessage buildHeartBeat() {
    NettyMessage message = new NettyMessage();
    Header header = new Header();
    header.setType(MessageType.HEARTBEAT_RESP.value());
    message.setHeader(header);

    return message;
  }
}
