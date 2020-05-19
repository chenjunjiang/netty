package com.chenjj.io.nio.netty.timeserver.solve_stick_dismantle;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

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

        /**
         * LineBasedFrameDecoder的工作原理是它依次遍历ByteBuf中可读的字节，判断看是否有"\n"或者"\r\n"，
         * 如果有，就以此位置为结束位置，从可读索引到结束位置区间的字节就组成一行。它是以换行符为结束标志的解码器
         * ，支持携带结束符或不携带结束符两种编码方式(
         * public LineBasedFrameDecoder(final int maxLength, final boolean stripDelimiter, final boolean failFast) {
         * this.maxLength = maxLength;
         * this.failFast = failFast;
         * this.stripDelimiter = stripDelimiter;
         * }
         * 通过这个构造函数的stripDelimiter就可以决定是否携带结束符)，同时支持配置单行的最大长度。
         * StringDecoder将接收到的对象转换成字符串，然后继续调用后面的Handler。
         * LineBasedFrameDecoder+StringDecoder组合就是按行切换的文本解码器。
         *
         * @param socketChannel
         * @throws Exception
         */
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
            socketChannel.pipeline().addLast(new StringDecoder());
            socketChannel.pipeline().addLast(new TimeServerHandler());
        }
    }

}
