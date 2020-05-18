package com.chenjj.io.nio.jdk.many_reactor;

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

    /**
     * 子Reactor个数是当前机器可用核数的两倍（与Netty默认的子Reactor个数一致）。
     * 对于每个成功连接的SocketChannel，通过round robin的方式交给不同的子Reactor。
     * 每个Processor实例均包含一个Selector实例。同时每次获取Processor实例时均提交一个任务到线程池，
     * 并且该任务正常情况下一直循环处理，不会停止。而提交给该Processor的SocketChannel通过在其Selector注册事件，
     * 加入到相应的任务中。由此实现了每个子Reactor包含一个Selector对象，并由一个独立的线程处理。
     */
    @Override
    public void run() {
        int coreNum = Runtime.getRuntime().availableProcessors();
        Processor[] processors = new Processor[coreNum * 2];
        for (int i = 0; i < processors.length; i++) {
            try {
                processors[i] = new Processor();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
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
                int index = 0;
                while (iterator.hasNext()) {
                    selectionKey = iterator.next();
                    iterator.remove();
                    try {
                        handleInput(selectionKey, processors, index, coreNum);
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

    private void handleInput(SelectionKey selectionKey, Processor[] processors, int index, int coreNum) throws IOException {
        if (selectionKey.isValid()) {
            /**
             * 完成TCP三次握手，建立物理链路，新连接建立成功，
             * 后续基于同一个连接的请求到达这里的时候selectionKey.isAcceptable()为false。
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
                System.out.println("Accept request from " + socketChannel.getRemoteAddress());
                Processor processor = processors[((index++) % coreNum)];
                processor.addChannel(socketChannel);
                /**
                 * https://www.zhihu.com/question/389599681
                 *
                 * 当B线程阻塞在select()或select(long)方法上时，A线程调用wakeup后，B线程会立刻返回。
                 * 如果没有线程阻塞在select()方法上，那么下一次某个线程调用select()或select(long)方法时，会立刻返回。
                 * 在多reactor模式下，我们这里调用wakeup的目的是：当建立新的连接后(SocketChannel)，希望子reactor中的selector能及时处理。
                 */
                processor.wakeup();
            }
        }
    }
}
