# EventLoop和EventLoopGroup解析

Netty框架的主要线程就是IO线程，线程模型设计的好坏，决定了系统的吞吐量、并发性和安全性等架构质量属性。
Netty的线程模型被精心地设计，既提升了框架的并发性能，又能在很大程度避免锁，局部实现了无锁化设计。

Reactor单线程模型
Reactor单线程模型，是指所有的IO操作都在同一个NIO线程上面完成。NIO线程的职责如下：
1、作为NIO服务端，接收客户端的TCP连接；
2、作为NIO客户端，向服务端发起TCP连接；
3、读取通信对端的请求或应答消息；
4、向通信对端发送请求消息或应答消息；
由于Reactor模式使用的是异步非阻塞IO，所有的IO操作都不会导致阻塞，理论上一个线程可以独立处理所有IO相关的操作。
从架构层面上看，一个NIO线程确实可以完成其承担的职责。例如，通过Acceptor类接收客户端的TCP连接请求消息，当链路建立
成功之后，通过Dispatch将对应的ByteBuffer派发到指定的Handler上，进行消息解码。用户线程消息解码后通过NIO线程将消息
发送给客户端。
在一些小容量应用场景下，可以使用单线程模型。但是对于高负载、大并发的应用场景却不合适。主要原因如下：
1、一个NIO线程同时处理成百上千的链路，性能上无法支撑，即便NIO线程的CPU负载达到100%，也无法满足海量消息的编码、解码
、读取和发送。
2、当NIO线程负载过重之后，处理速度将变慢，这会导致大量客户端连接超时，超时之后往往会进行重发，这更加重了NIO线程的负载，
最终导致大量消息积压和处理超时，成为系统的性能瓶颈。
3、可靠性问题，一旦NIO线程意外跑飞，或者进入死循环，会导致整个系统通信模块不可用，不能接收和处理外部消息，造成节点故障。
为了解决这些问题， 演进出了Reactor多线程模型。

Reactor多线程模型
Reactor多线程模型与单线程模型最大的区别就是有一组NIO线程来处理IO操作，特点如下：
1、有一个专门的NIO线程-Acceptor线程用于监听服务端，接收客户端的TCP连接请求。
2、网络IO操作-读、写等由一个NIO线程池负责，线程池可以采用标准的JDK线程池实现，它包含了一个任务队列和N个可用的线程，由这些
NIO线程负责消息的读取、解码、编码和发送。
3、一个NIO线程可以同时处理N条链路，但是一个链路只对应一个NIO线程，防止发生并发操作问题。
在绝大多数场景下，Reactor多线程模型可以满足性能需求。但是，在个别特殊场景中，一个NIO线程负责监听和处理所有的客户端连接
可能会存在性能问题。例如并发百万客户端连接，或者服务端需要对客户端握手进行安全认证，但是认证本身非常损耗性能。在这类场景下，
单独一个Acceptor线程可能会存在性能不足的问题，为了解决性能问题，产生了第三种Reactor线程模型-主从Reactor多线程模型。

主从Reactor多线程模型
主从Reactor多线程模型的特点是：服务端用于接收客户端连接的不再是一个单独的NIO线程，而是一个独立的NIO线程池。Acceptor接收
客户端TCP连接请求并处理完成后（可能包含接入认证等），将新创建的SocketChannel注册到IO线程池（sub reactor线程池）的某个
IO线程上，由它负责SocketChannel的读写和编解码工作。Acceptor线程池仅仅用于客户端的登录、握手和安全认证，一旦链路建立成功，
就将链路注册到后端subReactor线程池的IO线程上，由IO线程负责后续的IO操作。
利用主从NIO线程模型，可以解决一个服务端监听线程无法有效处理所有客户端连接的性能不足问题。因此，在Netty的官方demo中，
推荐使用该线程模型。

Netty的线程模型
Netty的线程模型并不是一层不变的，它实际取决于用户的启动参数配置。通过设置不同的启动参数，Netty可以同时支持Reactor单线程
模型、多线程模型和主从多线程模型。
可以通过Netty服务端启动代码了解它的线程模型
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024).childHandler(new ChildChannelHandler());
服务端启动的时候，创建了两个NioEventLoopGroup，它们实际是两个独立的Reactor线程池。一个用于接收客户端TCP连接，另一个用于
处理IO相关的读写操作，或者执行系统Task、定时任务Task等。

Netty用于接收客户端请求的线程池职责如下：
1、接收客户端TCP连接，初始化Channel参数；
2、将链路状态变更事件通知给ChannelPipeline；

Netty处理IO操作的Reactor线程池职责如下：
1、异步读取通信对端的数据报，发送读事件到ChannelPipeline；
2、异步发送消息到通信对端，调用ChannelPipeline的消息发送接口；
3、执行系统调用Task；
4、执行定时任务Task，例如链路空闲状态检测定时任务。

通过调整线程池的线程个数、是否共享线程池等方式，Netty的Reactor线程模型可以在单线程、多线程和主从多线程之间切换，这种灵活
的方式可以最大程度地满足不同用户的个性化定制。
为了尽可能地提升性能，Netty在很多地方进行了无锁化的设计，例如在IO线程内部进行串行操作，避免多线程竞争导致的性能下降问题。
表面上看，串行化设计似乎CPU利用率不高，并发程度不够。但是，通过调整NIO线程池的线程参数，可以同时启动多个串行化的线程并行
运行，这种局部无锁化的串行线程设计相比一个队列-多个工作线程的模型性能更优。
它的设计原理如下：
Netty的NioEventLoop读取消息之后，直接调用ChannelPipeline的fireChannelRead(Object msg)。只要用户不主动切换线程，一直
都是由NioEventLoop调用用户的Handler，期间不进行线程切换。这种串行化处理方式避免了多线程操作导致的锁的竞争，从性能角度看是最优的。

Netty多线程编程的最佳实践如下：
1、创建两个NioEventLoopGroup，用于逻辑隔离NIO Acceptor和 NIO IO线程。
2、尽量不要在ChannelHandler中启动用户线程（解码后用于将POJO消息派发到后端业务线程的除外）。
3、解码要放在NIO线程调用的解码Handler中进行，不要切换到用户线程中完成消息的解码。
4、如果业务逻辑操作非常简单，没有复杂的业务逻辑计算，没有可能导致线程被阻塞的磁盘操作、数据库操作、网络操作等，可以直接在NIO线程上
完成业务逻辑编排，不需要切换到用户线程。
5、如果业务逻辑处理复杂，不要在NIO线程上完成，建议将解码后的POJO消息封装成Task，派发到业务线程池中由业务线程执行，以保证NIO线程
尽快被释放，处理其它的IO操作。
推荐的线程数量计算公式有以下两种：
1、公式一：线程数量=(线程总时间/瓶颈资源时间)x瓶颈资源的线程并行数。
2、公式二：QPS=1000/线程总时间x线程数量
由于用户场景的不同，对于一些复杂的系统，实际上很难计算出最优线程配置，只能根据测试数据和用户场景，结合公式给出一个相对合理的
范围，然后对范围内的数据进行性能测试，选择相对最优值。

NioEventLoop设计原理
Netty的NioEventLoop并不是一个纯粹的IO线程，它除了负责IO的读写之外，还兼顾处理以下两类任务。
1、系统Task：通过调用NioEventLoop的execute(Runnable task)方法实现，Netty有很多系统task，创建他们的主要原因是：
当IO线程和用户线程同时操作网络资源时，为了防止并发操作导致的锁竞争，将用户线程的操作封装成Task放入消息队列中，
由IO线程负责执行，这样就实现了局部无锁化。
2、定时任务：通过调用NioEventLoop的schedule(Runnable command, long delay, TimeUnit unit)方法实现。

NioEventLoop作为NIO框架的Reactor线程，它需要处理网络IO读写事件，因此它必须聚合一个多路复用器对象。
   /**
     * The NIO {@link Selector}.
     */
    private Selector selector;

在Selector的轮询过程中出现了轮询结果为空，也没有wakeup操作或者新的消息需要处理，则说明是个空轮询，有可能触发了JDK的epoll
 bug，它会导致Selector的空轮询，是IO线程一直处于100%状态。关于JDK Epoll空轮询bug可以参考：
 https://www.jianshu.com/p/3ec120ca46b2
 所以Netty需要对该bug进行规避和修正。该bug的修复策略如下：
 1、对Selector的select操作周期进行统计；
 2、每完成一次空的select操作进行一次技术；
 3、在某个周期（例如100ms）内如果连续发送了N次空轮询，说明触发了JDK NIO的epoll死循环bug；
 4、通过销毁旧的、有问题的多路复用器，使用新建的selector，就可以解决空轮询Selector导致的IO线程CPU占用100%的问题。






