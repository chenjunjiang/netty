# netty
knowledge point of netty

典型的网络事件如下：
1、链路注册；
2、链路激活；
3、链路断开；
4、接收到请求消息；
5、请求消息接收并处理完毕；
6、发送应答消息；
7、链路发生异常；
8、发生用户自定义事件；

Netty提供了大量的系统ChannelHandler供用户使用，比较实用的如下：
1、系统编解码框架-ByteToMessageCodec；
2、通用基于长度的半包解码器-LengthFieldBasedFrameDecoder；
3、流码日志打印handler-LoggingHandler；
4、SSL安全认证Handler-SslHandler；
5、链路空闲检测Handler-IdleStateHandler；
6、流量整形Handler-ChannelTrafficShapingHandler；
7、Base64编解码-Base64Decoder和Base64Encoder；

无论是NIO还是BIO，创建客户端套接字的时候通常会设置连接参数，例如接收和发送缓冲区大小、连接超时时间等。
Netty提供的主要TCP参数如下：
1、SO_TIMEOUT：控制读取操作将阻塞多少毫秒。如果返回值为0，计时器就将被禁止了，该线程将无限期阻塞；
2、SO_SNDBUF：套接字使用的发送缓冲区大小；
3、SO_RCVBUF：套接字使用的接收缓冲区大小；
4、SO_REUSEADDR：用于决定如果网络上仍然有数据向旧的ServerSocket传输数据，是否允许新的ServerSocket绑定到与旧的
ServerSocket同样的端口上。SO_REUSEADDR选项的默认值与操作系统有关，有些操作系统中，允许重用端口，有些则不允许。
5、CONNECT_TIMEOUT_MILLIS：客户端连接超时时间，由于NIO原生的客户端并不提供设置连接超时的接口，因此，Netty采用的
是自定义连接超时定时器负责检测和超时控制；
6、TCP_NODELAY：激活或禁止TCP_NODELAY套接字选项，它决定是否使用Nagle算法
（Nagle算法主要是避免发送小的数据包，要求TCP连接上最多只能有一个未被确认的小分组，在该分组的确认到达之前不能发送其他的小分组。相反，TCP收集这些少量的小分组，并在确认到来时以一个分组的方式发出去。）。
如果是时延（时延是指一个报文或分组从一个网络的一端传送到另一个端所需要的时间）敏感型的应用，建议关闭；



