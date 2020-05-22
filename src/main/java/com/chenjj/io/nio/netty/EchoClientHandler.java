package com.chenjj.io.nio.netty;

import com.chenjj.io.nio.netty.codec.messagePack.Student;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class EchoClientHandler extends ChannelInboundHandlerAdapter {

  private final int sendNumber;

  public EchoClientHandler(int sendNumber) {
    this.sendNumber = sendNumber;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Student[] students = getStudents();
    for (Student student : students) {
      ctx.write(student);
    }
    ctx.flush();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    System.out.println("Client receive the msgpack message : " + msg);
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

  private Student[] getStudents() {
    Student[] students = new Student[sendNumber];
    Student student = null;
    for (int i = 0; i < sendNumber; i++) {
      student = new Student();
      student.setAge(i);
      student.setName("ABCDEFG --->" + i);
      students[i] = student;
    }

    return students;
  }

}
