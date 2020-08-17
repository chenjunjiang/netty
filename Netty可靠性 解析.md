# Netty可靠性解析

1、网络通信类故障
(1) 客户端连接超时
    在传统的同步阻塞编程模式下，客户端socket发起网络连接，往往需要指定连接超时时间，这样做的目的主要有两个。
    a、在同步阻塞IO模型中，连接操作是同步阻塞的，如果不设置超时时间，客户端IO线程可能会被长时间阻塞，这会导致系统
可用IO线程数的减少。
    b、业务层需要：大多数系统都会对业务流程执行时间有限制，例如WEB交互类的响应时间要小于3s。客户端设置连接超时时间
是为了实现业务层的超时。
    JDK原生的Socket连接接口定义如下：
    public void connect(SocketAddress endpoint, int timeout)
    用户调用Socket的connect方法将被阻塞，直到连接成功或者发生连接超时等异常。
    对于NIO的SocketChannel，在非阻塞模式下，它会直接返回连接结果，如果没有连接成功，也没有发生IO异常，则需要将
SocketChannel注册到Selector上监听连接结果。所以，异步连接的超时无法在API层面直接设置，而是需要通过定时器来主动监测。
下面我们看看JDK NIO类库的SocketChannel连接接口定义：
    public abstract boolean connect(SocketAddress remote)
    从上面的接口定义可以看出，NIO类库并没有现成的连接超时接口供用户直接使用，如果要在NIO编程中支持连接超时，往往需要
NIO框架或者自己封装实现。
    Netty是如何支持连接超时的呢？首先，在创建NIO客户端的时候，可以配置连接超时参数：
    Bootstrap bootstrap = new Bootstrap();
          bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
              .option(ChannelOption.TCP_NODELAY, true)
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
    设置完连接超时之后，Netty在发起连接之后，会根据超时时间创建ScheduledFuture挂载在Reactor线程上，用于定时
监测是否发生连接超时，相关源码在AbstractNioChannel的connect方法里面：
                        // Schedule connect timeout.
                        int connectTimeoutMillis = config().getConnectTimeoutMillis();
                        if (connectTimeoutMillis > 0) {
                            connectTimeoutFuture = eventLoop().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    ChannelPromise connectPromise = AbstractNioChannel.this.connectPromise;
                                    ConnectTimeoutException cause =
                                            new ConnectTimeoutException("connection timed out: " + remoteAddress);
                                    if (connectPromise != null && connectPromise.tryFailure(cause)) {
                                        close(voidPromise());
                                    }
                                }
                            }, connectTimeoutMillis, TimeUnit.MILLISECONDS);
                        }
    
   创建连接超时定时任务之后，会由NioEventLoop负责执行。如果已经连接超时，但是服务端仍然没有返回TCP握手应答，则关闭连接。
   如果在超时期限内完成了连接操作，则取消连接超时定时任务：
           @Override
           public final void finishConnect() {
               // Note this method is invoked by the event loop only if the connection attempt was
               // neither cancelled nor timed out.
   
               assert eventLoop().inEventLoop();
   
               try {
                   boolean wasActive = isActive();
                   doFinishConnect();
                   fulfillConnectPromise(connectPromise, wasActive);
               } catch (Throwable t) {
                   fulfillConnectPromise(connectPromise, annotateConnectException(t, requestedRemoteAddress));
               } finally {
                   // Check for null as the connectTimeoutFuture is only created if a connectTimeoutMillis > 0 is used
                   // See https://github.com/netty/netty/issues/1770
                   if (connectTimeoutFuture != null) {
                       connectTimeoutFuture.cancel(false);
                   }
                   connectPromise = null;
               }
           }
   Netty的客户端连接超时参数与其它常用的TCP参数一起配置，使用起来非常方便，上层用户不需要关心底层的超时实现机制。这既满足了用户的
个性化需求，又实现了故障的分层隔离。
   
   (2) 通信对端强制关闭连接
   在客户端和服务端正常通信过程中，如果发生网络闪断、对方进程突然宕机或者其它非正常关闭链路事件时，TCP链路就会发生异常。由于TCP是
全双工的，通信双方都需要关闭和释放Socket句柄才不会发生句柄的泄露。
   在实际的NIO编程过程中，我们经常会发现由于句柄没有被及时关闭导致的功能和可靠性问题。究其原因如下：
   a、IO的读写等操作并非仅仅集中在Reactor线程内部，用户上层的一些定制行为可能会导致IO操作的外逸，例如业务自定义心跳机制。这些定制行为
加大了统一异常处理的难度，IO操作越发散，故障发生的概率就越大；
   b、一些异常分支没有考虑到，由于外部环境诱因导致程序进入这些分支，就会引起故障。
   我们通过模拟故障，看Netty是如何处理对端链路强制关闭你异常的。首先启动Netty服务端和客户端，TCP链路建立成功之后，双方
维持该链路，查看链路状态：
   C:\Users\My>netstat -ano|find "8080"
     TCP    0.0.0.0:8080           0.0.0.0:0              LISTENING       2564
     TCP    127.0.0.1:8080         127.0.0.1:56160        ESTABLISHED     2564
     TCP    127.0.0.1:56160        127.0.0.1:8080         ESTABLISHED     4380
     TCP    [::]:8080              [::]:0                 LISTENING       2564
   
   强制关闭客户端，模拟客户端宕机，服务端控制台打印以下异常：
   java.io.IOException: 远程主机强迫关闭了一个现有的连接。
   从堆栈信息可以判断，服务端已经监控到客户端强制关闭了连接，下面我们看看服务端是否已经释放了连接句柄，再次执行netstat
   C:\Users\My>netstat -ano|find "8080"
     TCP    0.0.0.0:8080           0.0.0.0:0              LISTENING       2564
     TCP    [::]:8080              [::]:0                 LISTENING       2564
   从执行结果可以看出，服务端已经关闭了和客户端的TCP连接，句柄资源正常释放。由此可以得出结论，Netty底层已经自动对
该故障进行了处理。
   
   (3) 链路关闭
   对于短连接协议，例如HTTP协议，通信双方数据交互完成之后，通常按照双方的约定由服务端关闭连接，客户端获得TCP连接关闭请求
之后，关闭自身的Socket连接，双方正式断开连接。
   在实际的NIO编程过程中，经常存在一种误区：认为只要是对方关闭连接，就会发生IO异常，捕获IO异常之后再关闭连接即可。实际上，
连接的合法关闭不会发生IO异常，它是一种正常场景，如果遗漏了该场景的判断和处理就会导致连接句柄泄露。
   在Netty中，如果客户端正常关闭链路，服务端是如何判断出客户端关闭连接的？当连接被对方合法关闭后，被关闭的SocketChannel
会处于就绪状态，SocketChannel的read操作返回值为-1，说明连接已经被关闭。如果SocketChannel被设置为非阻塞，则它的read操作
可能返回三个值：
   a、大于0：表示读取到了字节数；
   b、等于0：没有读取到消息，可能TCP处于Keep-Alive状态，接收到的是TCP握手消息；
   c、-1：连接已被对方关闭。
   Netty通过判断Channel read操作的返回值进行不同的逻辑处理，如果返回-1，说明链路已经关闭，则调用closeOnRead方法关闭句柄，释放资源。
   己方或者对方主动关闭链接并不属于异常场景，因此不会产生Exception事件通知Pipeline。
   
   (4) 定制IO故障
   在大多数场景下，当底层网络发生故障的时候，应该由底层的NIO框架负责释放资源，处理异常等。上层的业务应用不需要关心底层的处理细节。
但是，在一些特殊的场景下，用户可能需要感知这些异常，并针对这些异常进行定制处理，例如：
   a、客户端的断连重连机制；
   b、消息的缓存重发；
   c、接口日志中详细记录故障细节；
   d、运维相关功能，例如告警、触发邮件/短信等。
   Netty的处理策略是发生IO异常，底层的资源由它负责释放，同时将异常堆栈信息以事件的形式通知给上层用户，由用户对异常进行定制。
这种处理机制既保证了异常处理的安全性，也向上层提供了灵活的定制能力。具体接口定义以及默认实现(ChannelHandlerAdapter)如下：
      @Skip
      @Override
      @Deprecated
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          ctx.fireExceptionCaught(cause);
      }
   

2、链路的有效性检测
   当网络发生单通、连接被防火墙Hang住、长时间GC或者通信线程发生非预期异常时，会导致链路不可用且不易被及时发现。特别是异常
发生在凌晨业务低谷期间，当早晨业务高峰期到来时，由于链路不可用会导致瞬间的大量业务失败或超时，这将对系统的可靠性产生重大威胁。
   从技术层面看，要解决链路的可靠性问题，必须周期性的对链路进行有效性检测，目前最流行和通用的做法就是心跳检测。
   心跳检测机制分为三个层面：
   a、TCP层面的心跳检测，即TCP的Keep-Alive机制，它的作用域是整个TCP协议栈；
   b、协议层的心跳检测，主要存在于长连接协议中。例如SMPP协议；
   c、应用层的心跳检测，它主要由各业务产品通过约定方式定时给对方发送心跳消息实现。
   心跳检测的目的就是确认当前链路可用，对方活着并且能够正常接收和发送消息。作为高可用的NIO框架，Netty也提供了心跳检测机制。
   不同的协议，心跳检测机制也存在差异，归纳起来主要分为两类：
   a、Ping-Pong型心跳：由通信一方定时发送Ping消息，对方接收到Ping消息之后，立即返回Pong应答消息给对方，属于请求-应答型心跳。
   b、Ping-Ping型心跳：不区分心跳请求和应答，由通信双方按照约定定时向对方发送心跳Ping消息，它属于双向心跳。
   心跳检测策略如下：
   a、连续N次心跳检测都没有接收到对方的Pong应答消息或者Ping请求消息，则认为链路已经发生了逻辑失效，这被称作心跳超时。
   b、读取和发送心跳消息的时候如果直接发生了IO异常，说明链路已经失效，这被称为心跳失败。
   无论发生了心跳超时还是心跳失败，都需要关闭链路，由客户端发起重连操作，保证链路能够恢复正常。
   Netty的心跳检测实际上是利用了链路空闲检测机制实现的，相关代码在io.netty.handler.timeout包下。每个类的作用看注释就能明白。
   Netty提供的空闲检测机制分三种，有不同的Handler与之对应：
   a、读空闲，链路持续时间t没有读取到任何消息；(ReadTimeoutHandler)
   b、写空闲，链路持续时间t没有发送任何消息；(WriteTimeoutHandler)
   c、读写空闲，链路持续时间t没有接收或者发送任何消息。(IdleStateHandler)
   链路空闲的时候并没有关闭链路，而是触发了IdleStateEvent事件，用户订阅IdleStateEvent事件，用于自定义逻辑处理，例如
关闭链路、客户端发起重新连接、告警和打印日志等。利用Netty提供的链路空闲检测机制，可以非常灵活的实现协议层的心跳检测。
   注意：当有业务消息时，无须心跳检测，业务消息本身就可以进行链路可用性检测。所以心跳消息往往是在链路空闲时发送的。
   

3、Reactor线程的保护
   Reactor线程是IO操作的核心，NIO框架的发动机，一旦出现故障，将会导致挂载在其上面的多路复用器和多个链路无法正常工作。
   因此它的可靠性要求非常高。下面来看看Netty是如何有效提升Reactor线程的可靠性的。
   a、异常处理要谨慎
   尽管Reactor线程主要处理IO操作，发生的异常通常是IO异常，但是，实际上在一些特殊场景下会发生非IO异常，如果仅仅捕获IO异常
可能就会导致Reactor线程跑飞。为了防止这种意外，在循环体内一定要捕获Throwable，而不是IO异常或者Exception。
   Netty的NioEventLoop代码如下：
   @Override
       protected void run() {
           int selectCnt = 0;
           for (;;) {
               try {
               ......
               } catch (Throwable t) {
                        handleLoopException(t);
               }
           }
       }
   private static void handleLoopException(Throwable t) {
           logger.warn("Unexpected exception in the selector loop.", t);
   
           // Prevent possible consecutive immediate failures that lead to
           // excessive CPU consumption.
           try {
               Thread.sleep(1000);
           } catch (InterruptedException e) {
               // Ignore.
           }
   }
   捕获Throwable之后，即便发生了未知的异常，线程也不会跑飞，它休眠一秒，防止可能出现的连续即时故障导致CPU过度消耗,然后继续恢复执行，
这样处理的核心理念就是：
   (1) 某个消息的异常不应该导致整条链路不可用；
   (2) 某条链路不可用不应该导致其它链路不可用；
   (3) 某个进程不可用不应该导致其它集群节点不可用。
   
   b、规避NIO BUG
   通常情况下，死循环是可检测、可预防但是无法完全避免的。Reactor线程通常处理的都是IO相关的操作，因为重点关注IO层面的死循环。
   JDK NIO类库最著名的就是epoll bug了，它会导致Selector空轮询，IO线程CPU 100%，严重影响系统的安全性和可靠性。
   Netty的解决策略：
   (1) 根据BUG的特征，首先侦测该BUG是否发生；
   (2) 将问题Selector上注册的Channel转移到新建的Selector上；
   (3) 老的Selector关闭，使用新建的Selector替换。
   相关源码可以参考NioEventLoop的rebuildSelector0方法。
   
   
4、内存保护
   NIO通信的内存保护主要集中在以下几点：
   (1) 链路总数的控制：每条链路都包含接收和发送缓冲区，链路个数太多容易导致内存溢出；
   (2) 单个缓冲区的上限控制：防止非法长度或者消息过大导致内存溢出；
   (3) 缓冲区内存释放：防止因为缓冲区使用不当导致的内存泄露；
   (4) NIO消息发送队列的长度上限控制。
   
   a、缓冲区的内存泄露保护
   为了提升内存的利用率，Netty提供了内存池和对象池。但是，基于缓存池实现以后需要对内存的申请和释放进行严格的管理，否则很容易导致内存泄露。
   如果不采用内存池技术实现，每次对象都是以方法的局部变量形式被创建，使用完成之后，只要不继续引用它，JVM会自动释放。但是，一旦引入内存池
机制，对象的生命周期将由内存池负责管理，这通常是个全局引用，如果不显示释放JVM是不会回收这部分内存的。
   对于Netty的用户而言，使用者的技术水平差异很大，一些对JVM内存模型和内存泄露机制不了解的用户，可能只记得申请内存，忘记主动释放内存。
   为了防止因为用户遗漏导致内存泄露，Netty在Pipeline的尾Handler中自动对内存进行释放，相关源码在DefaultChannelPipeline
   中的TailContext类中：
   @Override
           public void channelRead(ChannelHandlerContext ctx, Object msg) {
               onUnhandledInboundMessage(ctx, msg);
           }
   
   /**
        * Called once a message hit the end of the {@link ChannelPipeline} without been handled by the user
        * in {@link ChannelInboundHandler#channelRead(ChannelHandlerContext, Object)}. This method is responsible
        * to call {@link ReferenceCountUtil#release(Object)} on the given msg at some point.
        */
       protected void onUnhandledInboundMessage(Object msg) {
           try {
               logger.debug(
                       "Discarded inbound message {} that reached at the tail of the pipeline. " +
                               "Please check your pipeline configuration.", msg);
           } finally {
               ReferenceCountUtil.release(msg); // 释放内存
           }
       }
       
   对于内存池，实际就是将缓冲区重新放到内存池中循环使用，PooledByteBuf的内存回收代码如下：
   @Override
       protected final void deallocate() {
           if (handle >= 0) {
               final long handle = this.handle;
               this.handle = -1;
               memory = null;
               chunk.arena.free(chunk, tmpNioBuf, handle, maxLength, cache);
               tmpNioBuf = null;
               chunk = null;
               recycle();
           }
       }
   对于实现了AbstractReferenceCountedByteBuf的ByteBuf，内存申请、使用和释放的时候Netty都会自动进行引用计数检测，防止非法使用内存。
   
   b、缓冲区溢出保护
   做过协议栈的读者都知道，当我们对消息进行解码的时候，需要创建缓冲区。缓冲区的创建方式通常由两种：
   (1) 容量预分配，在实际读写过程中如果不够再扩展；
   (2) 根据协议消息长度创建缓冲区。
   在实际商用环境中，如果遇到畸形码流攻击、协议消息编码异常、消息丢包等问题时，可能会解析到一个超长的字段。曾经遇到过类似的问题，报文长度
竟然是2G多，由于代码的一个分支没有对长度上限做有效保护，结果导致内存溢出。
   Netty提供了编解码框架，因此对于解码缓冲区的上限保护就显得非常重要了。我们看看Netty是如何对缓冲区进行上限保护的：
   首先，在内存分配的时候指定缓冲区长度上限：
   ByteBufAllocator
       /**
        * Allocate a {@link ByteBuf} with the given initial capacity and the given
        * maximal capacity. If it is a direct or heap buffer depends on the actual
        * implementation.
        */
       ByteBuf buffer(int initialCapacity, int maxCapacity);
    
   其次，在对缓冲区进行写入操作的时候，如果缓冲区容量不足需要扩展，首先对最大容量进行判断，如果扩展后的容量超过上限，则拒绝扩展。
   在消息解码的时候，对消息长度进行判断，如果超过最大容量上限，则抛出解码异常，拒绝分配内存，以LengthFieldBasedFrameDecoder
   的decode方法为例：
   if (frameLength > maxFrameLength) {
               exceededFrameLength(in, frameLength);
               return null;
   }
   
   
5、流量整形
https://www.jianshu.com/p/bea1b4ea8402
https://www.jianshu.com/p/6c4a7cbbe2b5
   流量整形是一种主动调整流量输出速率的措施。一个典型应用是基于下游网络节点的TP指标来控制本地流量的输出。流量整形与流量监管的主要
区别在于，流量整形对流量监管中需要丢弃的报文进行缓存-通常是将它们放入缓冲区或队列内。当令牌桶有足够的令牌时，再均匀的向外发送这些被
缓存的报文。流量整形与流量监管的另一个区别是，整形可能增加延迟，而监管几乎不引入额外的延迟。
   作为高性能的NIO框架，Netty的流量整形有两个作用：
   (1) 防止由于上下游网元性能不均衡导致下游网元被压垮，业务流程中断；
   (2) 防止由于通信模块接收消息过快，后端业务线程处理不及时导致的“撑死”问题。
   
   a、全局流程整形
   全局流程整形的作用范围是进程级的，无论你创建了多少个Channel，它的作用域针对所有的Channel。
   用户可以通过参数设置：报文的接收速率、报文的发送速率、整形周期。GlobalTrafficShapingHandler的方法定义如下：
   public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit,
               long readLimit) {
           super(writeLimit, readLimit);
           createGlobalTrafficCounter(executor);
   }
   Netty的流量整形原理是：对每次读取到的ByteBuf可写字节数进行计算，获取当前的报文流量，然后与流量整形阀值对比。如果已经达到或
超过了阀值。则计算等待时间delay，将当前的ByteBuf放到定时任务Task中缓存，由定时任务线程池在延迟delay之后继续处理该ByteBuf。
   如果达到整形阀值，则对新接收的ByteBuf进行缓存，放入线程池的消息队列中，稍后处理。
   定时任务的延迟时间根据检查周期T和流量整形阀值计算得来。
   需要指出的是，流量整形的阀值limit越大，流量整形的精度越高，流量整形功能是可靠性的一种保障，它无法做到100%准确。这个跟后端的
编解码以及缓冲区的处理策略相关。
   流量整形与流控的最大区别在于流控会拒绝消息，流量整形不拒绝和丢弃消息，无论接收量多大，它总能以近似恒定的速度下发消息。
   GlobalChannelTrafficShapingHandler
   相比于GlobalTrafficShapingHandler增加了一个误差概念，以平衡各个Channel间的读/写操作。也就是说，使得各个Channel间的读/写操作尽量均衡。
比如，尽量避免不同Channel的大数据包都延迟近乎一样的是时间再操作，以及如果小数据包在一个大数据包后才发送，则减少该小数据包的延迟发送时间等。
   
   b、链路级流量整形
   除了全局流量整形，Netty也支持链路级的流量整形，ChannelTrafficShapingHandler的方法定义如下：
   public ChannelTrafficShapingHandler(long writeLimit,
               long readLimit, long checkInterval) {
           super(writeLimit, readLimit, checkInterval);
   }
   单链路流量整形与全局流量整形的最大区别就是它以单个链路为作用域，可以对不同的链路设置不同的整形策略。它的原理与全局流量整形类似。
值得说明的是，Netty支持用户自定义流量整形策略，通过继承AbstractTrafficShapingHandler并重写doAccounting方法即可。

6、优雅停机接口
https://www.jianshu.com/p/8bbe259ec1c4
   Java的优雅停机通常通过注册JDK的ShutdownHook来实现，当系统接收到退出指令后，首先标记系统处于退出状态，不再接收新的消息，然后将
积压的消息处理完毕，最后调用资源回收接口将资源销毁，最后各线程退出执行。
   通常优雅退出有个时间限制，例如30s，如果达到限制时间后仍然没有完成退出前的操作，则由监控脚本直接kill -9 pid，强制退出。
   Netty的优雅退出总结起来有三大步操作：
   (1) 把NIO线程的状态位设置成ST_SHUTTING_DOWN状态，不再处理新的消息（不允许再对外发送消息）；
   (2) 退出前的预处理操作：把发送队列中尚未发送或者正在发送的消息发送完、把已经到期或者在退出超时之前到期的定时任务执行完成、
把用户注册到NIO线程的退出Hook任务执行完成；
   (3) 资源的释放操作：所有Channel的释放、多路复用器的关闭、所有队列和定时任务的清空取消，最后是NIO线程的退出。
   
   
7、优化建议
   用户可以根据自己的实际需要进行优化：
   a、发送队列容量上限控制
   Netty的NIO消息发送队列到ChannelOutboudBuffer并没有容量上限控制，它会随着消息的积压自动扩展，直到到达0x7fffffff(2^31-1)。
   如果网络对方处理速度比较慢，导致TCP滑窗长时间为0；或者消息发送方发送速度过快，或者一次批量发送消息量过大，都可能会导致
ChannelOutboudBuffer的内存膨胀，这可能会导致系统的内存溢出。建议的优化方式如下：
   在启动客户端或者服务端的时候，通过启动项的ChannelOption设置发送队列的长度，或者通过-D启动参数配置该长度。
   
   b、回推发送失败的消息
   当网络发生故障的时候，Netty会关闭链路，然后循环释放待发送的消息，最后通知listener。
   这样的处理策略值得商榷，对于大多数用户而言，并不关心底层的网络IO异常，他们希望链路恢复之后可以自动将尚未发送的消息重新
发送给对方，而不是简单的销毁。
   Netty销毁尚未发送的消息，用户可以通过监听器来得到消息发送异常通知，但是却无法获取原始待发送的消息。如果要实现重发，需要
自己缓存消息，如果发送成功，自己删除缓存的消息，如果发送失败，重新发送。这对于大多数用户而言，非常麻烦，用户在开发业务代码的
同时，还需要考虑网络IO层的异常并为之做特殊的业务逻辑处理。
   我们看看Mina的实现，当发生链路异常之后，Mina会将尚未发送的整包消息队列封装到异常对象中，然后推送给用户Handler，由用户
来决定后续的处理策略。相比Netty野蛮的销毁策略，Mina的策略更灵活和合理，由用户自己决定发送失败消息的后续处理策略。
   大多数场景下，业务用户会使用RPC框架，他们通常不需要直接针对Netty编程，如果Netty提供了发送失败消息的回推功能，RPC框架
就可以进行封装，提供不同的策略给业务用户使用，例如：
   (1) 缓存重发策略：当链路发送异常后，尚未发送成功的消息自动缓存，待链路恢复正常之后重发失败的消息；
   (2) 失败删除策略：当链路发送异常后，尚未发送成功的消息自动销毁，它可能是非重要消息，例如日志消息，也可能是由业务直接监听
异常并做特殊处理；
   (3) 其它策略......









   