package com.chenjj.io.nio.jdk.classic_reactor;

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
 * @Description: JDK使用epoll代替了传统的select实现，所以没有最大连接句柄1024/2048的限制。
 * (只受限于操作系统的最大句柄数或者对单个进程的句柄限制)，这就意味着一个selector线程可以同时处理
 * 成千上万个客户端连接，而且性能不会随着客户端的增加而线性下降。非常适合做高性能、高负载的网络服务器。
 * 在Reactor模式中，包含如下角色：
 * Reactor 将I/O事件发派给对应的Handler
 * Acceptor 处理客户端连接请求
 * Handlers 执行非阻塞读/写
 * 为了方便阅读，下面的代码将Reactor模式中的所有角色放在了一个类中。
 */
public class MultiplexerTimeServer implements Runnable {

    private Selector selector;

    private ServerSocketChannel serverSocketChannel;

    private volatile boolean stop;

    /**
     * @class_name: MultiplexerTimeServer
     * @param: [port]
     * @describe: 初始化多路复用器，绑定监听端口
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
        // 多路复用器在无限循环体内轮询准备就绪的key(socket关注的事件)
        while (!stop) {
            try {
                /**
                 * 调用select会使线程阻塞，但是和阻塞I/O所不同的是，它可以同时阻塞多个I/O操作，直到超时或有可用的事件发生。
                 * 该方法返回可用通道个数，同时该方法只捕获Channel注册时指定的事件。
                 */
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                SelectionKey selectionKey;
                while (iterator.hasNext()) {
                    selectionKey = iterator.next();
                    iterator.remove();
                    try {
                        handleInput(selectionKey);
                    } catch (Exception e) {
                        e.printStackTrace();
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
            /**
             * 完成TCP三次握手，建立物理链路，新连接建立成功，后续基于同一个连接的请求到达这里的时候selectionKey.isAcceptable()为false
             * 一个连接对应一个selectionKey
             */
            if (selectionKey.isAcceptable()) {
                System.out.println("建立了新连接......" + selectionKey);
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
                SocketChannel socketChannel = serverSocketChannel.accept();
                // 设置为异步非阻塞
                socketChannel.configureBlocking(false);
                // Add the new connection to the selector，监听SocketChannel上的读操作，读取客户端发送的网络信息
                socketChannel.register(selector, SelectionKey.OP_READ);
            }
            /**
             * 如果客户端强制关闭与服务端的连接,那么selectionKey.isReadable()也为true，然后在下面读取数据的时候
             * 抛出异常：java.io.IOException: 远程主机强迫关闭了一个现有的连接。
             */
            if (selectionKey.isReadable()) {
                System.out.println("接收请求数据......" + selectionKey);
                // Read the data
                SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                // 因为上面已经将SocketChannel设置为异步非阻塞模式，因此read是非阻塞的，所以读取到的字节数有三种情况
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
                    // 对端链路已经关闭，需要关闭SocketChannel，释放资源
                    selectionKey.cancel();
                    socketChannel.close();
                } else {
                    // 读到0字节，忽略，属于正常现象
                }
            }
        }
    }

    private void doWrite(SocketChannel socketChannel, String response) throws IOException {
        if (response != null && response.trim().length() > 0) {
            System.out.println("response: " + response);
            System.out.println();
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            /**
             * TODO(chenjj):
             * 由于SocketChannel被设置成了异步非阻塞，所以它并不能保证一次能够把需要发送的字节数组发送完，
             * 此时会出现"写半包"的问题。我们需要注册写操作，不断轮询Selector将没有发送完的ByteBuffer发送完毕，
             * 然后可以通过ByteBuffer的hasRemain()方法判断消息是否发送完成。
             */
            socketChannel.write(writeBuffer);
        }
    }
}
