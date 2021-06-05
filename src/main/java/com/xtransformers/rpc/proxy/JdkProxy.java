package com.xtransformers.rpc.proxy;

import com.alibaba.fastjson.JSONObject;
import com.xtransformers.rpc.RequestFuture;
import com.xtransformers.rpc.annotation.RemoteInvoke;
import com.xtransformers.rpc.client.NettyClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author daniel
 * @date 2021-06-05
 */
//@Component
public class JdkProxy implements InvocationHandler, BeanPostProcessor {

    private Field target;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 采用 Netty 客户端调用服务端
        RequestFuture request = new RequestFuture();
        // 接口类名+方法名组装成 path
        request.setPath(target.getType().getName() + "." + method.getName());
        // 设置参数
        request.setRequest(args[0]);
        // 远程调用
        Object resp = NettyClient.sendRequest(request);
        Class returnType = method.getReturnType();
        if (resp == null) {
            return null;
        }
        // 对返回结果进行反序列化操作
        resp = JSONObject.parseObject(JSONObject.toJSONString(resp), returnType);
        return resp;
    }

    private Object getJDKProxy(Field field) {
        this.target = field;
        // JDK 动态代理只能针对接口进行代理
        return Proxy.newProxyInstance(field.getType().getClassLoader(),
                new Class[]{field.getType()}, this);
    }

    /**
     * 在所有 Bean 初始化完成前，为包含有 @RemoteInvoke 注解的属性重新赋值
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(RemoteInvoke.class)) {
                field.setAccessible(true);
                try {
                    field.set(bean, getJDKProxy(field));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
