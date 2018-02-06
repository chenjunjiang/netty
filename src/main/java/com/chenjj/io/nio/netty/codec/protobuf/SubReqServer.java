package com.chenjj.io.nio.netty.codec.protobuf;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @Author: chenjj
 * @Date: 2018-02-06
 * @Description:
 */
public class SubReqServer {

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
              // 用于半包处理
              socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
              // 解码器，参数是告诉ProtobufDecoder需要解码的目标类是什么；它仅仅负责解码，所以在它前面
              // 一定要有能够处理读半包的解码器，有以下三种方式可以选择：
              /**
               * 1、使用Netty提供的ProtobufVarint32FrameDecoder，它可以处理半包消息；
               * 2、继承Netty提供的通用半包解码器LengthFieldBasedFrameDecoder；
               * 3、继承ByteToMessageDecoder类，自己处理半包消息。
               */
              socketChannel.pipeline().addLast(
                  new ProtobufDecoder(SubscribeReqProto.SubscribeReq.getDefaultInstance()));
              socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
              socketChannel.pipeline().addLast(new ProtobufEncoder());
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
}
