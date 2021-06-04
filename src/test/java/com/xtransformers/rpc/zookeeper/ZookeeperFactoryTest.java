package com.xtransformers.rpc.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ZookeeperFactoryTest {

    @Test
    public void test() {
        CuratorFramework client = ZookeeperFactory.create();
        assertNotNull(client);
        try {
            String result = client.create().forPath("/netty");
            System.out.println(result);
            assertEquals("/netty", result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}