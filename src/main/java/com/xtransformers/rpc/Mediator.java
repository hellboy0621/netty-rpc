package com.xtransformers.rpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中介者
 * 对 Netty 网络通信与业务处理逻辑类起到沟通的关联作用
 *
 * @author daniel
 * @date 2021-06-02
 */
public class Mediator {

    public static Map<String, MethodBean> methodBeans;

    static {
        methodBeans = new HashMap<>();
    }

    /**
     * 请求分发处理
     *
     * @param requestFuture reequest
     * @return response
     */
    public static Response process(RequestFuture requestFuture) {
        Response response = new Response();
        try {
            String path = requestFuture.getPath();
            MethodBean methodBean = methodBeans.get(path);
            if (methodBean != null) {
                Object bean = methodBean.getBean();
                Method method = methodBean.getMethod();
                Object body = requestFuture.getRequest();
                // 只支持一个参数
                Class<?>[] paramTypes = method.getParameterTypes();
                Class<?> paramType = paramTypes[0];
                Object param = null;
                if (paramType.isAssignableFrom(List.class)) {
                    param = JSONArray.parseArray(JSONArray.toJSONString(body), paramType);
                } else if (paramType.getName().equals(String.class.getName())) {
                    param = body;
                } else {
                    // 采用 JSONObject 反序列化
                    param = JSONObject.parseObject(JSONObject.toJSONString(body), paramType);
                }
                Object result = method.invoke(bean, param);
                response.setResult(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.setId(requestFuture.getId());
        return response;
    }

    public static class MethodBean {
        private Object bean;
        private Method method;

        private Object getBean() {
            return bean;
        }

        public void setBean(Object bean) {
            this.bean = bean;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }
    }
}
