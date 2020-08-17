package com.chenjj.nio.netty;

/**
 * @Author: chenjj
 * @Date: 2018-01-31
 * @Description:
 */
public class Test {

  public static void main(String[] args) {
    System.out.println(System.getProperty("line.separator").length());
    System.out.println("\n");
    // 空白字符\u0000，打印出来就是方框
    System.out.println("\u0000"+"\u0000"+"\u0000"+"\u0000"+"\u0000");
  }
}
