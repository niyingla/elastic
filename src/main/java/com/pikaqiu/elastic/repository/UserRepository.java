package com.pikaqiu.elastic.repository;

import com.pikaqiu.elastic.entity.User;
import org.springframework.data.repository.CrudRepository;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-11-21 23:56
 **/
public interface UserRepository extends CrudRepository<User, Long> {
    User findByName(String name);
}
