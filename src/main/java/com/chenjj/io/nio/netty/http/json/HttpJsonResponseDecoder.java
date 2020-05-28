package com.chenjj.io.nio.netty.http.json;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.List;

/**
 * 将FullHttpResponse转换为HttpJsonResponse
 */
public class HttpJsonResponseDecoder extends AbstractHttpJsonDecoder<FullHttpResponse> {
    public HttpJsonResponseDecoder(Class<?> clazz) {
        this(clazz, false);
    }

    public HttpJsonResponseDecoder(Class<?> clazz, boolean isPrint) {
        super(clazz, isPrint);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpResponse msg, List out) throws Exception {
        System.out.println("开始解码...");
        out.add(new HttpJsonResponse(msg, decode0(ctx, msg.content())));
    }
}
