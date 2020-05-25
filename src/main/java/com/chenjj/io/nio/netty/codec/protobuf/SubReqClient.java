package com.chenjj.io.nio.netty.codec.protobuf;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * Netty源码分析-ProtobufVarint32LengthFieldPrepender
 * https://blog.csdn.net/nimasike/article/details/101378388
 * Netty源码分析-ProtobufVarint32FrameDecoder
 * https://blog.csdn.net/nimasike/article/details/101392803?utm_medium=distribute.pc_relevant.none-task-blog-baidujs-1
 *
 * @Author: chenjj
 * @Date: 2018-02-06
 * @Description:
 */
public class SubReqClient {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // 异常之后采用默认值
            }
        }
        new SubReqClient().connect(port, "127.0.0.1");
    }

    public void connect(int port, String host) throws Exception {
        // 配置客户端NIO线程组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            socketChannel.pipeline().addLast(new ProtobufEncoder());
                            socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            socketChannel.pipeline().addLast(new ProtobufDecoder(
                                    SubscribeRespProto.SubscribeResp.getDefaultInstance()));
                            socketChannel.pipeline().addLast(new SubReqClientHandler());
                        }
                    });
            // 发起异步连接操作
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            // 等待客户端链路个关闭
            channelFuture.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

}
