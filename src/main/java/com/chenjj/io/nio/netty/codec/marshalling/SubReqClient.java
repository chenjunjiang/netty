package com.chenjj.io.nio.netty.codec.marshalling;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @Author: chenjj
 * @Date: 2018-02-07
 * @Description:
 */
public class SubReqClient {

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
              // Netty的Marshalling编解码器支持半包和粘包的处理，不需要再添加其它解码器
              socketChannel.pipeline().addLast(MarshallingCodeFactory.buildMarshallingDecoder());
              socketChannel.pipeline().addLast(MarshallingCodeFactory.buildMarshallingEncoder());
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
}
