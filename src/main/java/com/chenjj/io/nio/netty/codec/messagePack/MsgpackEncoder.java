package com.chenjj.io.nio.netty.codec.messagePack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class MsgpackEncoder extends MessageToByteEncoder<Student[]> {

    /**
     * @param ctx
     * @param students 这个参数的类型和要发送的数据类型保持一致
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Student[] students, ByteBuf out) throws Exception {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packArrayHeader(students.length);
        for (Student student : students) {
            packer.packInt(student.getAge());
            packer.packString(student.getName());
            packer.packString(student.getAddress());
        }
        packer.close();
        // 写入到ByteBuf
        out.writeBytes(packer.toByteArray());
    }
}
