package com.chenjj.io.nio.netty.timeserver.start;

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

    /**
     * 当客户端和服务端TCP链路建立成功之后，Netty的NIO线程会调用这个方法
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 发送消息给服务端
        ctx.writeAndFlush(message);
    }

    /**
     * 当服务端返回应答消息时会调用这个方法
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        byte[] response = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(response);
        String body = new String(response, "UTF-8");
        System.out.println("Now is : " + body);
    }

    /**
     * 发送异常时，关闭ChannelHandlerContext，释放资源
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
