package com.chenjj.io.nio.netty.customProtocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author: chenjj
 * @Date: 2018-02-12
 * @Description: 握手的发起是在客户端和服务端TCP链路建立成功，通道激活时发起握手请求；
 * 握手消息的接入和安全认证在服务端处理。
 */
public class LoginAuthReqHandler extends ChannelInboundHandlerAdapter {

    /**
     * 当客户端跟服务端经过TCP三次握手之后，由客户端构造握手请求消息给服务端，由于采用IP白名单认证机制，
     * 因此，不需要携带 消息体，消息体为空，消息类型为：握手请求信息。握手请求发送之后，按照协议规范，
     * 服务端需要返回握手应答消息。
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端发送握手信息给服务端......");
        ctx.writeAndFlush(buildLoginReq());
    }

    /**
     * 对服务端返回的握手应答消息进行处理，首先判断消息是否是握手应答消息，如果不是，直接透传给后面的ChannelHandler
     * 处理；如果是，则对应答消息进行判断，如果非0，说明认证失败，关闭链路，重新发起连接
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        // 如果是握手应答消息，需要判断是否认证成功
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.LOGIN_RESP
                .value()) {
            byte loginResult = (byte) message.getBody();
            if (loginResult != (byte) 0) {
                // 握手失败，关闭连接
                ctx.close();
            } else {
                System.out.println("Login is ok : " + message);
                ctx.fireChannelRead(msg);
            }
        } else {// 直接传递给后面的ChannelHandler处理
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }

    private Object buildLoginReq() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.LOGIN_REQ.value());
        message.setHeader(header);

        return message;
    }
}
