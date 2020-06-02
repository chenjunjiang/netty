package com.chenjj.io.nio.netty.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.rtsp.RtspResponseStatuses.BAD_REQUEST;

/**
 * @Author: chenjj
 * @Date: 2018-02-09
 * @Description:
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 第一次建立WebSocket连接的时候使用的是HTTP接入，成功之后后面的通讯就走WebSocket接入
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) { // WebSocket接入
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 如果HTTP解析失败，返回HTTP异常
        if (!request.decoderResult().isSuccess() || (!"websocket"
                .equals(request.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }
        // 构建握手响应返回，本机测试
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8080/websocket", null, false);
        handshaker = factory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // handshake方法里面会添加WebSocketFrameDecoder和WebSocketFrameEncoder
            // 这样服务端就可以自动对WebSocket消息进行编解码了，后面业务handler可以直接对WebSocket对象进行操作
            handshaker.handshake(ctx.channel(), request);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        /**
         * 判断是否是关闭链路的指令，重新刷新浏览器页面或关掉浏览器，
         * 客户端都会认为是关闭连接，客户端可以把关闭帧发送给服务端。
         */
        if (frame instanceof CloseWebSocketFrame) {
            System.out.println("收到关闭连接的指令......");
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否是Ping消息
        if (frame instanceof PingWebSocketFrame) {
            System.out.println("收到ping消息......");
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 本列子只支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(
                    String.format("%s frame type not supported", frame.getClass().getName()));
        }
        // 返回应答消息
        String request = ((TextWebSocketFrame) frame).text();
        // 判断是否是Ping消息
        if ("ping".equals(request)) {
            System.out.println("收到ping消息......");
            // 这里不能用这种方式返回pong消息，因为接收到的frame不是PingWebSocketFrame
            // ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            ctx.channel().write(new TextWebSocketFrame("pong"));
            return;
        }
        System.out.println(String.format("%s received %s", ctx.channel(), request));
        // 注意：如果这里用的是write()方法，没有flush，那么在channelReadComplete()方法里面一定要flush
        ctx.channel().writeAndFlush(new TextWebSocketFrame(
                request + " , 欢迎使用Netty WebSocket服务，现在时刻：" + System.currentTimeMillis()));

        // 异步返回应答消息，只要这个WebSocket连接还有效，客户端和服务端就能一直通过这个连接通讯
        /*new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("异步发送消息给客户端" + i);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(
                        request + " , 欢迎使用Netty WebSocket服务" + i + "，现在时刻：" + System.currentTimeMillis()));
            }
        }).start();*/
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                  DefaultFullHttpResponse response) {
        // 返回应答给客户端
        if (response.status().code() != 200) {
            ByteBuf byteBuf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(byteBuf);
            byteBuf.release();
            HttpUtil.setContentLength(response, response.content().readableBytes());
        }
        ChannelFuture future = ctx.channel().writeAndFlush(response);
        // 如果非Keep-Alive，关闭连接
        if (!HttpUtil.isKeepAlive(request) || response.status().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
