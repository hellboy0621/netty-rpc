package com.xtransformers.rpc.server;

import com.xtransformers.rpc.Constants;
import com.xtransformers.rpc.zookeeper.ServerWatcher;
import com.xtransformers.rpc.zookeeper.ZookeeperFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author daniel
 * @date 2021-05-30
 */
public class NettyServer {

    public static final String SERVER_PATH = "/netty";

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        /**
         * Boss 线程组 启动一条线程，用来监听 OP_ACCEPT 事件
         * Worker 线程组默认启动 CPU * 2 的线程，用来监听客户端连接的 OP_READ 和 OP_WRITE 事件，并处理 I/O 事件
         */
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 服务启动辅助类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup);
            // TCP NioServerSocketChannel
            // UDP DatagramChannel
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 128)
                    // 客户端链路注册读写事件时，初始化 handler，并加入管道
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            // 把接收到的 ByteBuf 数据包转换成 String
                            socketChannel.pipeline().addLast(new StringDecoder());
                            /**
                             * 向 Worker 线程组的管道双向链表中添加处理类 ServerHandler
                             * 处理流向
                             * HeadContext - channelRead 读取数据
                             * ServerHandler - channelRead 读取数据做业务逻辑判断
                             * 将结果传递给客户端
                             * TailContext - write
                             * HeadContext - write
                             *
                             * 解码器执行顺序从上往下
                             * 编码期执行顺序从下往上
                             */
                            socketChannel.pipeline().addLast(new ServerHandler());
                            socketChannel.pipeline().addLast(new LengthFieldPrepender(4, false));
                            socketChannel.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
                        }
                    });
            // 同步绑定端口
            int port = 8080;
            ChannelFuture future = serverBootstrap.bind(port).sync();

            // 连接 zk
            CuratorFramework client = ZookeeperFactory.create();
            // 获取当前服务器 IP
            InetAddress netAddress = InetAddress.getLocalHost();
            int weight = 1;
            // 先判断 SERVER_PATH 路径是否存在，若不存在则需要创建
            Stat stat = client.checkExists().forPath(SERVER_PATH);
            if (stat == null) {
                client.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(SERVER_PATH, "0".getBytes());
            }
            // 构建临时节点 127.0.0.1#8080#1#00000
            client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(SERVER_PATH + "/" + netAddress.getHostAddress() + "#"
                            + port + "#" + weight + "#");
            // 在服务端加上 zk 的监听，以防 Session 中断导致临时节点丢失
            ServerWatcher.serverKey = netAddress.getHostAddress() + port
                    + Constants.weight;
            client.getChildren().usingWatcher(ServerWatcher.getInstance())
                    .forPath(SERVER_PATH);

            // 阻塞主线程，直到 Socket 通道被关闭
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
