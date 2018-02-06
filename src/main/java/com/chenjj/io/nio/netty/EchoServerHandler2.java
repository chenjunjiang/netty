package com.chenjj.io.nio.netty;

import com.chenjj.io.nio.netty.codec.msgpack.Student;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;
import java.util.AbstractList;
import java.util.List;
import org.msgpack.type.IntegerValue;
import org.msgpack.type.RawValue;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class EchoServerHandler2 extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    List<Object> values = (List<Object>) msg;
    IntegerValue integerValue = (IntegerValue) values.get(0);
    RawValue rawValue = (RawValue) values.get(1);
    System.out.println("id: " + integerValue.getInt() + ", name: " + rawValue.getString());
    System.out.println("Server receive the msgpack message : " + msg);
    ctx.write(msg);
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
