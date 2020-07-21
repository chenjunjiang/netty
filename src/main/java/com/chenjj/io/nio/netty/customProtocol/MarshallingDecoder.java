package com.chenjj.io.nio.netty.customProtocol;

import com.chenjj.io.nio.netty.codec.marshalling.MarshallingCodeFactory;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.Unmarshaller;

/**
 * @Author: chenjj
 * @Date: 2018-02-12
 * @Description: 消息解码工具类
 * 使用的是Jboss Marshall框架
 */
public class MarshallingDecoder {

    private final Unmarshaller unmarshaller;

    public MarshallingDecoder() throws IOException {
        unmarshaller = MarshallingCodeFactory.buildUnMarshalling();
    }

    protected Object decode(ByteBuf in) throws Exception {
        int objectSize = in.readInt();
        /**
         * 创建in的一个切片
         * Charset utf8 = Charset.forName("UTF-8");
         * ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);//创建一个ByteBuf
         * ByteBuf sliced = buf.slice(0, 14);//创建这个ByteBuf的一个切片
         * System.out.println(sliced.toString(utf8));//输出 Netty in Actio
         */
        ByteBuf byteBuf = in.slice(in.readerIndex(), objectSize);
        ByteInput input = new ChannelBufferByteInput(byteBuf);
        try {
            unmarshaller.start(input);
            Object object = unmarshaller.readObject();
            unmarshaller.finish();
            in.readerIndex(in.readerIndex() + objectSize);

            return object;
        } finally {
            unmarshaller.close();
        }
    }
}
