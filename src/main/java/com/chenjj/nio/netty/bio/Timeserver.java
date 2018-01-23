package com.chenjj.nio.netty.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Author: chenjj
 * @Date: 2018-01-23
 * @Description:
 */
public class Timeserver {

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
      while (true) {
        socket = serverSocket.accept();
        /**
         * BIO的主要问题在于每当一个新的客户端请求接入时，服务端必须创建一个新的线程处理新接入的客户端链路，
         * 一个线程只能处理一个客户端连接。这种模型显然不满足高想能、高并发接入的场景。
         */
        new Thread(new TimeServerHandler(socket)).start();
      }
    } finally {
      if (serverSocket != null) {
        System.out.println("The time server close");
        serverSocket.close();
      }
    }
  }
}
