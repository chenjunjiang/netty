package com.chenjj.io.nio.netty.codec.marshalling;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @Author: chenjj
 * @Date: 2018-02-07
 * @Description:
 */
public class SubReqServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // 异常之后采用默认值
            }
        }
        new SubReqServer().bind(port);
    }

    public void bind(int port) throws Exception {
        // 配置服务端的NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // Netty的Marshalling编解码器支持半包和粘包的处理，不需要再添加其它解码器
                            socketChannel.pipeline().addLast(MarshallingCodeFactory.buildMarshallingDecoder());
                            socketChannel.pipeline().addLast(MarshallingCodeFactory.buildMarshallingEncoder());
                            socketChannel.pipeline().addLast(new SubReqServerHandler());
                        }
                    });

            // 绑定端口，同步等待成功
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            // 等待服务端监听端口关闭
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
