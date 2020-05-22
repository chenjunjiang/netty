package com.chenjj.io.nio.netty.codec.messagePack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.util.List;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class MsgpackDecoder extends MessageToMessageDecoder<ByteBuf> {

    /*@Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        final byte[] byteArray;
        final int length = msg.readableBytes();
        byteArray = new byte[length];
        // 读取ByteBuf中的数据到byteArray中
        msg.getBytes(msg.readerIndex(), byteArray, 0, length);
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(byteArray);
        int age = unpacker.unpackInt();
        String name = unpacker.unpackString();
        String address = unpacker.unpackString();
        Student student = new Student();
        student.setAge(age);
        student.setName(name);
        student.setAddress(address);
        out.add(student);
    }*/

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        final byte[] byteArray;
        final int length = msg.readableBytes();
        byteArray = new byte[length];
        // 读取ByteBuf中的数据到byteArray中
        msg.getBytes(msg.readerIndex(), byteArray, 0, length);
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(byteArray);
        int size = unpacker.unpackArrayHeader();
        Student[] students = new Student[size];
        for (int i = 0; i < size; i++) {
            int age = unpacker.unpackInt();
            String name = unpacker.unpackString();
            String address = unpacker.unpackString();
            Student student = new Student();
            student.setAge(age);
            student.setName(name);
            student.setAddress(address);
            students[i] = student;
            // out.add(student);
        }
        // 如果希望一次性接收到数据，这里需要把数据一次性放到out中，如果是像上面注释那样单独放入，就会出现多次接收数据的现象
        out.add(students);
    }
}
