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
public class TimeServerHandler1 extends ChannelInboundHandlerAdapter {

  private int counter;

  /**
   * @param: ctx
   * @param: msg
   * @throws: Exception
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // 接收到的msg就是删除回车换行符后的请求消息，不需要额外考虑处理半包问题，也不需要对请求消息进行编码
    String body = (String) msg;
    System.out
        .println("The time server receive order : " + body + "; the counter is : " + ++counter);
    String currentTime =
        "QUERY TIME ORDER".equalsIgnoreCase(body) ? System.currentTimeMillis() + "" : "BAD ORDER";
    // System.getProperty("line.separator")是换行符,功能和"\n"是一致的,但是此种写法屏蔽了 Windows和Linux的区别 ，更保险一些.
    currentTime = currentTime + System.getProperty("line.separator");
    ByteBuf response = Unpooled.copiedBuffer(currentTime.getBytes());
    ctx.writeAndFlush(response);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}
