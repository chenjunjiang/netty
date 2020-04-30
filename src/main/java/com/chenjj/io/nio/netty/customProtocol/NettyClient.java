package com.chenjj.io.nio.netty.customProtocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author: chenjj
 * @Date: 2018-02-13
 * @Description:
 */
public class NettyClient {

  private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
  EventLoopGroup group = new NioEventLoopGroup();

  public void connect(int port, String host) throws Exception {
    // 配置客户端NIO线程组
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group).channel(NioSocketChannel.class)
          .option(ChannelOption.TCP_NODELAY, true)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
              // 为了防止由于单条消息过大导致的内存溢出或者畸形码流导致解码错位引起内存分配失败，我们对单条消息最大长度进行限制
              ch.pipeline().addLast("MessageDecoder", new NettyMessageDecoder(1024 * 1024, 4, 4));
              ch.pipeline().addLast("MessageEncoder", new NettyMessageEncoder());
              // 心跳超时直接利用Netty的ReadTimeoutHandler机制，当一定周期内（默认值50s）没有读取到对方任何消息时，
              // 需要主动关闭链路。如果是客户端，重新发起连接（见下面finally里面的代码）；如果是服务端，释放资源，清除客户端登录缓存信息，等待客户端重连
              ch.pipeline().addLast("ReadTimeoutHandler", new ReadTimeoutHandler(50));
              ch.pipeline().addLast("LoginAuthReqHandler", new LoginAuthReqHandler());
              ch.pipeline().addLast("HeartBeatReqHandler", new HeartBeatReqHandler());
            }
          });
      // 发送异步连接操作，这和之前的写法不同，主要用于服务端重复登录保护，另外，从产品管理角度看，一般情况下不允许系统随便使用随机端口
      ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port),
          new InetSocketAddress(NettyConstant.LOCALIP, NettyConstant.LOCAL_PORT)).sync();
      // 会一直等待，直到客户端链路关闭
      future.channel().closeFuture().sync();
    } finally {
      System.out.println("当客户端感知断连事件之后，释放资源，重新发起连接......");
      // 当客户端感知断连事件之后，释放资源，重新发起连接
      // 客户端挂在closeFuture上监听链路关闭信号，一旦关闭，则创建重连定时器，5s之后重新发起连接，直到重连成功
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          try {
            TimeUnit.SECONDS.sleep(5);
            // 发起重连操作
            connect(NettyConstant.PORT, NettyConstant.REMOTEIP);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
    }
  }

  public static void main(String[] args) throws Exception {
    new NettyClient().connect(NettyConstant.PORT, NettyConstant.REMOTEIP);
  }
}
