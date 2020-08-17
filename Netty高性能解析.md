# Netty高性能解析

1、RPC调用性能模型分析
   (1) 传统RPC调用性能差的三宗罪
   "罪行一"：网络传输方式问题。传统的RPC框架或者基于RMI等方式的远程服务(过程)调用采用了同步阻塞IO，当客户端的并发压力或者
网络延迟增大之后，同步阻塞IO会由于频繁的wait导致IO线程经常性的阻塞，由于线程无法高效的工作，IO处理能力自然下降。
   采用BIO通信模型的服务端，通常有一个Acceptor线程负责监听客户端的连接，接收到客户端连接之后，为其创建一个新的线程处理消息，
处理完成之后，返回应答消息给客户端，线程销毁，这就是典型的一请求一应答模型。该架构最大的问题就是不具备弹性伸缩能力，
当并发访问量增加后，服务端的线程个数和并发访问数成线性正比，由于线程是Java虚拟机非常宝贵的系统资源，当线程数膨胀之后，
系统的性能急剧下降，随着并发量的继续增加，可能会发生句柄溢出、线程堆栈溢出等问题，并导致服务器最终宕机。
   "罪行二"：序列化性能差。Java序列化存在如下几个问题：
   a、Java序列化机制是Java内部的一种对象编解码技术，无法跨语言使用。对于异构系统之间的对接，Java序列化后的码流
需要能够通过其他语言反序列化长原始对象(副本)，目前很难支持。
   b、相比于其他开源的序列化框架，Java序列化后的码流太大，无论网络传输还是持久化到磁盘，都会导致额外的资源占用。
   c、序列化性能差，资源占用高(主要是CPU资源占用高)。
   "罪行三"：线程模型问题。由于采用同步阻塞IO，这会导致每个TCP连接都占用1个线程，由于线程资源是JVM虚拟机非常宝贵
的资源，当IO读写阻塞线程无法及时释放时，会导致系统性能急剧下降，严重的甚至会导致虚拟机无法创建新的线程。
   (2) IO通信性能三原则
   尽管影响IO通信性能的因素非常多，但是从架构层面看主要有三个要素。
   a、传输：用什么样的通道将数据发送给对方。可以选择BIO、NIO或者AIO，IO模型在很大程度上决定了通信的性能；
   b、协议：采用什么样的通信协议，HTTP等公有协议或者内部私有协议。协议的选择不同，性能也不同。相比公有协议，内部私有协议的性能
通常可以被设计得更优；
   c、线程：数据报如何读取？读取之后的编解码在哪个线程进行，编解码后的消息如何派发，Reactor线程模型的不同，对性能影响也非常大。

2、Netty高性能之道
   (1) 异步非阻塞通信
   在IO编程过程中，当需要同时处理多个客户端接入请求时，可以利用多线程或者IO多路复用技术进行处理。IO多路复用技术
通过把多个IO额阻塞复用到同一个select的阻塞上，从而使得系统在单线程的情况下可以同时处理多个客户端请求。与传统的
多线程/多进程模型比，IO多路复用的最大优势是系统开销小，系统不需要创建新的额外进程或者线程，也不需要维护这些进程和
线程的运行，降低了系统的维护工作量，节省了系统资源。
   JDK1.4提供了对非阻塞IO(NIO)的支持，JDK1.5_update10版本使用epoll替代了传统的select/poll，极大提升了NIO通信性能。
   与Socket和ServletSocket类相对应，NIO也提供了SocketChannel和ServerSocketChannel两种不同的套接字通道实现。
这两种新增的通道都支持阻塞和非阻塞两种模式。阻塞模式使用非常简单，但是性能和可靠性都不好，非阻塞版本则正好相反。
开发人员一般可以根据自己的需求来选择合适的模式，一般来说，低负载，低并发的应用程序可以用选择同步阻塞IO以降低编程
复杂度。但是对于高负载、高并发的应用网络应用，需要使用NIO的非阻塞模式进行开发。
   Netty的IO线程NioEventLoop由于聚合了多路复用器Selector， 可以同时并发处理成百上千个客户端SocketChannel。
由于读写操作都是非阻塞的，这就可以充分提升IO线程的运行效率，避免由频繁的IO阻塞导致的线程挂起。另外，由于Netty
采用了异步通信模式，一个IO线程可以并发地处理N个客户端连接和读写操作，这从根本上解决了传统同步阻塞IO一连接一线程
模型，架构的性能、弹性伸缩能力和可靠性得到了极大的提升。

   (2) 高效的Reactor线程模型
   常用的Reactor线程模型有三种，分别如下：
   a、Reactor单线程模型；
   b、Reactor多线程模型；
   c、主从Reactor多线程模型；
   Reactor单线程模型，指的是所有的IO操作都在同一个NIO线程上面完成，NIO线程的职责如下：
   a、作为NIO服务端，接收客户端的TCP连接；
   b、作为NIO客户端，向服务端发起TCP连接；
   c、读取通信对端的请求或者应答消息；
   d、向通信对端发送消息请求或者应答消息。
   由于Reactor模式使用的是异步非阻塞IO，所有的IO操作都不会导致阻塞，理论上一个线程可以独立处理所有IO相关的操作。从架构层面
看，一个NIO线程确实可以完成其承担的职责。例如，通过Acceptor接收客户端的TCP连接请求消息，链路建立成功之后，通过Dispatch将
对应的ByteBuffer派发到指定的Handler上进行消息解码，用户Handler可以通过NIO线程将消息发送给客户端。
   对于一些小容量应用场景，可以使用单线程模型，但是对于高负载、大并发的应用却不合适，主要原因如下：
   a、一个NIO线程同时处理成百上千的链路，性能上无法支撑。即便NIO线程的CPU负荷达到100%。也无法满足海量消息的编码，解码、读取和发送；
   b、当NIO线程负载过重之后，处理速度将变慢，这会导致大量客户端连接超时，超时之后往往会进行重发，这更加重了NIO线程的负载，
最终导致大量消息积压和处理超时，NIO线程会成为系统的性能瓶颈；
   c、可靠性问题。一旦NIO线程意外跑飞，或者进入死循环，会导致整个系统通信模块不可用，不能接收和处理外部消息，造成节点故障。
   为了解决这些问题，演进出了Reactor多线程模型，Reactor多线程模型与单线程模型最大的区别就是有一组NIO线程处理IO操作，它的
特点如下：
   a、有一个专门的NIO线程-Acceptor线程用于监听服务端，接收客户端的TCP连接请求；
   b、网络IO操作，-读、写等由一个NIO线程池负责，线程池可以采用标准的JDK线程池实现，它包含了一个任务队列和N个可用的线程，由
这些NIO线程负责消息的读取、解码和发送；
   c、1个NIO线程可以同时处理N调链路，但是1个链路只对应1个NIO线程，防止发生并发操作问题。
   在绝大多数场景下，Reactor多线程模型可以满足性能需求；但是，在极特殊应用场景中，一个NIO线程负责监听和处理所有的客户端连接
可能会存在性能问题。例如百万客户端并发连接，或者服务端需要对客户端的握手消息进行安全认证，认证本身非常耗损性能。在这类场景下，
单独一个Acceptor线程可能会存在性能不足的问题。为了解决性能问题，产生了第三种Reactor线程模型-主从Reactor多线程模型。
    主从Reactor线程模型的特点是：服务端用于接收客户端连接的不再是1个单独的NIO线程，而是一个独立的NIO线程池。Acceptor接收到
客户端TCP连接请求处理完成后(可能包含接入认证等)，将新创建的SocketChannel注册到IO线程池(sub reactor线程池)的某个IO线程上，
由它负责SocketChannel的读写和编码工作。Acceptor线程池只用于客户端登录、握手和安全认证，一旦链路建立成功，就将链路注册到后端
sub reactor线程池的IO线程上，由IO线程负责后续的IO操作。
    利用主从NIO线程模型，可以解决1个服务端监听线程无法有效处理所有客户端连接的性能不足问题。因此，在Netty的官方Demo中，推荐使用该线程模型。
    事实上，Netty的线程模型并非固定不变，通过在启动辅助类中创建不同的EventLoopGroup实例并进行适当的参数配置，就可以支持上述
三种Reactor线程模型。
    Netty单线程模型服务端实例代码如下：
    EventLoopGroup group = new NioEventLoopGroup(1); // 1个线程
        try {
          ServerBootstrap serverBootstrap = new ServerBootstrap();
          serverBootstrap.group(group, group).channel(NioServerSocketChannel.class)。。。。。。
    Netty多线程模型服务端实例代码如下：
    EventLoopGroup acceptorGroup = new NioEventLoopGroup(1); // 1个线程
    EventLoopGroup ioGroup = new NioEventLoopGroup();
                try {
                  ServerBootstrap serverBootstrap = new ServerBootstrap();
                  serverBootstrap.group(acceptorGroup, ioGroup).channel(NioServerSocketChannel.class)。。。。。。
    Netty主从模型服务端实例代码如下：
    EventLoopGroup acceptorGroup = new NioEventLoopGroup();
    EventLoopGroup ioGroup = new NioEventLoopGroup();
            try {
              ServerBootstrap serverBootstrap = new ServerBootstrap();
              serverBootstrap.group(acceptorGroup, ioGroup).channel(NioServerSocketChannel.class)。。。。。。
          用户需要在理解Netty线程模型的基础上，根据业务的实际需求选择合适的线程模型和参数。
          
   (3) 无锁化的串行设计
   在大多数场景下，并发多线程处理可以提升系统的并发性能。但是，如果对于共享资源的并发处理不当，会带来严重的锁竞争，这最终会
导致性能的下降。为了尽可能地避免锁竞争带来的性能损耗，可以通过串行化设计，即消息的处理尽可能在同一线程内完成，期间不进行线程
切换，这样就避免了多线程竞争和同步锁。
   为了尽可能提升性能，Netty采用了串行无锁化设计，在IO线程内部进行串行操作，避免多线程竞争导致的性能下降。表面上看，串行化
设计似乎CPU利用率不高，并发程度不够。但是，通过调整NIO线程池的线程参数，可以同时启动多个串行化的线程并行运行，这种局部无锁
化的串行线程设计相比一个队列-对个工作线程模型性能更优。Netty串行化设计的工作流程如下：
NioEventLoop->(read/write)->decode Handler->log Handler->encode Handler->...... Handler
   Netty的NioEventLoop读取到消息之后，直接调用ChannelPipeline的fireChannelRead(Object msg)，只要用户不主动切换线程，
一直会由NioEventLoop调用用户的Handler，期间不进行线程切换，这种串行化处理方式避免了多线程操作导致的锁竞争，从性能角度看是最优的。
    
   (4) 高效的并发编程
   Netty的高效并发编程主要体现在以下几点：
   a、volatile的大量、正确使用；
   b、CAS和原子类的广泛使用；
   c、线程安全容器的使用；
   d、通过读写锁提升并发性能。
   
   (5) 高性能的序列化框架
   影响序列化性能的关键因素总结如下：
   a、序列化后的码流大小(网络带宽的占用)；
   b、序列化&反序列化的性能(CPU资源占用)；
   c、是否支持跨语言(异构系统的对接和开发语言切换)。
   Netty默认提供了对Google ProtoBuf的支持，通过扩展Netty的编解码接口，用户可以实现其它高性能序列化框架。
   
   (6) 零拷贝
   Netty的"零拷贝"主要体现在如下几个方面：
   a、Netty的接收和发送ByteBuffer采用DIRECT BUFFERS，使用堆外直接内存进行Socket读写，不需要进行字节缓冲区的二次拷贝。
如果使用传统的堆内存(HEAP BUFFERS)进行Socket读写，JVM会将堆内存Buffer拷贝一份到直接内存中，然后才写入Socket中。相比于
堆外直接内存，消息在发送过程中多了一次缓冲区的内存拷贝。
   b、第二种零拷贝的实现CompositeByteBuf，它对外将多个ByteBuf封装成一个ByteBuf，对外提供统一封装后的ByteBuf接口，
多个ByteBuf组合成一个集合，添加ByteBuf，不需要做内存拷贝。
   c、第三种"零拷贝"就是文件传输，Netty文件传输类DefaultFileRegion通过transferTo方法将文件发送到目标Channel中，
重点看FileChannel的transferTo方法。很多操作系统直接将文件缓冲区的内容发送到目标Channel中，而不需要通过循环拷贝的方式，这是一种更加高效的传输方式，提升了
传输性能，降低了CPU和内存占用，实现了文件传输的"零拷贝"。

   (7) 内存池
   随着JVM虚拟机和JIT即时编译技术的发展，对象的分配和回收是个非常轻量级的工作。但是对于缓冲区Buffer，情况却稍有不同，
特别是对于堆外直接内存的分配和回收，是一件非常耗时的操作。为了尽量重用缓冲区，Netty提供了基于内存池的缓冲区重用机制。
   Netty提供了多种内存管理策略，通过在启动辅助类中配置相关参数，可以实现差异化的定制。
   下面通过性能测试，看下基于内存池循环利用的ByteBuf和普通ByteBuf的性能差异：
   a、使用内存池分配器创建直接内存缓冲区：
   int loop = 3000000;
   long startTime  = System.currentTimeMillions();
   ByteBuf poolBuffer = null;
   for( int i=0;i<loop;i++){
       poolBuffer = PooledByteBufAllocator.DEFAULT.directBuffer(1024);
       poolBuffer.writeBytes(CONTENT);
       poolBuffer.release();
   }
   b、使用非内存池分配器创建直接内存缓冲区：
   long startTime2  = System.currentTimeMillions();
   ByteBuf buffer = null;
      for( int i=0;i<loop;i++){
          buffer = Unpooled.directBuffer(1024);
          poolBuffer.writeBytes(CONTENT);
      }
   各执行300万次，性能对比结果如下：
   内存池分配器创建的直接内存缓冲区花费： 4125ms
   非内存池分配器创建的直接内存缓冲区花费： 95312ms
   分析一下内存池分配器创建的直接内存缓冲区的源码，主要是查看PooledDirectByteBuf.newInstance,
   static PooledDirectByteBuf newInstance(int maxCapacity) {
           PooledDirectByteBuf buf = RECYCLER.get();
           buf.reuse(maxCapacity);
           return buf;
       }
   通过RECYCLER的get方法循环使用ByteBuf对象，如果是非内存池实现，则直接创建一个新的ByteBuf对象。从缓冲池中获取ByteBuf
之后，调用AbstractReferenceCountedByteBuf的setRefCnt方法设置引用计数器，用于对象的引用计数和内存回收(类似JVM垃圾回收机制)。
   (8) 灵活的TCP参数配置能力
   合理设置TCP参数在某些场景下对于性能的提升可以起到显著的效果，例如SO_RCVBUF和SO_SNDBUF。如果设置不当，对性能的影响是非常大的。
   下面总结下对性能影响比较大的几个配置项：
   a、SO_RCVBUF和SO_SNDBUF：通常建议值为128KB或者256KB；
   SO_SNDBUF
   Sets send buffer size. This option takes an int value. (it is 8K by default).
   SO_RCVBUF
   Sets receive buffer size. This option takes an int value.
   每个套接口都有一个发送缓冲区和一个接收缓冲区，使用SO_SNDBUF & SO_RCVBUF可以改变缺省缓冲区大小。
   对于客户端，SO_RCVBUF选项须在connect之前设置.
   对于服务器，SO_RCVBUF选项须在listen前设置.
   b、TCP_NODELAY：TCP_NODELAY选项是用来控制是否开启Nagle算法，该算法是为了提高较慢的广域网传输效率，减小小分组的报文个数，完整描述：
   Nagle算法就是为了尽可能发送大块数据，避免网络中充斥着许多小数据块。Nagle算法的基本定义是任意时刻，最多只能有一个未被确认的小段。 
所谓“小段”，指的是小于MSS尺寸的数据块，所谓“未被确认”，是指一个数据块发送出去后，没有收到对方发送的ACK确认该数据已收到。
   TCP/IP协议中针对TCP默认开启了Nagle算法。启动TCP_NODELAY，就意味着禁用了Nagle算法，允许小包的发送。
   对于时延敏感的应用场景需要关闭该算法，即启动TCP_NODELAY。关于TCP_NODELAY的细节可以参考：https://blog.csdn.net/lclwjl/article/details/80154565
   c、软中断：如果Linux内核版本支持RPS(2.6.35以上版本)，开启RPS后可以实现软中断，提升网络吞吐量。RPS根据数据包的源地址，目的地址
以及目的端口和源端口，计算出一个hash值，然后根据这个hash值来选择软中断允许的CPU。从上层来看，也就是说将每个连接和CPU绑定，并通过
这个hash值，来均衡软中断在多个CPU上，提升网络并行处理能力。
   关于软中断可以参考这篇文章：https://www.jianshu.com/p/586b76b6962a
   Netty在启动辅助类中可以灵活的配置TCP参数，满足不同的用户场景。在ChannelOption这个类里面可以看到各个选项。