package com.chenjj.io.nio.netty.ssl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class SecureChatServer {
    private final int port;
    private final String sslMode;

    public SecureChatServer(int port, String sslMode) {
        this.port = port;
        this.sslMode = sslMode;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Please input ssl mode");
            System.exit(-1);
        }
        String sslMode = args[0];
        int port = 8443;
        new SecureChatServer(port, sslMode).run();
    }

    public void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new SecureChatServerInitializer(sslMode));
            serverBootstrap.bind(port).sync().channel().closeFuture().sync();
            System.out.println("---------------------------------");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
