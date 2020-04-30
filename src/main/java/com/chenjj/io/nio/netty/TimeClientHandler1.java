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
public class TimeClientHandler1 extends ChannelInboundHandlerAdapter {

  private int counter;
  private byte[] request;

  public TimeClientHandler1() {
    request = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
  }

  /**
   * 连接成功之后触发channelActive
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    // 发送消息给服务端
    ByteBuf message = null;
    for (int i = 0; i < 100; i++) {
      message = Unpooled.buffer(request.length);
      message.writeBytes(request);
      ctx.writeAndFlush(message);
    }
  }

  /**
   * 故意制造粘包、拆包情况
   * @param: ctx
   * @param: cause
   * @throws: Exception
   */
  /*@Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ByteBuf byteBuf = (ByteBuf) msg;
    byte[] response = new byte[byteBuf.readableBytes()];
    byteBuf.readBytes(response);
    String body = new String(response, "UTF-8");
    System.out.println("Now is : " + body + " ; the counter is : " + ++counter);
  }*/

  /**
   * @param: ctx
   * @param: msg
   * @throws: Exception
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // 拿到的msg已经是解码成字符串之后的应答消息了
    String body = (String) msg;
    System.out.println("Now is : " + body + " ; the counter is : " + ++counter);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}
