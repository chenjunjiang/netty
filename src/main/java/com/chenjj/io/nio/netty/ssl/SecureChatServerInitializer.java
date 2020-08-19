package com.chenjj.io.nio.netty.ssl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;

import javax.net.ssl.SSLEngine;

public class SecureChatServerInitializer extends ChannelInitializer<SocketChannel> {
    private final String tlsMode;

    public SecureChatServerInitializer(String tlsMode) {
        this.tlsMode = tlsMode;
    }

    /**
     * Add SSL handler first to encrypt and decrypt everything.
     * In this example, we use a bogus certificate in the server side and accept any invalid certificates in the client side.
     * You will need something more complicated to identify both and server in the real world.
     *
     * @param ch
     * @throws Exception
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        SSLEngine sslEngine = null;
        // 单向认证
        if (SSLMODE.CA.toString().equals(tlsMode)) {
            sslEngine = SecureChatSslContextFactory.getServerContext(tlsMode, System.getProperty("user.dir")
                    + "/src/main/java/com/chenjj/io/nio/netty/ssl/oneway/sChat.jks", null).createSSLEngine();
        } else if (SSLMODE.CSA.toString().equals(tlsMode)) { // 双向认证
            sslEngine = SecureChatSslContextFactory
                    .getServerContext(
                            tlsMode,
                            System.getProperty("user.dir")
                                    + "/src/main/java/com/chenjj/io/nio/netty/ssl/twoway/sChat.jks",
                            System.getProperty("user.dir")
                                    + "/src/main/java/com/chenjj/io/nio/netty/ssl/twoway/sChat.jks")
                    .createSSLEngine();
        } else {
            System.err.println("ERROR : " + tlsMode);
            System.exit(-1);
        }
        // 设置sslEngine为服务端模式
        sslEngine.setUseClientMode(false);
        // 双向认证是需要对客户端进行认证的，所有下面设置为true
        if (SSLMODE.CSA.toString().equals(tlsMode)) {
            sslEngine.setNeedClientAuth(true);
        }
        pipeline.addLast("ssl", new SslHandler(sslEngine));
        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
        pipeline.addLast("handler", new SecureChatServerHandler());
    }
}
