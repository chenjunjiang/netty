package com.chenjj.io.nio.netty.customProtocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: chenjj
 * @Date: 2018-02-12
 * @Description: 消息解码器
 * LengthFieldBasedFrameDecoder支持自动的TCP粘包和拆包处理，只需要给出标识消息长度的字段偏移量和消息长度自身所占的
 * 字节数，Netty就能自动实现对半包的处理
 */
public class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {

    MarshallingDecoder marshallingDecoder;

    /**
     * 这里设置的lengthFieldOffset和lengthFieldLength参数与NettyMessageEncoder
     * 里面写入ByteBuf的值有关，因为写入的第一个字段是int，第二个int字段才表示的是消息长度，所以这里设置的
     * lengthFieldOffset和lengthFieldLength分别为4
     *
     * @param maxFrameLength
     * @param lengthFieldOffset
     * @param lengthFieldLength
     * @throws IOException
     */
    public NettyMessageDecoder(int maxFrameLength, int lengthFieldOffset,
                               int lengthFieldLength) throws IOException {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
        marshallingDecoder = new MarshallingDecoder();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in)
            throws Exception {
        /**
         * 调用父类的decode方法之后，返回的就是整包消息或者为空，如果为空则说明是个半包消息，直接返回，
         * 继续由IO线程读取后续的码流
         */
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setCrcCode(frame.readInt());
        header.setLength(frame.readInt());
        header.setSessionID(frame.readLong());
        header.setType(frame.readByte());
        header.setPriority(frame.readByte());

        int size = frame.readInt();
        if (size > 0) {
            Map<String, Object> attch = new HashMap<String, Object>(size);
            int keySize = 0;
            byte[] keyArray = null;
            String key = null;
            for (int i = 0; i < size; i++) {
                keySize = frame.readInt();
                keyArray = new byte[keySize];
                frame.readBytes(keyArray);
                key = new String(keyArray, "UTF-8");
                attch.put(key, marshallingDecoder.decode(frame));
            }
            keyArray = null;
            key = null;
            header.setAttachment(attch);
        }
        /**
         * 这里是否需要反序列化body是根据NettyMessageEncoder中约定的逻辑：
         * 剩余可读字节数大于4个字节，说明body是被序列化了的
         */
        if (frame.readableBytes() > 4) {
            message.setBody(marshallingDecoder.decode(frame));
        }
        message.setHeader(header);
        return message;
    }
}
