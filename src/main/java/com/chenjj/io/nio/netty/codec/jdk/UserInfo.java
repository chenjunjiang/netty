package com.chenjj.io.nio.netty.codec.jdk;

import java.io.Serializable;
import java.nio.ByteBuffer;
import javax.swing.plaf.PanelUI;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 3280444735359611957L;

    private String userName;

    private int userID;

    public UserInfo buildUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public UserInfo buildUserID(int userID) {
        this.userID = userID;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    /**
     * 使用基于ByteBuffer的通用二进制编解码技术对UserInfo对象进行编码，编码结果仍然是byte数组， 可以与传统的JDK序列化后的码流大小进行比较
     */
    public byte[] codeC() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byte[] value = this.userName.getBytes();
        byteBuffer.putInt(value.length);
        byteBuffer.put(value);
        byteBuffer.putInt(this.userID);
        byteBuffer.flip();
        value = null;
        byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        return result;
    }

    public byte[] codeC(ByteBuffer byteBuffer) {
        byteBuffer.clear();
        byte[] value = this.userName.getBytes();
        byteBuffer.putInt(value.length);
        byteBuffer.put(value);
        byteBuffer.putInt(this.userID);
        byteBuffer.flip();
        value = null;
        byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        return result;
    }

}
