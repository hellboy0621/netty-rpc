package com.xtransformers.rpc.spring;

import com.xtransformers.rpc.Mediator;
import com.xtransformers.rpc.annotation.Remote;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Spring 容器初始化后，把带有 @Remote 注解的方法与其对象加载到缓存中
 * 需要在 Netty 服务器启动前初始化好
 *
 * @author daniel
 * @date 2021-06-02
 */
@Component
public class InitLoadRemoteMethod implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        // 从 Spring 容器中获取标有 @Remote 注解的对象
        Map<String, Object> controllerBeans = contextRefreshedEvent.getApplicationContext()
                .getBeansWithAnnotation(Remote.class);
        controllerBeans.forEach((key, bean) -> {
            Method[] methods = bean.getClass().getDeclaredMethods();
            for (Method method : methods) {
                String methodValue = bean.getClass().getInterfaces()[0].getName()
                        + "."
                        + method.getName();
                Mediator.MethodBean methodBean = new Mediator.MethodBean();
                methodBean.setBean(bean);
                methodBean.setMethod(method);
                // 把 接口名+方法名 作为 key
                Mediator.methodBeans.put(methodValue, methodBean);
            }
        });
    }

    /**
     * 值越小优先级越高
     *
     * @return 值
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
