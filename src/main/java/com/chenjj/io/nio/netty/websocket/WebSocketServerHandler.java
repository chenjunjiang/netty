package com.chenjj.io.nio.netty.websocket;

import static io.netty.handler.codec.http.HttpVersion.*;
import static io.netty.handler.codec.rtsp.RtspResponseStatuses.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

/**
 * @Author: chenjj
 * @Date: 2018-02-09
 * @Description:
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

  private WebSocketServerHandshaker handshaker;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    // 传统的HTTP接入
    if (msg instanceof FullHttpRequest) {
      handleHttpRequest(ctx, (FullHttpRequest) msg);
    }
    // WebSocket接入
    else if (msg instanceof WebSocketFrame) {
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
    // 判断是否是关闭链路的指令
    if (frame instanceof CloseWebSocketFrame) {
      handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
      return;
    }
    // 判断是否是Ping消息
    if (frame instanceof PingWebSocketFrame) {
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
    System.out.println(String.format("%s received %s", ctx.channel(), request));
    ctx.channel().write(new TextWebSocketFrame(
        request + " , 欢迎使用Netty WebSocket服务，现在时刻：" + System.currentTimeMillis()));
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
    // 如果非Keep-Alive，关闭连接
    ChannelFuture future = ctx.channel().writeAndFlush(response);
    if (!HttpUtil.isKeepAlive(request) || response.status().code() != 200) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }
}
