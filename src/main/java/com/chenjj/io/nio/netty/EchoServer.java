package com.chenjj.io.nio.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @Author: chenjj
 * @Date: 2018-01-31
 * @Description:
 */
public class EchoServer {

  public void bind(int port) throws Exception {
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
              // 使用$_作为分隔符
              ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());
              // 1024表示单条消息的最大长度，当达到该长度之后仍然没有查找到分隔符，就抛出
              // TooLongFrameException异常，防止由于异常码流缺失分隔符导致的内存溢出，这是
              // Netty解码器的可靠性保护
              socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter));
              socketChannel.pipeline().addLast(new StringDecoder());
              socketChannel.pipeline().addLast(new EchoServerHandler());
            }
          });
      ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
      channelFuture.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public static void main(String[] args) throws Exception {
    int port = 8080;
    if (args != null && args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        // 异常之后采用默认值
      }
    }
    new EchoServer().bind(port);
  }
}
