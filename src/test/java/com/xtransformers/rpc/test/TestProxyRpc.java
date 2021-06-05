package com.xtransformers.rpc.test;

import com.xtransformers.rpc.controller.LoginController;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author daniel
 * @date 2021-06-05
 */
public class TestProxyRpc {

    @Test
    public void test() {
        // 不能包含服务器的启动类所在的包
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(
                        "com.xtransformers.rpc.controller",
                        "com.xtransformers.rpc.proxy");
        LoginController loginController = context.getBean(LoginController.class);
        Object result = loginController.getUserByName("Smith");
        System.out.println(result);
        Assert.assertEquals("server response ok", result);
    }
}
