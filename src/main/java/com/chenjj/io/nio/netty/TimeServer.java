package com.chenjj.io.nio.netty;

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

  public void bind(int port) throws Exception {
    //配置服务端的NIO线程组

    // 用于接受客户端的连接
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    // 用于SocketChannel的网络读写
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
          .option(ChannelOption.SO_BACKLOG, 1024).childHandler(new ChildChannelHandler());
      //绑定端口，同步等待成功
      ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
      //等待服务端链路关闭之后main函数才退出
      channelFuture.channel().closeFuture().sync();
    } finally {
      // 优雅退出，释放线程池资源
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  /*private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
      socketChannel.pipeline().addLast(new TimeServerHandler());
    }
  }*/

  /**
   * 添加解码器
   */
  private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
      /**
       *LineBasedFrameDecoder的工作原理是它依次遍历ByteBuf中的可读字节，判断看是否有"\n"或者
       * "\r\n"，如果有，就以此位置为结束位置，从可读索引到结束位置区间的字节就组成了一行。它是以换行符为结束
       * 标志的解码器，支持携带结束符或者不携带结束符两张解码方式，同时支持配置单行的最大长度。如果连续读取到最大
       * 长度后仍然没有发现换行符，就会抛出异常，同时忽略掉之前读到的异常码流。
       */
      socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
      socketChannel.pipeline().addLast(new StringDecoder());
      socketChannel.pipeline().addLast(new TimeServerHandler1());
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
    new TimeServer().bind(port);
  }
}
