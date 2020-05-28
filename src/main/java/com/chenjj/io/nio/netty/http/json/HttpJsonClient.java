package com.chenjj.io.nio.netty.http.json;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;

public class HttpJsonClient {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }
        new HttpJsonClient().connect("127.0.0.1", port);
    }

    /**
     * 发送请求消息的过程：
     * Order -> HttpJsonRequest -> HttpJsonRequestEncoder -> FullHttpRequest -> HttpRequestEncoder -> ByteBuf
     * 接收响应消息的过程：
     * ByteBuf -> HttpResponseDecoder -> FullHttpResponse -> HttpJsonResponseDecoder -> HttpJsonResponse -> Order
     *
     * @param host
     * @param port
     * @throws Exception
     */
    public void connect(String host, int port) throws Exception {
        // 配置客户端NIO线程池
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast("http-decoder", new HttpResponseDecoder());
                            ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                            // json解码器
                            ch.pipeline().addLast("json-decoder", new HttpJsonResponseDecoder(Order.class, true));
                            ch.pipeline().addLast("http-encoder", new HttpRequestEncoder());
                            ch.pipeline().addLast("json-encoder", new HttpJsonRequestEncoder());
                            ch.pipeline().addLast("jsonClientHandler", new HttpJsonClientHandler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }
}
