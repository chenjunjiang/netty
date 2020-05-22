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

    /**
     * The jdk serializable length is: 127
     * ------------------------------------
     * The byte array serializable length is: 24
     * 从结果可以看出， JDK序列化机制编码后的二进制数组大小远大于二进制编码。
     * 在同等情况下，编码后的字节数组越大，存储的时候就越占空间，并且在网络传输的时候就更占带宽，导致
     * 系统的吞吐量降低。
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        UserInfo userInfo = new UserInfo();
        userInfo.buildUserID(100).buildUserName("Welcome to Netty");
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
