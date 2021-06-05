package com.xtransformers.rpc.proxy;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.xtransformers.rpc.RequestFuture;
import com.xtransformers.rpc.annotation.RemoteInvoke;
import com.xtransformers.rpc.client.NettyClient;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author daniel
 * @date 2021-06-05
 */
@Component
public class CglibProxy implements BeanPostProcessor {

    /**
     * 在所有 Bean 初始化完成之前
     * 为 Bean 中包含有 @RemoteInvoke 注解的属性重新赋值
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
                // 此处需要把属性和属性对应的方法放入 methodClassMap 对象中
                // 方便 callBack 中的 intercept() 方法使用
                final Map<Method, Class> methodClassMap = Maps.newHashMap();
                putMethodClass(methodClassMap, field);
                // 动态创建给定类的子类，能拦截代理类的所有方法
                Enhancer enhancer = new Enhancer();
                enhancer.setInterfaces(new Class[]{field.getType()});
                // 设置回调方法
                enhancer.setCallback(new MethodInterceptor() {
                    /**
                     * 拦截代理类所有方法
                     * @param instance
                     * @param method
                     * @param args
                     * @param proxy
                     * @return
                     * @throws Throwable
                     */
                    @Override
                    public Object intercept(Object instance, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                        // 采用 Netty 客户端调用服务器
                        RequestFuture request = new RequestFuture();
                        request.setPath(methodClassMap.get(method).getName()
                                + "." + method.getName());
                        request.setRequest(args[0]);
                        // 远程调用
                        Object resp = NettyClient.sendRequest(request);
                        Class<?> returnType = method.getReturnType();
                        if (resp == null) {
                            return null;
                        }
                        resp = JSONObject.parseObject(JSONObject.toJSONString(resp), returnType);
                        return resp;
                    }
                });

                try {
                    // 为包含 @RemoteInvoke 注解的属性重新赋值
                    field.set(bean, enhancer.create());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }

    private void putMethodClass(Map<Method, Class> methodClassMap, Field field) {
        Method[] methods = field.getType().getDeclaredMethods();
        for (Method method : methods) {
            methodClassMap.put(method, field.getType());
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
