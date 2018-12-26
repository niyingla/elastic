package com.pikaqiu.elastic.service;

import com.pikaqiu.elastic.entity.User;

import java.util.concurrent.Future;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-12-02 17:08
 **/
public interface IUserService {

    /**
     * 根据用户名查找用户
     * @param userName
     * @return
     */
    User findUserByName(String userName);


    /**
     * 异步测试类
     * @return
     */
    Future<String> testAsync();
}
