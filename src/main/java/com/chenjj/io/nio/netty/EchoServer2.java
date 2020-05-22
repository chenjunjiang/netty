package com.chenjj.io.nio.netty;

import com.chenjj.io.nio.netty.codec.messagePack.MsgpackDecoder;
import com.chenjj.io.nio.netty.codec.messagePack.MsgpackEncoder;
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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @Author: chenjj
 * @Date: 2018-01-31
 * @Description:
 */
public class EchoServer2 {

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
              socketChannel.pipeline()
                  .addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
              socketChannel.pipeline().addLast("msgpack decoder", new MsgpackDecoder());
              socketChannel.pipeline().addLast("frameEecoder", new LengthFieldPrepender(2));
              socketChannel.pipeline().addLast("msgpack encoder", new MsgpackEncoder());
              socketChannel.pipeline().addLast(new EchoServerHandler2());
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
    new EchoServer2().bind(port);
  }
}
