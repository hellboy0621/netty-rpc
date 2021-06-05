package com.xtransformers.rpc.zookeeper;

import com.xtransformers.rpc.Constants;
import com.xtransformers.rpc.server.NettyServer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 服务端与 ZK 连接监听类
 * 以防 Session 中断导致服务端注册到 ZK 的临时节点丢失
 *
 * @author daniel
 * @date 2021-06-03
 */
public class ServerWatcher implements CuratorWatcher {

    public static String serverKey = "";
    public static ServerWatcher serverWatcher = null;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ServerWatcher getInstance() {
        if (serverWatcher == null) {
            serverWatcher = new ServerWatcher();
        }
        return serverWatcher;
    }

    /**
     * 监听 ZK 路径的变化
     * 只要有服务器 session 断了就会触发
     * 当本机与 ZK 的 session 中断时，需要重新创建临时节点
     *
     * @param event
     * @throws Exception
     */
    @Override
    public void process(WatchedEvent event) throws Exception {
        String msg = " watch zk event :"
                + event.getState() + " "
                + DATE_TIME_FORMATTER.format(LocalDateTime.now());
        System.out.println(msg);

        // 当会话丢失时
        if (Watcher.Event.KeeperState.Disconnected.equals(event.getState())
                || Watcher.Event.KeeperState.Expired.equals(event.getState())) {
            try {
                try {
                    // 先尝试关闭旧连接
                    ZookeeperFactory.create().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                CuratorFramework client = ZookeeperFactory.recreate();
                client.getChildren().usingWatcher(this).forPath(NettyServer.SERVER_PATH);
                InetAddress netAddress = InetAddress.getLocalHost();
                Stat stat = client.checkExists().forPath(NettyServer.SERVER_PATH);
                if (stat == null) {
                    client.create().creatingParentsIfNeeded()
                            .withMode(CreateMode.PERSISTENT)
                            .forPath(NettyServer.SERVER_PATH, "0".getBytes());
                    // 构建临时节点 ip#port#weight#id
                    // 127.0.0.1#8080#1#0000000000
                    // 127.0.0.1#8080#1#0000000001
                    client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                            .forPath(NettyServer.SERVER_PATH + "/" + netAddress.getHostAddress() + "#"
                                    + Constants.port + "#" + Constants.weight + "#");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 其他事件发生时，只需设置监听即可
            CuratorFramework client = ZookeeperFactory.create();
            client.getChildren().usingWatcher(this)
                    .forPath(NettyServer.SERVER_PATH);
        }
    }
}
