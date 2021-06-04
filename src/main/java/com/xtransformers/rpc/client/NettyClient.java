package com.xtransformers.rpc.client;

import com.alibaba.fastjson.JSON;
import com.xtransformers.rpc.RequestFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;

/**
 * @author daniel
 * @date 2021-05-31
 */
public class NettyClient {
    public static EventLoopGroup group;
    public static Bootstrap bootstrap;
    private static ChannelFuture future;

    static {
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(group);
        // 设置内存分配器
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final ClientHandler handler = new ClientHandler();
        // 把 handler 加入管道中
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,
                        0, 4, 0, 4));
                // 把接收到的 ByteBuf 数据包转换成 String
                ch.pipeline().addLast(new StringDecoder());
                // 业务逻辑处理 handler
                ch.pipeline().addLast(handler);
                ch.pipeline().addLast(new LengthFieldPrepender(4, false));
                // 把字符串消息转换成 ByteBuf
                ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
            }
        });

        // 连接服务器
        try {
            future = bootstrap.connect("127.0.0.1", 8080).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Object sendRequest(Object msg, String path) {
        try {
            RequestFuture request = new RequestFuture();
            request.setRequest(msg);
            request.setPath(path);
            String requestStr = JSON.toJSONString(request);
            future.channel().writeAndFlush(requestStr);
            return request.get();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        NettyClient client = new NettyClient();
        for (int i = 0; i < 10; i++) {
            Object result = client.sendRequest("Hello:" + i, "getUserNameById");
            System.out.println(result);
        }
    }
}
