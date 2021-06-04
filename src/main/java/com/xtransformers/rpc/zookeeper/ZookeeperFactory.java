package com.xtransformers.rpc.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @author daniel
 * @date 2021-06-03
 */
public class ZookeeperFactory {

    public static CuratorFramework client;

    public static CuratorFramework create() {
        if (client == null) {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
            client = CuratorFrameworkFactory.newClient("localhost:2181",
                    1000, 5000, retryPolicy);
            client.start();
        }
        return client;
    }

    // 重新创建连接，主要是为了防止会话丢失时需要重新创建
    public static CuratorFramework recreate() {
        client = null;
        create();
        return client;
    }
}
