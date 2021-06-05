package com.xtransformers.rpc.annotation;

import java.lang.annotation.*;

/**
 * 此注解用于远程调用的接口属性
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemoteInvoke {
}
