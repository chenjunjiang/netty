package com.chenjj.io.nio.netty.http.json;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.nio.charset.Charset;

/**
 * 定义抽象类，封装json转换方法
 *
 * @param <T>
 */
public abstract class AbstractHttpJsonEncoder<T> extends MessageToMessageEncoder<T> {
    final static Charset UTF_8 = Charset.forName("utf-8");

    protected ByteBuf encode0(ChannelHandlerContext ctx, Object body) {
        String jsonStr = FastJsonUtils.convertObjectToJSON(body);
        ByteBuf encodeBuf = Unpooled.copiedBuffer(jsonStr, UTF_8);
        return encodeBuf;
    }
}
