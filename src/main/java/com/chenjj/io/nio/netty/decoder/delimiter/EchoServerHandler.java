package com.chenjj.io.nio.netty.decoder.delimiter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author: chenjj
 * @Date: 2018-01-31
 * @Description:
 */
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    int counter = 0;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        /**
         * DelimiterBasedFrameDecoder自动对请求消息进行了解码，后续的handler接收到的msg对象就是个完整的消息包；
         * 第二个handler是StringDecoder，它会将ByteBuf解码成字符串对象；第三个handler(EchoServerHandler)接收到的msg
         * 消息就是解码后的字符串对象。
         * 由于我们用DelimiterBasedFrameDecoder过滤掉了分隔符(因为我们使用的DelimiterBasedFrameDecoder构造函数中stripDelimiter参数默认为true)，
         * 所以返回给客户端时需要在请求尾部拼接分隔符$_。
         */
        String body = (String) msg;
        System.out.println("This is " + ++counter + " times receive client: [" + body + "]");
        body += "$_";
        ByteBuf echo = Unpooled.copiedBuffer(body.getBytes());
        ctx.writeAndFlush(echo);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
