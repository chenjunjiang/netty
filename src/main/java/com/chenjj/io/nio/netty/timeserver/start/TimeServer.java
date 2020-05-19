package com.chenjj.io.nio.netty.timeserver.start;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @Author: chenjj
 * @Date: 2018-01-30
 * @Description:
 */
public class TimeServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // 异常之后采用默认值
            }
        }
        new TimeServer().bind(port);
    }

    public void bind(int port) throws Exception {
        //配置服务端的NIO线程组，采用多reactor模式
        // 用于接受客户端的连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 用于SocketChannel的网络读写
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            /**
             * 浅谈tcp socket的backlog参数
             * https://www.jianshu.com/p/e6f2036621f4
             * https://www.cnblogs.com/Orgliny/p/5780796.html
             */
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024).childHandler(new ChildChannelHandler());
            //绑定端口，同步等待绑定操作完成
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            //等待服务端链路关闭之后main函数才退出
            channelFuture.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放相关资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * IO事件处理类
     */
    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast(new TimeServerHandler());
        }
    }

}
