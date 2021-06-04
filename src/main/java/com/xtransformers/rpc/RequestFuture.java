package com.xtransformers.rpc;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author daniel
 * @date 2021-05-30
 */
public class RequestFuture {

    public static Map<Long, RequestFuture> futures = new ConcurrentHashMap<>();

    public static final AtomicLong aid = new AtomicLong();

    private long id;
    private Object request;
    private Object result;
    private long timeout = 3000;
    private String path;

    public RequestFuture() {
        id = aid.getAndIncrement();
        addFuture(this);
    }

    /**
     * 把请求放入缓存
     *
     * @param future 请求
     */
    public static void addFuture(RequestFuture future) {
        futures.put(future.getId(), future);
    }

    /**
     * 同步获取响应结果
     *
     * @return 响应结果
     */
    public Object get() {
        synchronized (this) {
            while (this.result == null) {
                try {
                    this.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return this.result;
    }

    /**
     * 异步线程将结果返回主线程
     *
     * @param resp Response
     */
    public static void received(Response resp) {
        RequestFuture future = futures.remove(resp.getId());
        if (future != null) {
            // 回写结果
            future.setResult(resp.getResult());
        }

        // 通知主线程
        synchronized (Objects.requireNonNull(future)) {
            future.notify();
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
