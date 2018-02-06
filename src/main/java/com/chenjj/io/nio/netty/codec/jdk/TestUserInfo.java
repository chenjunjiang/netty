package com.chenjj.io.nio.netty.codec.jdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class TestUserInfo {

  public static void main(String[] args) throws IOException {
    UserInfo userInfo = new UserInfo();
    userInfo.buildUserID(100).buildUserName("chenjunjiang");
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objectOutputStream.writeObject(userInfo);
    objectOutputStream.flush();
    objectOutputStream.close();
    byte[] result = byteArrayOutputStream.toByteArray();
    System.out.println("The jdk serializable length is: " + result.length);// 123
    byteArrayOutputStream.close();
    System.out.println("------------------------------------");
    System.out.println("The byte array serializable length is: " + userInfo.codeC().length);// 20
  }
}
