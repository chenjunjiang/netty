package com.chenjj.io.nio.netty.codec.marshalling;

import com.chenjj.io.nio.netty.codec.protobuf.SubscribeReqProto;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: chenjj
 * @Date: 2018-02-07
 * @Description:
 */
public class SubReqClientHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    for (int i = 1; i < 11; i++) {
      ctx.write(subReq(i));
    }
    ctx.flush();
  }

  private SubscribeReqProto.SubscribeReq subReq(int i) {
    SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
    builder.setSubReqID(i);
    builder.setUserName("chenjunjiang");
    builder.setProductName("Netty Book For Marshalling");
    List<String> address = new ArrayList<>();
    address.add("chengdu");
    address.add("beijing");
    address.add("xiamen");
    builder.addAllAddress(address);
    return builder.build();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    System.out.println("Receive server response : [" + msg + "]");
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
  }
}
