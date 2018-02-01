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
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 没有编写客户端代码测试该服务端，使用telnet进行测试 输入telnet 127.0.0.1 8080 ,在连接上的TCP服务器的telnet程序窗口中同时按下 "Ctrl" 和 "]"
 * ,接着在提示窗口中执行 set    localecho命令,按下回车,命令执行,再次按下回车,退出当前窗口 ,会来到一个新的 telnet窗口,在此窗口中输入的内容就可以显示出来了 .
 *
 * @Author: chenjj
 * @Date: 2018-01-31
 * @Description:
 */
public class EchoServer1 {

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
              // 利用FixedLengthFrameDecoder解码器，无论一次接收到多少数据报，它都会按照构造函数中
              // 设置的固定长度进行解码，如果是半包消息，FixedLengthFrameDecoder会缓存半包消息并等待
              // 下一个包到达后进行拼包，直到读取到一个完整的包
              // 这里按照20个字节长度对请求消息进行截取
              socketChannel.pipeline().addLast(new FixedLengthFrameDecoder(20));
              socketChannel.pipeline().addLast(new StringDecoder());
              socketChannel.pipeline().addLast(new EchoServerHandler1());
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
    new EchoServer1().bind(port);
  }
}
