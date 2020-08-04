# ChannelFuture和Promise解析

   在Netty中，所有的IO操作都是异步的，这意味着任何IO调用都会立即返回，而不是像传统IO那样等待操作完成。异步操作会带来一个问题：
调用者如何获取异步操作的结果？ChannelFuture就是为了解决这个问题而专门设计的。
ChannelFuture有两种状态：uncompleted和completed。当开始一个IO操作时，一个新的ChannelFuture被创建，此时它处于uncompleted
状态-非失败、非成功、非取消 ，因为IO操作此时还没有完成。一旦IO操作完成，ChannelFuture的状态将会被设置成completed，它的结果
有如下三种可能：
1、操作成功；
2、操作失败；
3、操作被取消。
    ChannelFuture提供了一系列新的API，用于获取操作结果、添加事件监听器、取消IO操作、同步等待等。
    Netty强烈建议直接通过添加监听器的方式获取IO操作结果，或者进行后续的相关操作。ChannelFuture可以同时增加一个或者多个
GenericFutureListener，也可以用remove方法删除GenericFutureListener。
   当IO操作完成之后，IO线程会回调ChannelFuture中GenericFutureListener的operationComplete方法，并把ChannelFuture对象
作为方法的入参。如果用户需要做上下文相关的操作，需要将上下文信息保存到对用的ChannelFuture中。
推荐通过GenericFutureListener代替ChannelFuture的get等方法的原因是：当我们进行异步IO操作时，完成的时间是无法预测的，如果
不设置超时时间，它会导致调用线程长时间被阻塞，甚至挂死。而设置超时时间，时间又无法精确预测。利用异步通知机制回调GenericFutureListener
是最佳的解决方案，它的性能最优。

添加listener的例子：
ChannelFuture future = ctx.channel().writeAndFlush(response);
        // 如果非Keep-Alive，关闭连接
        if (!HttpUtil.isKeepAlive(request) || response.status().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

   需要注意的是：不要在ChannelHandler中调用ChannelFuture的await()方法，这会导致死锁。原因是发起IO操作之后，由IO线程负责
异步通知 发起IO操作的用户线程，如果IO线程和用户线程是同一个线程，就会导致IO线程等待自己通知操作完成，就这导致了死锁，这跟
经典的两个线程互相等待死锁不同，属于自己把自己挂死。
   异步IO操作有两类超时：一个是TCP层面的IO超时，另一个是业务逻辑层面的操作超时。两者没有必然的联系，但是通常情况下业务逻辑超时
时间应该大于IO超时时间。
IO超时时间配置：
Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)

ChannelFuture超时时间配置：
ChannelFuture channelFuture = bootstrap.connect(uri.getHost(), port);
            channelFuture.awaitUninterruptibly(10, TimeUnit.SECONDS);
需要指出的是：ChannelFuture超时并不代表IO超时，这就意味着ChannelFuture超时后，如果没有关闭连接资源，随后连接依旧可能会
成功，这会导致严重的问题。所以通常情况下，必须考虑究竟是设置IO超时还是ChannelFuture超时。


Promise功能介绍
Promise是可写的Future，Future自身并没有写操作相关的接口，Netty通过Promise对Future进行扩展，用于设置IO操作的结果。
Netty发起IO操作的时候，会创建一个新的Promise对象，例如调用ChannelHandlerContext的write(Object msg)方法时，会创建
一个新的ChannelPromise：
    @Override
    public ChannelFuture write(Object msg) {
        return write(msg, newPromise());
    }
当IO操作发生异常或者完成时，设置Promise的结果。