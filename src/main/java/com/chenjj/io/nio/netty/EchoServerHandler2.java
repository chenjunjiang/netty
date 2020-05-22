package com.chenjj.io.nio.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.RawValue;

import java.util.List;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class EchoServerHandler2 extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    /*List<Object> values = (List<Object>) msg;
    IntegerValue integerValue = (IntegerValue) values.get(0);
    RawValue rawValue = (RawValue) values.get(1);
    System.out.println("id: " + integerValue.getInt() + ", name: " + rawValue.getString());
    System.out.println("Server receive the msgpack message : " + msg);
    ctx.write(msg);*/
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
