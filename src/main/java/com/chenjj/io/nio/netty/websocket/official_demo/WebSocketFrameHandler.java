package com.chenjj.io.nio.netty.websocket.official_demo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.Locale;

/**
 * Echoes uppercase content of text frames.
 * 这个handler只能处理WebSocketFrame类型的消息
 */
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        /**
         * 官方例子在这里有句注释： ping and pong frames already handled
         * 意思是执行到这的时候ping、pong消息已经处理完了(前提是客户端发送了ping消息)，
         * 为什么会这样呢？ 因为我们在WebSocketServerInitializer里面配置了WebSocketServerProtocolHandler，
         * 它又是配置在WebSocketFrameHandler的前面，所以它会先执行，而WebSocketServerProtocolHandler
         * 又继承自WebSocketProtocolHandler， 它里面的这个方法：
         * @Override
         *     protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
         *         if (frame instanceof PingWebSocketFrame) {
         *             frame.content().retain();
         *             ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
         *             readIfNeeded(ctx);
         *             return;
         *         }
         *         if (frame instanceof PongWebSocketFrame && dropPongFrames) {
         *             readIfNeeded(ctx);
         *             return;
         *         }
         *
         *         out.add(frame.retain());
         *     }
         * 能清楚的看到，已经处理了来自客户端的ping消息
         */
        if (frame instanceof TextWebSocketFrame) {
            // Send the uppercase string back.
            String request = ((TextWebSocketFrame) frame).text();
            ctx.channel().writeAndFlush(new TextWebSocketFrame(request.toUpperCase(Locale.US)));
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }
}
