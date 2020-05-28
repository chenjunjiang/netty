package com.chenjj.io.nio.netty.http.json;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 服务器端编码器：发送响应，将生成的json数据转换为DefaultFullHttpResponse对象发送出去
 */
public class HttpJsonResponseEncoder extends AbstractHttpJsonEncoder<HttpJsonResponse> {
    @Override
    protected void encode(ChannelHandlerContext ctx, HttpJsonResponse msg, List<Object> out) throws Exception {
        ByteBuf body = encode0(ctx, msg.getBody());
        FullHttpResponse response = msg.getResponse();
        /**
         * 由于Netty的DefaultFullHttpResponse没有提供动态设置消息体content的接口。
         * 因此我们只能复制一个新的HTTP消息，将动态内容加入，生成一个DefaultFullHttpResponse对象。
         */
        if (response == null) {
            response = new DefaultFullHttpResponse(HTTP_1_1, OK, body);
        } else {
            response = new DefaultFullHttpResponse(msg.getResponse().protocolVersion(),
                    msg.getResponse().status(), body);
        }
        response.headers().set(CONTENT_TYPE, "text/json");
        HttpUtil.setContentLength(response, body.readableBytes());
        out.add(response);
    }
}
