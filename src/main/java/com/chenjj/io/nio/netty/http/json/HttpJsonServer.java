package com.chenjj.io.nio.netty.http.json;

import com.chenjj.io.nio.netty.codec.messagePack.MsgpackDecoder;
import com.chenjj.io.nio.netty.codec.messagePack.MsgpackEncoder;
import com.chenjj.io.nio.netty.http.HttpFileServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @Author: chenjj
 * @Date: 2018-02-07
 * @Description:
 */
public class HttpJsonServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // 异常之后采用默认值
            }
        }
        new HttpJsonServer().run(port);
    }

    /**
     * 要接收HttpJsonRequest，解码过程是：
     * ByteBuf -> HttpRequestDecoder -> FullHttpRequest -> HttpJsonRequestDecoder -> HttpJsonRequest -> Order
     * 要发送HttpJsonResponse，编码过程是：
     * Order -> HttpJsonResponse -> HttpJsonResponseEncoder-> FullHttpResponse -> HttpResponseEncoder -> ByteBuf
     *
     * @param port
     * @throws Exception
     */
    public void run(final int port) throws Exception {
        // 配置服务端的NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(
                    ChannelOption.SO_BACKLOG, 100).handler(new LoggingHandler(LogLevel.INFO)).childHandler(
                    new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 添加HTTP消息请求解码器
                            socketChannel.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                            /**
                             * 添加HttpObjectAggregator解码器，它的作用是将多个消息转换为单一的FullHttpRequest或者FullHttpResponse，
                             * 原因是HTTP消息解码器在每个HTTP消息中会生成多个消息对象：
                             * 1、HttpRequest/HttpResponse
                             * 2、HttpContent
                             * 3、LastHttpContent
                             */
                            socketChannel.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                            socketChannel.pipeline().addLast("json-decoder", new HttpJsonRequestDecoder(Order.class, true));
                            // 添加响应编码器，对响应消息进行编码
                            socketChannel.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                            socketChannel.pipeline().addLast("json-encoder", new HttpJsonResponseEncoder());
                            socketChannel.pipeline().addLast("jsonServerHandler", new HttpJsonServerHandler());
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
