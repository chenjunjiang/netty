package com.chenjj.io.nio.netty.timeserver.stick_dismantle;

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

    private int counter;
    private byte[] req;

    public TimeClientHandler() {
        req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
    }

    /**
     * 当客户端和服务端TCP链路建立成功之后，Netty的NIO线程会调用这个方法
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf message;
        // 循环发送100条消息，每发送一条就刷新一次，保证每条消息都会被写入Channel中
        for (int i = 0; i < 100; i++) {
            message = Unpooled.buffer(req.length);
            message.writeBytes(req);
            ctx.writeAndFlush(message);
        }
    }

    /**
     * 当服务端返回应答消息时会调用这个方法。
     * 按照设计初衷，客户端应该收到100条当前系统时间的消息，但只收到了一条消息
     * "Now is : BAD ORDERBAD ORDER ; the counter is : 1"，说明服务端返回的应答消息
     * 也发生了粘包。
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
        System.out.println("Now is : " + body + " ; the counter is : " + ++counter);
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
