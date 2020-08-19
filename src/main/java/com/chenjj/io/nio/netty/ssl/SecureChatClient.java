package com.chenjj.io.nio.netty.ssl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SecureChatClient {
    private final String host;
    private final int port;
    private final String sslMode;

    public SecureChatClient(String host, int port, String sslMode) {
        this.host = host;
        this.port = port;
        this.sslMode = sslMode;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: "
                    + SecureChatClient.class.getSimpleName() + " <sslmode>");
            return;
        }
        String sslMode = args[0];
        new SecureChatClient("localhost", 8443, sslMode).run();
    }

    public void run() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new SecureChatClientInitializer(sslMode));
            Channel channel = bootstrap.connect(host, port).sync().channel();
            ChannelFuture lastWriteFuture = null;
            // Read commands from the stdin.
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            for (; ; ) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                // Sends the received line to the server
                lastWriteFuture = channel.writeAndFlush(line + "\r\n");
                // If user typed the 'bye' command, wait until the server closes the connection.
                if ("bye".equals(line.toLowerCase())) {
                    channel.closeFuture().sync();
                    channel.close();
                    break;
                }
            }
            // Wait until all messages are flushed before closing the channel.
            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
        } finally {
            group.shutdownGracefully();
        }
    }
}
