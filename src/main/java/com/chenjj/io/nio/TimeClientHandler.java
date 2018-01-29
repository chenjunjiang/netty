package com.chenjj.io.nio;

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
public class TimeClientHandler implements Runnable {

  private String host;
  private int port;
  private Selector selector;
  private SocketChannel socketChannel;
  private volatile boolean stop;

  public TimeClientHandler(String host, int port) {
    this.host = host == null ? "127.0.0.1" : host;
    this.port = port;
    try {
      selector = Selector.open();
      socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  @Override
  public void run() {
    try {
      doConnect();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
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
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    if (selector != null) {
      try {
        socketChannel.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void handleInput(SelectionKey selectionKey) throws IOException {
    if (selectionKey.isValid()) {
      // 判断是否连接成功
      SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
      if (selectionKey.isConnectable()) {
        if (socketChannel.finishConnect()) {
          socketChannel.register(selector, SelectionKey.OP_READ);
          doWrite(socketChannel);
        } else {
          System.exit(1);
        }
      }
      if (selectionKey.isReadable()) {
        System.out.println("可以读取数据了.......");
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int readBytes = socketChannel.read(readBuffer);
        if (readBytes > 0) {
          readBuffer.flip();
          byte[] bytes = new byte[readBuffer.remaining()];
          readBuffer.get(bytes);
          String body = new String(bytes, "UTF-8");
          System.out.println("Now is: " + body);
          this.stop = true;
        } else if (readBytes < 0) {
          selectionKey.cancel();
          socketChannel.close();
        } else {

        }
      }
    }
  }

  private void doConnect() throws IOException {
    // 如果直接连接成功，则注册到多路复用器上，发送请求消息，读应答
    if (socketChannel.connect(new InetSocketAddress(host, port))) {
      socketChannel.register(selector, SelectionKey.OP_READ);
      doWrite(socketChannel);
    } else {
      socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }
  }

  private void doWrite(SocketChannel socketChannel) throws IOException {
    byte[] request = "QUERY TIME ORDER".getBytes();
    ByteBuffer writeBuffer = ByteBuffer.allocate(request.length);
    writeBuffer.put(request);
    writeBuffer.flip();
    socketChannel.write(writeBuffer);
    if (!writeBuffer.hasRemaining()) {
      System.out.println("Send order 2 server succeed.");
    }
  }
}
