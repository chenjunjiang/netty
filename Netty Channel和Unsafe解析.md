# Netty Channel和Unsafe解析
io.netty.channel.Channel是Netty网络操作抽象类，它聚合了一组功能，包括但不限于网络的读、写，客户端发起连接，主动
关闭连接，链路关闭，获取通信双方的网络地址等。它也包含了Netty框架相关的一些功能，包括获取该Channel的EventLoop，获取
缓冲区分配器ByteBufAllocator和pipeline等。
为什么不使用JDK NIO原生的Channel？主要原因如下：
1、JDK的SocketChannel和ServerSocketChannel没有统一的Channel接口供业务开发者使用，对于用户而言，没有统一的操作
视图，使用起来并不方便。
2、JDK的SocketChannel和ServerSocketChannel的主要职责就是网络IO操作，由于它们是SPI类接口，由具体的虚拟机厂家
来提供，所以通过继承SPI功能类来扩展其功能的难度很大；直接实现SocketChannel和ServerSocketChannel抽象类，其工作量
和重新开发一个新的Channel功能类是差不多的。
3、Netty的Channel需要能够跟Netty的整体架构融合在一起，例如IO模型，基于ChannelPipeline的定制模型，以及基于元数据
描述配置化的TCP参数等，这些JDK的SocketChannel和ServerSocketChannel都没有提供，需要重新封装。
4、自定义的Channel，功能实现更加灵活。
基于上述原因，Netty重新设计了Channel接口，并且给予了很多不同的实现。它的设计原理比较简单，但是功能比较繁杂，主要的
设计理念如下：
1、在Channel接口层，采用Facade模式进行统一封装，将网络IO操作、网络IO相关联的其它操作封装起来，统一对外提供。
2、Channel接口的定义尽量大而全，为SocketChannel和ServerSocketChannel提供统一的视图，由不同的子类实现不同的功能
，公共功能在抽象父类中实现，最大程度地实现功能和接口的重用。
3、具体实现采用聚合而非包含的方式(聚合和组合的区别：https://blog.csdn.net/u012557814/article/details/79461756)，
将相关的功能类聚合在Channel中，由Channel统一负责分配和调度，功能实现更加灵活。

Channel的功能介绍
1、网络IO操作
Netty是基于事件驱动的，我们可以理解为当Channel进行IO操作时会产生对应的IO事件，然后驱动事件在ChannelPipeline中传播，
由对应的ChannelHandler对事件进行拦截和处理，不关心的事件可以直接忽略。采用时间驱动的方式可以非常轻松地通过事件定义
来划分事件拦截切面，方便业务的定制和功能扩展，相比AOP，其性能更高，但是功能却基本等价。
(1) Channel read(): 从当前的Channel中读取数据到第一个inbound缓冲区中，如果数据被成功读取，触发ChannelHandler
.channelRead(ChannelHandlerContext, Object)事件。
(2) ChannelFuture write(Object msg): 请求将当前的msg通过ChannelPipeline写入目标Channel中。注意，write操作
只是将消息存入到消息发送环形数组中，并没有真正被发送，只有调用flush操作才会被写入Channel中，发送给对方。
(3) ChannelFuture write(Object msg, ChannelPromise promise): 功能和上面一个相似，但是携带了ChannelPromise
参数负责设置写入操作的结果。
(4) ChannelFuture writeAndFlush(Object msg, ChannelPromise promise): 与方法(3)功能类似，不同之处在于它会
将消息写入Channel中发送，等价于单独调用write和flush操作的组合。
(5) ChannelFuture writeAndFlush(Object msg): 功能等同于方法(4)，但是没有携带ChannelPromise参数。
(6) Channel flush(): 将之前写入到发送环形数组中的消息全部写入到目标Channel中，发送给通信对方。
(7) ChannelFuture close(ChannelPromise promise): 主动关闭当前连接，通过ChannelPromise设置操作结果并进行结果
通知，无论操作成功与否，都可以通过ChannelPromise获取操作结果。该操作会级联触发ChannelPipeline中所有ChannelHandler
的ChannelHandler.close(ChannelHandlerContext, ChannelPromise)事件。
(8) ChannelFuture disconnect(ChannelPromise promise): 请求断开与远程通信对端的连接并使用ChannelPromise
来获取操作结果的通知消息。该操作会级联触发ChannelPipeline中所有ChannelHandler的
ChannelHandler.disconnect(ChannelHandlerContext, ChannelPromise)事件。
(9) ChannelFuture connect(SocketAddress remoteAddress): 客户端使用指定的服务端地址remoteAddress发起连接
请求，如果连接因为应答超时而失败，ChannelFuture中的操作结果就是ConnectTimeoutException异常；如果连接被拒绝，
操作结果为ConnectException。该方法会级联触发
ChannelHandler.connect(ChannelHandlerContext, SocketAddress, SocketAddress)事件。
(10) ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress): 与方法(9)功能类似，
唯一不同的就是先绑定指定的本地地址localAddress，然后再连接服务端。
(11) ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise): 与方法(9)功能类似，
唯一不同的是携带了ChannelPromise参数用于写入操作结果。
(12) ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise): 
与方法(11)功能类似，唯一不同的是绑定了本地地址localAddress。
(13) ChannelFuture bind(SocketAddress localAddress): 绑定指定的本地Socket地址localAddress，该方法会级联触发
ChannelHandler.bind(ChannelHandlerContext, SocketAddress, ChannelPromise)事件。
(14) ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise): 与方法(13)功能类似，多携带了
一个ChannelPromise用于写入操作结果。
(15) ChannelConfig config(): 获取当前Channel的配置信息，例如CONNECT_TIMEOUT_MILLIS。
(16) boolean isOpen(): 判断当前Channel是否已经打开。
(17) boolean isRegistered(): 判断当前Channel是否已经注册到EventLoop上。
(18) boolean isActive(): 判断当前Channel是否已经处于激活状态。
(19) ChannelMetadata metadata(): 获取当前Channel的元数据描述信息，包括TCP参数配置等。
(20) SocketAddress localAddress(): 获取当前Channel的本地绑定地址。
(21) SocketAddress remoteAddress(): 获取当前Channel的远程Socket地址。
还有一个比较重要的方法是eventLoop()。Channel需要注册到EventLoop的多路复用器上， 用于处理IO事件，通过eventLoop()
方法可以获取到Channel注册的EventLoop。EventLoop本质上就是处理网络事件的Reactor线程。在Netty中，它不仅仅用来处理
网络事件，还可以用来执行定时任务和用户自定义NioTask等任务。
还有一个比较常用的方法是metadata()方法。熟悉TCp协议的朋友可能知道，当创建Socket的时候需要指定TCP参数，例如接收和发送
的TCP缓冲区大小、TCP的超时时间、是否重用地址等。在Netty中，每个Channel对应一个物理连接，每个连接都有自己的TCP参数配置。
所以，Channel会聚合一个ChannelMetadata用来对TCP参数提供元数据描述信息，通过metadata()方法就可以获取当前Channel的
TCP参数配置。
第三个方法是parent()。对于服务端Channel而言，它的父Channel为空；对于客户端Channel，它的父Channel就是创建它的ServerSocketChannel。
第四个方法是用户获取Channel标识的id()，它返回ChannelId对象，ChannelId是Channel的唯一标识，它的可能生成策略如下：
1、机器MAC地址(EUI-48或者EUI-64)等可以代表全局唯一的信息；
2、当前的进程ID；
3、当前系统时间的毫秒；
4、当前系统时间的纳秒；
5、32位的随机整数；
6、32位自增的序列数。


Unsafe接口实际上是Channel接口的辅助接口，它不应该被用户代码直接使用。实际的IO读写操作都是由Unsafe接口负责完成的。
功能列表如下：
方法名                                       返回值                 功能说明
localAddress()                              SocketAddress          返回本地绑定的Socket地址
remoteAddress()                             SocketAddress          返回通信对端的Socket地址
register(EventLoop eventLoop, 
ChannelPromise promise)                     void                   注册Channel到多路复用器上，一旦完成注册
                                                                   操作，通知ChannelFuture
bind(SocketAddress localAddress, 
ChannelPromise promise)                     void                   绑定指定的本地地址localAddress到当前的Channel
                                                                   上，一旦完成，通知ChannelFuture
connect(SocketAddress remoteAddress, 
SocketAddress localAddress, 
ChannelPromise promise)                     void                    绑定本地的localAddress之后，连接服务端，一旦操作
                                                                    完成，通知ChannelFuture
disconnect(ChannelPromise promise)          void                    断开Channel的连接，一旦完成，通知ChannelFuture
close(ChannelPromise promise)               void                    强制立即关闭连接
beginRead()                                 void                    设置网络操作位为读，用于读取消息
write(Object msg, ChannelPromise promise)   void                    发送消息，一旦完成，通知ChannelFuture
flush()                                     void                    将发送到缓冲数组中的消息写入Channel中
voidPromise()                               ChannelPromise          返回一个特殊的可重用和传递的ChannelPromise，
                                                                    它不用于操作成功或失败的通知器，仅仅作为
                                                                    一个容器被使用
outboundBuffer()                            ChannelOutboundBuffer   返回消息发送缓冲区             


    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    