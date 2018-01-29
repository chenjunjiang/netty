package com.chenjj.io.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Author: chenjj
 * @Date: 2018-01-24
 * @Description: 用线程池实现伪异步, 但是它底层的通讯依然采用同步阻塞模型
 */
public class FakeTimeServer {

  public static void main(String[] args) throws IOException {
    int port = 8080;
    if (args != null && args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        // 异常之后采用默认值
      }
    }
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(port);
      System.out.println("The time server is start in port: " + port);
      Socket socket = null;
      TimeServerHandlerExecutePool timeServerHandlerExecutePool = new TimeServerHandlerExecutePool(
          50, 1000);// 创建线程池
      while (true) {
        socket = serverSocket.accept();
        timeServerHandlerExecutePool.execute(new TimeServerHandler(socket));
      }
    } finally {
      if (serverSocket != null) {
        System.out.println("The time server close");
        serverSocket.close();
      }
    }
  }
}
