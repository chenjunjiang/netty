package com.chenjj.io.nio.jdk.thread_reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private volatile boolean stop;

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
                SelectionKey readKey = socketChannel.register(selector, SelectionKey.OP_READ);
                System.out.println("readKey: " + readKey);
                /**
                 * 注册完SocketChannel的OP_READ事件后，可以对相应的SelectionKey attach一个对象（本例中attach了一个Processor对象，
                 * 该对象处理读请求），并且在获取到可读事件后，可以取出该对象。
                 * 注：attach对象及取出该对象是NIO提供的一种操作，但该操作并非Reactor模式的必要操作，本文使用它，只是为了方便演示NIO的接口。
                 */
                readKey.attach(new Processor());
            } else if (selectionKey.isReadable()) {
                Processor processor = (Processor) selectionKey.attachment();
                processor.process(selectionKey);
            }
        }
    }
}
