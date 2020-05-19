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
public class TimeServerHandler extends ChannelInboundHandlerAdapter {

    private int counter;

    /**
     * 故意制造粘包、拆包情况
     * 每读到一条消息后，就计数一次，然后发送应答消息给客户端。按照设计，服务端接收到的消息总数应该跟
     * 客户端总数相同，而且请求消息删除回车换行符之后应该为"QUERY TIME ORDER"。
     * 但是运行之后发现，服务端只收到了两条信息(客户端发送了100条消息)，一条消息包含了57条
     * "QUERY TIME ORDER"，另一条消息包含了43条"QUERY TIME ORDER"，这说明发送了TCP粘包。
     *
     * @param: ctx
     * @param: msg
     * @throws: Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        byte[] request = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(request);
        // System.getProperty("line.separator")是换行符,功能和"\n"是一致的,但是此种写法屏蔽了 Windows和Linux的区别 ，更保险一些.
        String body = new String(request, "UTF-8")
                .substring(0, request.length - System.getProperty("line.separator").length());
        System.out
                .println("The time server receive order : " + body + "; the counter is : " + ++counter);
        String currentTime =
                "QUERY TIME ORDER".equalsIgnoreCase(body) ? System.currentTimeMillis() + "" : "BAD ORDER";
        ByteBuf response = Unpooled.copiedBuffer(currentTime.getBytes());
        ctx.write(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        /**
         * 将消息发送队列中的消息写入到SocketChannel中发送给对方。从性能角度考虑，为了防止频繁地唤醒Selector进行消息发送，
         * Netty的write方法并不直接将消息写入SocketChannel中，调用write方法只是把待发送的消息放到发送缓冲数组中，
         * 再通过调用flush方法，将发送缓冲区中的消息全部写到SocketChannel中。
         */
        ctx.flush();
    }

    /**
     * 发生异常时，关闭ChannelHandlerContext，释放和ChannelHandlerContext相关联的句柄资源
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
