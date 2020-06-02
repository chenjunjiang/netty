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
 * @Description: 如果客户端(WebSocketServer.html ， 这里指浏览器)没有加应用层面的心跳机制，
 * 那么默认使用的是基于TCP keepalive，客户端与服务端第一次建立连接成功并发送消息之后，
 * 如果在tcp_keepalive_time(默认7200s)时间内客户端和服务端没有进行任何数据传输，
 * TCP层将发送相应的KeepAlive探针以确定连接可用性，探测失败后重试 10（参数 tcp_keepalive_probes）次，
 * 每次间隔时间 75s（参数tcp_keepalive_intvl），所有探测失败后，就认为当前连接已经不可用。
 * 服务端会关闭这个连接，客户端收到这个关闭连接的消息后就会执行socket.onclose回调函数。
 * 所以我们再点击页面上"发送 WebSocket 请求消息"按钮时会提示"WebSocket连接没有建立成功!"。
 * <p>
 * 如果客户端(这里指浏览器)使用WebSocketServer-heartbeat.html，
 * 那么连接的检测是通过应用层面的心跳机制(ping pong)来保证的。
 * <p>
 * 服务端主动关闭连接后也会执行客户端的socket.onclose回调函数。
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
