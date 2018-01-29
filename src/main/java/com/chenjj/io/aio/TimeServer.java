package com.chenjj.io.aio;

/**
 * @Author: chenjj
 * @Date: 2018-01-29
 * @Description:
 */
public class TimeServer {

  public static void main(String[] args) {
    int port = 8080;
    if (args != null && args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        // 采用默认值
      }
    }
    AsyncTimeServerHandler asyncTimeServerHandler = new AsyncTimeServerHandler(port);
    new Thread(asyncTimeServerHandler, "AIO-AsyncTimeServerHandler-001").start();
  }
}
