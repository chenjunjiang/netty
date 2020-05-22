package com.chenjj.io.nio.netty.codec.marshalling;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author: chenjj
 * @Date: 2018-02-07
 * @Description:
 */
@Sharable
public class SubReqServerHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
   /* SubscribeReqProto.SubscribeReq subscribeReq = (SubscribeReqProto.SubscribeReq) msg;
    if ("chenjunjiang".equalsIgnoreCase(subscribeReq.getUserName())) {
      System.out
          .println("Service accept client subscribe request : [" + subscribeReq.toString() + "]");
      ctx.writeAndFlush(resp(subscribeReq.getSubReqID()));
    }*/
  }

  /*private SubscribeRespProto.SubscribeResp resp(int subReqID) {
    SubscribeRespProto.SubscribeResp.Builder builder = SubscribeRespProto.SubscribeResp
        .newBuilder();
    builder.setSubReqID(subReqID);
    builder.setRespCode(0);
    builder.setDesc("Netty book order succeed, 3 days later, sent to the designated address");
    return builder.build();
  }*/

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
  }
}
