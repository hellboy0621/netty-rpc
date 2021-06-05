package com.xtransformers.rpc.client;

import com.alibaba.fastjson.JSONObject;
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

    // 开启一个线程组，线程组一定要注意静态化
    public static EventLoopGroup group = new NioEventLoopGroup();

    public static Bootstrap getBootStrap() {
        // 客户端启动辅助类
        Bootstrap bootstrap = new Bootstrap();
        // 设置 Socket 通道
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(group);
        // 设置内存分配器
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final ClientHandler handler = new ClientHandler();
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                ch.pipeline().addLast(new StringDecoder());
                ch.pipeline().addLast(handler);
                ch.pipeline().addLast(new LengthFieldPrepender(4, false));
                ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
            }
        });
        return bootstrap;
    }

    public Object sendRequest(Object msg, String path) throws Exception {
        try {
            RequestFuture request = new RequestFuture();
            request.setPath(path);
            request.setRequest(msg);
            // 转换成 JSON 并发送给编码器 StringEncoder
            // StringEncoder 编码器再发送给 LengthFieldPrepender 长度编码器
            // 最终写到 TCP 缓存中并传送给客户端
            String requestStr = JSONObject.toJSONString(request);
            ChannelFuture future = ChannelFutureManager.get();
            future.channel().writeAndFlush(requestStr);
            // 同步等待响应结果，只有当 promise 有值时才会继续向下执行
            return request.get();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static Object sendRequest(RequestFuture request) throws Exception {
        try {
            String requestStr = JSONObject.toJSONString(request);
            ChannelFuture future = ChannelFutureManager.get();
            future.channel().writeAndFlush(requestStr);
            return request.get();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        NettyClient client = new NettyClient();
        for (int i = 0; i < 10; i++) {
            Object result = client.sendRequest("Hello:" + i, "getUserNameById");
            System.out.println(result);
        }
    }
}
