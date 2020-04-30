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
      Socket socket;
      TimeServerHandlerExecutePool timeServerHandlerExecutePool = new TimeServerHandlerExecutePool(
          50, 1000);// 创建线程池
      while (true) {
        socket = serverSocket.accept();
        /**
         * 伪异步IO(采用线程池)无法从根本上解决同步IO导致的通讯线程阻塞的问题。
         * 我们简单分析下通讯对方返回应答时间过长会引起的级联故障。
         * 1、服务端处理缓慢，返回应答消息耗费60s，平时只要10ms。
         * 2、采用伪异步IO的线程正在读取故障服务节点的响应，由于读取输入流是阻塞的，它将会被同步阻塞60s。
         * 3、加入所有的可用线程都被故障服务器阻塞，那么后续所有的IO消息都将在队列中排队。
         * 4、由于线程池采用阻塞队列实现(ArrayBlockingQueue)，当队列积满并且无空闲线程之后，后续入队的操作将被阻塞。
         * 5、由于入队操作的阻塞，最终会导致执行入队操作的线程阻塞，就是接收客户端接入请求的线程(上面的accept())，
         * 这样以后新的客户端请求消息将被拒绝，客户端会发生大量的连接超时。
         */
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
