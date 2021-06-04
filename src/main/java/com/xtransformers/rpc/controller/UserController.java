package com.xtransformers.rpc.controller;

import com.xtransformers.rpc.annotation.Remote;
import org.springframework.stereotype.Controller;

/**
 * @author daniel
 * @date 2021-06-03
 */
@Controller
public class UserController {

    @Remote("getUserNameById")
    public Object getUserNameById(String userId) {
        System.out.println("client request's user id : " + userId);
        return "response result : user Smith:" + userId;
    }
}
