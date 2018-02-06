package com.chenjj.io.nio.netty.codec.msgpack;

import org.msgpack.annotation.Message;

/**
 * 要传输的javabean一定要加上注解@Message
 *
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
@Message
public class Student {

  private int id;
  private String name;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /*@Override
  public String toString() {
    return "[" + id +
        ", '" + name + '\'' + ']';
  }*/

  @Override
  public String toString() {
    return "Student{" +
        "id=" + id +
        ", name='" + name + '\'' +
        '}';
  }
}
