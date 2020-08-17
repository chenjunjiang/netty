package com.chenjj.io.nio.netty.trafficshaping;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import java.nio.charset.Charset;

public class MyServerInitializer extends ChannelInitializer<SocketChannel> {
    final static int M = 1024 * 1024;
    Charset utf8 = Charset.forName("utf-8");

    /**
     * LengthFieldPrepender和LengthFieldBasedFrameDecoder配对出现进行编码和解码。
     * <p>
     * trafficShaping是通过程序来达到控制流量的作用，并不是网络层真实的传输流量大小的控制。
     * TrafficShapingHandler仅仅是根据消息大小（待发送出去的数据包大小）和设定的流量限制来得出延迟发送该包的时间，
     * 即同一时刻不会发送过大的数据导致带宽负荷不了。但是并没有对大数据包进行拆分的作用，
     * 这会使在发送这个大数据包时同样可能会导致带宽爆掉的情况。所以你需要注意一次发送数据包的大小，
     * 不要大于你设置限定的写带宽大小(writeLimit)。你可以通过在业务handler中自己控制的方式，
     * 或者考虑使用ChunkedWriteHandler，如果它能满足你的要求的话(所以我们编写了自己的MyServerChunkHandler，把一次发送数据包的大小设置为8192字节)。
     * 同时注意，不要将writeLimit和readLimit设置的过小，这是没有意义的，只会导致读/写操作的不断停顿。
     * <p>
     * ChunkedWriteHandler支持异步写大数据流既不会消耗大量内存又不会引起内存溢出。
     * ChunkedWriteHandler的使用方式是： 首先创建一个ChunkedWriteHandler的实例添加到Pipeline，
     * 在它前面一般还会有一个Handler，里面会写ChunkedInput，比如：MyServerChunkHandler里面就是写的
     * ChunkedStream，它是ChunkedInput的实现；然后ChunkedWriteHandler就可以从ChunkedInput里面把数据一块一块的取出来
     * 然后写到队列里面等待发送，每块的大小由ChunkedInput指定的。
     * 所以如果是写文件，也可以用ChunkedWriteHandler(https://www.w3cschool.cn/essential_netty_in_action/essential_netty_in_action-f6pe28c6.html)。
     *
     * @param ch
     * @throws Exception
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        /**
         * 写每秒1M，读每秒10M
         */
        GlobalTrafficShapingHandler globalTrafficShapingHandler = new GlobalTrafficShapingHandler(ch.eventLoop().parent()
                , 1 * M, 10 * M);
        ch.pipeline()
                .addLast("LengthFieldBasedFrameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4, true))
                .addLast("LengthFieldPrepender", new LengthFieldPrepender(4, 0))
                .addLast("GlobalTrafficShapingHandler", globalTrafficShapingHandler)
                .addLast("chunkedWriteHandler", new ChunkedWriteHandler())
                .addLast("myServerChunkHandler", new MyServerChunkHandler())
                .addLast("StringDecoder", new StringDecoder(utf8))
                .addLast("StringEncoder", new StringEncoder(utf8))
                .addLast("myServerHandler", new MyServerHandlerForPlain());

    }
}
