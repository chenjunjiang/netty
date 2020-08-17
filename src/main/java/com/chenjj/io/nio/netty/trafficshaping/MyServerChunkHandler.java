package com.chenjj.io.nio.netty.trafficshaping;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.ReferenceCountUtil;

public class MyServerChunkHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            ByteInputStream in = new ByteInputStream();
            byte[] data = null;
            if (byteBuf.hasArray()) {
                System.out.println("+++ is array");
                data = byteBuf.array().clone();
            } else {
                System.out.println("--- is direct");
                data = new byte[byteBuf.readableBytes()];
                System.out.println("MyServerChunkHandler写数据的大小是：" + data.length); // 26M
                byteBuf.readBytes(data);
            }
            in.setBuf(data);
            // ChunkedStream的DEFAULT_CHUNK_SIZE = 8192，这也是为什么客户端收到的消息长度是8192的原因
            ChunkedStream stream = new ChunkedStream(in);
            ReferenceCountUtil.release(msg);
            ctx.writeAndFlush(stream, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
