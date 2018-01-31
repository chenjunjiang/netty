package com.chenjj.io.aio;

/**
 * @Author: chenjj
 * @Date: 2018-01-25
 * @Description:
 */
public class TimeClient {

  public static void main(String[] args) {
    int port = 8080;
    if (args != null && args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        // 异常之后采用默认值
      }
    }

    new Thread(new AsyncTimeClientHandler("127.0.0.1", port), "AIO-AsyncTimeClientHandler-001")
        .start();
  }
}
