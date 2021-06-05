package com.xtransformers.rpc.client;

import com.google.common.collect.Lists;
import com.xtransformers.rpc.zookeeper.ServerChangeWatcher;
import io.netty.channel.ChannelFuture;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author daniel
 * @date 2021-06-05
 */
public class ChannelFutureManager {

    // 服务器地址列表，由于 List 设计多线程读写
    // 主要是当 ZK 的监听器类监听到服务器列表发生变化时，会去修改，因此不能使用 ArrayList
    public static CopyOnWriteArrayList<String> serverList = Lists.newCopyOnWriteArrayList();

    // 与服务器构建连接的 ChannelFuture 列表页涉及多线程的读写
    public static CopyOnWriteArrayList<ChannelFuture> channelFutures = Lists.newCopyOnWriteArrayList();

    // 此属性记录每次在服务器列表中获取服务器时的下标
    public static AtomicInteger position = new AtomicInteger(0);

    /**
     * 若未获取到，则通过初始化 Zookeeper 注册的服务器列表再去获取
     *
     * @return ChannelFuture
     */
    public static ChannelFuture get() throws Exception {
        ChannelFuture channelFuture = get(position);
        if (channelFuture == null) {
            // 初始化 ZK 注册的服务器列表
            // 在应用刚刚启动时可能会被调用
            ServerChangeWatcher.initChannelFuture();
        }
        return get(position);
    }

    /**
     * 从 channelFutures 中获取 ChannelFuture
     *
     * @param position 下标
     * @return ChannelFuture
     */
    private static ChannelFuture get(AtomicInteger position) {
        int size = channelFutures.size();
        if (size == 0) {
            return null;
        }
        ChannelFuture channel = null;
        // 需要加锁，与 ServerChangeWatcher 类的 process() 方法中的锁相同
        // 在获取 channel 时，不能清空 channel 链表
        synchronized (position) {
            if (position.get() >= size) {
                position.set(0);
            } else {
                channel = channelFutures.get(position.getAndIncrement());
            }
            if (!channel.channel().isActive()) {
                channelFutures.remove(channel);
                return get(position);
            }
        }
        return channel;
    }

    public static void add(ChannelFuture channel) {
        channelFutures.add(channel);
    }

    public static void addAll(List<ChannelFuture> channels) {
        channelFutures.addAll(channels);
    }

    public static void clear() {
        for (ChannelFuture future : channelFutures) {
            future.channel().close();
        }
        channelFutures.clear();
    }
}
