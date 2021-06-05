package com.xtransformers.rpc.service.impl;

import com.xtransformers.rpc.annotation.Remote;
import com.xtransformers.rpc.service.UserService;

/**
 * @author daniel
 * @date 2021-06-05
 */
@Remote
public class UserServiceImpl implements UserService {
    @Override
    public Object getUserByName(String userName) {
        System.out.println("userName : " + userName);
        return "server response ok";
    }
}
