package com.chenjj.io.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author: chenjj
 * @Date: 2018-01-25
 * @Description:
 */
public class MultiplexerTimeServer implements Runnable {

  private Selector selector;

  private ServerSocketChannel serverSocketChannel;

  private volatile boolean stop;

  /**
   * @class_name: MultiplexerTimeServer
   * @param: [port]
   * @describe: 初始化多路复用器，绑定监听端口
   * @creat_user: wanwt@senthinkcom
   * @creat_date: 2018-01-25
   * @creat_time: 下午 03:10
   **/
  public MultiplexerTimeServer(int port) {
    try {
      selector = Selector.open();
      // 打开ServerSocketChannel，用于监听客户端连接，它是所有客户端连接的父管道
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.configureBlocking(false);
      serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
      // 将ServerSocketChannel注册到多路复用器Selector上，监听ACCEPT事件
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
      System.out.println("The time server is start in port: " + port);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public void stop() {
    this.stop = true;
  }

  @Override
  public void run() {
    // 多路复用器在无限循环体内轮训准备就绪的key
    while (!stop) {
      try {
        selector.select(1000);
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectionKeys.iterator();
        SelectionKey selectionKey = null;
        while (iterator.hasNext()) {
          selectionKey = iterator.next();
          iterator.remove();
          try {
            handleInput(selectionKey);
          } catch (Exception e) {
            if (selectionKey != null) {
              selectionKey.cancel();
              if (selectionKey.channel() != null) {
                selectionKey.channel().close();
              }
            }
          }
        }
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    // 多路复用器关闭之后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源。
    if (selector != null) {
      try {
        selector.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  private void handleInput(SelectionKey selectionKey) throws IOException {
    if (selectionKey.isValid()) {
      // 处理新接入的请求消息，完成TCP三次握手，建立物理链路
      if (selectionKey.isAcceptable()) {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        // Add the new connection to the selector，监听读操作，读取客户端发送的网络信息
        socketChannel.register(selector, SelectionKey.OP_READ);
      }
      if (selectionKey.isReadable()) {
        // Read the data
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int readBytes = socketChannel.read(readBuffer);
        if (readBytes > 0) {
          readBuffer.flip();
          byte[] bytes = new byte[readBuffer.remaining()];
          readBuffer.get(bytes);
          String body = new String(bytes, "UTF-8");
          System.out.println("The time server receive order: " + body);
          String currentTime =
              "QUERY TIME ORDER".equalsIgnoreCase(body) ? System.currentTimeMillis() + ""
                  : "BAD ORDER";
          doWrite(socketChannel, currentTime);
        } else if (readBytes < 0) {
          // 对端链路关闭
          selectionKey.cancel();
          socketChannel.close();
        } else {
          // 读到0字节，忽略
        }
      }
    }
  }

  private void doWrite(SocketChannel socketChannel, String response) throws IOException {
    if (response != null && response.trim().length() > 0) {
      System.out.println("response: " + response);
      byte[] bytes = response.getBytes();
      ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
      writeBuffer.put(bytes);
      writeBuffer.flip();
      socketChannel.write(writeBuffer);
    }
  }
}
