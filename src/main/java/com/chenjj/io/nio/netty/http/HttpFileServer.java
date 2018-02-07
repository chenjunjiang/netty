package com.chenjj.io.nio.netty.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @Author: chenjj
 * @Date: 2018-02-07
 * @Description:
 */
public class HttpFileServer {

  private static final String DEFAULT_URL = "/src/main";

  public void run(final int port, final String url) throws Exception {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
              // 请求解码器
              socketChannel.pipeline().addLast("http-decoder", new HttpRequestDecoder());
              // HttpObjectAggregator的作用是将多个消息转换为单一的FullHttpRequest或者FullHttpResponse，
              // 原因是HTTP解码器在每个HTTP消息中会生成多个消息对象
              // HttpRequest/HttpResponse HttpContent LastHttpContent
              socketChannel.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
              // 响应编码器
              socketChannel.pipeline().addLast("http-encoder", new HttpResponseEncoder());
              // ChunkedWriteHandler的作用是支持异步发送大的码流（例如大的文件传输），但不占用过多的内存，防止发生java内存溢出
              socketChannel.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
              socketChannel.pipeline().addLast("fileServerHandler", new HttpFileServerHandler(url));
            }
          });
      ChannelFuture channelFuture = serverBootstrap.bind("127.0.0.1", port).sync();
      System.out.println("HTTP 文件目录服务器启动，网址是 ：" + "http://127.0.0.1:" + port + url);
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
    String url = DEFAULT_URL;
    if (args.length > 1) {
      url = args[1];
    }
    new HttpFileServer().run(port, url);
  }
}
