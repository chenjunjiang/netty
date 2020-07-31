# ChannelPipeline和ChannelHandler解析
Netty的ChannelPipeline和ChannelHandler机制类似于Servlet和Filter，这类拦截器实际上是责任链模式的一种变形，
主要是为了方便事件的拦截和用户业务逻辑的定制。Netty将Channel的数据管道抽象为ChannelPipeline，消息在ChannelPipeline
中流动和传递。ChannelPipeline持有IO事件拦截器ChannelHandler的链表，由ChannelHandler对IO事件进行拦截和处理，可以
方便地通过新增和删除ChannelHandler来实现不同的业务逻辑定制，不需要对已有的ChannelHandler进行修改，能够实现对修改封闭
、对扩展支持。

ChannelPipeline功能说明
ChannelPipeline是ChannelHandler的容器，它负责ChannelHandler的管理和事件的拦截与调度。
1、ChannelPipeline的事件处理
消息的读取和发送处理全流程描述如下：
(1) 底层的SocketChannel read()方法读取ByteBuf，触发ChannelRead事件，由IO线程NioEventLoop调用ChannelPipeline
的fireChannelRead(Object msg)方法，将消息ByteBuf传输到ChannelPipeline中。
(2) 消息依次被HeadHandler、ChannelHandler1、ChannelHandler2......TailHandler拦截和处理，在这个过程中，任何
ChannelHandler都可以中断当前的流程，结束消息的传递。
(3) 调用ChannelHandlerContext的write方法发送消息，消息从TailHandler开始，途径ChannelHandlerN......
ChannelHandler1、HeadHandler，最终被添加到消息发送缓冲区中等待刷新和发送，在此过程中也可以中断消息的传递，例如当
编码失败时，就需要中断流程，构造异常的Future返回。

Netty中的事件分为inboud事件和outboud事件。
inboud事件通常由IO线程触发，例如TCP链路建立事件、链路关闭事件、读事件、异常通知事件等。
触发inboud事件的方法如下：
(1) ChannelHandlerContext.fireChannelRegistered(): Channel注册事件；
(2) ChannelHandlerContext.fireChannelActive(): Channel注册事件；
(3) ChannelHandlerContext.fireChannelRead(Object msg): 读事件；
(4) ChannelHandlerContext.fireChannelReadComplete(): 读操作完成通知事件；
(5) ChannelHandlerContext.fireExceptionCaught(): 异常通知事件；
(6) ChannelHandlerContext.fireUserEventTriggered(): 用户自定义事件；
(7) ChannelHandlerContext.fireChannelWritabilityChanged(): Channel的可写状态变化通知事件；
(8) ChannelHandlerContext.fireChannelInactive(): TCP连接关闭，链路不可用通知事件；
outboud事件通常是由用户主动发起的网络IO操作，例如用户发起的连接操作、绑定操作、消息发送等操作。
触发outboud事件的方法如下：
(1) ChannelHandlerContext.bind(SocketAddress localAddress): 绑定本地地址事件；
(2) ChannelHandlerContext.connect(SocketAddress remoteAddress, SocketAddress localAddress): 连接服务端事件；
(3) ChannelHandlerContext.write(Object msg, ChannelPromise promise): 发送事件；
(4) ChannelHandlerContext.flush(): 刷新事件；
(5) ChannelHandlerContext.disconnect(ChannelPromise promise): 断开连接事件；
(6) ChannelHandlerContext.read(): 读事件；
(7) ChannelHandlerContext.close(ChannelPromise promise): 关闭当前Channel事件；

2、自定义拦截器
ChannelPipeline通过ChannelHandler接口来实现事件的拦截和处理，由于ChannelHandler中的事件种类繁多，不同的ChannelHandler
可能只需要关心其中的某一个或者几个事件，所以，通常ChannelHandler只需要集成ChannelHandlerAdapter类覆盖自己关心的方法即可。

3、构建Pipeline
事实上，用户不需要自己创建Pipeline，因为使用ServerBootstrap或者Bootstrap启动服务端或者客户端时，Netty会为每个Channel
连接创建一个独立的pipeline。对于使用者而言，只需要将自定义的拦截器加入到pipeline中即可。
对于类似编解码这样的ChannelHandler，它存在先后顺序，例如MessageToMessageDecoder，在它之前往往需要有ByteToMessageDecoder
将ByteBuf解码为对象，然后对对象进行二次解码得到最终的POJO对象。Pipeline支持指定位置添加或删除拦截器。

4、ChannelPipeline的主要特性
ChannelPipeline支持运行态动态的添加或者删除ChannelHandler，在某些场景下这个特性非常有用。例如当业务高峰期需要对系统
做拥塞保护时，就可以根据当前的系统时间进行判断，如果处于业务高峰期，则动态地将系统拥塞保护ChannelHandler添加到当前的
ChannelPipeline中，当高峰期过去之后，就可以动态删除拥塞保护ChannelHandler了。
ChannelPipeline是线程安全的，这意味着N个业务线程可以并发地操作ChannelPipeline而不存在多线程并发问题。但是ChannelHandler
却不是线程安全的，这意味着尽管ChannelPipeline是线程安全的，用户仍然需要自己保证ChannelHandler的线程安全。


ChannelHandler功能说明
ChannelHandler类似于Servlet的Filter，负责对IO事件或者IO操作进行拦截和处理，它可以选择性地拦截和处理自己感兴趣的事件，
也可以透传和终止事件的传递。
基于ChannelHandler接口，用户可以方便地进行业务逻辑定制，例如打印日志、统一封装异常信息、性能统计和消息编解码等。
ChannelHandler支持注解，目前支持的注解有两种：
Sharable: 多个ChannelPipeline共用同一个ChannelHandler；
Skip: 被Skip注解的方法不会被调用，直接忽略；
(1) ChannelHandlerAdapter功能说明
对于大多数的ChannelHandler会选择性地拦截和处理某个事件或者某些事件，其它的事件会忽略，由下一个ChannelHandler进行
拦截和处理。这就会导致一个问题，用户ChannelHandler必须要实现ChannelHandler的所有接口，包括它不关心的那些事件处理接口，
这会导致用户代码的冗余和臃肿，代码的可维护性也会变差。
为了解决这个问题，Netty提供了ChannelHandlerAdapter基类，它的所有接口实现都是事件透传，如果用户ChannelHandler关心
某个事件，只需要覆盖ChannelHandlerAdapter对应的方法即可，对于不关心的，可以直接继承使用父类的方法。
ChannelInboundHandlerAdapter相关的代码如下：
    @Skip
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();
    }
我们可以发现这些透传方法被@Skip注解了，这些方法在执行的过程中会被忽略，直接跳到下一个ChannelHandler中只执行对应的方法。

(2) ByteToMessageDecoder功能说明
利用NIO进行网络编程时，往往需要将读取到的字节数组或字节缓冲区编码为业务可以使用的POJO对象。为了方便业务将ByteBuf解码
成业务POJO对象，Netty提供了ByteToMessageDecoder抽象工具解码类。
用户的解码器继承ByteToMessageDecoder，只需要实现decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
抽象方法即可完成ByteBuf到POJO对象的解码。
由于ByteToMessageDecoder并没有考虑TCP粘包和组包等场景，读半包需要用户解码器自己处理。正因为如此，对于大多数场景不会
直接继承ByteToMessageDecoder，而是继承另外一些更高级的解码器来屏蔽半包的处理，下面一一介绍。

(3) MessageToMessageDecoder功能说明
MessageToMessageDecoder实际上是Netty的二次解码器，它的职责是讲一个对象二次解码对其它对象。
为什么称它为二次解码器呢？我们知道，从SocketChannel读取到的TCP数据是ByteBuffer，实际就是字节数组，我们首先将ByteBuffer
缓冲区中的数据报读取出来，并将其解码为Java对象，然后对Java对象根据某些规则做二次解码，将其解码为一个POJO对象。
因为MessageToMessageDecoder在ByteToMessageDecoder之后，所有称之为二次解码器。
二次解码器在实际的商业项目中非常有用，以HTTP+XML协议栈为例，第一次解码往往是将字节数组解码成HttpRequest对象，然后对
HttpRequest消息中的消息体字符串进行二次解码，将XML格式的字符串解码为POJO对象，这就用到了二次解码器。
事实上，做一个超级复杂的解码器将多个解码器组合成一个大而全的MessageToMessageDecoder解码器似乎也能解决多次解码的问题，
但是采用这种方式的代码可维护性会非常差。例如，如果我们打算在HTTP+XML协议栈中增加一个打印码流的功能，即首次加码获取
HttpRequest对象之后打印XML格式的码流。如果采用多个解码器组合，在中间插入一个一个打印消息体的Handler即可，不需要修改
原来的代码；如果做一个大而全的解码器，就需要在解码的方法中增加打印码流的代码，可扩展性和可维护性都会变差。
用户的解码器只需要实现decode(ChannelHandlerContext ctx, I msg, List<Object> out)抽象方法即可，由于它是将一个
POJO对象解码为另一个POJO对象，所以一般不会涉及半包的处理。

(4) LengthFieldBasedFrameDecoder功能说明
它是一个通用的半包解码器。
如何区分一个整包消息，通常由如下4种方法：
1、固定长度，例如每120个字节代表一个整包消息，不足的前面补零。解码器在处理这类定长消息的时候比较简单，每次读到指定
长度的字节后再进行解码。
2、通过回车换行符区分消息，例如FTP协议。这类区分消息的方式多用于文本协议。
3、通过分隔符区分整包消息。
4、通过指定长度来标识整包消息。
如果消息是通过长度进行区分的，LengthFieldBasedFrameDecoder都可以自动处理粘包和半包问题，只需要传入正确的参数，
即可轻松搞定"读半包"问题。
关于它的使用方法，可以到源码中看它的注释，还有自定义协议案例中也有它的使用说明。

(5) MessageToByteEncoder功能说明
MessageToByteEncoder负责将POJO对象编码成ByteBuf，用户的编码器继承MessageToByteEncoder，实现
encode(ChannelHandlerContext ctx, I msg, ByteBuf out)方法。

(6) MessageToMessageEncoder功能说明
将一个POJO对象编码为另一个POJO对象，以HTTP+XML为例，它的一种实现方式是：先将POJO对象编码成XML字符串，再将字符串
编码为HTTP请求或者应答消息。对于复杂协议，往往需要经历多次编码，为了便于功能扩展，可以通过多个编码器组合来实现相关功能。
用户的编码器继承MessageToMessageEncoder，实现encode(ChannelHandlerContext ctx, I msg, List<Object> out)
方法即可。注意，它与MessageToByteEncoder的区别是输出的是对象列表而不是ByteBuf。

(7) LengthFieldPrepender功能说明
如果协议中的第一个字段为长度字段，Netty提供了LengthFieldPrepender编码器，它可以计算当前待发送消息的二进制字节长度，
将该长度添加到ByteBuf的缓冲区头中，
 * 编码前 (12 bytes)         编码后 (14 bytes)
 * +----------------+      +--------+----------------+
 * |"HELLO, WORLD"  | ---> | 0x000C | "HELLO, WORLD" |
 * +--------+-------+      +--------+----------------+
通过LengthFieldPrepender可以将待发送消息的长度写入到ByteBuf的前2个字节，编码后的消息组成为长度字段+原消息的方式。
通过设置LengthFieldPrepender为true，消息长度将包含长度本身占用的字节数。