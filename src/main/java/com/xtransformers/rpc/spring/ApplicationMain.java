package com.xtransformers.rpc.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author daniel
 * @date 2021-06-02
 */
public class ApplicationMain {

    private static volatile boolean running = true;

    public static void main(String[] args) {
        try {
            // 此处扫描包不包含代理类的包
            // 只包含 service、监听器的包
            AnnotationConfigApplicationContext context =
                    new AnnotationConfigApplicationContext(
                            "com.xtransformers.rpc.controller",
                            "com.xtransformers.rpc.spring",
                            "com.xtransformers.rpc.service");

            // 增加关闭 JVM 的钩子
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        context.stop();
                    } catch (Exception e) {
                    }
                    synchronized (ApplicationMain.class) {
                        running = false;
                        ApplicationMain.class.notify();
                    }
                }
            });
            context.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("server is started.");
        synchronized (ApplicationMain.class) {
            while (running) {
                try {
                    ApplicationMain.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
