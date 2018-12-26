package com.pikaqiu.elastic.service.impl;

import com.pikaqiu.elastic.entity.Role;
import com.pikaqiu.elastic.entity.User;
import com.pikaqiu.elastic.repository.RoleRepository;
import com.pikaqiu.elastic.repository.UserRepository;
import com.pikaqiu.elastic.service.IUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-12-02 17:09
 **/
@Service
public class userServiceImpl implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public User findUserByName(String userName) {

        if (StringUtils.isBlank(userName)) {
            return null;
        }
        User user = userRepository.findByName(userName);

        if (user == null) {
            throw new DisabledException("用户不存在");
        }

        //拿到所有权限
        List<Role> roles = roleRepository.findRolesByUserId(user.getId());

        if (CollectionUtils.isEmpty(roles)) {
            throw new DisabledException("权限非法");
        }

        List<GrantedAuthority> list = new ArrayList<>();

        roles.forEach(role -> {
            list.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
        });

        //设置权限
        user.setAuthorityList(list);

        return user;
    }


    @Override
    @Async
    public Future<String> testAsync() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new AsyncResult<>("hello world !!!!");
    }

}
