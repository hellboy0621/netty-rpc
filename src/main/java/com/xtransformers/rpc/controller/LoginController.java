package com.xtransformers.rpc.controller;

import com.xtransformers.rpc.annotation.RemoteInvoke;
import com.xtransformers.rpc.service.UserService;
import org.springframework.stereotype.Component;

/**
 * @author daniel
 * @date 2021-06-05
 */
@Component
public class LoginController {

    @RemoteInvoke
    private UserService userService;

    public Object getUserByName(String userName) {
        return userService.getUserByName(userName);
    }
}
