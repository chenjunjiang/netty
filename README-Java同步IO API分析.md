# Java同步I/O API(Read/Write)分析
public int read(byte b[]) throws IOException{
    return read(b, 0, b.length);
}
当对Socket的输入流进行读取操作的时候，它会一直阻塞下去，直到
发生以下三种事件：
1、有数据可读。
2、可用数据已经读取完毕。
3、发生空指针或I/O异常。
这意味着当对方发送请求或者应答消息比较缓慢，或者网络传输较慢时，读取
输入流的一方的通讯线程将被长时间阻塞，如果对方要60s才能够将数据发送
完成，读取一方的I/O线程也将会被同步阻塞60s。

public void write(byte b[]) throws IOException
当调用write方法写入的时候会被阻塞，知道所有要发送的字节全部写入完毕
，或者发送异常。学习过TCP/IP相关知识的都知道，当消息的接收方处理缓慢
的时候，将不能及时地从TCP缓冲区读取数据，这就会导致发送方的TCP
window size不断减少，直到为0，双方处于Keep-Alive状态，消息发送方
将不能再向TCP缓冲区写入消息，这时如果采用的是同步阻塞I/O，write
操作将会被无限期阻塞，直到TCP window size大于0或发送I/O异常。
