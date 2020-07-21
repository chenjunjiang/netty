package com.chenjj.io.nio.netty.customProtocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;
import java.util.Map.Entry;


/**
 * @Author: chenjj
 * @Date: 2018-02-13
 * @Description: 在encode和decode过程中会用到大量关于ByteBuf的API，了解ByteBuf及其API的使用非常重要， 可以参考：
 * https://blog.csdn.net/yjw123456/article/details/77843931
 */
public class NettyMessageEncoder extends MessageToByteEncoder<NettyMessage> {

    MarshallingEncoder marshallingEncoder;

    public NettyMessageEncoder() throws IOException {
        this.marshallingEncoder = new MarshallingEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage msg, ByteBuf out) throws Exception {
        if (msg == null || msg.getHeader() == null) {
            throw new Exception("The encode message is null");
        }
        // 将int值写入到当前writerIndex(0)，然后将writerIndex加4
        out.writeInt((msg.getHeader().getCrcCode()));
        // 将int值写入到当前writerIndex(4)，然后将writerIndex加4，这里的length现在是0，下面的setInt为它设置真正长度
        out.writeInt((msg.getHeader().getLength()));
        // 将long值写入到当前writerIndex，然后将writerIndex加8
        out.writeLong((msg.getHeader().getSessionID()));
        // 将byte值写入到当前writerIndex，然后将writerIndex加1
        out.writeByte((msg.getHeader().getType()));
        out.writeByte((msg.getHeader().getPriority()));
        out.writeInt((msg.getHeader().getAttachment().size()));
        String key = null;
        byte[] keyArray = null;
        Object value = null;
        for (Entry<String, Object> param : msg.getHeader().getAttachment()
                .entrySet()) {
            key = param.getKey();
            keyArray = key.getBytes("UTF-8");
            out.writeInt(keyArray.length);
            out.writeBytes(keyArray);
            value = param.getValue();
            marshallingEncoder.encode(value, out);
        }
        key = null;
        keyArray = null;
        value = null;
        if (msg.getBody() != null) {
            // 把消息体对象序列化到out中
            marshallingEncoder.encode(msg.getBody(), out);
        } else {
            /**
             * 如果消息体为空，就写入int类型0，此时写入字节数是4个字节，NettyMessageDecoder中
             * 反序列化消息体的时候会根据可读字节数判断消息体是否为空
             */
            out.writeInt(0);
        }
        /**
         * index是4，设置的其实就是上面writeInt后length的值，
         * 这里的值为什么要用消息的字节数减8，原因是和LengthFieldBasedFrameDecoder的处理逻辑有关：
         * 我们在NettyMessageDecoder里面设置的lengthFieldOffset和lengthFieldLength都为4，
         * LengthFieldBasedFrameDecoder的decode(ChannelHandlerContext ctx, ByteBuf in)
         * 方法在处理时有个逻辑：frameLength += lengthAdjustment + lengthFieldEndOffset;
         * frameLength就是消息的字节数，lengthFieldEndOffset的值是lengthFieldOffset、lengthFieldLength之和，
         * lengthAdjustment此时为0，所以frameLength的值就变成了消息的字节数加上lengthFieldEndOffset；
         * 源码里面还有一个逻辑：
         * if (in.readableBytes() < frameLengthInt) {
         *             return null;
         * }
         * 所以说，如果这里不减8(lengthFieldOffset、lengthFieldLength之和)，上面这个判断逻辑就有问题
         */
        out.setInt(4, out.readableBytes() - 8);
    }
}
