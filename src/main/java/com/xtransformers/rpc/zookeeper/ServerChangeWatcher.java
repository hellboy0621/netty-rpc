package com.xtransformers.rpc.zookeeper;

import com.google.common.collect.Lists;
import com.xtransformers.rpc.Constants;
import com.xtransformers.rpc.client.ChannelFutureManager;
import com.xtransformers.rpc.client.NettyClient;
import com.xtransformers.rpc.server.NettyServer;
import io.netty.channel.ChannelFuture;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.List;

/**
 * @author daniel
 * @date 2021-06-04
 */
public class ServerChangeWatcher implements CuratorWatcher {

    public static ServerChangeWatcher serverChangeWatcher = null;

    // server_count * 服务端权重 = 连接数
    public static final int SERVER_COUNT = 100;

    public static ServerChangeWatcher getInstance() {
        if (serverChangeWatcher == null) {
            serverChangeWatcher = new ServerChangeWatcher();
        }
        return serverChangeWatcher;
    }

    /**
     * 监听 ZK 路径的变化
     * 只要有事件发生，就需要重新获取所有服务器列表并更新连接列表
     *
     * @param event event
     * @throws Exception exception
     */
    @Override
    public void process(WatchedEvent event) throws Exception {
        // 连接中断，需要重连
        if (Watcher.Event.KeeperState.Disconnected.equals(event.getState())
                || Watcher.Event.KeeperState.Expired.equals(event.getState())) {
            CuratorFramework client = ZookeeperFactory.recreate();
            client.getChildren().usingWatcher(this).forPath(NettyServer.SERVER_PATH);
            return;
        } else if (Watcher.Event.KeeperState.SyncConnected.equals(event.getState())
                && !Watcher.Event.EventType.NodeChildrenChanged.equals(event)) {
            CuratorFramework client = ZookeeperFactory.create();
            client.getChildren().usingWatcher(this).forPath(NettyServer.SERVER_PATH);
            return;
        }
        System.out.println("reinit server connection process");
        CuratorFramework client = ZookeeperFactory.create();
        // 每次只能监听一次，因此需要重复加入
        String path = NettyServer.SERVER_PATH;
        client.getChildren().usingWatcher(this).forPath(path);
        List<String> serverPaths = client.getChildren().forPath(path);
        List<String> servers = Lists.newArrayList();
        for (String serverPath : serverPaths) {
            // ip#port#weight#id
            String[] str = serverPath.split("#");
            int weight = Integer.parseInt(str[2]);
            if (weight > 0) {
                for (int i = 0; i < weight * SERVER_COUNT; i++) {
                    servers.add(str[0] + "#" + str[1]);
                }
            }
        }
        ChannelFutureManager.serverList.clear();
        ChannelFutureManager.serverList.addAll(servers);
        // 根据服务器地址和 IP 构建连接，并交给 ChannelFuture 保存
        List<ChannelFuture> futures = Lists.newArrayList();
        for (String realServer : ChannelFutureManager.serverList) {
            String[] str = realServer.split("#");
            try {
                // 此处 NettyClient 的 bootstrap 不能静态化
                ChannelFuture channelFuture = NettyClient.getBootStrap()
                        .connect(str[0], Integer.parseInt(str[1])).sync();
                futures.add(channelFuture);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 加锁，防止获取不到 ChannelFuture
        synchronized (ChannelFutureManager.position) {
            // 先清空 ChannelFuture 列表
            ChannelFutureManager.clear();
            ChannelFutureManager.addAll(futures);
        }
    }

    /**
     * 初始化服务器连接列表
     */
    public static void initChannelFuture() throws Exception {
        CuratorFramework client = ZookeeperFactory.create();
        List<String> servers = client.getChildren().forPath(Constants.SERVER_PATH);
        System.out.println("init server connection");
        for (String server : servers) {
            String[] split = server.split("#");
            try {
                int weight = Integer.parseInt(split[2]);
                if (weight > 0) {
                    for (int i = 0; i < weight * SERVER_COUNT; i++) {
                        ChannelFuture channelFuture = NettyClient.getBootStrap()
                                .connect(split[0], Integer.parseInt(split[1])).sync();
                        ChannelFutureManager.add(channelFuture);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 初始化后，加上监听
        client.getChildren().usingWatcher(getInstance())
                .forPath(Constants.SERVER_PATH);
    }
}
