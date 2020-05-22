package com.chenjj.io.nio.netty.codec.messagePack;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.RawValue;

import java.util.List;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Student[] students = (Student[]) msg;
        for (Student student : students) {
            System.out.println("age: " + student.getAge() + ", name: " + student.getName() + ", address: " + student.getAddress());
        }
        System.out.println("Server receive the msgpack message : " + msg);
        Thread.sleep(1000);
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
