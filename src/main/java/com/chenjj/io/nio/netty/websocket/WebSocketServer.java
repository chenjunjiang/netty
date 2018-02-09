package com.chenjj.io.nio.netty.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @Author: chenjj
 * @Date: 2018-02-09
 * @Description: HTTP协议的弊端： (1) HTTP协议为半双工协议。半双工协议指数据可以在客户端和服务端两个方向上传输，但是不能同时传输。
 * 它意味着在同一时刻，只要一个方向上的数据传送。 (2) HTTP消息冗长而繁琐。HTTP消息包含消息头，消息体，换行符等，通常情况下采用文本方式传输，相比 其它二进制通信协议，冗长而繁琐。
 * (3) 针对服务器推送的黑客攻击，例如长时间轮询。 WebSocket的特点： 1、单一的TCP连接，采用全双工模式通信； 2、对代理、防火墙和路由器透明；
 * 3、无头部消息、Cookie和身份验证； 4、无安全开销； 5、通过"ping/pong"帧保持链路激活； 6、服务器可以主动传递消息给客户端，不需要客户端轮询。
 */
public class WebSocketServer {

  public void run(int port) throws Exception {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
          .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
              ChannelPipeline pipeline = ch.pipeline();
              // 将请求和应答消息编码或解码为HTTP消息
              pipeline.addLast("http-codec", new HttpServerCodec());
              // HttpObjectAggregator的目的是将HTTP消息的多个部分组合成一条完整的HTTP消息
              pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
              // ChunkedWriteHandler用于向客户端发送HTML5文件，它主要用于支持浏览器和服务端进行WebSocket通信
              pipeline.addLast("http-chunked", new ChunkedWriteHandler());
              pipeline.addLast("handler", new WebSocketServerHandler());
            }
          });
      Channel channel = serverBootstrap.bind(port).sync().channel();
      System.out.println("Web socket server started at port " + port + ".");
      System.out.println("Open your browser and navigate to http://localhost:" + port + "/");
      channel.closeFuture().sync();
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
    new WebSocketServer().run(port);
  }
}
