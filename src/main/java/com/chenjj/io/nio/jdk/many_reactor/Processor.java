package com.chenjj.io.nio.jdk.many_reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Processor {
    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors());

    private Selector selector;

    public Processor() throws IOException {
        // 开启新的多路复用器(Selector)
        this.selector = SelectorProvider.provider().openSelector();
        start();
    }

    /**
     * 在selector上注册socketChannel,感兴趣的事件是读
     *
     * @param socketChannel
     * @throws ClosedChannelException
     */
    public void addChannel(SocketChannel socketChannel) throws ClosedChannelException {
        socketChannel.register(this.selector, SelectionKey.OP_READ);
    }

    public void wakeup() {
        this.selector.wakeup();
    }

    private void start() {
        executorService.submit(() -> {
            while (true) {
                if (selector.select(1000) <= 0) {
                    continue;
                }
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
            }
        });
    }

    private void handleInput(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isValid()) {
            if (selectionKey.isReadable()) {
                System.out.println("开始处理请求数据......" + Thread.currentThread().getName());
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
                    System.out.println("The time server receive order: " + body + " " + Thread.currentThread().getName());
                    String currentTime =
                            "QUERY TIME ORDER".equalsIgnoreCase(body) ? System.currentTimeMillis() + ""
                                    : "BAD ORDER";
                    doWrite(socketChannel, currentTime);
                } else if (readBytes < 0) {
                    System.out.println("对端链路已经关闭......" + Thread.currentThread().getName());
                    // 对端链路已经关闭，需要关闭SocketChannel，释放资源
                    selectionKey.cancel();
                    socketChannel.close();
                } else {
                    // 读到0字节，忽略，属于正常现象
                    System.out.println("读到0字节......" + Thread.currentThread().getName());
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
            socketChannel.write(writeBuffer);
        }
    }
}
