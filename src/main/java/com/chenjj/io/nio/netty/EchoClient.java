package com.chenjj.io.nio.netty;

import com.chenjj.io.nio.netty.codec.msgpack.MsgpackDecoder;
import com.chenjj.io.nio.netty.codec.msgpack.MsgpackEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * @Author: chenjj
 * @Date: 2018-01-31
 * @Description:
 */
public class EchoClient {

  public void connect(int port, String host) throws Exception {
    // 配置客户端NIO线程池
    EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
              socketChannel.pipeline()
                  .addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
              socketChannel.pipeline().addLast("msgpack decoder", new MsgpackDecoder());
              socketChannel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(2));
              socketChannel.pipeline().addLast("msgpack encoder", new MsgpackEncoder());
              socketChannel.pipeline().addLast(new EchoClientHandler(1));
            }
          });
      ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
      channelFuture.channel().closeFuture().sync();
    } finally {
      eventLoopGroup.shutdownGracefully();
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
    new EchoClient().connect(port, "127.0.0.1");
  }
}
