package com.xtransformers.rpc.spring;

import com.xtransformers.rpc.server.NettyServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * @author daniel
 * @date 2021-06-02
 */
@Component
public class NettyApplicationListener implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        // 开启额外线程，启动 Netty 服务
        new Thread(NettyServer::start).start();
    }
}
