# tcp socket的backlog参数
https://www.jianshu.com/p/e6f2036621f4
https://www.cnblogs.com/Orgliny/p/5780796.html

serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
.option(ChannelOption.SO_BACKLOG, 1024)

backlog参数主要用于底层方法int listen(int fd, int backlog)，它指定了内核为此套接口排队的最大连接个数，对于给定的监听
套接口，内核需要维护两个队列： syns queue和accept queue，根据TCP三次握手的过程来分隔这两个队列。
1、client发送SYN到server，将状态修改为SYN_SEND，如果server收到请求，则将状态修改为SYN_RCVD，并把该请求放到syns queue队列中。
2、server回复SYN+ACK给client，如果client收到请求，则将状态修改为ESTABLISHED，并发送ACK给server。
3、server收到ACK，将状态修改为ESTABLISHED，并把该请求从syns queue中放到accept queue。
syns queue
用于保存半连接状态的请求，其大小通过/proc/sys/net/ipv4/tcp_max_syn_backlog指定，一般默认值是512，
不过这个设置有效的前提是系统的syncookies功能被禁用。互联网常见的TCP SYN FLOOD恶意DOS攻击方式就是建立大量的半连接状态的请求，然后丢弃，
导致syns queue不能保存其它正常的请求。
accept queue
用于保存全连接状态的请求，其大小通过/proc/sys/net/core/somaxconn指定。
如果accept queue队列满了，server将发送一个ECONNREFUSED错误信息Connection refused到client。
backlog被规定为两个队列总和的最大值，大多数实现默认值为5，在高并发Web服务器中显然不够，Lighttpd中此值达到128x8。